package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.MediaAttachment
import com.example.data.NewsArticle
import com.example.data.ThemeMode
import com.example.ui.AppStrings
import com.example.ui.DateFormatter
import com.example.ui.NewsUiState
import com.example.ui.NewsViewModel
import com.example.ui.stringsFor
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: NewsViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val layoutDirection =
        if (uiState.settings.language == AppLanguage.ARABIC) {
          LayoutDirection.Rtl
        } else {
          LayoutDirection.Ltr
        }

      MyApplicationTheme(themeMode = uiState.settings.themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
          IraqNewsHubApp(viewModel = viewModel, uiState = uiState)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IraqNewsHubApp(viewModel: NewsViewModel, uiState: NewsUiState) {
  val strings = remember(uiState.settings.language) { stringsFor(uiState.settings.language) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.newArticlesAlertCount) {
    val count = uiState.newArticlesAlertCount
    if (count != null && count > 0) {
      snackbarHostState.showSnackbar(strings.newNewsBanner(count))
      viewModel.dismissAlert()
    }
  }

  if (selectedArticle != null) {
    BackHandler {
      viewModel.togglePlayVideo(null)
      selectedArticle = null
    }
    ArticleDetailScreen(
      article = selectedArticle!!,
      strings = strings,
      language = uiState.settings.language,
      activeVideoUrl = uiState.activeVideoUrl,
      onTogglePlayVideo = viewModel::togglePlayVideo,
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
            strings = strings,
            onSearchChange = viewModel::updateSearchQuery,
            onSelectSource = viewModel::selectSource,
            onTogglePlayVideo = viewModel::togglePlayVideo,
            onArticleClick = {
              viewModel.togglePlayVideo(null)
              selectedArticle = it
            },
          )
        1 ->
          SettingsScreen(
            uiState = uiState,
            strings = strings,
            onMuteChange = viewModel::setMuteNotifications,
            onThemeChange = viewModel::setThemeMode,
            onLanguageChange = viewModel::setLanguage,
            onSyncNow = viewModel::refresh,
          )
      }
    }
  }
}

@Composable
fun NewsFeedScreen(
  uiState: NewsUiState,
  strings: AppStrings,
  onSearchChange: (String) -> Unit,
  onSelectSource: (String) -> Unit,
  onTogglePlayVideo: (String?) -> Unit,
  onArticleClick: (NewsArticle) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .widthIn(max = 720.dp)
        .padding(horizontal = 16.dp),
  ) {
    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text(strings.searchHint, style = MaterialTheme.typography.bodyMedium) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      textStyle = MaterialTheme.typography.bodyMedium,
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 10.dp)
          .testTag("search_input"),
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 10.dp),
    ) {
      item {
        FilterChip(
          selected = uiState.selectedSource == "all",
          onClick = { onSelectSource("all") },
          label = {
            Text(
              "${strings.allSources} (${uiState.articles.size})",
              style = MaterialTheme.typography.labelMedium,
            )
          },
        )
      }
      items(uiState.sources, key = { it.username }) { src ->
        FilterChip(
          selected = uiState.selectedSource == src.username,
          onClick = { onSelectSource(src.username) },
          label = { Text(src.name, style = MaterialTheme.typography.labelMedium) },
        )
      }
    }

    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (uiState.filteredArticles.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = uiState.errorMessage ?: strings.emptyNews,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
      ) {
        items(uiState.filteredArticles, key = { it.id }) { article ->
          NewsArticleCard(
            article = article,
            strings = strings,
            language = uiState.settings.language,
            activeVideoUrl = uiState.activeVideoUrl,
            onTogglePlayVideo = onTogglePlayVideo,
            onClick = { onArticleClick(article) },
          )
        }
      }
    }
  }
}

@Composable
fun NewsArticleCard(
  article: NewsArticle,
  strings: AppStrings,
  language: AppLanguage,
  activeVideoUrl: String?,
  onTogglePlayVideo: (String?) -> Unit,
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
      // Top metadata row: Source badge + Date
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

      // Attached Video (priority inline player) and/or Photo
      if (videos.isNotEmpty()) {
        val primaryVideo = videos.first()
        InlineVideoAttachment(
          video = primaryVideo,
          videoCount = videos.size,
          isPlaying = activeVideoUrl == primaryVideo.url,
          strings = strings,
          onTogglePlay = { onTogglePlayVideo(primaryVideo.url) },
          onOpenExternal = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primaryVideo.url)))
          },
        )
      }

      if (photos.isNotEmpty() && (videos.isEmpty() || photos.first() != videos.first().thumbnail)) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(200.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
          AsyncImage(
            model = photos.first(),
            contentDescription = article.title.ifBlank { strings.mediaOnlyPost },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
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

      // Footer row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = article.category,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.readMore,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
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
  onOpenExternal: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(210.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
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
        modifier = Modifier.fillMaxSize(),
      )
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = CircleShape,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
  article: NewsArticle,
  strings: AppStrings,
  language: AppLanguage,
  activeVideoUrl: String?,
  onTogglePlayVideo: (String?) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val formattedDate = remember(article.publishedAt, language) {
    DateFormatter.formatBaghdadDateTime(article.publishedAt, language)
  }

  Scaffold(
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
      )
    }
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

      // All attached videos
      article.videos.forEach { video ->
        InlineVideoAttachment(
          video = video,
          videoCount = 1,
          isPlaying = activeVideoUrl == video.url,
          strings = strings,
          onTogglePlay = { onTogglePlayVideo(video.url) },
          onOpenExternal = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
          },
        )
      }

      // All attached photos
      article.displayImages.forEach { imageUrl ->
        AsyncImage(
          model = imageUrl,
          contentDescription = article.title.ifBlank { strings.mediaOnlyPost },
          contentScale = ContentScale.FillWidth,
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        )
      }

      Text(
        text = article.content.ifBlank { strings.noAdditionalText },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

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

    // 1. Mute Notifications Card
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
