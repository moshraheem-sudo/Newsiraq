package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.MediaAttachment
import com.example.data.NewsArticle
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "favorite_articles")
data class FavoriteArticleEntity(
  @PrimaryKey val id: String,
  val sourceId: String,
  val sourceName: String,
  val sourceUsername: String,
  val messageId: Long,
  val category: String,
  val title: String,
  val content: String,
  val publishedAt: String,
  val fetchedAt: String,
  val updatedAt: String,
  val originalUrl: String,
  val imagesJson: String,
  val mediaJson: String,
  val linksJson: String,
  val isEdited: Boolean,
  val savedAt: Long = System.currentTimeMillis(),
) {
  fun toNewsArticle(): NewsArticle {
    return NewsArticle(
      id = id,
      sourceId = sourceId,
      sourceName = sourceName,
      sourceUsername = sourceUsername,
      messageId = messageId,
      category = category,
      title = title,
      content = content,
      publishedAt = publishedAt,
      fetchedAt = fetchedAt,
      updatedAt = updatedAt,
      originalUrl = originalUrl,
      images = decodeStringList(imagesJson),
      media = decodeMediaList(mediaJson),
      links = decodeStringList(linksJson),
      isEdited = isEdited,
    )
  }

  companion object {
    fun fromNewsArticle(article: NewsArticle): FavoriteArticleEntity {
      return FavoriteArticleEntity(
        id = article.id,
        sourceId = article.sourceId,
        sourceName = article.sourceName,
        sourceUsername = article.sourceUsername,
        messageId = article.messageId,
        category = article.category,
        title = article.title,
        content = article.content,
        publishedAt = article.publishedAt,
        fetchedAt = article.fetchedAt,
        updatedAt = article.updatedAt,
        originalUrl = article.originalUrl,
        imagesJson = encodeStringList(article.images),
        mediaJson = encodeMediaList(article.media),
        linksJson = encodeStringList(article.links),
        isEdited = article.isEdited,
        savedAt = System.currentTimeMillis(),
      )
    }

    private fun encodeStringList(items: List<String>): String {
      val arr = JSONArray()
      items.forEach { arr.put(it) }
      return arr.toString()
    }

    private fun decodeStringList(raw: String): List<String> {
      return try {
        val arr = JSONArray(raw)
        List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
      } catch (_: Exception) {
        emptyList()
      }
    }

    private fun encodeMediaList(items: List<MediaAttachment>): String {
      val arr = JSONArray()
      items.forEach { m ->
        arr.put(
          JSONObject().apply {
            put("type", m.type)
            put("url", m.url)
            put("thumbnail", m.thumbnail)
            put("title", m.title)
          }
        )
      }
      return arr.toString()
    }

    private fun decodeMediaList(raw: String): List<MediaAttachment> {
      return try {
        val arr = JSONArray(raw)
        List(arr.length()) { idx ->
          val o = arr.optJSONObject(idx) ?: return@List null
          MediaAttachment(
            type = o.optString("type"),
            url = o.optString("url"),
            thumbnail = o.optString("thumbnail"),
            title = o.optString("title"),
          )
        }.filterNotNull()
      } catch (_: Exception) {
        emptyList()
      }
    }
  }
}
