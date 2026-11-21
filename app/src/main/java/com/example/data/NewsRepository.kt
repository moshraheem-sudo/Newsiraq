package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class NewsRepository(private val context: Context) {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()

  private val noCache = CacheControl.Builder().noCache().noStore().build()

  private val localCacheFile: File
    get() = File(context.applicationContext.filesDir, "cached_news.json")

  private val localSourcesCacheFile: File
    get() = File(context.applicationContext.filesDir, "cached_sources.json")

  suspend fun fetchNews(remoteJsonUrl: String): NewsFeedPayload =
    withContext(Dispatchers.IO) {
      val trimmed = remoteJsonUrl.trim()
      val basePayload = loadLocalCachedPayload() ?: loadBundledAssetPayload()
      val remoteSourcesDeferred =
        async {
          fetchAndCacheRemoteSources()
        }

      val sourceConfigs = remoteSourcesDeferred.await() ?: loadSourceConfigs()

      val remoteDeferred =
        async {
          if (trimmed.startsWith("https://") && !trimmed.contains("USERNAME")) {
            fetchRemoteJsonPayload(trimmed)
              ?: fetchRemoteJsonPayload(RAW_FALLBACK_JSON_URL)
          } else {
            null
          }
        }

      val directLiveDeferred =
        async {
          runCatching { LiveFeedFetcher.fetchAllLive(sourceConfigs) }.getOrNull()
        }

      val remotePayload = remoteDeferred.await()
      val directLive = directLiveDeferred.await()
      val liveSucceeded = (directLive?.sourcesSucceeded ?: 0) > 0
      val anyOnlineSuccess = remotePayload != null || liveSucceeded

      if (!anyOnlineSuccess) {
        return@withContext basePayload.copy(loadedFromRemote = false)
      }

      // Merge order: base cached/bundled -> remote news.json -> freshly scraped direct live items
      val mergedMap = LinkedHashMap<String, NewsArticle>(1800)
      basePayload.items.forEach { if (it.id.isNotBlank()) mergedMap[it.id] = it }
      remotePayload?.items?.forEach { if (it.id.isNotBlank()) mergedMap[it.id] = it }
      directLive?.articles?.forEach { if (it.id.isNotBlank()) mergedMap[it.id] = it }

      val sortedMerged =
        mergedMap.values
          .sortedWith(
            compareByDescending<NewsArticle> { it.publishedAt }.thenByDescending { it.messageId }
          )
          .take(MAX_STORED_ARTICLES)

      val mergedSources =
        when {
          sourceConfigs.isNotEmpty() ->
            sourceConfigs.map {
              NewsSourceInfo(
                id = it.id,
                name = it.name,
                username = it.username,
                category = it.category,
              )
            }
          remotePayload != null && remotePayload.sources.isNotEmpty() -> remotePayload.sources
          else -> basePayload.sources
        }

      val bestTimestamp =
        if (liveSucceeded) {
          directLive!!.fetchedAtIso
        } else {
          remotePayload?.lastSuccessfulUpdate?.takeIf { it.isNotBlank() }
            ?: LiveFeedFetcher.nowUtcIso()
        }

      val finalPayload =
        NewsFeedPayload(
          appVersion = remotePayload?.appVersion ?: basePayload.appVersion,
          lastSuccessfulUpdate = bestTimestamp,
          totalCount = sortedMerged.size,
          sources = mergedSources,
          items = sortedMerged,
          loadedFromRemote = true,
        )

      saveCacheAtomically(serializePayload(finalPayload))
      finalPayload
    }

  suspend fun loadInstantSnapshot(): NewsFeedPayload? =
    withContext(Dispatchers.IO) {
      loadLocalCachedPayload() ?: runCatching { loadBundledAssetPayload() }.getOrNull()
    }

  private fun fetchAndCacheRemoteSources(): List<NewsSourceConfig>? {
    return runCatching {
      val bustUrl = "$RAW_SOURCES_JSON_URL?t=${System.currentTimeMillis()}"
      val request = Request.Builder().url(bustUrl).cacheControl(noCache).build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val parsed = LiveFeedFetcher.parseSourcesConfig(body)
        if (parsed.isNotEmpty()) {
          saveTextAtomically(localSourcesCacheFile, body)
          parsed
        } else {
          null
        }
      }
    }.getOrNull()
  }

  private fun fetchRemoteJsonPayload(url: String): NewsFeedPayload? {
    return runCatching {
      val sep = if (url.contains("?")) "&" else "?"
      val bustUrl = "${url}${sep}t=${System.currentTimeMillis()}"
      val request = Request.Builder().url(bustUrl).cacheControl(noCache).build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string()
        if (body.isNullOrBlank()) null else parsePayload(body, loadedFromRemote = true)
      }
    }.getOrNull()
  }

  private fun loadSourceConfigs(): List<NewsSourceConfig> {
    val cached =
      runCatching {
        val file = localSourcesCacheFile
        if (file.exists()) file.readText(Charsets.UTF_8) else null
      }.getOrNull()
    if (!cached.isNullOrBlank()) {
      val parsedCached = LiveFeedFetcher.parseSourcesConfig(cached)
      if (parsedCached.isNotEmpty()) return parsedCached
    }

    val bundled =
      runCatching {
        context.assets.open("sources.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
      }.getOrNull()
    return if (bundled.isNullOrBlank()) emptyList() else LiveFeedFetcher.parseSourcesConfig(bundled)
  }

  private fun loadBundledAssetPayload(): NewsFeedPayload {
    val fallbackJson =
      context.assets.open("news.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    return parsePayload(fallbackJson, loadedFromRemote = false)
  }

  private fun loadLocalCachedPayload(): NewsFeedPayload? {
    val file = localCacheFile
    if (!file.exists()) return null
    return runCatching {
      val text = file.readText(Charsets.UTF_8)
      if (text.isBlank()) null else parsePayload(text, loadedFromRemote = false)
    }.getOrNull()
  }

  private fun saveCacheAtomically(rawJson: String) {
    saveTextAtomically(localCacheFile, rawJson)
  }

  private fun saveTextAtomically(target: File, content: String) {
    runCatching {
      val tmp = File(target.parentFile, "${target.name}.tmp")
      tmp.writeText(content, Charsets.UTF_8)
      if (!tmp.renameTo(target)) {
        target.writeText(content, Charsets.UTF_8)
        tmp.delete()
      }
    }
  }

  private fun serializePayload(payload: NewsFeedPayload): String {
    val root = JSONObject()
    root.put("app_version", payload.appVersion)
    root.put("last_successful_update", payload.lastSuccessfulUpdate)
    root.put("total_count", payload.items.size)

    val sourcesArr = JSONArray()
    payload.sources.forEach { s ->
      sourcesArr.put(
        JSONObject().apply {
          put("id", s.id)
          put("name", s.name)
          put("username", s.username)
          put("category", s.category)
        }
      )
    }
    root.put("sources", sourcesArr)

    val itemsArr = JSONArray()
    payload.items.forEach { a ->
      itemsArr.put(
        JSONObject().apply {
          put("id", a.id)
          put("source_id", a.sourceId)
          put("source_name", a.sourceName)
          put("source_username", a.sourceUsername)
          put("message_id", a.messageId)
          put("category", a.category)
          put("title", a.title)
          put("content", a.content)
          put("published_at", a.publishedAt)
          put("fetched_at", a.fetchedAt)
          put("updated_at", a.updatedAt)
          put("original_url", a.originalUrl)
          put("images", JSONArray(a.images))
          put(
            "media",
            JSONArray().apply {
              a.media.forEach { m ->
                put(
                  JSONObject().apply {
                    put("type", m.type)
                    put("url", m.url)
                    put("thumbnail", m.thumbnail)
                    put("title", m.title)
                  }
                )
              }
            },
          )
          put("links", JSONArray(a.links))
          put("is_edited", a.isEdited)
        }
      )
    }
    root.put("items", itemsArr)
    return root.toString()
  }

  private fun parsePayload(rawJson: String, loadedFromRemote: Boolean): NewsFeedPayload {
    val root = JSONObject(rawJson)
    val itemsArr = root.optJSONArray("items") ?: JSONArray()
    val sourcesArr = root.optJSONArray("sources") ?: JSONArray()

    val sources =
      List(sourcesArr.length()) { idx ->
        val obj = sourcesArr.getJSONObject(idx)
        NewsSourceInfo(
          id = obj.optString("id"),
          name = obj.optString("name"),
          username = obj.optString("username"),
          category = obj.optString("category"),
        )
      }

    val articles =
      List(itemsArr.length()) { idx ->
        val obj = itemsArr.getJSONObject(idx)
        NewsArticle(
          id = obj.optString("id"),
          sourceId = obj.optString("source_id"),
          sourceName = obj.optString("source_name"),
          sourceUsername = obj.optString("source_username"),
          messageId = obj.optLong("message_id"),
          category = obj.optString("category"),
          title = obj.optString("title"),
          content = obj.optString("content"),
          publishedAt = obj.optString("published_at"),
          fetchedAt = obj.optString("fetched_at"),
          updatedAt = obj.optString("updated_at"),
          originalUrl = obj.optString("original_url"),
          images = jsonArrayToList(obj.optJSONArray("images")),
          media = parseMediaArray(obj.optJSONArray("media")),
          links = jsonArrayToList(obj.optJSONArray("links")),
          isEdited = obj.optBoolean("is_edited", false),
        )
      }.sortedWith(
        compareByDescending<NewsArticle> { it.publishedAt }.thenByDescending { it.messageId }
      )

    return NewsFeedPayload(
      appVersion = root.optString("app_version", "1.7.0"),
      lastSuccessfulUpdate = root.optString("last_successful_update", ""),
      totalCount = root.optInt("total_count", articles.size),
      sources = sources,
      items = articles,
      loadedFromRemote = loadedFromRemote,
    )
  }

  private fun parseMediaArray(arr: JSONArray?): List<MediaAttachment> {
    if (arr == null) return emptyList()
    return List(arr.length()) { idx ->
      val obj = arr.optJSONObject(idx) ?: return@List null
      MediaAttachment(
        type = obj.optString("type"),
        url = obj.optString("url"),
        thumbnail = obj.optString("thumbnail"),
        title = obj.optString("title"),
      )
    }.filterNotNull()
  }

  private fun jsonArrayToList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
  }

  companion object {
    private const val MAX_STORED_ARTICLES = 1600
    private const val RAW_FALLBACK_JSON_URL =
      "https://raw.githubusercontent.com/moshraheem-sudo/Newsiraq/main/iraq-news-hub/data/news.json"
    private const val RAW_SOURCES_JSON_URL =
      "https://raw.githubusercontent.com/moshraheem-sudo/Newsiraq/main/iraq-news-hub/config/sources.json"
  }
}
