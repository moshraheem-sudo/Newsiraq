package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.NewsArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Database(entities = [FavoriteArticleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun favoriteArticleDao(): FavoriteArticleDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "iraq_news_db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}

class FavoritesRepository(private val dao: FavoriteArticleDao) {
  val favorites: Flow<List<NewsArticle>> =
    dao.getAllFavorites().map { entities -> entities.map { it.toNewsArticle() } }

  val favoriteIds: Flow<Set<String>> = dao.getAllFavoriteIds().map { it.toSet() }

  suspend fun toggleFavorite(article: NewsArticle, currentlyFavorite: Boolean): Boolean {
    return if (currentlyFavorite) {
      dao.deleteFavoriteById(article.id)
      false
    } else {
      dao.insertFavorite(FavoriteArticleEntity.fromNewsArticle(article))
      true
    }
  }
}
