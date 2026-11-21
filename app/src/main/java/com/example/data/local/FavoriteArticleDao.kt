package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteArticleDao {
  @Query("SELECT * FROM favorite_articles ORDER BY savedAt DESC")
  fun getAllFavorites(): Flow<List<FavoriteArticleEntity>>

  @Query("SELECT id FROM favorite_articles")
  fun getAllFavoriteIds(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorite(article: FavoriteArticleEntity)

  @Query("DELETE FROM favorite_articles WHERE id = :id")
  suspend fun deleteFavoriteById(id: String)
}
