package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppLanguage
import com.example.data.NewsArticle
import com.example.data.SettingsPreferences
import org.json.JSONArray

object NewsNotificationHelper {
  const val CHANNEL_ID = "iraq_news_alerts"
  const val EXTRA_ARTICLE_ID = "extra_article_id"
  private const val GROUP_KEY = "com.example.NEWS_ALERTS"
  private const val SUMMARY_ID = 9001
  private const val JOB_ID = 4102
  private const val PREFS_NAME = "iraq_news_notified_store"
  private const val KEY_SEEN_IDS = "seen_article_ids"
  private const val KEY_BASELINE_DONE = "baseline_initialized"
  private const val MAX_STORED_IDS = 500
  private const val MAX_NOTIFICATIONS_PER_BATCH = 15

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun ensureChannel(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val name = if (language == AppLanguage.ARABIC) "تنبيهات الأخبار" else "News Alerts"
    val desc =
      if (language == AppLanguage.ARABIC) {
        "إشعارات فورية في مركز الإشعارات عند وصول أخبار عراقية جديدة"
      } else {
        "Instant alerts in the notification tray when new Iraqi news arrives"
      }
    val channel =
      NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
        description = desc
        enableVibration(true)
        setShowBadge(true)
      }
    manager.createNotificationChannel(channel)
  }

  fun processAndNotify(context: Context, articles: List<NewsArticle>): Int {
    if (articles.isEmpty()) return 0
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val baselineDone = prefs.getBoolean(KEY_BASELINE_DONE, false)
    val seenIds = loadSeenIds(prefs).toMutableList()
    val seenSet = seenIds.toHashSet()

    if (!baselineDone) {
      // Initial install / first run: seed baseline IDs so user isn't flooded by old history
      val initialIds = articles.map { it.id }.filter { it.isNotBlank() }.distinct().take(MAX_STORED_IDS)
      saveSeenIds(prefs, initialIds, baselineDone = true)
      return 0
    }

    val newArticles = articles.filter { it.id.isNotBlank() && it.id !in seenSet }
    if (newArticles.isEmpty()) return 0

    val updatedIds = (newArticles.map { it.id } + seenIds).distinct().take(MAX_STORED_IDS)
    saveSeenIds(prefs, updatedIds, baselineDone = true)

    val settings = SettingsPreferences(context).load()
    if (settings.muteNotifications || !hasNotificationPermission(context)) {
      return newArticles.size
    }

    ensureChannel(context, settings.language)
    val notificationManager = NotificationManagerCompat.from(context)
    val appName = context.getString(R.string.app_name)

    val toPost = newArticles.take(MAX_NOTIFICATIONS_PER_BATCH).asReversed()
    toPost.forEach { article ->
      val openIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra(EXTRA_ARTICLE_ID, article.id)
        }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          article.id.hashCode(),
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val title = article.title.ifBlank { article.sourceName }
      val body = article.content.ifBlank { article.title }.take(400)

      val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_launcher_foreground)
          .setContentTitle(title)
          .setContentText(body)
          .setSubText(article.sourceName.ifBlank { appName })
          .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setCategory(NotificationCompat.CATEGORY_STATUS)
          .setAutoCancel(true)
          .setGroup(GROUP_KEY)
          .setContentIntent(pendingIntent)
          .build()

      try {
        notificationManager.notify(article.id.hashCode(), notification)
      } catch (_: SecurityException) {
        // Permission revoked between check and post
      }
    }

    if (toPost.size > 1) {
      val summaryIntent =
        PendingIntent.getActivity(
          context,
          SUMMARY_ID,
          Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
          },
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      val summaryTitle =
        if (settings.language == AppLanguage.ARABIC) {
          "$appName • ${newArticles.size} أخبار جديدة"
        } else {
          "$appName • ${newArticles.size} new articles"
        }
      val summary =
        NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_launcher_foreground)
          .setContentTitle(summaryTitle)
          .setContentText(toPost.last().title)
          .setGroup(GROUP_KEY)
          .setGroupSummary(true)
          .setAutoCancel(true)
          .setContentIntent(summaryIntent)
          .build()
      try {
        notificationManager.notify(SUMMARY_ID, summary)
      } catch (_: SecurityException) {
      }
    }

    return newArticles.size
  }

  fun schedulePeriodicSync(context: Context) {
    val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
    val existing = scheduler.allPendingJobs.any { it.id == JOB_ID }
    if (existing) return

    val jobInfo =
      JobInfo.Builder(JOB_ID, ComponentName(context, NewsSyncJobService::class.java))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPeriodic(15 * 60 * 1000L)
        .setPersisted(true)
        .build()
    scheduler.schedule(jobInfo)
  }

  private fun loadSeenIds(prefs: android.content.SharedPreferences): List<String> {
    val raw = prefs.getString(KEY_SEEN_IDS, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveSeenIds(
    prefs: android.content.SharedPreferences,
    ids: List<String>,
    baselineDone: Boolean,
  ) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    prefs.edit().putString(KEY_SEEN_IDS, arr.toString()).putBoolean(KEY_BASELINE_DONE, baselineDone).apply()
  }
}
