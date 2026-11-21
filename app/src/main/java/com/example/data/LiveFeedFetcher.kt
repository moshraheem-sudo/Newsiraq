package com.example.data

import android.util.Xml
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32

data class LiveFetchResult(
  val articles: List<NewsArticle>,
  val sourcesSucceeded: Int,
  val fetchedAtIso: String,
)

object LiveFeedFetcher {
  private val liveClient =
    OkHttpClient.Builder()
      .connectTimeout(7, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()

  private val noCache = CacheControl.Builder().noCache().noStore().build()

  private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

  private val multiSpaceRegex = Regex("[ \\t\\r\\u000C\\u000B]+")
  private val multiNewlineRegex = Regex("\\n{3,}")
  private val videoExtRegex = Regex("\\.(mp4|m3u8|webm|mov)(\\?|$)", RegexOption.IGNORE_CASE)
  private val imageExtRegex = Regex("\\.(jpg|jpeg|png|webp|gif)(\\?|$)", RegexOption.IGNORE_CASE)
  private val imgSrcRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
  private val videoSrcRegex = Regex("""<(?:video|source)[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
  private val dataPostRegex = Regex("""data-post=["']([^"']+)["']""")
  private val timeAttrRegex = Regex("""<time[^>]+datetime=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
  private val msgTextRegex =
    Regex("""<div class="tgme_widget_message_text[^"]*js-message_text[^"]*"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL))
  private val msgAnyTextRegex =
    Regex("""<div class="tgme_widget_message_text[^"]*"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL))
  private val photoWrapRegex =
    Regex("""tgme_widget_message_photo_wrap[^>]*style=["'][^"']*url\(['"]?([^'")]+)['"]?\)""", RegexOption.IGNORE_CASE)
  private val videoThumbRegex =
    Regex("""tgme_widget_message_(?:round)?video_thumb[^>]*style=["'][^"']*url\(['"]?([^'")]+)['"]?\)""", RegexOption.IGNORE_CASE)
  private val hrefRegex = Regex("""href=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
  private val linkPreviewImgRegex =
    Regex("""link_preview_image[^>]*style=["'][^"']*url\(['"]?([^'")]+)['"]?\)""", RegexOption.IGNORE_CASE)
  private val msgInfoRegex =
    Regex("""<div class="tgme_widget_message_info[^"]*"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL))
  private val bareAmpersandRegex =
    Regex("""&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)""")
  private val nextDataRegex =
    Regex("""<script\s+id=["']__NEXT_DATA__["']\s+type=["']application/json["']>(.*?)</script>""", setOf(RegexOption.DOT_MATCHES_ALL))
  private val digitsRegex = Regex("""(\d{6,})""")

  private val rfcDatePatterns =
    listOf(
      "EEE, dd MMM yyyy HH:mm:ss Z",
      "EEE, dd MMM yyyy HH:mm:ss z",
      "EEE, d MMM yyyy HH:mm:ss Z",
      "EEE, d MMM yyyy HH:mm:ss z",
      "EEE, dd MMM yyyy HH:mm Z",
      "EEE, dd MMM yyyy HH:mm z",
      "dd MMM yyyy HH:mm:ss Z",
      "EEE MMM dd HH:mm:ss Z yyyy",
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "yyyy-MM-dd HH:mm:ss Z",
      "yyyy-MM-dd HH:mm:ss",
    )

  fun nowUtcIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
  }

  fun parseSourcesConfig(rawJson: String): List<NewsSourceConfig> {
    return runCatching {
      val root = JSONObject(rawJson)
      val arr = root.optJSONArray("sources") ?: JSONArray()
      List(arr.length()) { idx ->
        val obj = arr.getJSONObject(idx)
        NewsSourceConfig(
          id = obj.optString("id"),
          type = obj.optString("type", "rss"),
          name = obj.optString("name"),
          username = obj.optString("username"),
          url = obj.optString("url"),
          category = obj.optString("category"),
          maxPosts = obj.optInt("max_posts", 35),
          enabled = obj.optBoolean("enabled", true),
        )
      }.filter { it.enabled && it.url.startsWith("http") }
    }.getOrDefault(emptyList())
  }

  suspend fun fetchAllLive(sources: List<NewsSourceConfig>): LiveFetchResult = coroutineScope {
    val fetchedAt = nowUtcIso()
    if (sources.isEmpty()) {
      return@coroutineScope LiveFetchResult(emptyList(), 0, fetchedAt)
    }

    val semaphore = Semaphore(8)
    val perSourceResults =
      sources.map { source ->
        async {
          semaphore.withPermit {
            runCatching { fetchSingleSource(source, fetchedAt) }.getOrNull()
          }
        }
      }.awaitAll()

    val validLists = perSourceResults.filterNotNull()
    val allArticles = validLists.flatten()
    LiveFetchResult(
      articles = allArticles,
      sourcesSucceeded = validLists.size,
      fetchedAtIso = fetchedAt,
    )
  }

  private fun fetchSingleSource(source: NewsSourceConfig, fetchedAt: String): List<NewsArticle>? {
    return when (source.type) {
      "rss" -> {
        val body = httpGet(source.url) ?: return null
        parseRssXml(body, source, fetchedAt)
      }
      "telegram_web" -> {
        val body = httpGet(source.url) ?: return null
        parseTelegramHtml(body, source, fetchedAt)
      }
      "x_list" -> {
        val match = digitsRegex.find(source.url) ?: digitsRegex.find(source.username) ?: return null
        val listId = match.groupValues[1]
        val body = httpGet("https://syndication.twitter.com/srv/timeline-list/list-id/$listId") ?: return null
        parseXListHtml(body, source, fetchedAt)
      }
      else -> null
    }
  }

  private fun httpGet(url: String): String? {
    val request =
      Request.Builder()
        .url(url)
        .cacheControl(noCache)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "*/*")
        .build()
    return liveClient.newCall(request).execute().use { resp ->
      if (!resp.isSuccessful) return null
      resp.body?.string()?.takeIf { it.isNotBlank() }
    }
  }

