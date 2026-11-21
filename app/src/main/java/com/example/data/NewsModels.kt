package com.example.data

enum class ThemeMode(val key: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromKey(key: String?): ThemeMode =
      entries.firstOrNull { it.key == key } ?: SYSTEM
  }
}

enum class AppLanguage(val code: String) {
  ARABIC("ar"),
  ENGLISH("en");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code } ?: ARABIC
  }
}

data class AppSettings(
  val muteNotifications: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val language: AppLanguage = AppLanguage.ARABIC,
)

data class MediaAttachment(
  val type: String,
  val url: String,
  val thumbnail: String,
  val title: String,
)

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
  val media: List<MediaAttachment>,
  val links: List<String>,
  val isEdited: Boolean,
) {
  val videos: List<MediaAttachment>
    get() = media.filter { it.type == "video" && it.url.isNotBlank() }

  val displayImages: List<String>
    get() {
      val videoThumbs = videos.map { it.thumbnail }.filter { it.isNotBlank() }.toSet()
      val nonThumbImages = images.filter { it !in videoThumbs }
      return nonThumbImages.ifEmpty { images }
    }
}

data class NewsFeedPayload(
  val appVersion: String,
  val lastSuccessfulUpdate: String,
  val totalCount: Int,
  val sources: List<NewsSourceInfo>,
  val items: List<NewsArticle>,
  val loadedFromRemote: Boolean,
)
