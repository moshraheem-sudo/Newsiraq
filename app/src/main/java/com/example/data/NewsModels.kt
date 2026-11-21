package com.example.data

data class NewsSourceInfo(
  val id: String,
  val name: String,
  val username: String,
  val category: String,
)

data class NewsArticle(
  val id: String,
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
  val images: List<String>,
  val links: List<String>,
  val isEdited: Boolean,
)

data class NewsFeedPayload(
  val appVersion: String,
  val lastSuccessfulUpdate: String,
  val totalCount: Int,
  val sources: List<NewsSourceInfo>,
  val items: List<NewsArticle>,
  val loadedFromRemote: Boolean,
)
