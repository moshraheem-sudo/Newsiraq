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
  val navFavorites: String,
  val navSettings: String,
  val searchHint: String,
  val clearSearch: String,
  val allSources: String,
  val allCategories: String,
  val emptyNews: String,
  val emptyFavoritesTitle: String,
  val emptyFavoritesSubtitle: String,
  val mediaOnlyPost: String,
  val noAdditionalText: String,
  val editedBadge: String,
  val videoBadge: String,
  val playVideo: String,
  val closeVideo: String,
  val fullscreenVideo: String,
  val exitFullscreen: String,
  val openVideoExternal: String,
  val openOriginal: String,
  val readMore: String,
  val publishedAtLabel: String,
  val refreshAction: String,
  val refreshPill: String,
  val refreshingPill: String,
  val backAction: String,
  val newNewsBanner: (Int) -> String,
  val downloadVideo: String,
  val downloadImage: String,
  val downloadStarted: String,
  val downloadFailed: String,
  val shareAction: String,
  val copyAction: String,
  val copiedMessage: String,
  val shareFooter: String,
  val saveFavorite: String,
  val savedFavorite: String,
  val addedToFavorites: String,
  val removedFromFavorites: String,
  val settingsTitle: String,
  val notificationsSection: String,
  val muteNotificationsTitle: String,
  val muteNotificationsDescOn: String,
  val muteNotificationsDescOff: String,
  val systemNotifPermissionGranted: String,
  val systemNotifPermissionDenied: String,
  val enablePermissionBtn: String,
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
        appTitle = "أخبار العراق",
        liveConnected = "بث مباشر محدّث",
        offlineSnapshot = "نسخة محفوظة محلياً",
        navNews = "الأخبار",
        navFavorites = "المفضلة",
        navSettings = "الإعدادات",
        searchHint = "بحث...",
        clearSearch = "مسح البحث",
        allSources = "كل المصادر",
        allCategories = "الكل",
        emptyNews = "لا توجد أخبار مطابقة للبحث الحالي.",
        emptyFavoritesTitle = "المفضلة فارغة",
        emptyFavoritesSubtitle = "اضغط على زر الحفظ (المفضلة) في أي خبر لتذكره وقراءته لاحقاً حتى بدون إنترنت.",
        mediaOnlyPost = "منشور وسائط",
        noAdditionalText = "لا يوجد نص إضافي مرفق مع هذا المنشور.",
        editedBadge = "معدّل",
        videoBadge = "فيديو",
        playVideo = "تشغيل الفيديو",
        closeVideo = "إيقاف",
        fullscreenVideo = "ملء الشاشة",
        exitFullscreen = "خروج من ملء الشاشة",
        openVideoExternal = "مشغل خارجي",
        openOriginal = "فتح المنشور الأصلي",
        readMore = "التفاصيل",
        publishedAtLabel = "وقت النشر",
        refreshAction = "تحديث الأخبار",
        refreshPill = "تحديث",
        refreshingPill = "تحديث...",
        backAction = "رجوع",
        newNewsBanner = { count -> "وصلت $count أخبار جديدة" },
        downloadVideo = "تنزيل الفيديو",
        downloadImage = "تنزيل الصورة",
        downloadStarted = "بدأ التنزيل إلى مجلد التنزيلات (Downloads/IraqNews)",
        downloadFailed = "تعذر بدء تنزيل الملف",
        shareAction = "مشاركة",
        copyAction = "نسخ",
        copiedMessage = "تم نسخ الخبر مع اسم التطبيق",
        shareFooter = "— عبر تطبيق أخبار العراق",
        saveFavorite = "مفضلة",
        savedFavorite = "محفوظ",
        addedToFavorites = "تم حفظ الخبر في المفضلة",
        removedFromFavorites = "تمت إزالة الخبر من المفضلة",
        settingsTitle = "إعدادات التطبيق",
        notificationsSection = "الإشعارات والتنبيهات",
        muteNotificationsTitle = "كتم الإشعارات",
        muteNotificationsDescOn = "إشعارات مركز النظام مكتومة عند وصول أخبار جديدة",
        muteNotificationsDescOff = "يصلك إشعار في مركز الإشعارات عند وصول كل خبر جديد",
        systemNotifPermissionGranted = "إذن إشعارات مركز النظام مفعّل",
        systemNotifPermissionDenied = "يلزم منح إذن الإشعارات ليصل كل خبر إلى مركز الإشعارات",
        enablePermissionBtn = "تفعيل إشعارات النظام",
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
        appTitle = "Iraq News",
        liveConnected = "Live feed synced",
        offlineSnapshot = "Offline saved snapshot",
        navNews = "News",
        navFavorites = "Favorites",
        navSettings = "Settings",
        searchHint = "Search...",
        clearSearch = "Clear search",
        allSources = "All Sources",
        allCategories = "All",
        emptyNews = "No matching news articles found.",
        emptyFavoritesTitle = "No favorites saved yet",
        emptyFavoritesSubtitle = "Tap Save on any article to bookmark and read it later offline.",
        mediaOnlyPost = "Media Post",
        noAdditionalText = "No additional text accompanied this post.",
        editedBadge = "Edited",
        videoBadge = "Video",
        playVideo = "Play Video",
        closeVideo = "Stop",
        fullscreenVideo = "Fullscreen",
        exitFullscreen = "Exit Fullscreen",
        openVideoExternal = "External Player",
        openOriginal = "Open Original Post",
        readMore = "Details",
        publishedAtLabel = "Published",
        refreshAction = "Refresh News",
        refreshPill = "Refresh",
        refreshingPill = "Syncing...",
        backAction = "Back",
        newNewsBanner = { count -> "$count new articles arrived" },
        downloadVideo = "Download Video",
        downloadImage = "Download Photo",
        downloadStarted = "Download started to Downloads/IraqNews",
        downloadFailed = "Could not start media download",
        shareAction = "Share",
        copyAction = "Copy",
        copiedMessage = "News copied with app name",
        shareFooter = "— Via Iraq News App",
        saveFavorite = "Save",
        savedFavorite = "Saved",
        addedToFavorites = "Added to Favorites",
        removedFromFavorites = "Removed from Favorites",
        settingsTitle = "App Settings",
        notificationsSection = "Notifications & Alerts",
        muteNotificationsTitle = "Mute Notifications",
        muteNotificationsDescOn = "System notification center alerts are muted",
        muteNotificationsDescOff = "Receive a notification center alert for each new article",
        systemNotifPermissionGranted = "System notification permission is granted",
        systemNotifPermissionDenied = "Grant permission so each news item reaches the notification center",
        enablePermissionBtn = "Enable System Notifications",
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
