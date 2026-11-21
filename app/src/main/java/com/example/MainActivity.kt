package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.data.NewsArticle
import com.example.ui.NewsUiState
import com.example.ui.NewsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          IraqNewsHubApp()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IraqNewsHubApp(viewModel: NewsViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }

  if (selectedArticle != null) {
    BackHandler { selectedArticle = null }
    ArticleDetailScreen(
      article = selectedArticle!!,
      onBack = { selectedArticle = null },
    )
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "مركز أخبار العراق",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                if (uiState.loadedFromRemote) "متصل بالبث المباشر (GitHub Pages)"
                else "عرض أحدث الأخبار المحفوظة",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.refresh() },
            modifier = Modifier.testTag("refresh_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "تحديث الأخبار")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
      )
    },
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Newspaper, contentDescription = null) },
          label = { Text("الأخبار") },
          modifier = Modifier.testTag("nav_feed_tab"),
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.Public, contentDescription = null) },
          label = { Text("الموقع") },
          modifier = Modifier.testTag("nav_webview_tab"),
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Info, contentDescription = null) },
          label = { Text("حول") },
          modifier = Modifier.testTag("nav_about_tab"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (selectedTab) {
        0 ->
          NewsFeedScreen(
            uiState = uiState,
            onSearchChange = viewModel::updateSearchQuery,
            onSelectSource = viewModel::selectSource,
            onArticleClick = { selectedArticle = it },
          )
        1 -> WebPortalScreen(webUrl = uiState.webUrl)
        2 ->
          AboutAndIntegrationScreen(
            uiState = uiState,
            onApplyEndpoints = viewModel::updateEndpoints,
          )
      }
    }
  }
}

@Composable
fun NewsFeedScreen(
  uiState: NewsUiState,
  onSearchChange: (String) -> Unit,
  onSelectSource: (String) -> Unit,
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
      placeholder = { Text("ابحث في العناوين والمحتوى والمصادر...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 8.dp)
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
          label = { Text("الكل (${uiState.articles.size})") },
        )
      }
      items(uiState.sources, key = { it.username }) { src ->
        FilterChip(
          selected = uiState.selectedSource == src.username,
          onClick = { onSelectSource(src.username) },
          label = { Text(src.name) },
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
          text = uiState.errorMessage ?: "لا توجد أخبار مطابقة للبحث الحالي.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
      ) {
        items(uiState.filteredArticles, key = { it.id }) { article ->
          NewsArticleCard(article = article, onClick = { onArticleClick(article) })
        }
      }
    }
  }
}

@Composable
fun NewsArticleCard(article: NewsArticle, onClick: () -> Unit) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("news_item_card_${article.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (article.images.isNotEmpty()) {
        AsyncImage(
          model = article.images.first(),
          contentDescription = article.title.ifBlank { "صورة الخبر" },
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(12.dp)),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = article.sourceName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Text(
          text = article.publishedAt.take(16).replace("T", " "),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = article.title.ifBlank { "منشور وسائط" },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (article.content.isNotBlank()) {
        Text(
          text = article.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(article: NewsArticle, onBack: () -> Unit) {
  val context = LocalContext.current
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(article.sourceName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text(article.category.ifBlank { "أخبار" }) })
        if (article.isEdited) {
          AssistChip(onClick = {}, label = { Text("معدّل") })
        }
      }
      Text(
        text = article.title.ifBlank { "منشور وسائط" },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "تاريخ النشر: ${article.publishedAt.take(19).replace("T", " ")} UTC",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      article.images.forEach { imageUrl ->
        AsyncImage(
          model = imageUrl,
          contentDescription = "صورة مرفقة",
          contentScale = ContentScale.FillWidth,
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        )
      }
      Text(
        text = article.content.ifBlank { "لا يوجد نص إضافي في المصدر." },
        style = MaterialTheme.typography.bodyLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = {
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl))
          context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth().testTag("open_original_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text("فتح المنشور الأصلي على تلغرام")
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalScreen(webUrl: String) {
  if (webUrl.isBlank() || webUrl.contains("USERNAME")) {
    Box(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "سيتم فتح رابط موقع GitHub Pages تلقائياً فور اكتمال النشر وتعيين اسم المستخدم.",
        style = MaterialTheme.typography.bodyLarge,
      )
    }
    return
  }

  AndroidView(
    factory = { context ->
      WebView(context).apply {
        webViewClient = WebViewClient()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        loadUrl(webUrl)
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
fun AboutAndIntegrationScreen(
  uiState: NewsUiState,
  onApplyEndpoints: (String, String) -> Unit,
) {
  var webInput by remember(uiState.webUrl) { mutableStateOf(uiState.webUrl) }
  var jsonInput by remember(uiState.jsonUrl) { mutableStateOf(uiState.jsonUrl) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .widthIn(max = 680.dp)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Iraq News Hub — مركز أخبار العراق",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text =
        "تطبيق متكامل متصل بمجمع الأخبار العراقي عبر GitHub Pages وملف news.json، مع نسخة احتياطية مدمجة للعمل الفوري.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = "آخر تحديث ناجح: ${uiState.lastSuccessfulUpdate.ifBlank { "محلي" }}",
      style = MaterialTheme.typography.labelLarge,
    )
    OutlinedTextField(
      value = webInput,
      onValueChange = { webInput = it },
      label = { Text("رابط الموقع (WebView)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = jsonInput,
      onValueChange = { jsonInput = it },
      label = { Text("رابط ملف JSON") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
      onClick = { onApplyEndpoints(webInput, jsonInput) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("حفظ وتحديث المصدر")
    }
  }
}
