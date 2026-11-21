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
  val updatedUpToDate: String,
  val offlineRefreshError: String,
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
  val scrollToTop: String,
  val videoPlayError: String,
  val noAppForLink: String,
  val mentionedLinksTitle: String,
  val resetFilters: String,
  val emptySearchFavorites: String,
  val activeSourcesCountLabel: String,
  val appVersionLabel: String,
  val loadingVideo: String,
  val appUpdatesSection: String,
  val checkForUpdatesButton: String,
  val checkingUpdates: String,
  val updateAvailableTitle: String,
  val currentVersionLabel: String,
  val latestVersionLabel: String,
  val updateSizeLabel: String,
  val releaseNotesTitle: String,
  val updateNowButton: String,
  val downloadingUpdate: String,
  val stopAndCancelButton: String,
  val downloadCancelledMessage: String,
  val installUpdateButton: String,
  val installPermissionTitle: String,
  val installPermissionMessage: String,
  val grantPermissionButton: String,
  val upToDateMessage: String,
  val updateCheckFailedMessage: String,
  val noApkAssetMessage: String,
  val openGitHubRepo: String,
  val openReleasePage: String,
  val laterButton: String,
  val updateAvailableBanner: (String) -> String,
  val updateReadyBanner: (String) -> String,
  val localizeCategory: (String) -> String,
)

