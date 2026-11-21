package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NewsRepository(private val context: Context) {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()

  suspend fun fetchNews(remoteJsonUrl: String): NewsFeedPayload =
    withContext(Dispatchers.IO) {
      val trimmed = remoteJsonUrl.trim()
      if (trimmed.startsWith("https://") && !trimmed.contains("USERNAME")) {
        try {
          val bustUrl = "$trimmed?t=${System.currentTimeMillis()}"
          val request = Request.Builder().url(bustUrl).build()
          client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
              val body = response.body?.string()
              if (!body.isNullOrBlank()) {
                return@withContext parsePayload(body, loadedFromRemote = true)
              }
            }
          }
        } catch (_: Exception) {
          // Fall back to bundled snapshot in assets
        }
      }

      val fallbackJson =
        context.assets.open("news.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
      parsePayload(fallbackJson, loadedFromRemote = false)
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
      appVersion = root.optString("app_version", "1.0.0"),
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
}