  private fun parseRssXml(rawXml: String, source: NewsSourceConfig, fetchedAt: String): List<NewsArticle> {
    return try {
      parseRssXmlInternal(rawXml, source, fetchedAt)
    } catch (_: Exception) {
      val sanitized = bareAmpersandRegex.replace(rawXml, "&amp;")
      parseRssXmlInternal(sanitized, source, fetchedAt)
    }
  }

  private fun parseRssXmlInternal(rawXml: String, source: NewsSourceConfig, fetchedAt: String): List<NewsArticle> {
    val cleanedXml = rawXml.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(StringReader(cleanedXml))

    val articles = mutableListOf<NewsArticle>()
    val seenIds = mutableSetOf<Long>()
    val limit = source.maxPosts.coerceIn(10, 50)

    var inItem = false
    var currentTag = ""
    var title = ""
    var link = ""
    var guid = ""
    var descHtml = ""
    var contentEncodedHtml = ""
    var summaryHtml = ""
    var pubRaw = ""
    var activePubTag = ""
    val images = mutableListOf<String>()
    val seenImages = mutableSetOf<String>()
    val videos = mutableListOf<MediaAttachment>()
    val seenVideos = mutableSetOf<String>()
    val mediaImageCandidates = mutableListOf<Pair<Int, String>>()

    fun addImage(rawUrl: String) {
      val u = normalizeUrl(rawUrl)
      if (u.startsWith("http") && seenImages.add(u)) {
        images.add(u)
      }
    }

    fun addVideo(rawUrl: String, thumb: String = "") {
      val u = normalizeUrl(rawUrl)
      if (u.startsWith("http") && seenVideos.add(u)) {
        videos.add(MediaAttachment(type = "video", url = u, thumbnail = normalizeUrl(thumb), title = ""))
      }
    }

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          val tag = parser.name?.lowercase(Locale.US) ?: ""
          if (tag == "item" || tag == "entry") {
            inItem = true
            currentTag = ""
            title = ""
            link = ""
            guid = ""
            descHtml = ""
            contentEncodedHtml = ""
            summaryHtml = ""
            pubRaw = ""
            activePubTag = ""
            images.clear()
            seenImages.clear()
            videos.clear()
            seenVideos.clear()
            mediaImageCandidates.clear()
          } else if (inItem) {
            currentTag = tag
            when (tag) {
              "link" -> {
                val href = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                if (href.startsWith("http") && link.isBlank()) {
                  link = href
                }
              }
              "enclosure" -> {
                val encUrl = parser.getAttributeValue(null, "url")?.trim().orEmpty()
                val encType = parser.getAttributeValue(null, "type")?.lowercase(Locale.US).orEmpty()
                if (encUrl.isNotBlank()) {
                  if (encType.startsWith("video/") || videoExtRegex.containsMatchIn(encUrl)) {
                    addVideo(encUrl)
                  } else if (encType.startsWith("image/") || imageExtRegex.containsMatchIn(encUrl) || encType.isBlank()) {
                    addImage(encUrl)
                  }
                }
              }
              "media:content", "media:thumbnail" -> {
                val mUrl = parser.getAttributeValue(null, "url")?.trim().orEmpty()
                val mType = parser.getAttributeValue(null, "type")?.lowercase(Locale.US).orEmpty()
                val mMedium = parser.getAttributeValue(null, "medium")?.lowercase(Locale.US).orEmpty()
                val wStr = parser.getAttributeValue(null, "width")?.trim().orEmpty()
                val width = wStr.toIntOrNull() ?: 0
                if (mUrl.isNotBlank()) {
                  if (mType.startsWith("video/") || mMedium == "video" || videoExtRegex.containsMatchIn(mUrl)) {
                    addVideo(mUrl)
                  } else {
                    mediaImageCandidates.add(width to mUrl)
                  }
                }
              }
            }
          }
        }

        XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
          if (inItem) {
            val txt = parser.text.orEmpty()
            when (currentTag) {
              "title" -> title += txt
              "link" -> if (link.isBlank() || !link.startsWith("http")) link += txt
              "guid", "id" -> guid += txt
              "content:encoded" -> contentEncodedHtml += txt
              "description", "content" -> descHtml += txt
              "summary" -> summaryHtml += txt
              "pubdate", "dc:date", "published", "updated", "date" -> {
                if (activePubTag.isEmpty() || activePubTag == currentTag) {
                  activePubTag = currentTag
                  pubRaw += txt
                }
              }
              "enclosure" -> {
                if (txt.contains("<img", ignoreCase = true)) {
                  imgSrcRegex.findAll(txt).forEach { addImage(it.groupValues[1]) }
                }
              }
            }
          }
        }

        XmlPullParser.END_TAG -> {
          val tag = parser.name?.lowercase(Locale.US) ?: ""
          if (tag == currentTag) {
            currentTag = ""
          }
          if ((tag == "item" || tag == "entry") && inItem) {
            inItem = false
            val cleanTitle = cleanPlainText(stripHtml(title))
            val cleanLink = link.trim()
            val cleanGuid = guid.trim()
            val uniqueKey = cleanGuid.ifBlank { cleanLink }.ifBlank { cleanTitle }

            if (uniqueKey.isNotBlank()) {
              val msgId = stableCrc32Id(uniqueKey)
              if (seenIds.add(msgId)) {
                val chosenHtml = contentEncodedHtml.ifBlank { descHtml }.ifBlank { summaryHtml }
                imgSrcRegex.findAll(chosenHtml).forEach { addImage(it.groupValues[1]) }
                videoSrcRegex.findAll(chosenHtml).forEach { addVideo(it.groupValues[1]) }

                mediaImageCandidates.sortedByDescending { it.first }.take(2).forEach { (_, u) ->
                  addImage(u)
                }

                val primaryThumb = images.firstOrNull().orEmpty()
                val finalizedMedia =
                  buildList {
                    videos.forEach { v ->
                      add(
                        if (v.thumbnail.isBlank() && primaryThumb.isNotBlank()) {
                          v.copy(thumbnail = primaryThumb)
                        } else {
                          v
                        }
                      )
                    }
                    images.forEach { im ->
                      add(MediaAttachment(type = "photo", url = im, thumbnail = im, title = ""))
                    }
                  }

                val cleanBody = cleanPlainText(stripHtml(chosenHtml))
                val finalTitle = cleanTitle.ifBlank { deriveTitle(cleanBody) }.ifBlank { source.name }
                val finalBody = cleanBody.ifBlank { finalTitle }
                val pubIso = normalizeDateToUtcIso(pubRaw.trim())

                articles.add(
                  NewsArticle(
                    id = "${source.username}_$msgId",
                    sourceId = source.id,
                    sourceName = source.name,
                    sourceUsername = source.username,
                    messageId = msgId,
                    category = source.category,
                    title = finalTitle,
                    content = finalBody,
                    publishedAt = pubIso,
                    fetchedAt = fetchedAt,
                    updatedAt = "",
                    originalUrl = cleanLink.ifBlank { source.url },
                    images = images.toList(),
                    media = finalizedMedia,
                    links = if (cleanLink.startsWith("http")) listOf(cleanLink) else emptyList(),
                    isEdited = false,
                  )
                )
              }
            }
            if (articles.size >= limit) break
          }
        }
      }
      eventType = parser.next()
    }

    return articles
  }

  private fun parseTelegramHtml(html: String, source: NewsSourceConfig, fetchedAt: String): List<NewsArticle> {
    if (!html.contains("tgme_widget_message")) return emptyList()
    val chunks = html.split("""<div class="tgme_widget_message_wrap""")
    if (chunks.size <= 1) return emptyList()

    val parsed = mutableListOf<NewsArticle>()
    for (chunk in chunks.drop(1)) {
      val dataPost = dataPostRegex.find(chunk)?.groupValues?.getOrNull(1)?.trim().orEmpty()
      if (!dataPost.contains("/")) continue
      val channel = dataPost.substringBeforeLast("/")
      val msgId = dataPost.substringAfterLast("/").toLongOrNull() ?: continue

      val dateRaw = timeAttrRegex.find(chunk)?.groupValues?.getOrNull(1)?.trim().orEmpty()
      val publishedAt = normalizeDateToUtcIso(dateRaw)

      val rawMsgHtml =
        (msgTextRegex.findAll(chunk).lastOrNull() ?: msgAnyTextRegex.findAll(chunk).lastOrNull())
          ?.groupValues?.getOrNull(1)
          .orEmpty()
      val content = cleanPlainText(stripHtml(rawMsgHtml))
      val links = hrefRegex.findAll(rawMsgHtml).map { it.groupValues[1] }.distinct().toList()

      val images = mutableListOf<String>()
      val seenImages = mutableSetOf<String>()
      val mediaList = mutableListOf<MediaAttachment>()

      photoWrapRegex.findAll(chunk).forEach { match ->
        val img = normalizeUrl(match.groupValues[1])
        if (img.startsWith("http") && seenImages.add(img)) {
          images.add(img)
          mediaList.add(MediaAttachment(type = "photo", url = img, thumbnail = img, title = ""))
        }
      }

      val thumbUrl =
        videoThumbRegex.find(chunk)?.groupValues?.getOrNull(1)?.let { normalizeUrl(it) }.orEmpty()
      val seenVids = mutableSetOf<String>()
      videoSrcRegex.findAll(chunk).forEach { match ->
        val vid = normalizeUrl(match.groupValues[1])
        if (vid.startsWith("http") && seenVids.add(vid)) {
          mediaList.add(MediaAttachment(type = "video", url = vid, thumbnail = thumbUrl, title = ""))
          if (thumbUrl.startsWith("http") && seenImages.add(thumbUrl)) {
            images.add(thumbUrl)
          }
        }
      }

      if (images.isEmpty()) {
        linkPreviewImgRegex.find(chunk)?.groupValues?.getOrNull(1)?.let { rawPreview ->
          val img = normalizeUrl(rawPreview)
          if (img.startsWith("http") && seenImages.add(img)) {
            images.add(img)
          }
        }
      }

      if (content.isBlank() && images.isEmpty() && mediaList.isEmpty()) continue

      val infoHtml = msgInfoRegex.find(chunk)?.groupValues?.getOrNull(1).orEmpty()
      val isEdited =
        infoHtml.contains("edited", ignoreCase = true) ||
          chunk.contains(">edited<", ignoreCase = true)
      val title = deriveTitle(content).ifBlank { source.name }
      val resolvedChannel = channel.ifBlank { source.username }

      parsed.add(
        NewsArticle(
          id = "${source.username}_$msgId",
          sourceId = source.id,
          sourceName = source.name,
          sourceUsername = source.username,
          messageId = msgId,
          category = source.category,
          title = title,
          content = content,
          publishedAt = publishedAt,
          fetchedAt = fetchedAt,
          updatedAt = "",
          originalUrl = "https://t.me/$resolvedChannel/$msgId",
          images = images,
          media = mediaList,
          links = links,
          isEdited = isEdited,
        )
      )
    }

    return parsed.takeLast(source.maxPosts.coerceIn(10, 50))
  }

  private fun parseXListHtml(html: String, source: NewsSourceConfig, fetchedAt: String): List<NewsArticle> {
    val jsonStr = nextDataRegex.find(html)?.groupValues?.getOrNull(1) ?: return emptyList()
    val entries =
      runCatching {
        JSONObject(jsonStr)
          .optJSONObject("props")
          ?.optJSONObject("pageProps")
          ?.optJSONObject("timeline")
          ?.optJSONArray("entries")
      }.getOrNull() ?: return emptyList()

    val parsed = mutableListOf<NewsArticle>()
    val limit = source.maxPosts.coerceIn(20, 100)

    for (i in 0 until entries.length()) {
      val tweet = entries.optJSONObject(i)?.optJSONObject("content")?.optJSONObject("tweet") ?: continue
      val idStr = tweet.optString("id_str").trim()
      val msgId = idStr.toLongOrNull() ?: continue

      val user = tweet.optJSONObject("user")
      val screenName = user?.optString("screen_name")?.ifBlank { "x" } ?: "x"
      val authorName = user?.optString("name")?.ifBlank { screenName } ?: screenName

      var rawText = tweet.optString("full_text").ifBlank { tweet.optString("text") }.trim()
      val entities = tweet.optJSONObject("entities")
      val links = mutableListOf<String>()
      val urlArr = entities?.optJSONArray("urls")
      if (urlArr != null) {
        for (uIdx in 0 until urlArr.length()) {
          val uObj = urlArr.optJSONObject(uIdx) ?: continue
          val shortUrl = uObj.optString("url")
          val expanded = uObj.optString("expanded_url").ifBlank { shortUrl }.trim()
          if (shortUrl.isNotBlank() && expanded.isNotBlank()) {
            rawText = rawText.replace(shortUrl, expanded)
          }
          if (expanded.startsWith("http") && expanded !in links) {
            links.add(expanded)
          }
        }
      }

      val extMedia =
        tweet.optJSONObject("extended_entities")?.optJSONArray("media")
          ?: entities?.optJSONArray("media")

      val images = mutableListOf<String>()
      val seenImages = mutableSetOf<String>()
      val mediaItems = mutableListOf<MediaAttachment>()

      if (extMedia != null) {
        for (mIdx in 0 until extMedia.length()) {
          val m = extMedia.optJSONObject(mIdx) ?: continue
          val shortMediaUrl = m.optString("url")
          if (shortMediaUrl.isNotBlank()) {
            rawText = rawText.replace(shortMediaUrl, "").trim()
          }
          val mType = m.optString("type", "photo")
          val thumbUrl = m.optString("media_url_https").ifBlank { m.optString("media_url") }.trim()

          if (mType == "photo") {
            if (thumbUrl.startsWith("http") && seenImages.add(thumbUrl)) {
              images.add(thumbUrl)
              mediaItems.add(MediaAttachment(type = "photo", url = thumbUrl, thumbnail = thumbUrl, title = ""))
            }
          } else if (mType == "video" || mType == "animated_gif") {
            val variants = m.optJSONObject("video_info")?.optJSONArray("variants")
            var bestVideo = ""
            var bestBitrate = -1
            if (variants != null) {
              for (vIdx in 0 until variants.length()) {
                val v = variants.optJSONObject(vIdx) ?: continue
                if (v.optString("content_type") == "video/mp4") {
                  val url = v.optString("url")
                  val bitrate = v.optInt("bitrate", 0)
                  if (url.isNotBlank() && bitrate >= bestBitrate) {
                    bestBitrate = bitrate
                    bestVideo = url
                  }
                }
              }
            }
            if (thumbUrl.startsWith("http") && seenImages.add(thumbUrl)) {
              images.add(thumbUrl)
            }
            if (bestVideo.isNotBlank() || thumbUrl.isNotBlank()) {
              mediaItems.add(MediaAttachment(type = "video", url = bestVideo, thumbnail = thumbUrl, title = ""))
            }
          }
        }
      }

      val cleanBody = cleanPlainText(rawText)
      if (cleanBody.isBlank() && images.isEmpty() && mediaItems.isEmpty()) continue

      val attributedContent = "@$screenName ($authorName)\n$cleanBody".trim()
      val title = deriveTitle(cleanBody).ifBlank { "$authorName (@$screenName)" }
      val pubIso = normalizeDateToUtcIso(tweet.optString("created_at"))

      parsed.add(
        NewsArticle(
          id = "${source.username}_$msgId",
          sourceId = source.id,
          sourceName = source.name,
          sourceUsername = source.username,
          messageId = msgId,
          category = source.category,
          title = title,
          content = attributedContent,
          publishedAt = pubIso,
          fetchedAt = fetchedAt,
          updatedAt = "",
          originalUrl = "https://x.com/$screenName/status/$idStr",
          images = images,
          media = mediaItems,
          links = links,
          isEdited = false,
        )
      )

      if (parsed.size >= limit) break
    }
    return parsed
  }

  private fun stableCrc32Id(key: String): Long {
    val crc = CRC32()
    crc.update(key.toByteArray(Charsets.UTF_8))
    val masked = crc.value and 0x7FFFFFFFL
    return if (masked > 0L) masked else 1L
  }

  private fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("//")) "https:$trimmed" else trimmed
  }

  private fun stripHtml(raw: String): String {
    if (raw.isBlank()) return ""
    val cleanedTags =
      raw
        .replace("<img[^>]*>".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
    return HtmlCompat.fromHtml(cleanedTags, HtmlCompat.FROM_HTML_MODE_COMPACT)
      .toString()
      .replace("\uFFFC", "")
  }

  private fun cleanPlainText(raw: String): String {
    if (raw.isBlank()) return ""
    val lines =
      raw.replace("\r\n", "\n").split("\n").map { line ->
        multiSpaceRegex.replace(line, " ").trim()
      }
    return multiNewlineRegex.replace(lines.filter { it.isNotBlank() }.joinToString("\n").trim(), "\n\n")
  }

  private fun deriveTitle(content: String, maxLen: Int = 140): String {
    if (content.isBlank()) return ""
    val firstLine = content.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    if (firstLine.length <= maxLen) return firstLine
    val clipped = firstLine.take(maxLen).substringBeforeLast(" ").trim()
    return (clipped.ifBlank { firstLine.take(maxLen).trim() }) + "…"
  }

  private fun normalizeDateToUtcIso(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return nowUtcIso()
    for (pattern in rfcDatePatterns) {
      runCatching {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        if (pattern.endsWith("'Z'") || pattern.endsWith("ss")) {
          sdf.timeZone = TimeZone.getTimeZone("UTC")
        }
        val d = sdf.parse(trimmed)
        if (d != null) {
          val out = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
          out.timeZone = TimeZone.getTimeZone("UTC")
          return out.format(d)
        }
      }
    }
    return nowUtcIso()
  }
}
