package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.AppUpdateStage
import com.example.data.GitHubUpdateManager
import com.example.data.MediaAttachment
import com.example.data.NewsArticle
import com.example.data.ThemeMode
import com.example.ui.AppStrings
import com.example.ui.AppUpdateBanner
import com.example.ui.AppUpdateDialog
import com.example.ui.DateFormatter
import com.example.ui.FullscreenVideoRequest
import com.example.ui.GitHubUpdateSettingsCard
import com.example.ui.InstallPermissionRationaleDialog
import com.example.ui.NewsUiState
import com.example.ui.NewsViewModel
import com.example.ui.UpdateFeedbackKind
import com.example.ui.stringsFor
import com.example.ui.theme.MyApplicationTheme
import com.example.util.MediaDownloader
import com.example.util.NewsShareHelper
import com.example.util.SafeIntentHelper
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
  private val newsViewModel: NewsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val uiState by newsViewModel.uiState.collectAsStateWithLifecycle()
      val favoriteArticles by newsViewModel.favoriteArticles.collectAsStateWithLifecycle()
      val favoriteIds by newsViewModel.favoriteIds.collectAsStateWithLifecycle()
      val layoutDirection =
        if (uiState.settings.language == AppLanguage.ARABIC) {
          LayoutDirection.Rtl
        } else {
          LayoutDirection.Ltr
        }

      MyApplicationTheme(themeMode = uiState.settings.themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
          WorldNewsApp(
            viewModel = newsViewModel,
            uiState = uiState,
            favoriteArticles = favoriteArticles,
            favoriteIds = favoriteIds,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldNewsApp(
  viewModel: NewsViewModel,
  uiState: NewsUiState,
  favoriteArticles: List<NewsArticle>,
  favoriteIds: Set<String>,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  val strings = remember(uiState.settings.language) { stringsFor(uiState.settings.language) }
  val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
  val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
  val updateFeedback by viewModel.updateFeedback.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
  var pendingInstallFile by remember { mutableStateOf<File?>(null) }
  var showInstallPermissionDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val launchSystemInstaller: (File) -> Unit = { apkFile ->
    runCatching {
      context.startActivity(GitHubUpdateManager.createInstallApkIntent(context, apkFile))
    }.onFailure {
      coroutineScope.launch { snackbarHostState.showSnackbar(strings.noAppForLink) }
    }
  }

  val installPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      val target = pendingInstallFile
      if (target != null && GitHubUpdateManager.canInstallPackages(context)) {
        pendingInstallFile = null
        launchSystemInstaller(target)
      }
    }

  val requestInstallApk: (File) -> Unit = { apkFile ->
    if (GitHubUpdateManager.canInstallPackages(context)) {
      launchSystemInstaller(apkFile)
    } else {
      pendingInstallFile = apkFile
      showInstallPermissionDialog = true
    }
  }

  LaunchedEffect(updateFeedback) {
    val fb = updateFeedback
    if (fb != null) {
      val msg =
        when (fb.kind) {
          UpdateFeedbackKind.UP_TO_DATE -> strings.upToDateMessage
          UpdateFeedbackKind.CHECK_FAILED -> strings.updateCheckFailedMessage
          UpdateFeedbackKind.CANCELLED -> strings.downloadCancelledMessage
          UpdateFeedbackKind.NO_APK_ASSET -> strings.noApkAssetMessage
        }
      snackbarHostState.showSnackbar(msg)
      viewModel.clearUpdateFeedback()
    }
  }

  LaunchedEffect(uiState.refreshFeedback) {
    val feedback = uiState.refreshFeedback
    if (feedback != null) {
      val message =
        when {
          !feedback.isOnline -> strings.offlineRefreshError
          feedback.newCount > 0 -> strings.newNewsBanner(feedback.newCount)
          else -> strings.updatedUpToDate
        }
      snackbarHostState.showSnackbar(message)
      viewModel.dismissAlert()
    }
  }

  LaunchedEffect(uiState.favoriteFeedback) {
    val fb = uiState.favoriteFeedback
    if (fb != null) {
      snackbarHostState.showSnackbar(
        if (fb.added) strings.addedToFavorites else strings.removedFromFavorites
      )
      viewModel.dismissFavoriteFeedback()
    }
  }

  val onDownloadMedia: (String, Boolean, String) -> Unit = { url, isVideo, title ->
    val result =
      MediaDownloader.enqueueDownload(
        context = context,
        url = url,
        isVideo = isVideo,
        articleTitle = title,
        language = uiState.settings.language,
      )
    coroutineScope.launch {
      snackbarHostState.showSnackbar(
        if (result.isSuccess) strings.downloadStarted else strings.downloadFailed
      )
    }
  }

  val onOpenExternalUrl: (String) -> Unit = { rawUrl ->
    if (!SafeIntentHelper.openExternalUrl(context, rawUrl)) {
      coroutineScope.launch { snackbarHostState.showSnackbar(strings.noAppForLink) }
    }
  }

  val onShareArticle: (NewsArticle) -> Unit = { article ->
    val text = NewsShareHelper.formatArticleForShare(article, strings)
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title.ifBlank { strings.appTitle })
        putExtra(Intent.EXTRA_TEXT, text)
      }
    runCatching {
      context.startActivity(Intent.createChooser(shareIntent, strings.shareAction))
    }.onFailure {
      coroutineScope.launch { snackbarHostState.showSnackbar(strings.noAppForLink) }
    }
  }

  val onCopyArticle: (NewsArticle) -> Unit = { article ->
    val text = NewsShareHelper.formatArticleForShare(article, strings)
    clipboardManager.setText(AnnotatedString(text))
    coroutineScope.launch { snackbarHostState.showSnackbar(strings.copiedMessage) }
  }

  // True Fullscreen Video Dialog (overlaps feed or detail screen)
  uiState.fullscreenVideo?.let { fsRequest ->
    FullscreenVideoDialog(
      request = fsRequest,
      strings = strings,
      onDownload = { onDownloadMedia(fsRequest.url, true, fsRequest.title) },
      onOpenExternal = { onOpenExternalUrl(fsRequest.url) },
      onDismiss = viewModel::closeFullscreenVideo,
    )
  }

  if (showInstallPermissionDialog) {
    InstallPermissionRationaleDialog(
      strings = strings,
      onGrantClick = {
        showInstallPermissionDialog = false
        runCatching {
          installPermissionLauncher.launch(
            GitHubUpdateManager.createUnknownSourcesSettingsIntent(context)
          )
        }.onFailure {
          coroutineScope.launch { snackbarHostState.showSnackbar(strings.noAppForLink) }
        }
      },
      onDismiss = { showInstallPermissionDialog = false },
    )
  }

  if (showUpdateDialog) {
    AppUpdateDialog(
      updateState = appUpdateState,
      strings = strings,
      onStartDownload = { release -> viewModel.startAppUpdateDownload(release) },
      onCancelDownload = viewModel::cancelAppUpdateDownload,
      onInstallApk = requestInstallApk,
      onOpenReleasePage = onOpenExternalUrl,
      onDismiss = viewModel::dismissUpdateDialog,
    )
  }

  if (selectedArticle != null) {
    val currentArticle = selectedArticle!!
    BackHandler {
      viewModel.togglePlayVideo(null)
      selectedArticle = null
    }
    ArticleDetailScreen(
      article = currentArticle,
      isFavorite = currentArticle.id in favoriteIds,
      strings = strings,
      language = uiState.settings.language,
      activeVideoUrl = uiState.activeVideoUrl,
      snackbarHostState = snackbarHostState,
      onTogglePlayVideo = viewModel::togglePlayVideo,
      onOpenFullscreen = { videoUrl ->
        viewModel.openFullscreenVideo(videoUrl, currentArticle.title.ifBlank { currentArticle.sourceName })
      },
      onToggleFavorite = { viewModel.toggleFavorite(currentArticle) },
      onDownloadMedia = onDownloadMedia,
      onOpenExternalUrl = onOpenExternalUrl,
      onShareArticle = { onShareArticle(currentArticle) },
      onCopyArticle = { onCopyArticle(currentArticle) },
      onBack = {
        viewModel.togglePlayVideo(null)
        selectedArticle = null
      },
    )
    return
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 840.dp

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (uiState.loadedFromRemote) strings.liveConnected else strings.offlineSnapshot,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          },
          actions = {
            IconButton(
              onClick = { viewModel.refresh(isManual = true) },
              enabled = !uiState.isRefreshing,
              modifier = Modifier.testTag("refresh_button"),
            ) {
              if (uiState.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Refresh, contentDescription = strings.refreshAction)
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
      },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar {
            NavigationBarItem(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              icon = { Icon(Icons.Default.Newspaper, contentDescription = null) },
              label = { Text(strings.navNews, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_feed_tab"),
            )
            NavigationBarItem(
              selected = selectedTab == 1,
              onClick = {
                viewModel.togglePlayVideo(null)
                selectedTab = 1
              },
              icon = {
                BadgedBox(
                  badge = {
                    if (favoriteArticles.isNotEmpty()) {
                      Badge { Text("${favoriteArticles.size}") }
                    }
                  }
                ) {
                  Icon(Icons.Default.Bookmarks, contentDescription = null)
                }
              },
              label = { Text(strings.navFavorites, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_favorites_tab"),
            )
            NavigationBarItem(
              selected = selectedTab == 2,
              onClick = {
                viewModel.togglePlayVideo(null)
                selectedTab = 2
              },
              icon = { Icon(Icons.Default.Settings, contentDescription = null) },
              label = { Text(strings.navSettings, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_settings_tab"),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (useNavRail) {
          NavigationRail {
            Spacer(modifier = Modifier.height(12.dp))
            NavigationRailItem(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              icon = { Icon(Icons.Default.Newspaper, contentDescription = null) },
              label = { Text(strings.navNews, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_feed_tab"),
            )
            NavigationRailItem(
              selected = selectedTab == 1,
              onClick = {
                viewModel.togglePlayVideo(null)
                selectedTab = 1
              },
              icon = {
                BadgedBox(
                  badge = {
                    if (favoriteArticles.isNotEmpty()) {
                      Badge { Text("${favoriteArticles.size}") }
                    }
                  }
                ) {
                  Icon(Icons.Default.Bookmarks, contentDescription = null)
                }
              },
              label = { Text(strings.navFavorites, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_favorites_tab"),
            )
            NavigationRailItem(
              selected = selectedTab == 2,
              onClick = {
                viewModel.togglePlayVideo(null)
                selectedTab = 2
              },
              icon = { Icon(Icons.Default.Settings, contentDescription = null) },
              label = { Text(strings.navSettings, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("nav_settings_tab"),
            )
          }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          AppUpdateBanner(
            updateState = appUpdateState,
            strings = strings,
            onClick = viewModel::showUpdateDialog,
          )

          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
          ) {
            when (selectedTab) {
              0 ->
                NewsFeedScreen(
                  uiState = uiState,
                  favoriteIds = favoriteIds,
                  favoriteCount = favoriteArticles.size,
                  strings = strings,
                  onRefresh = { viewModel.refresh(isManual = true) },
                  onSearchChange = viewModel::updateSearchQuery,
                  onSelectCategory = viewModel::selectCategory,
                  onSelectSource = viewModel::selectSource,
                  onResetFilters = {
                    viewModel.updateSearchQuery("")
                    viewModel.selectCategory("all")
                    viewModel.selectSource("all")
                  },
                  onTogglePlayVideo = viewModel::togglePlayVideo,
                  onOpenFullscreen = viewModel::openFullscreenVideo,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onOpenFavoritesTab = { selectedTab = 1 },
                  onDownloadMedia = onDownloadMedia,
                  onOpenExternalUrl = onOpenExternalUrl,
                  onShareArticle = onShareArticle,
                  onCopyArticle = onCopyArticle,
                  onArticleClick = {
                    viewModel.togglePlayVideo(null)
                    selectedArticle = it
                  },
                )
              1 ->
                FavoritesScreen(
                  favorites = favoriteArticles,
                  strings = strings,
                  language = uiState.settings.language,
                  activeVideoUrl = uiState.activeVideoUrl,
                  onTogglePlayVideo = viewModel::togglePlayVideo,
                  onOpenFullscreen = viewModel::openFullscreenVideo,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onDownloadMedia = onDownloadMedia,
                  onOpenExternalUrl = onOpenExternalUrl,
                  onShareArticle = onShareArticle,
                  onCopyArticle = onCopyArticle,
                  onArticleClick = {
                    viewModel.togglePlayVideo(null)
                    selectedArticle = it
                  },
                )
              2 ->
                SettingsScreen(
                  uiState = uiState,
                  appUpdateState = appUpdateState,
                  strings = strings,
                  onThemeChange = viewModel::setThemeMode,
                  onLanguageChange = viewModel::setLanguage,
                  onSyncNow = { viewModel.refresh(isManual = true) },
                  onCheckForUpdates = { viewModel.checkForAppUpdates(isManual = true) },
                  onStartUpdateDownload = { release -> viewModel.startAppUpdateDownload(release) },
                  onCancelUpdateDownload = viewModel::cancelAppUpdateDownload,
                  onInstallUpdateApk = requestInstallApk,
                  onOpenExternalUrl = onOpenExternalUrl,
                )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CompactSearchPill(
  query: String,
  hint: String,
  clearDescription: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    modifier = modifier.height(42.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (query.isEmpty()) {
          Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle =
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth().testTag("search_input"),
        )
      }
      if (query.isNotEmpty()) {
        Box(
          modifier =
            Modifier.size(34.dp)
              .clip(CircleShape)
              .clickable { onQueryChange("") }
              .testTag("clear_search_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = clearDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
  uiState: NewsUiState,
  favoriteIds: Set<String>,
  favoriteCount: Int,
  strings: AppStrings,
  onRefresh: () -> Unit,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (String) -> Unit,
  onSelectSource: (String) -> Unit,
  onResetFilters: () -> Unit,
  onTogglePlayVideo: (String?) -> Unit,
  onOpenFullscreen: (String, String) -> Unit,
  onToggleFavorite: (NewsArticle) -> Unit,
  onOpenFavoritesTab: () -> Unit,
  onDownloadMedia: (String, Boolean, String) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onShareArticle: (NewsArticle) -> Unit,
  onCopyArticle: (NewsArticle) -> Unit,
  onArticleClick: (NewsArticle) -> Unit,
) {
  if (uiState.isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val pullToRefreshState = rememberPullToRefreshState()
  val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
  val hasActiveFilter =
    uiState.searchQuery.isNotBlank() ||
      uiState.selectedCategory != "all" ||
      uiState.selectedSource != "all"

  LaunchedEffect(uiState.selectedCategory, uiState.selectedSource) {
    if (listState.firstVisibleItemIndex > 0) {
      listState.scrollToItem(0)
    }
  }

  PullToRefreshBox(
    isRefreshing = uiState.isRefreshing,
    onRefresh = onRefresh,
    state = pullToRefreshState,
    indicator = {
      PullToRefreshDefaults.Indicator(
        state = pullToRefreshState,
        isRefreshing = uiState.isRefreshing,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.align(Alignment.TopCenter),
      )
    },
    modifier = Modifier.fillMaxSize().testTag("feed_pull_to_refresh"),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 76.dp),
      ) {
      // Scrollable header controls: disappears smoothly when scrolling down!
      item(key = "feed_controls", contentType = "feed_controls") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Compact search pill + Refresh + Favorites shortcut
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CompactSearchPill(
              query = uiState.searchQuery,
              hint = strings.searchHint,
              clearDescription = strings.clearSearch,
              onQueryChange = onSearchChange,
              modifier = Modifier.weight(1f),
            )

            FilledTonalButton(
              onClick = onRefresh,
              enabled = !uiState.isRefreshing,
              shape = RoundedCornerShape(999.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
              modifier = Modifier.height(42.dp).testTag("main_refresh_button"),
            ) {
              if (uiState.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(
                  Icons.Default.Refresh,
                  contentDescription = strings.refreshAction,
                  modifier = Modifier.size(17.dp),
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (uiState.isRefreshing) strings.refreshingPill else strings.refreshPill,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
              )
            }

            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier.height(42.dp)
                  .clickable { onOpenFavoritesTab() }
                  .testTag("favorites_shortcut_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  Icons.Filled.Bookmark,
                  contentDescription = strings.navFavorites,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(16.dp),
                )
                if (favoriteCount > 0) {
                  Text(
                    text = "$favoriteCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }
          }

          // Category strip (All / Iraq / Arab & World / International / X)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterChip(
                selected = uiState.selectedCategory == "all",
                onClick = { onSelectCategory("all") },
                label = {
                  Text(
                    "${strings.allCategories} (${uiState.articles.size})",
                    style = MaterialTheme.typography.labelMedium,
                  )
                },
              )
            }
            items(uiState.availableCategories, key = { it }) { cat ->
              val catCount = uiState.categoryCounts[cat] ?: 0
              val catLabel = strings.localizeCategory(cat)
              FilterChip(
                selected = uiState.selectedCategory == cat,
                onClick = { onSelectCategory(cat) },
                label = { Text("$catLabel ($catCount)", style = MaterialTheme.typography.labelMedium) },
              )
            }
          }

          // Source filter strip scoped to active category
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterChip(
                selected = uiState.selectedSource == "all",
                onClick = { onSelectSource("all") },
                label = { Text(strings.allSources, style = MaterialTheme.typography.labelSmall) },
              )
            }
            items(uiState.filteredSources, key = { it.username }) { src ->
              val srcCount = uiState.sourceCounts[src.username] ?: 0
              FilterChip(
                selected = uiState.selectedSource == src.username,
                onClick = { onSelectSource(src.username) },
                label = { Text("${src.name} ($srcCount)", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }

      if (uiState.filteredArticles.isEmpty()) {
        item(key = "empty_news", contentType = "empty_state") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = if (hasActiveFilter) Icons.Default.SearchOff else Icons.Default.Newspaper,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = uiState.errorMessage ?: strings.emptyNews,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            if (hasActiveFilter) {
              FilledTonalButton(
                onClick = onResetFilters,
                modifier = Modifier.testTag("reset_filters_btn"),
              ) {
                Text(strings.resetFilters, style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      } else {
        items(
          items = uiState.filteredArticles,
          key = { it.id },
          contentType = { "news_card" },
        ) { article ->
          NewsArticleCard(
            article = article,
            isFavorite = article.id in favoriteIds,
            strings = strings,
            language = uiState.settings.language,
            activeVideoUrl = uiState.activeVideoUrl,
            onTogglePlayVideo = onTogglePlayVideo,
            onOpenFullscreen = { videoUrl ->
              onOpenFullscreen(videoUrl, article.title.ifBlank { article.sourceName })
            },
            onToggleFavorite = { onToggleFavorite(article) },
            onDownloadMedia = onDownloadMedia,
            onOpenExternalUrl = onOpenExternalUrl,
            onShareArticle = { onShareArticle(article) },
            onCopyArticle = { onCopyArticle(article) },
            onClick = { onArticleClick(article) },
          )
        }
      }
    }

    AnimatedVisibility(
      visible = showScrollToTop,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    ) {
      SmallFloatingActionButton(
        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag("scroll_to_top_fab"),
      ) {
        Icon(
          Icons.Default.ArrowUpward,
          contentDescription = strings.scrollToTop,
        )
      }
    }
    }
  }
}

@Composable
fun FavoritesScreen(
  favorites: List<NewsArticle>,
  strings: AppStrings,
  language: AppLanguage,
  activeVideoUrl: String?,
  onTogglePlayVideo: (String?) -> Unit,
  onOpenFullscreen: (String, String) -> Unit,
  onToggleFavorite: (NewsArticle) -> Unit,
  onDownloadMedia: (String, Boolean, String) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onShareArticle: (NewsArticle) -> Unit,
  onCopyArticle: (NewsArticle) -> Unit,
  onArticleClick: (NewsArticle) -> Unit,
) {
  var favQuery by remember { mutableStateOf("") }
  val filtered = remember(favorites, favQuery) {
    val q = favQuery.trim().lowercase()
    if (q.isEmpty()) {
      favorites
    } else {
      favorites.filter {
        it.title.lowercase().contains(q) ||
          it.content.lowercase().contains(q) ||
          it.sourceName.lowercase().contains(q)
      }
    }
  }

  if (favorites.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          Icons.Outlined.BookmarkBorder,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(52.dp),
        )
        Text(
          text = strings.emptyFavoritesTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = strings.emptyFavoritesSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
    return
  }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 10.dp, bottom = 76.dp),
    ) {
      item(key = "fav_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${strings.navFavorites} (${favorites.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.weight(1f))
          CompactSearchPill(
            query = favQuery,
            hint = strings.searchHint,
            clearDescription = strings.clearSearch,
            onQueryChange = { favQuery = it },
            modifier = Modifier.widthIn(min = 140.dp, max = 220.dp),
          )
        }
      }

      if (filtered.isEmpty()) {
        item(key = "fav_empty_search") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.SearchOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = strings.emptySearchFavorites,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = { favQuery = "" }) {
              Text(strings.clearSearch, style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      } else {
        items(filtered, key = { it.id }, contentType = { "news_card" }) { article ->
          NewsArticleCard(
            article = article,
            isFavorite = true,
            strings = strings,
            language = language,
            activeVideoUrl = activeVideoUrl,
            onTogglePlayVideo = onTogglePlayVideo,
            onOpenFullscreen = { videoUrl ->
              onOpenFullscreen(videoUrl, article.title.ifBlank { article.sourceName })
            },
            onToggleFavorite = { onToggleFavorite(article) },
            onDownloadMedia = onDownloadMedia,
            onOpenExternalUrl = onOpenExternalUrl,
            onShareArticle = { onShareArticle(article) },
            onCopyArticle = { onCopyArticle(article) },
            onClick = { onArticleClick(article) },
          )
        }
      }
    }

    AnimatedVisibility(
      visible = showScrollToTop,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    ) {
      SmallFloatingActionButton(
        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag("favorites_scroll_to_top_fab"),
      ) {
        Icon(
          Icons.Default.ArrowUpward,
          contentDescription = strings.scrollToTop,
        )
      }
    }
  }
}

@Composable
fun NewsArticleCard(
  article: NewsArticle,
  isFavorite: Boolean,
  strings: AppStrings,
  language: AppLanguage,
  activeVideoUrl: String?,
  onTogglePlayVideo: (String?) -> Unit,
  onOpenFullscreen: (String) -> Unit,
  onToggleFavorite: () -> Unit,
  onDownloadMedia: (String, Boolean, String) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onShareArticle: () -> Unit,
  onCopyArticle: () -> Unit,
  onClick: () -> Unit,
) {
  val videos = article.videos
  val photos = article.displayImages
  val formattedDate = remember(article.publishedAt, language) {
    DateFormatter.formatBaghdadDateTime(article.publishedAt, language)
  }

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("news_item_card_${article.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Top metadata row: Source badge + Edited badge + Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = article.sourceName,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          if (article.isEdited) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = strings.editedBadge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = formattedDate,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }

      // Headline
      Text(
        text = article.title.ifBlank { strings.mediaOnlyPost },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )

      // Attached Video (with inline player + Fullscreen button + Download button)
      if (videos.isNotEmpty()) {
        val primaryVideo = videos.first()
        InlineVideoAttachment(
          video = primaryVideo,
          videoCount = videos.size,
          isPlaying = activeVideoUrl == primaryVideo.url,
          strings = strings,
          onTogglePlay = { onTogglePlayVideo(primaryVideo.url) },
          onFullscreen = { onOpenFullscreen(primaryVideo.url) },
          onDownload = { onDownloadMedia(primaryVideo.url, true, article.title) },
          onOpenExternal = { onOpenExternalUrl(primaryVideo.url) },
        )
      }

      // Attached Photo (with Download button)
      if (photos.isNotEmpty() && (videos.isEmpty() || photos.first() != videos.first().thumbnail)) {
        val primaryPhoto = photos.first()
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
          AsyncImage(
            model = primaryPhoto,
            contentDescription = article.title.ifBlank { strings.mediaOnlyPost },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.align(Alignment.BottomStart)
                .padding(10.dp)
                .clickable { onDownloadMedia(primaryPhoto, false, article.title) }
                .testTag("download_photo_btn_${article.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Download,
                contentDescription = strings.downloadImage,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = strings.downloadImage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          if (photos.size > 1) {
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            ) {
              Text(
                text = "+${photos.size - 1}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      // Body excerpt
      if (article.content.isNotBlank() && article.content != article.title) {
        Text(
          text = article.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      // Footer row with Bookmark, Copy, Share, and Details
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.localizeCategory(article.category),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Favorite / Bookmark Toggle Pill
          Surface(
            color =
              if (isFavorite) MaterialTheme.colorScheme.tertiaryContainer
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.clickable { onToggleFavorite() }.testTag("favorite_btn_${article.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector =
                  if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isFavorite) strings.savedFavorite else strings.saveFavorite,
                tint =
                  if (isFavorite) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (isFavorite) strings.savedFavorite else strings.saveFavorite,
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (isFavorite) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.clickable { onCopyArticle() }.testTag("copy_article_btn_${article.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.ContentCopy,
                contentDescription = strings.copyAction,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = strings.copyAction,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.clickable { onShareArticle() }.testTag("share_article_btn_${article.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Share,
                contentDescription = strings.shareAction,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = strings.shareAction,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun InlineVideoAttachment(
  video: MediaAttachment,
  videoCount: Int,
  isPlaying: Boolean,
  strings: AppStrings,
  onTogglePlay: () -> Unit,
  onFullscreen: () -> Unit,
  onDownload: () -> Unit,
  onOpenExternal: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(230.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.Black),
  ) {
    if (isPlaying) {
      var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
      var hasPlaybackError by remember(video.url, isPlaying) { mutableStateOf(false) }
      var isBuffering by remember(video.url, isPlaying) { mutableStateOf(true) }

      DisposableEffect(video.url) {
        onDispose {
          try {
            videoViewRef?.stopPlayback()
          } catch (_: Exception) {}
        }
      }
      if (!hasPlaybackError) {
        AndroidView(
          factory = { ctx ->
            VideoView(ctx).apply {
              val controller = MediaController(ctx)
              controller.setAnchorView(this)
              setMediaController(controller)
              setVideoURI(Uri.parse(video.url))
              setOnPreparedListener { mp ->
                isBuffering = false
                mp.isLooping = false
                start()
              }
              setOnErrorListener { _, _, _ ->
                isBuffering = false
                hasPlaybackError = true
                true
              }
              videoViewRef = this
            }
          },
          modifier = Modifier.fillMaxSize().align(Alignment.Center),
        )
        if (isBuffering) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(30.dp),
            )
            Text(
              text = strings.loadingVideo,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = strings.videoPlayError,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
              onClick = onOpenExternal,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text(strings.openVideoExternal, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = onDownload,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text(
                strings.downloadVideo,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
            }
          }
        }
      }

      // Top-start: Prominent Fullscreen button during inline playback
      if (!hasPlaybackError) {
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.align(Alignment.TopStart)
              .padding(10.dp)
              .clickable { onFullscreen() }
              .testTag("fullscreen_video_playing_btn"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.Fullscreen,
              contentDescription = strings.fullscreenVideo,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = strings.fullscreenVideo,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = CircleShape,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(10.dp)
            .size(36.dp)
            .clickable { onTogglePlay() },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Default.Close,
            contentDescription = strings.closeVideo,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    } else {
      Box(
        modifier = Modifier.fillMaxSize().clickable { onTogglePlay() },
        contentAlignment = Alignment.Center,
      ) {
        if (video.thumbnail.isNotBlank()) {
          AsyncImage(
            model = video.thumbnail,
            contentDescription = strings.videoBadge,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
        )
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = CircleShape,
          shadowElevation = 4.dp,
          modifier = Modifier.size(58.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.PlayArrow,
              contentDescription = strings.playVideo,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.Videocam,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = if (videoCount > 1) "${strings.videoBadge} ($videoCount)" else strings.videoBadge,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }

        // Top-end: Fullscreen button right on video thumbnail!
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(10.dp)
              .clickable { onFullscreen() }
              .testTag("fullscreen_video_btn"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.Fullscreen,
              contentDescription = strings.fullscreenVideo,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = strings.fullscreenVideo,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }

        // Bottom-start: Download Video button
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.align(Alignment.BottomStart)
              .padding(10.dp)
              .clickable { onDownload() },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.Download,
              contentDescription = strings.downloadVideo,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = strings.downloadVideo,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        // Bottom-end: External player
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.align(Alignment.BottomEnd)
              .padding(10.dp)
              .clickable { onOpenExternal() },
        ) {
          Text(
            text = strings.openVideoExternal,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
    }
  }
}

@Composable
fun FullscreenVideoDialog(
  request: FullscreenVideoRequest,
  strings: AppStrings,
  onDownload: () -> Unit,
  onOpenExternal: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    BackHandler { onDismiss() }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var hasPlaybackError by remember(request.url) { mutableStateOf(false) }
    var isBuffering by remember(request.url) { mutableStateOf(true) }

    DisposableEffect(request.url) {
      onDispose {
        try {
          videoViewRef?.stopPlayback()
        } catch (_: Exception) {}
      }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding()) {
      if (!hasPlaybackError) {
        AndroidView(
          factory = { ctx ->
            VideoView(ctx).apply {
              val controller = MediaController(ctx)
              controller.setAnchorView(this)
              setMediaController(controller)
              setVideoURI(Uri.parse(request.url))
              setOnPreparedListener { mp ->
                isBuffering = false
                mp.isLooping = false
                start()
              }
              setOnErrorListener { _, _, _ ->
                isBuffering = false
                hasPlaybackError = true
                true
              }
              videoViewRef = this
            }
          },
          modifier = Modifier.fillMaxSize().align(Alignment.Center),
        )
        if (isBuffering) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Text(
              text = strings.loadingVideo,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = strings.videoPlayError,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onOpenExternal) {
              Text(strings.openVideoExternal)
            }
            OutlinedButton(onClick = onDownload) {
              Text(strings.downloadVideo, color = Color.White)
            }
          }
        }
      }

      // Top overlay bar with title, external player, download, and exit fullscreen
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = request.title,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(end = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onOpenExternal,
            modifier = Modifier.testTag("fullscreen_external_btn"),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = strings.openVideoExternal,
              tint = Color.White,
            )
          }
          IconButton(
            onClick = onDownload,
            modifier = Modifier.testTag("fullscreen_download_btn"),
          ) {
            Icon(
              Icons.Default.Download,
              contentDescription = strings.downloadVideo,
              tint = Color.White,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("fullscreen_exit_btn"),
          ) {
            Icon(
              Icons.Default.FullscreenExit,
              contentDescription = strings.exitFullscreen,
              tint = Color.White,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
  article: NewsArticle,
  isFavorite: Boolean,
  strings: AppStrings,
  language: AppLanguage,
  activeVideoUrl: String?,
  snackbarHostState: SnackbarHostState,
  onTogglePlayVideo: (String?) -> Unit,
  onOpenFullscreen: (String) -> Unit,
  onToggleFavorite: () -> Unit,
  onDownloadMedia: (String, Boolean, String) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onShareArticle: () -> Unit,
  onCopyArticle: () -> Unit,
  onBack: () -> Unit,
) {
  val formattedDate = remember(article.publishedAt, language) {
    DateFormatter.formatBaghdadDateTime(article.publishedAt, language)
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = article.sourceName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backAction)
          }
        },
        actions = {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.testTag("detail_favorite_button"),
          ) {
            Icon(
              imageVector =
                if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription = if (isFavorite) strings.savedFavorite else strings.saveFavorite,
              tint =
                if (isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onCopyArticle,
            modifier = Modifier.testTag("detail_copy_button"),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = strings.copyAction)
          }
          IconButton(
            onClick = onShareArticle,
            modifier = Modifier.testTag("detail_share_button"),
          ) {
            Icon(Icons.Default.Share, contentDescription = strings.shareAction)
          }
        },
      )
    },
  ) { innerPadding ->
    val extraLinks = remember(article.links, article.originalUrl) {
      article.links
        .map { it.trim() }
        .filter { it.startsWith("http") && it != article.originalUrl.trim() }
        .distinct()
    }

    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 720.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AssistChip(
            onClick = {},
            label = {
              Text(
                strings.localizeCategory(article.category.ifBlank { strings.navNews }),
                style = MaterialTheme.typography.labelMedium,
              )
            },
          )
          if (article.isEdited) {
            AssistChip(
              onClick = {},
              label = { Text(strings.editedBadge, style = MaterialTheme.typography.labelMedium) },
            )
          }
        }

        Text(
          text = article.title.ifBlank { strings.mediaOnlyPost },
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = "${strings.publishedAtLabel}: $formattedDate",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // All attached videos with inline play, Fullscreen, and Download
        article.videos.forEach { video ->
          InlineVideoAttachment(
            video = video,
            videoCount = 1,
            isPlaying = activeVideoUrl == video.url,
            strings = strings,
            onTogglePlay = { onTogglePlayVideo(video.url) },
            onFullscreen = { onOpenFullscreen(video.url) },
            onDownload = { onDownloadMedia(video.url, true, article.title) },
            onOpenExternal = { onOpenExternalUrl(video.url) },
          )
        }

        // All attached photos with download button overlay
        article.displayImages.forEach { imageUrl ->
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
            AsyncImage(
              model = imageUrl,
              contentDescription = article.title.ifBlank { strings.mediaOnlyPost },
              contentScale = ContentScale.FillWidth,
              modifier = Modifier.fillMaxWidth(),
            )
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier.align(Alignment.BottomStart)
                  .padding(10.dp)
                  .clickable { onDownloadMedia(imageUrl, false, article.title) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Default.Download,
                  contentDescription = strings.downloadImage,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = strings.downloadImage,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        Text(
          text = article.content.ifBlank { strings.noAdditionalText },
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )

        if (extraLinks.isNotEmpty()) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = strings.mentionedLinksTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              extraLinks.forEach { linkUrl ->
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth().clickable { onOpenExternalUrl(linkUrl) },
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      Icons.Default.Link,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = linkUrl,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Icon(
                      Icons.AutoMirrored.Filled.OpenInNew,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalButton(
          onClick = onToggleFavorite,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("detail_fav_full_btn"),
        ) {
          Icon(
            imageVector =
              if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            if (isFavorite) strings.savedFavorite else strings.saveFavorite,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
          )
        }
        FilledTonalButton(
          onClick = onShareArticle,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("detail_share_full_btn"),
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(strings.shareAction, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        OutlinedButton(
          onClick = onCopyArticle,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("detail_copy_full_btn"),
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(strings.copyAction, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
      }

      Button(
        onClick = { onOpenExternalUrl(article.originalUrl) },
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .testTag("open_original_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(strings.openOriginal, style = MaterialTheme.typography.labelLarge)
      }

      Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun SettingsScreen(
  uiState: NewsUiState,
  appUpdateState: AppUpdateStage,
  strings: AppStrings,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onSyncNow: () -> Unit,
  onCheckForUpdates: () -> Unit,
  onStartUpdateDownload: (com.example.data.GitHubReleaseInfo) -> Unit,
  onCancelUpdateDownload: () -> Unit,
  onInstallUpdateApk: (File) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
) {
  val settings = uiState.settings
  val formattedSync = remember(uiState.lastSuccessfulUpdate, settings.language) {
    DateFormatter.formatBaghdadDateTime(uiState.lastSuccessfulUpdate, settings.language)
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .widthIn(max = 680.dp)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = strings.settingsTitle,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )

    // 1. Theme Selector Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = strings.appearanceSection,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemeOptionChip(
            label = strings.themeSystem,
            icon = Icons.Default.SettingsBrightness,
            selected = settings.themeMode == ThemeMode.SYSTEM,
            onClick = { onThemeChange(ThemeMode.SYSTEM) },
            modifier = Modifier.weight(1f).testTag("theme_system_chip"),
          )
          ThemeOptionChip(
            label = strings.themeLight,
            icon = Icons.Default.LightMode,
            selected = settings.themeMode == ThemeMode.LIGHT,
            onClick = { onThemeChange(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f).testTag("theme_light_chip"),
          )
          ThemeOptionChip(
            label = strings.themeDark,
            icon = Icons.Default.DarkMode,
            selected = settings.themeMode == ThemeMode.DARK,
            onClick = { onThemeChange(ThemeMode.DARK) },
            modifier = Modifier.weight(1f).testTag("theme_dark_chip"),
          )
        }
      }
    }

    // 2. Language Selector Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = strings.languageSection,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilterChip(
            selected = settings.language == AppLanguage.ARABIC,
            onClick = { onLanguageChange(AppLanguage.ARABIC) },
            label = {
              Text(
                strings.languageArabic,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 6.dp),
              )
            },
            modifier = Modifier.weight(1f).testTag("lang_ar_chip"),
          )
          FilterChip(
            selected = settings.language == AppLanguage.ENGLISH,
            onClick = { onLanguageChange(AppLanguage.ENGLISH) },
            label = {
              Text(
                strings.languageEnglish,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 6.dp),
              )
            },
            modifier = Modifier.weight(1f).testTag("lang_en_chip"),
          )
        }
      }
    }

    // 3. GitHub App Updates Card
    GitHubUpdateSettingsCard(
      updateState = appUpdateState,
      strings = strings,
      onCheckForUpdates = onCheckForUpdates,
      onStartDownload = onStartUpdateDownload,
      onCancelDownload = onCancelUpdateDownload,
      onInstallApk = onInstallUpdateApk,
      onOpenRepo = { onOpenExternalUrl(GitHubUpdateManager.GITHUB_REPO_URL) },
      onOpenReleasePage = onOpenExternalUrl,
    )

    // 4. Clean Data Status & Sync Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.dataStatusSection,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.lastSyncLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(formattedSync, style = MaterialTheme.typography.labelLarge)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.storedNewsCountLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${uiState.articles.size}", style = MaterialTheme.typography.labelLarge)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.activeSourcesCountLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${uiState.sources.size}", style = MaterialTheme.typography.labelLarge)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.appVersionLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
          onClick = onSyncNow,
          enabled = !uiState.isRefreshing,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_now_button"),
        ) {
          if (uiState.isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (uiState.isRefreshing) strings.refreshingPill else strings.syncNowButton,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun ThemeOptionChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    modifier = modifier.height(44.dp),
  )
}
