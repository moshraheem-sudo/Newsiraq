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
import com.example.data.local.AppDatabase
import com.example.data.local.FavoritesRepository
import com.example.notifications.NewsNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FullscreenVideoRequest(
  val url: String,
  val title: String,
)

data class FavoriteFeedbackEvent(
  val id: Long,
  val added: Boolean,
)

data class NewsUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val articles: List<NewsArticle> = emptyList(),
  val filteredArticles: List<NewsArticle> = emptyList(),
  val sources: List<NewsSourceInfo> = emptyList(),
  val availableCategories: List<String> = emptyList(),
  val filteredSources: List<NewsSourceInfo> = emptyList(),
  val categoryCounts: Map<String, Int> = emptyMap(),
  val sourceCounts: Map<String, Int> = emptyMap(),
  val selectedCategory: String = "all",
  val selectedSource: String = "all",
  val searchQuery: String = "",
  val lastSuccessfulUpdate: String = "",
  val loadedFromRemote: Boolean = false,
  val activeVideoUrl: String? = null,
  val fullscreenVideo: FullscreenVideoRequest? = null,
  val newArticlesAlertCount: Int? = null,
  val favoriteFeedback: FavoriteFeedbackEvent? = null,
  val pendingArticleId: String? = null,
  val settings: AppSettings = AppSettings(),
) {
  companion object {
    const val DEFAULT_JSON_URL = "https://moshraheem-sudo.github.io/Newsiraq/news.json"
  }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = NewsRepository(application.applicationContext)
  private val settingsPrefs = SettingsPreferences(application.applicationContext)
  private val favoritesRepo =
    FavoritesRepository(AppDatabase.getInstance(application.applicationContext).favoriteArticleDao())

  private val _uiState = MutableStateFlow(NewsUiState(settings = settingsPrefs.load()))
  val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

  private var filterJob: Job? = null

  val favoriteArticles: StateFlow<List<NewsArticle>> =
    favoritesRepo.favorites.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val favoriteIds: StateFlow<Set<String>> =
    favoritesRepo.favoriteIds.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptySet(),
    )

  init {
    viewModelScope.launch {
      // Load cached/bundled snapshot immediately for 0ms startup, then fetch fresh live feed
      val snapshot = repository.loadInstantSnapshot()
      if (snapshot != null && _uiState.value.articles.isEmpty()) {
        applyDerivedState(
          baseState =
            _uiState.value.copy(
              isLoading = false,
              articles = snapshot.items,
              sources = snapshot.sources,
              lastSuccessfulUpdate = snapshot.lastSuccessfulUpdate,
              loadedFromRemote = false,
            )
        )
      }
      refresh()
    }
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
        val updated =
          _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            articles = payload.items,
            sources = payload.sources,
            lastSuccessfulUpdate = payload.lastSuccessfulUpdate,
            loadedFromRemote = payload.loadedFromRemote,
            newArticlesAlertCount = if (shouldAlert) inSessionNewCount else null,
          )
        applyDerivedState(updated)
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

  private fun scheduleFilterUpdate(updated: NewsUiState) {
    _uiState.value = updated
    filterJob?.cancel()
    filterJob = viewModelScope.launch { applyDerivedState(updated) }
  }

  private suspend fun applyDerivedState(baseState: NewsUiState) {
    val computed =
      withContext(Dispatchers.Default) {
        val articles = baseState.articles
        val sources = baseState.sources
        val categories =
          (sources.map { it.category } + articles.map { it.category })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val catCounts = articles.groupingBy { it.category }.eachCount()
        val srcCounts = articles.groupingBy { it.sourceUsername }.eachCount()

        val filteredSrcs =
          if (baseState.selectedCategory == "all") {
            sources
          } else {
            sources.filter { it.category == baseState.selectedCategory }
          }

        val q = baseState.searchQuery.trim().lowercase()
        val filtered =
          articles.filter { article ->
            val matchesCategory =
              baseState.selectedCategory == "all" || article.category == baseState.selectedCategory
            val matchesSource =
              baseState.selectedSource == "all" || article.sourceUsername == baseState.selectedSource
            val matchesQuery =
              q.isEmpty() ||
                article.title.lowercase().contains(q) ||
                article.content.lowercase().contains(q) ||
                article.sourceName.lowercase().contains(q)
            matchesCategory && matchesSource && matchesQuery
          }

        baseState.copy(
          availableCategories = categories,
          filteredSources = filteredSrcs,
          categoryCounts = catCounts,
          sourceCounts = srcCounts,
          filteredArticles = filtered,
        )
      }
    _uiState.value = computed
  }

  fun toggleFavorite(article: NewsArticle) {
    viewModelScope.launch {
      val currentlyFav = article.id in favoriteIds.value
      val added = favoritesRepo.toggleFavorite(article, currentlyFav)
      _uiState.update {
        it.copy(
          favoriteFeedback = FavoriteFeedbackEvent(System.currentTimeMillis(), added),
        )
      }
    }
  }

  fun dismissFavoriteFeedback() {
    _uiState.update { it.copy(favoriteFeedback = null) }
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
    scheduleFilterUpdate(_uiState.value.copy(searchQuery = query))
  }

  fun selectCategory(category: String) {
    val state = _uiState.value
    val validSource =
      category == "all" ||
        state.selectedSource == "all" ||
        state.sources.any { it.username == state.selectedSource && it.category == category }
    scheduleFilterUpdate(
      state.copy(
        selectedCategory = category,
        selectedSource = if (validSource) state.selectedSource else "all",
      )
    )
  }

  fun selectSource(username: String) {
    scheduleFilterUpdate(_uiState.value.copy(selectedSource = username))
  }

  fun togglePlayVideo(videoUrl: String?) {
    _uiState.update { state ->
      val next = if (state.activeVideoUrl == videoUrl) null else videoUrl
      state.copy(activeVideoUrl = next)
    }
  }

  fun openFullscreenVideo(url: String, title: String) {
    _uiState.update {
      it.copy(
        activeVideoUrl = null,
        fullscreenVideo = FullscreenVideoRequest(url = url, title = title),
      )
    }
  }

  fun closeFullscreenVideo() {
    _uiState.update { it.copy(fullscreenVideo = null) }
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
