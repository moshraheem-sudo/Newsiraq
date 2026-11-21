package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.AppUpdateStage
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdateManager
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.data.NewsSourceInfo
import com.example.data.SettingsPreferences
import com.example.data.ThemeMode
import com.example.data.local.AppDatabase
import com.example.data.local.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

data class RefreshFeedbackEvent(
  val id: Long,
  val newCount: Int,
  val isOnline: Boolean,
)

enum class UpdateFeedbackKind {
  UP_TO_DATE,
  CHECK_FAILED,
  CANCELLED,
  NO_APK_ASSET,
}

data class UpdateFeedbackEvent(
  val id: Long,
  val kind: UpdateFeedbackKind,
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
  val refreshFeedback: RefreshFeedbackEvent? = null,
  val favoriteFeedback: FavoriteFeedbackEvent? = null,
  val settings: AppSettings = AppSettings(),
) {
  companion object {
    const val DEFAULT_JSON_URL = "https://moshraheem-sudo.github.io/Newsiraq/news.json"
    const val AUTO_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
  }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
  private val appContext = application.applicationContext
  private val repository = NewsRepository(appContext)
  private val settingsPrefs = SettingsPreferences(appContext)
  private val favoritesRepo =
    FavoritesRepository(AppDatabase.getInstance(appContext).favoriteArticleDao())

  private val installedAppInfo = GitHubUpdateManager.getInstalledAppInfo(appContext)

  private val _uiState = MutableStateFlow(NewsUiState(settings = settingsPrefs.load()))
  val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

  private val _appUpdateState =
    MutableStateFlow<AppUpdateStage>(AppUpdateStage.Idle(installedAppInfo.versionName))
  val appUpdateState: StateFlow<AppUpdateStage> = _appUpdateState.asStateFlow()

  private val _showUpdateDialog = MutableStateFlow(false)
  val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

  private val _updateFeedback = MutableStateFlow<UpdateFeedbackEvent?>(null)
  val updateFeedback: StateFlow<UpdateFeedbackEvent?> = _updateFeedback.asStateFlow()

  private var filterJob: Job? = null
  private var refreshJob: Job? = null
  private var updateCheckJob: Job? = null
  private var updateDownloadJob: Job? = null
  private var pendingManualFeedback: Boolean = false

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
      // Load cached/bundled snapshot immediately for 0ms startup, then sync live immediately
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
      refresh(isManual = false)

      // Keep foreground feed fresh every 10 minutes automatically while user browses
      while (isActive) {
        delay(NewsUiState.AUTO_REFRESH_INTERVAL_MS)
        refresh(isManual = false)
      }
    }

    // Check GitHub for newer app versions automatically on startup when connected to the internet
    viewModelScope.launch {
      if (GitHubUpdateManager.isNetworkConnected(appContext)) {
        checkForAppUpdates(isManual = false)
      }
    }
  }

  fun refresh(isManual: Boolean = true) {
    if (refreshJob?.isActive == true) {
      if (isManual) pendingManualFeedback = true
      return
    }
    pendingManualFeedback = isManual

    refreshJob = viewModelScope.launch {
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
        val newCount =
          if (previousIds.isNotEmpty()) {
            payload.items.count { it.id !in previousIds }
          } else {
            0
          }
        val manualRequested = pendingManualFeedback
        pendingManualFeedback = false
        val showBanner = manualRequested || newCount > 0
        val updated =
          _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            articles = payload.items,
            sources = payload.sources,
            lastSuccessfulUpdate = payload.lastSuccessfulUpdate,
            loadedFromRemote = payload.loadedFromRemote,
            refreshFeedback =
              if (showBanner) {
                RefreshFeedbackEvent(
                  id = System.currentTimeMillis(),
                  newCount = newCount,
                  isOnline = payload.loadedFromRemote,
                )
              } else {
                null
              },
          )
        applyDerivedState(updated)
      } catch (e: Exception) {
        val manualRequested = pendingManualFeedback
        pendingManualFeedback = false
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            errorMessage = if (it.articles.isEmpty()) (e.localizedMessage ?: "تعذر تحميل الأخبار") else null,
            refreshFeedback =
              if (manualRequested) {
                RefreshFeedbackEvent(
                  id = System.currentTimeMillis(),
                  newCount = 0,
                  isOnline = false,
                )
              } else {
                null
              },
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
        val rawSrcCounts = articles.groupingBy { it.sourceUsername.lowercase() }.eachCount()
        val srcCounts =
          sources.associate { src ->
            src.username to (rawSrcCounts[src.username.lowercase()] ?: 0)
          }

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
              baseState.selectedSource == "all" ||
                article.sourceUsername.equals(baseState.selectedSource, ignoreCase = true)
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

  fun dismissAlert() {
    _uiState.update { it.copy(refreshFeedback = null) }
  }

  fun updateSearchQuery(query: String) {
    scheduleFilterUpdate(_uiState.value.copy(searchQuery = query))
  }

  fun selectCategory(category: String) {
    val state = _uiState.value
    val validSource =
      category == "all" ||
        state.selectedSource == "all" ||
        state.sources.any {
          it.username.equals(state.selectedSource, ignoreCase = true) && it.category == category
        }
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

  fun setThemeMode(mode: ThemeMode) {
    settingsPrefs.saveThemeMode(mode)
    _uiState.update { it.copy(settings = it.settings.copy(themeMode = mode)) }
  }

  fun setLanguage(language: AppLanguage) {
    settingsPrefs.saveLanguage(language)
    _uiState.update { it.copy(settings = it.settings.copy(language = language)) }
  }

  fun checkForAppUpdates(isManual: Boolean = true) {
    if (updateDownloadJob?.isActive == true) {
      if (isManual) _showUpdateDialog.value = true
      return
    }
    if (updateCheckJob?.isActive == true) return

    updateCheckJob =
      viewModelScope.launch {
        if (!GitHubUpdateManager.isNetworkConnected(appContext)) {
          if (isManual) {
            _appUpdateState.value =
              AppUpdateStage.Error(
                installedVersion = installedAppInfo.versionName,
                message = "offline",
              )
            _updateFeedback.value =
              UpdateFeedbackEvent(System.currentTimeMillis(), UpdateFeedbackKind.CHECK_FAILED)
          }
          return@launch
        }

        _appUpdateState.value = AppUpdateStage.Checking(installedAppInfo.versionName)
        val result = GitHubUpdateManager.fetchLatestRelease()
        result.fold(
          onSuccess = { release ->
            if (GitHubUpdateManager.isNewerVersion(release.versionName, installedAppInfo.versionName)) {
              val readyFile = GitHubUpdateManager.findReadyApkIfExists(appContext, release)
              _appUpdateState.value =
                if (readyFile != null) {
                  AppUpdateStage.ReadyToInstall(
                    installedVersion = installedAppInfo.versionName,
                    release = release,
                    apkFile = readyFile,
                    apkSizeBytes = readyFile.length(),
                  )
                } else {
                  AppUpdateStage.UpdateAvailable(
                    installedVersion = installedAppInfo.versionName,
                    release = release,
                  )
                }
              _showUpdateDialog.value = true
            } else {
              _appUpdateState.value =
                AppUpdateStage.UpToDate(
                  installedVersion = installedAppInfo.versionName,
                  latestRelease = release,
                )
              if (isManual) {
                _updateFeedback.value =
                  UpdateFeedbackEvent(System.currentTimeMillis(), UpdateFeedbackKind.UP_TO_DATE)
              }
            }
          },
          onFailure = { err ->
            if (isManual) {
              _appUpdateState.value =
                AppUpdateStage.Error(
                  installedVersion = installedAppInfo.versionName,
                  message = err.localizedMessage ?: "",
                )
              _updateFeedback.value =
                UpdateFeedbackEvent(System.currentTimeMillis(), UpdateFeedbackKind.CHECK_FAILED)
            } else {
              _appUpdateState.value = AppUpdateStage.Idle(installedAppInfo.versionName)
            }
          },
        )
      }
  }

  fun startAppUpdateDownload(releaseOverride: GitHubReleaseInfo? = null) {
    if (updateDownloadJob?.isActive == true) return
    val current = _appUpdateState.value
    val release =
      releaseOverride
        ?: when (current) {
          is AppUpdateStage.UpdateAvailable -> current.release
          is AppUpdateStage.Error -> current.release
          is AppUpdateStage.ReadyToInstall -> current.release
          else -> null
        } ?: return

    if (release.apkDownloadUrl.isNullOrBlank()) {
      _updateFeedback.value =
        UpdateFeedbackEvent(System.currentTimeMillis(), UpdateFeedbackKind.NO_APK_ASSET)
      return
    }

    val existingReady = GitHubUpdateManager.findReadyApkIfExists(appContext, release)
    if (existingReady != null) {
      _appUpdateState.value =
        AppUpdateStage.ReadyToInstall(
          installedVersion = installedAppInfo.versionName,
          release = release,
          apkFile = existingReady,
          apkSizeBytes = existingReady.length(),
        )
      return
    }

    updateDownloadJob =
      viewModelScope.launch {
        _appUpdateState.value =
          AppUpdateStage.Downloading(
            installedVersion = installedAppInfo.versionName,
            release = release,
            downloadedBytes = 0L,
            totalBytes = release.apkSizeBytes,
          )

        val downloadResult =
          GitHubUpdateManager.downloadApk(appContext, release) { downloaded, total ->
            _appUpdateState.value =
              AppUpdateStage.Downloading(
                installedVersion = installedAppInfo.versionName,
                release = release,
                downloadedBytes = downloaded,
                totalBytes = total,
              )
          }

        downloadResult.fold(
          onSuccess = { apkFile ->
            _appUpdateState.value =
              AppUpdateStage.ReadyToInstall(
                installedVersion = installedAppInfo.versionName,
                release = release,
                apkFile = apkFile,
                apkSizeBytes = apkFile.length(),
              )
          },
          onFailure = { err ->
            if (err is CancellationException) {
              _appUpdateState.value =
                AppUpdateStage.UpdateAvailable(
                  installedVersion = installedAppInfo.versionName,
                  release = release,
                )
            } else {
              _appUpdateState.value =
                AppUpdateStage.Error(
                  installedVersion = installedAppInfo.versionName,
                  message = err.localizedMessage ?: "Download failed",
                  release = release,
                )
            }
          },
        )
      }
  }

  fun cancelAppUpdateDownload() {
    val current = _appUpdateState.value
    val release = (current as? AppUpdateStage.Downloading)?.release
    GitHubUpdateManager.cancelActiveDownload(appContext)
    updateDownloadJob?.cancel()
    updateDownloadJob = null
    _appUpdateState.value =
      if (release != null) {
        AppUpdateStage.UpdateAvailable(
          installedVersion = installedAppInfo.versionName,
          release = release,
        )
      } else {
        AppUpdateStage.Idle(installedAppInfo.versionName)
      }
    _updateFeedback.value =
      UpdateFeedbackEvent(System.currentTimeMillis(), UpdateFeedbackKind.CANCELLED)
  }

  fun showUpdateDialog() {
    _showUpdateDialog.value = true
  }

  fun dismissUpdateDialog() {
    _showUpdateDialog.value = false
  }

  fun clearUpdateFeedback() {
    _updateFeedback.value = null
  }
}
