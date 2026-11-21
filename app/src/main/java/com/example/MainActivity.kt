package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.MediaAttachment
import com.example.data.NewsArticle
import com.example.data.ThemeMode
import com.example.notifications.NewsNotificationHelper
import com.example.ui.AppStrings
import com.example.ui.DateFormatter
import com.example.ui.FullscreenVideoRequest
import com.example.ui.NewsUiState
import com.example.ui.NewsViewModel
import com.example.ui.stringsFor
import com.example.ui.theme.MyApplicationTheme
import com.example.util.MediaDownloader
import com.example.util.NewsShareHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val newsViewModel: NewsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NewsNotificationHelper.schedulePeriodicSync(this)
    newsViewModel.openArticleFromNotification(
      intent?.getStringExtra(NewsNotificationHelper.EXTRA_ARTICLE_ID)
    )

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
          IraqNewsApp(
            viewModel = newsViewModel,
            uiState = uiState,
            favoriteArticles = favoriteArticles,
            favoriteIds = favoriteIds,
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    newsViewModel.openArticleFromNotification(
      intent.getStringExtra(NewsNotificationHelper.EXTRA_ARTICLE_ID)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IraqNewsApp(
  viewModel: NewsViewModel,
  uiState: NewsUiState,
  favoriteArticles: List<NewsArticle>,
  favoriteIds: Set<String>,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  val strings = remember(uiState.settings.language) { stringsFor(uiState.settings.language) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  var notificationGranted by remember {
    mutableStateOf(NewsNotificationHelper.hasNotificationPermission(context))
  }
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notificationGranted = granted || NewsNotificationHelper.hasNotificationPermission(context)
    }

  LaunchedEffect(Unit) {
    NewsNotificationHelper.ensureChannel(context, uiState.settings.language)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !uiState.settings.muteNotifications &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  LaunchedEffect(uiState.pendingArticleId, uiState.articles, favoriteArticles) {
    val targetId = uiState.pendingArticleId
    if (!targetId.isNullOrBlank()) {
      val match =
        uiState.articles.firstOrNull { it.id == targetId }
          ?: favoriteArticles.firstOrNull { it.id == targetId }
      if (match != null) {
        selectedArticle = match
        selectedTab = 0
        viewModel.consumePendingArticle()
      } else if (uiState.articles.isNotEmpty()) {
        viewModel.consumePendingArticle()
      }
    }
  }

  LaunchedEffect(uiState.newArticlesAlertCount) {
    val count = uiState.newArticlesAlertCount
    if (count != null && count > 0) {
      snackbarHostState.showSnackbar(strings.newNewsBanner(count))
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

  val onShareArticle: (NewsArticle) -> Unit = { article ->
    val text = NewsShareHelper.formatArticleForShare(article, strings)
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title.ifBlank { strings.appTitle })
        putExtra(Intent.EXTRA_TEXT, text)
      }
    context.startActivity(Intent.createChooser(shareIntent, strings.shareAction))
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
      onDismiss = viewModel::closeFullscreenVideo,
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
      onShareArticle = { onShareArticle(currentArticle) },
      onCopyArticle = { onCopyArticle(currentArticle) },
      onBack = {
        viewModel.togglePlayVideo(null)
        selectedArticle = null
      },
    )
    return
  }

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
            onClick = { viewModel.refresh() },
            modifier = Modifier.testTag("refresh_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = strings.refreshAction)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    bottomBar = {
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
            notificationGranted = NewsNotificationHelper.hasNotificationPermission(context)
            selectedTab = 2
          },
          icon = { Icon(Icons.Default.Settings, contentDescription = null) },
          label = { Text(strings.navSettings, style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.testTag("nav_settings_tab"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (selectedTab) {
        0 ->
          NewsFeedScreen(
            uiState = uiState,
            favoriteIds = favoriteIds,
            favoriteCount = favoriteArticles.size,
            strings = strings,
            onRefresh = viewModel::refresh,
            onSearchChange = viewModel::updateSearchQuery,
            onSelectCategory = viewModel::selectCategory,
            onSelectSource = viewModel::selectSource,
            onTogglePlayVideo = viewModel::togglePlayVideo,
            onOpenFullscreen = viewModel::openFullscreenVideo,
            onToggleFavorite = viewModel::toggleFavorite,
            onOpenFavoritesTab = { selectedTab = 1 },
            onDownloadMedia = onDownloadMedia,
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
            strings = strings,
            notificationPermissionGranted = notificationGranted,
            onRequestSystemNotificationPermission = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val deniedBefore =
                  ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                  ) != PackageManager.PERMISSION_GRANTED
                if (deniedBefore) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  val intent =
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                      putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                  context.startActivity(intent)
                }
              } else {
                val intent =
                  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                  }
                context.startActivity(intent)
              }
            },
            onMuteChange = { muted ->
              viewModel.setMuteNotifications(muted)
              if (
                !muted &&
                  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                  !NewsNotificationHelper.hasNotificationPermission(context)
              ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
            },
            onThemeChange = viewModel::setThemeMode,
            onLanguageChange = viewModel::setLanguage,
            onSyncNow = viewModel::refresh,
          )
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
        Icon(
          Icons.Default.Close,
          contentDescription = clearDescription,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp).clickable { onQueryChange("") },
        )
      }
    }
  }
}

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
  onTogglePlayVideo: (String?) -> Unit,
  onOpenFullscreen: (String, String) -> Unit,
  onToggleFavorite: (NewsArticle) -> Unit,
  onOpenFavoritesTab: () -> Unit,
  onDownloadMedia: (String, Boolean, String) -> Unit,
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

  LazyColumn(
    modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
  ) {
    // Scrollable header controls: disappears smoothly when scrolling down!
    item(key = "feed_controls") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Compact narrower search pill + Refresh + Favorites shortcut
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
            modifier = Modifier.weight(1f).widthIn(max = 220.dp),
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
            val catCount = remember(uiState.articles, cat) {
              uiState.articles.count { it.category == cat }
            }
            FilterChip(
              selected = uiState.selectedCategory == cat,
              onClick = { onSelectCategory(cat) },
              label = { Text("$cat ($catCount)", style = MaterialTheme.typography.labelMedium) },
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
            val srcCount = remember(uiState.articles, src.username) {
              uiState.articles.count { it.sourceUsername == src.username }
            }
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
      item(key = "empty_news") {
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = uiState.errorMessage ?: strings.emptyNews,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      items(uiState.filteredArticles, key = { it.id }) { article ->
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
          onShareArticle = { onShareArticle(article) },
          onCopyArticle = { onCopyArticle(article) },
          onClick = { onArticleClick(article) },
        )
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

  LazyColumn(
    modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
  ) {
    item(key = "fav_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${strings.navFavorites} (${favorites.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        CompactSearchPill(
          query = favQuery,
          hint = strings.searchHint,
          clearDescription = strings.clearSearch,
          onQueryChange = { favQuery = it },
          modifier = Modifier.widthIn(min = 140.dp, max = 200.dp),
        )
      }
    }

    items(filtered, key = { it.id }) { article ->
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
        onShareArticle = { onShareArticle(article) },
        onCopyArticle = { onCopyArticle(article) },
        onClick = { onArticleClick(article) },
      )
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
  onShareArticle: () -> Unit,
  onCopyArticle: () -> Unit,
  onClick: () -> Unit,
) {
  val context = LocalContext.current
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
          onOpenExternal = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primaryVideo.url)))
          },
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
                text = "+${photos.size}",
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = article.category,
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
      DisposableEffect(video.url) {
        onDispose {
          try {
            videoViewRef?.stopPlayback()
          } catch (_: Exception) {}
        }
      }
      AndroidView(
        factory = { ctx ->
          VideoView(ctx).apply {
            val controller = MediaController(ctx)
            controller.setAnchorView(this)
            setMediaController(controller)
            setVideoURI(Uri.parse(video.url))
            setOnPreparedListener { mp ->
              mp.isLooping = false
              start()
            }
            videoViewRef = this
          }
        },
        modifier = Modifier.fillMaxSize().align(Alignment.Center),
      )

      // Top-start: Prominent Fullscreen button during inline playback
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

    DisposableEffect(request.url) {
      onDispose {
        try {
          videoViewRef?.stopPlayback()
        } catch (_: Exception) {}
      }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      AndroidView(
        factory = { ctx ->
          VideoView(ctx).apply {
            val controller = MediaController(ctx)
            controller.setAnchorView(this)
            setMediaController(controller)
            setVideoURI(Uri.parse(request.url))
            setOnPreparedListener { mp ->
              mp.isLooping = false
              start()
            }
            videoViewRef = this
          }
        },
        modifier = Modifier.fillMaxSize().align(Alignment.Center),
      )

      // Top overlay bar with title, download, and exit fullscreen
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
  onShareArticle: () -> Unit,
  onCopyArticle: () -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
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
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
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
          label = { Text(article.category.ifBlank { strings.navNews }, style = MaterialTheme.typography.labelMedium) },
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
          onOpenExternal = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
          },
        )
      }

      // All attached photos with download button overlay
      article.displayImages.forEach { imageUrl ->
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
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
        onClick = {
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl)))
        },
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

