package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.data.NewsSourceInfo
import com.example.data.SettingsPreferences
import com.example.data.ThemeMode
import com.example.notifications.NewsNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val articles: List<NewsArticle> = emptyList(),
  val sources: List<NewsSourceInfo> = emptyList(),
  val selectedSource: String = "all",
  val searchQuery: String = "",
  val lastSuccessfulUpdate: String = "",
  val loadedFromRemote: Boolean = false,
  val activeVideoUrl: String? = null,
  val newArticlesAlertCount: Int? = null,
  val pendingArticleId: String? = null,
  val settings: AppSettings = AppSettings(),
) {
  val filteredArticles: List<NewsArticle>
    get() {
      val q = searchQuery.trim().lowercase()
      return articles.filter { article ->
        val matchesSource =
          selectedSource == "all" || article.sourceUsername == selectedSource
        val matchesQuery =
          q.isEmpty() ||
            article.title.lowercase().contains(q) ||
            article.content.lowercase().contains(q) ||
            article.sourceName.lowercase().contains(q)
        matchesSource && matchesQuery
      }
    }

  companion object {
    const val DEFAULT_JSON_URL = "https://moshraheem-sudo.github.io/Newsiraq/news.json"
  }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = NewsRepository(application.applicationContext)
  private val settingsPrefs = SettingsPreferences(application.applicationContext)

  private val _uiState = MutableStateFlow(NewsUiState(settings = settingsPrefs.load()))
  val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      val previousIds = _uiState.value.articles.map { it.id }.toSet()
      _uiState.update {
        it.copy(
          isLoading = it.articles.isEmpty(),
          isRefreshing = true,
          errorMessage = null,
        )
      }
      try {
        val payload = repository.fetchNews(NewsUiState.DEFAULT_JSON_URL)
        val systemNotifiedCount =
          if (payload.loadedFromRemote) {
            NewsNotificationHelper.processAndNotify(getApplication(), payload.items)
          } else {
            0
          }
        val inSessionNewCount =
          if (previousIds.isNotEmpty()) {
            payload.items.count { it.id !in previousIds }
          } else {
            systemNotifiedCount
          }
        val shouldAlert = inSessionNewCount > 0 && !_uiState.value.settings.muteNotifications
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            articles = payload.items,
            sources = payload.sources,
            lastSuccessfulUpdate = payload.lastSuccessfulUpdate,
            loadedFromRemote = payload.loadedFromRemote,
            newArticlesAlertCount = if (shouldAlert) inSessionNewCount else null,
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            errorMessage = e.localizedMessage ?: "تعذر تحميل الأخبار",
          )
        }
      }
    }
  }

  fun openArticleFromNotification(articleId: String?) {
    if (!articleId.isNullOrBlank()) {
      _uiState.update { it.copy(pendingArticleId = articleId) }
    }
  }

  fun consumePendingArticle() {
    _uiState.update { it.copy(pendingArticleId = null) }
  }

  fun dismissAlert() {
    _uiState.update { it.copy(newArticlesAlertCount = null) }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun selectSource(username: String) {
    _uiState.update { it.copy(selectedSource = username) }
  }

  fun togglePlayVideo(videoUrl: String?) {
    _uiState.update { state ->
      val next = if (state.activeVideoUrl == videoUrl) null else videoUrl
      state.copy(activeVideoUrl = next)
    }
  }

  fun setMuteNotifications(muted: Boolean) {
    settingsPrefs.saveMuteNotifications(muted)
    _uiState.update {
      it.copy(
        settings = it.settings.copy(muteNotifications = muted),
        newArticlesAlertCount = if (muted) null else it.newArticlesAlertCount,
      )
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    settingsPrefs.saveThemeMode(mode)
    _uiState.update { it.copy(settings = it.settings.copy(themeMode = mode)) }
  }

  fun setLanguage(language: AppLanguage) {
    settingsPrefs.saveLanguage(language)
    _uiState.update { it.copy(settings = it.settings.copy(language = language)) }
  }
}
