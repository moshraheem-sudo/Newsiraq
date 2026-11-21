package com.example.ui

import com.example.data.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AppStrings(
  val appTitle: String,
  val liveConnected: String,
  val offlineSnapshot: String,
  val navNews: String,
  val navSettings: String,
  val searchHint: String,
  val allSources: String,
  val emptyNews: String,
  val mediaOnlyPost: String,
  val noAdditionalText: String,
  val editedBadge: String,
  val videoBadge: String,
  val playVideo: String,
  val closeVideo: String,
  val openVideoExternal: String,
  val openOriginal: String,
  val readMore: String,
  val publishedAtLabel: String,
  val refreshAction: String,
  val backAction: String,
  val newNewsBanner: (Int) -> String,
  val settingsTitle: String,
  val notificationsSection: String,
  val muteNotificationsTitle: String,
  val muteNotificationsDescOn: String,
  val muteNotificationsDescOff: String,
  val appearanceSection: String,
  val themeSystem: String,
  val themeLight: String,
  val themeDark: String,
  val languageSection: String,
  val languageArabic: String,
  val languageEnglish: String,
  val dataStatusSection: String,
  val lastSyncLabel: String,
  val storedNewsCountLabel: String,
  val syncNowButton: String,
)

fun stringsFor(language: AppLanguage): AppStrings =
  when (language) {
    AppLanguage.ARABIC ->
      AppStrings(
        appTitle = "مركز أخبار العراق",
        liveConnected = "بث مباشر محدّث",
        offlineSnapshot = "نسخة محفوظة محلياً",
        navNews = "الأخبار",
        navSettings = "الإعدادات",
        searchHint = "ابحث في العناوين والمحتوى والمصادر...",
        allSources = "الكل",
        emptyNews = "لا توجد أخبار مطابقة للبحث الحالي.",
        mediaOnlyPost = "منشور وسائط",
        noAdditionalText = "لا يوجد نص إضافي مرفق مع هذا المنشور.",
        editedBadge = "معدّل",
        videoBadge = "فيديو",
        playVideo = "تشغيل الفيديو",
        closeVideo = "إيقاف",
        openVideoExternal = "مشغل خارجي",
        openOriginal = "فتح المنشور الأصلي على تلغرام",
        readMore = "التفاصيل",
        publishedAtLabel = "وقت النشر",
        refreshAction = "تحديث الأخبار",
        backAction = "رجوع",
        newNewsBanner = { count -> "وصلت $count أخبار جديدة" },
        settingsTitle = "إعدادات التطبيق",
        notificationsSection = "الإشعارات والتنبيهات",
        muteNotificationsTitle = "كتم الإشعارات",
        muteNotificationsDescOn = "التنبيهات مكتومة عند وصول أخبار جديدة",
        muteNotificationsDescOff = "سيتم تنبيهك داخل التطبيق عند وصول أخبار جديدة",
        appearanceSection = "المظهر والثيم",
        themeSystem = "تلقائي",
        themeLight = "فاتح",
        themeDark = "داكن",
        languageSection = "لغة التطبيق",
        languageArabic = "العربية",
        languageEnglish = "English",
        dataStatusSection = "حالة التحديث والبيانات",
        lastSyncLabel = "آخر تحديث ناجح",
        storedNewsCountLabel = "عدد الأخبار المحفوظة",
        syncNowButton = "تحديث الأخبار الآن",
      )

    AppLanguage.ENGLISH ->
      AppStrings(
        appTitle = "Iraq News Hub",
        liveConnected = "Live feed synced",
        offlineSnapshot = "Offline saved snapshot",
        navNews = "News",
        navSettings = "Settings",
        searchHint = "Search headlines, content, or sources...",
        allSources = "All",
        emptyNews = "No matching news articles found.",
        mediaOnlyPost = "Media Post",
        noAdditionalText = "No additional text accompanied this post.",
        editedBadge = "Edited",
        videoBadge = "Video",
        playVideo = "Play Video",
        closeVideo = "Stop",
        openVideoExternal = "External Player",
        openOriginal = "Open Original Post on Telegram",
        readMore = "Details",
        publishedAtLabel = "Published",
        refreshAction = "Refresh News",
        backAction = "Back",
        newNewsBanner = { count -> "$count new articles arrived" },
        settingsTitle = "App Settings",
        notificationsSection = "Notifications & Alerts",
        muteNotificationsTitle = "Mute Notifications",
        muteNotificationsDescOn = "Alerts are muted when new news arrives",
        muteNotificationsDescOff = "Show alerts when fresh news is fetched",
        appearanceSection = "Theme & Appearance",
        themeSystem = "System",
        themeLight = "Light",
        themeDark = "Dark",
        languageSection = "Language",
        languageArabic = "العربية",
        languageEnglish = "English",
        dataStatusSection = "Feed & Sync Status",
        lastSyncLabel = "Last Successful Sync",
        storedNewsCountLabel = "Stored Articles",
        syncNowButton = "Sync News Now",
      )
  }

object DateFormatter {
  private val isoFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
  )

  fun formatBaghdadDateTime(isoRaw: String, language: AppLanguage): String {
    if (isoRaw.isBlank()) {
      return if (language == AppLanguage.ARABIC) "غير متوفر" else "Unavailable"
    }
    val parsed = parseIsoDate(isoRaw) ?: return isoRaw.take(16).replace("T", " ")

    val diffMs = System.currentTimeMillis() - parsed.time
    val mins = (diffMs / 60_000L).coerceAtLeast(0L)
    val relative =
      if (language == AppLanguage.ARABIC) {
        when {
          mins < 1L -> "الآن"
          mins < 60L -> "منذ $mins د"
          mins < 1440L -> "منذ ${mins / 60L} س"
          else -> "منذ ${mins / 1440L} ي"
        }
      } else {
        when {
          mins < 1L -> "Just now"
          mins < 60L -> "${mins}m ago"
          mins < 1440L -> "${mins / 60L}h ago"
          else -> "${mins / 1440L}d ago"
        }
      }

    val outFmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("Asia/Baghdad")
    }
    val clockStr = outFmt.format(parsed)
    return "$relative • $clockStr"
  }

  private fun parseIsoDate(raw: String): Date? {
    for (pattern in isoFormats) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        if (pattern.endsWith("'Z'")) {
          sdf.timeZone = TimeZone.getTimeZone("UTC")
        }
        val d = sdf.parse(raw)
        if (d != null) return d
      } catch (_: Exception) {
        // try next format
      }
    }
    return null
  }
}
