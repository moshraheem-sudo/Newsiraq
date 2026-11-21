package com.example.notifications

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.data.NewsRepository
import com.example.data.SettingsPreferences
import com.example.ui.NewsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NewsSyncJobService : JobService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onStartJob(params: JobParameters?): Boolean {
    val settings = SettingsPreferences(applicationContext).load()
    if (settings.muteNotifications || !NewsNotificationHelper.hasNotificationPermission(applicationContext)) {
      return false
    }

    scope.launch {
      var needsReschedule = false
      try {
        val repo = NewsRepository(applicationContext)
        val payload = repo.fetchNews(NewsUiState.DEFAULT_JSON_URL)
        if (payload.loadedFromRemote) {
          NewsNotificationHelper.processAndNotify(applicationContext, payload.items)
        }
      } catch (_: Exception) {
        needsReschedule = true
      } finally {
        jobFinished(params, needsReschedule)
      }
    }
    return true
  }

  override fun onStopJob(params: JobParameters?): Boolean {
    return true
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
