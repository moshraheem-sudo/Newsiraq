package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.data.NewsSourceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val articles: List<NewsArticle> = emptyList(),
  val sources: List<NewsSourceInfo> = emptyList(),
  val selectedSource: String = "all",
  val searchQuery: String = "",
  val lastSuccessfulUpdate: String = "",
  val loadedFromRemote: Boolean = false,
  val webUrl: String = DEFAULT_WEB_URL,
  val jsonUrl: String = DEFAULT_JSON_URL,
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
    const val DEFAULT_WEB_URL = "https://USERNAME.github.io/iraq-news-hub/"
    const val DEFAULT_JSON_URL = "https://USERNAME.github.io/iraq-news-hub/news.json"
  }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = NewsRepository(application.applicationContext)

  private val _uiState = MutableStateFlow(NewsUiState())
  val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      try {
        val payload = repository.fetchNews(_uiState.value.jsonUrl)
        _uiState.update {
          it.copy(
            isLoading = false,
            articles = payload.items,
            sources = payload.sources,
            lastSuccessfulUpdate = payload.lastSuccessfulUpdate,
            loadedFromRemote = payload.loadedFromRemote,
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = e.localizedMessage ?: "تعذر تحميل الأخبار",
          )
        }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun selectSource(username: String) {
    _uiState.update { it.copy(selectedSource = username) }
  }

  fun updateEndpoints(webUrl: String, jsonUrl: String) {
    _uiState.update { it.copy(webUrl = webUrl, jsonUrl = jsonUrl) }
    refresh()
  }
}