fun stringsFor(language: AppLanguage): AppStrings =
  when (language) {
    AppLanguage.ARABIC ->
      AppStrings(
        appTitle = "أخبار العالم",
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
        newNewsBanner = { count -> "تم التحديث • وصلت $count أخبار جديدة" },
        updatedUpToDate = "تم التحديث • الأخبار والمصادر محدّثة",
        offlineRefreshError = "تعذر الاتصال بالإنترنت — يتم عرض النسخة المحفوظة",
        downloadVideo = "تنزيل الفيديو",
        downloadImage = "تنزيل الصورة",
        downloadStarted = "بدأ التنزيل إلى مجلد التنزيلات (Downloads/WorldNews)",
        downloadFailed = "تعذر بدء تنزيل الملف",
        shareAction = "مشاركة",
        copyAction = "نسخ",
        copiedMessage = "تم نسخ الخبر مع اسم التطبيق",
        shareFooter = "— عبر تطبيق أخبار العالم",
        saveFavorite = "مفضلة",
        savedFavorite = "محفوظ",
        addedToFavorites = "تم حفظ الخبر في المفضلة",
        removedFromFavorites = "تمت إزالة الخبر من المفضلة",
        settingsTitle = "إعدادات التطبيق",
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
        scrollToTop = "الأعلى",
        videoPlayError = "تعذر تشغيل الفيديو هنا — جرب المشغل الخارجي أو التنزيل",
        noAppForLink = "لا يوجد تطبيق متوافق لفتح هذا الرابط",
        mentionedLinksTitle = "روابط مذكورة في الخبر",
        resetFilters = "إعادة ضبط التصفية",
        emptySearchFavorites = "لا توجد أخبار محفوظة مطابقة لبحثك.",
        activeSourcesCountLabel = "عدد المصادر النشطة",
        appVersionLabel = "إصدار التطبيق",
        loadingVideo = "جاري تحميل الفيديو...",
        appUpdatesSection = "تحديث التطبيق عبر GitHub",
        checkForUpdatesButton = "تحديث التطبيق",
        checkingUpdates = "جاري التحقق من GitHub...",
        updateAvailableTitle = "يتوفر تحديث جديد للتطبيق",
        currentVersionLabel = "الإصدار الحالي المنصّب",
        latestVersionLabel = "الإصدار الجديد على GitHub",
        updateSizeLabel = "حجم ملف التحديث",
        releaseNotesTitle = "الجديد في هذا الإصدار",
        updateNowButton = "تنزيل التحديث",
        downloadingUpdate = "جاري تنزيل التحديث داخل التطبيق...",
        stopAndCancelButton = "إيقاف وإلغاء التنزيل",
        downloadCancelledMessage = "تم إيقاف وإلغاء تنزيل التحديث.",
        installUpdateButton = "تثبيت التحديث الآن (Install)",
        installPermissionTitle = "موافقة النظام مطلوبة للتثبيت",
        installPermissionMessage = "يرجى تفعيل خيار السماح بتثبيت التطبيقات من هذا المصدر في إعدادات النظام لإكمال التثبيت.",
        grantPermissionButton = "منح الإذن والمتابعة",
        upToDateMessage = "أنت تستخدم أحدث إصدار متاح بالفعل.",
        updateCheckFailedMessage = "تعذر التحقق من التحديثات عبر GitHub. تأكد من الاتصال بالإنترنت.",
        noApkAssetMessage = "يتوفر إصدار جديد على GitHub لكن لم يُرفق ملف APK مباشر بعد. يمكنك فتح صفحة الإصدار.",
        openGitHubRepo = "مستودع التطبيق على GitHub",
        openReleasePage = "فتح صفحة الإصدار على GitHub",
        laterButton = "لاحقاً",
        updateAvailableBanner = { ver -> "يتوفر تحديث جديد v$ver على GitHub — اضغط للتحديث" },
        updateReadyBanner = { ver -> "اكتمل تنزيل التحديث v$ver — اضغط للتثبيت" },
        localizeCategory = { raw -> raw },
      )

    AppLanguage.ENGLISH ->
      AppStrings(
        appTitle = "World News",
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
        newNewsBanner = { count -> "Updated • $count new articles arrived" },
        updatedUpToDate = "Updated • Feeds & sources up to date",
        offlineRefreshError = "Unable to connect — showing saved snapshot",
        downloadVideo = "Download Video",
        downloadImage = "Download Photo",
        downloadStarted = "Download started to Downloads/WorldNews",
        downloadFailed = "Could not start media download",
        shareAction = "Share",
        copyAction = "Copy",
        copiedMessage = "News copied with app name",
        shareFooter = "— Via World News App",
        saveFavorite = "Save",
        savedFavorite = "Saved",
        addedToFavorites = "Added to Favorites",
        removedFromFavorites = "Removed from Favorites",
        settingsTitle = "App Settings",
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
        scrollToTop = "Top",
        videoPlayError = "Unable to play video here — try External Player or Download",
        noAppForLink = "No compatible app found to open this link",
        mentionedLinksTitle = "Links Mentioned in Post",
        resetFilters = "Reset Filters",
        emptySearchFavorites = "No saved articles match your search.",
        activeSourcesCountLabel = "Active Sources",
        appVersionLabel = "App Version",
        loadingVideo = "Loading video...",
        appUpdatesSection = "App Updates via GitHub",
        checkForUpdatesButton = "Update App",
        checkingUpdates = "Checking GitHub releases...",
        updateAvailableTitle = "New App Update Available",
        currentVersionLabel = "Installed Version",
        latestVersionLabel = "Latest on GitHub",
        updateSizeLabel = "Download Size",
        releaseNotesTitle = "What's New",
        updateNowButton = "Download Update",
        downloadingUpdate = "Downloading update in app...",
        stopAndCancelButton = "Stop & Cancel",
        downloadCancelledMessage = "Update download stopped and cancelled.",
        installUpdateButton = "Install Update",
        installPermissionTitle = "System Approval Required",
        installPermissionMessage = "Allow installing apps from this source in system settings to finish installing the update.",
        grantPermissionButton = "Allow & Continue",
        upToDateMessage = "You already have the latest version installed.",
        updateCheckFailedMessage = "Unable to check GitHub for updates. Check your internet connection.",
        noApkAssetMessage = "A newer release was found on GitHub without a direct APK attachment. Open the release page to inspect.",
        openGitHubRepo = "GitHub Repository",
        openReleasePage = "Open GitHub Release",
        laterButton = "Later",
        updateAvailableBanner = { ver -> "Update v$ver available on GitHub — Tap to update" },
        updateReadyBanner = { ver -> "Update v$ver downloaded — Tap to install" },
        localizeCategory = { raw ->
          when (raw.trim()) {
            "أخبار العراق" -> "Iraq News"
            "أخبار عربية وعالمية" -> "Arab & World"
            "أخبار عالمية" -> "World News"
            "أخبار منوعة (X)" -> "X Feed"
            else -> raw
          }
        },
      )
  }

object DateFormatter {
  private val isoFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
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