@Composable
fun SettingsScreen(
  uiState: NewsUiState,
  strings: AppStrings,
  notificationPermissionGranted: Boolean,
  onRequestSystemNotificationPermission: () -> Unit,
  onMuteChange: (Boolean) -> Unit,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onSyncNow: () -> Unit,
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

    // 1. Mute Notifications + OS Notification Center Permission Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.notificationsSection,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector =
                if (settings.muteNotifications) Icons.Default.NotificationsOff
                else Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = strings.muteNotificationsTitle,
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text =
                  if (settings.muteNotifications) strings.muteNotificationsDescOn
                  else strings.muteNotificationsDescOff,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = settings.muteNotifications,
            onCheckedChange = onMuteChange,
            modifier = Modifier.testTag("mute_notifications_switch"),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
          text =
            if (notificationPermissionGranted) strings.systemNotifPermissionGranted
            else strings.systemNotifPermissionDenied,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (notificationPermissionGranted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
        if (!notificationPermissionGranted) {
          FilledTonalButton(
            onClick = onRequestSystemNotificationPermission,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("enable_notif_perm_btn"),
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.enablePermissionBtn, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    // 2. Theme Selector Card
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

    // 3. Language Selector Card
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
        OutlinedButton(
          onClick = onSyncNow,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_now_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.syncNowButton, style = MaterialTheme.typography.labelLarge)
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
