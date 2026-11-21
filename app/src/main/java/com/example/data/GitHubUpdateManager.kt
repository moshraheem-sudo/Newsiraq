package com.example.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class InstalledAppInfo(
  val versionName: String,
  val versionCode: Long,
)

data class GitHubReleaseInfo(
  val versionName: String,
  val tagName: String,
  val title: String,
  val releaseNotes: String,
  val publishedAt: String,
  val releasePageUrl: String,
  val apkDownloadUrl: String?,
  val apkFileName: String?,
  val apkSizeBytes: Long,
)

sealed interface AppUpdateStage {
  data class Idle(
    val installedVersion: String,
    val latestRelease: GitHubReleaseInfo? = null,
  ) : AppUpdateStage

  data class Checking(
    val installedVersion: String,
  ) : AppUpdateStage

  data class UpToDate(
    val installedVersion: String,
    val latestRelease: GitHubReleaseInfo? = null,
  ) : AppUpdateStage

  data class UpdateAvailable(
    val installedVersion: String,
    val release: GitHubReleaseInfo,
  ) : AppUpdateStage

  data class Downloading(
    val installedVersion: String,
    val release: GitHubReleaseInfo,
    val downloadedBytes: Long,
    val totalBytes: Long,
  ) : AppUpdateStage {
    val progressFraction: Float
      get() =
        if (totalBytes > 0L) {
          (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
          -1f
        }
  }

  data class ReadyToInstall(
    val installedVersion: String,
    val release: GitHubReleaseInfo,
    val apkFile: File,
    val apkSizeBytes: Long,
  ) : AppUpdateStage

  data class Error(
    val installedVersion: String,
    val message: String,
    val release: GitHubReleaseInfo? = null,
  ) : AppUpdateStage
}

object GitHubUpdateManager {
  const val GITHUB_REPO_URL = "https://github.com/moshraheem-sudo/Newsiraq"
  const val GITHUB_RELEASES_PAGE_URL = "https://github.com/moshraheem-sudo/Newsiraq/releases"
  private const val GITHUB_LATEST_RELEASE_API =
    "https://api.github.com/repos/moshraheem-sudo/Newsiraq/releases/latest"
  private const val GITHUB_RELEASES_LIST_API =
    "https://api.github.com/repos/moshraheem-sudo/Newsiraq/releases?per_page=5"
  private const val PAGES_RELEASES_JSON_URL =
    "https://moshraheem-sudo.github.io/Newsiraq/releases.json"
  private const val RAW_RELEASES_JSON_URL =
    "https://raw.githubusercontent.com/moshraheem-sudo/Newsiraq/main/iraq-news-hub/web/releases.json"

  private val apiClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()

  private val downloadClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  private val activeDownloadCall = AtomicReference<Call?>(null)
  private val noCache = CacheControl.Builder().noCache().noStore().build()

  fun getInstalledAppInfo(context: Context): InstalledAppInfo {
    return runCatching {
      val pm = context.packageManager
      val pkgInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0L),
          )
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(context.packageName, 0)
        }
      InstalledAppInfo(
        versionName = pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME,
        versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
      )
    }.getOrDefault(
      InstalledAppInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
      )
    )
  }

  fun isNetworkConnected(context: Context): Boolean {
    return runCatching {
      val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
          ?: return false
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
  }

  fun normalizeVersionName(raw: String): String {
    return raw.trim().removePrefix("v").removePrefix("V")
  }

  fun parseVersionSegments(raw: String): List<Int> {
    val core = normalizeVersionName(raw).substringBefore('-').substringBefore('+')
    if (core.isBlank()) return emptyList()
    return core.split('.').mapNotNull { part ->
      val digits = part.takeWhile { it.isDigit() }
      digits.toIntOrNull()
    }
  }

  fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = parseVersionSegments(remoteVersion)
    val current = parseVersionSegments(currentVersion)
    if (remote.isEmpty()) return false
    val maxLen = maxOf(remote.size, current.size)
    for (i in 0 until maxLen) {
      val r = remote.getOrElse(i) { 0 }
      val c = current.getOrElse(i) { 0 }
      if (r != c) return r > c
    }
    return false
  }

  fun formatMegabytes(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
  }

  suspend fun fetchLatestRelease(): Result<GitHubReleaseInfo> =
    withContext(Dispatchers.IO) {
      runCatching {
        val latestObj = fetchJsonText(GITHUB_LATEST_RELEASE_API, githubHeaders = true)
        if (latestObj != null) {
          parseGitHubReleaseJson(latestObj)?.let { return@runCatching it }
        }

        val listRaw = fetchJsonText(GITHUB_RELEASES_LIST_API, githubHeaders = true)
        if (listRaw != null) {
          parseGitHubReleasesArrayJson(listRaw)?.let { return@runCatching it }
        }

        val fallbackRaw =
          fetchJsonText(PAGES_RELEASES_JSON_URL, githubHeaders = false)
            ?: fetchJsonText(RAW_RELEASES_JSON_URL, githubHeaders = false)
        if (fallbackRaw != null) {
          parseReleasesFallbackJson(fallbackRaw)?.let { return@runCatching it }
        }

        error("Could not retrieve release information from GitHub")
      }
    }

  private fun fetchJsonText(url: String, githubHeaders: Boolean): String? {
    return runCatching {
      val sep = if (url.contains("?")) "&" else "?"
      val reqBuilder =
        Request.Builder()
          .url("$url${sep}t=${System.currentTimeMillis()}")
          .cacheControl(noCache)
          .header("User-Agent", "WorldNewsAndroid/${BuildConfig.VERSION_NAME}")
      if (githubHeaders) {
        reqBuilder.header("Accept", "application/vnd.github+json")
      }
      apiClient.newCall(reqBuilder.build()).execute().use { response ->
        if (!response.isSuccessful) return null
        response.body?.string()?.takeIf { it.isNotBlank() }
      }
    }.getOrNull()
  }

  fun parseGitHubReleaseJson(rawJson: String): GitHubReleaseInfo? {
    return runCatching {
      val obj = JSONObject(rawJson)
      if (obj.optBoolean("draft", false)) return null
      val tagName = obj.optString("tag_name").trim()
      if (tagName.isBlank()) return null
      val version = normalizeVersionName(tagName)
      val title = obj.optString("name").trim().ifBlank { "v$version" }
      val body = obj.optString("body").trim()
      val publishedAt = obj.optString("published_at").trim()
      val htmlUrl = obj.optString("html_url").trim().ifBlank { GITHUB_RELEASES_PAGE_URL }

      val assets = obj.optJSONArray("assets") ?: JSONArray()
      var apkUrl: String? = null
      var apkName: String? = null
      var apkSize = 0L
      for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        val name = asset.optString("name").trim()
        val contentType = asset.optString("content_type").trim()
        val downloadUrl = asset.optString("browser_download_url").trim()
        val isApk =
          name.endsWith(".apk", ignoreCase = true) ||
            contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
        if (isApk && downloadUrl.startsWith("https://")) {
          apkUrl = downloadUrl
          apkName = name.ifBlank { "WorldNews-v$version.apk" }
          apkSize = asset.optLong("size", 0L).coerceAtLeast(0L)
          break
        }
      }

      GitHubReleaseInfo(
        versionName = version,
        tagName = tagName,
        title = title,
        releaseNotes = body,
        publishedAt = publishedAt,
        releasePageUrl = htmlUrl,
        apkDownloadUrl = apkUrl,
        apkFileName = apkName,
        apkSizeBytes = apkSize,
      )
    }.getOrNull()
  }

  fun parseGitHubReleasesArrayJson(rawJson: String): GitHubReleaseInfo? {
    return runCatching {
      val arr = JSONArray(rawJson)
      var fallbackPrerelease: GitHubReleaseInfo? = null
      for (i in 0 until arr.length()) {
        val item = arr.optJSONObject(i) ?: continue
        if (item.optBoolean("draft", false)) continue
        val parsed = parseGitHubReleaseJson(item.toString()) ?: continue
        if (!item.optBoolean("prerelease", false)) {
          return parsed
        }
        if (fallbackPrerelease == null) {
          fallbackPrerelease = parsed
        }
      }
      fallbackPrerelease
    }.getOrNull()
  }

  fun parseReleasesFallbackJson(rawJson: String): GitHubReleaseInfo? {
    return runCatching {
      val root = JSONObject(rawJson)
      val currentVer = normalizeVersionName(root.optString("current_version"))
      val releases = root.optJSONArray("releases") ?: JSONArray()
      val first = releases.optJSONObject(0)
      val version =
        normalizeVersionName(first?.optString("version").orEmpty()).ifBlank { currentVer }
      if (version.isBlank()) return null

      val tag = first?.optString("tag")?.takeIf { it.isNotBlank() } ?: "v$version"
      val title = first?.optString("title")?.takeIf { it.isNotBlank() } ?: "v$version"
      val summary = first?.optString("summary")?.trim().orEmpty()
      val highlightsArr = first?.optJSONArray("highlights")
      val notes = buildString {
        if (summary.isNotBlank()) append(summary)
        if (highlightsArr != null && highlightsArr.length() > 0) {
          for (i in 0 until highlightsArr.length()) {
            val line = highlightsArr.optString(i).trim()
            if (line.isNotBlank()) {
              if (isNotEmpty()) append("\n")
              append("• ").append(line)
            }
          }
        }
      }

      val explicitApkUrl = first?.optString("apk_url")?.trim()?.takeIf { it.startsWith("https://") }
      val explicitApkName = first?.optString("apk_name")?.trim()?.takeIf { it.isNotBlank() }
      val explicitApkSize = first?.optLong("apk_size_bytes", 0L)?.coerceAtLeast(0L) ?: 0L

      GitHubReleaseInfo(
        versionName = version,
        tagName = tag,
        title = title,
        releaseNotes = notes,
        publishedAt = first?.optString("date").orEmpty(),
        releasePageUrl = "$GITHUB_REPO_URL/releases/tag/$tag",
        apkDownloadUrl = explicitApkUrl,
        apkFileName = explicitApkName,
        apkSizeBytes = explicitApkSize,
      )
    }.getOrNull()
  }

  fun getUpdatesDir(context: Context): File {
    return File(context.applicationContext.filesDir, "updates").apply {
      if (!exists()) mkdirs()
    }
  }

  fun findReadyApkIfExists(context: Context, release: GitHubReleaseInfo): File? {
    val file = targetApkFile(context, release)
    if (!file.exists() || !file.isFile) return null
    if (release.apkSizeBytes > 0L && file.length() != release.apkSizeBytes) return null
    return if (hasZipMagicBytes(file)) file else null
  }

  private fun targetApkFile(context: Context, release: GitHubReleaseInfo): File {
    val safeName =
      (release.apkFileName ?: "WorldNews-v${release.versionName}.apk")
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
    return File(getUpdatesDir(context), safeName)
  }

  suspend fun downloadApk(
    context: Context,
    release: GitHubReleaseInfo,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
  ): Result<File> =
    withContext(Dispatchers.IO) {
      val downloadUrl =
        release.apkDownloadUrl?.takeIf { it.startsWith("https://") }
          ?: return@withContext Result.failure(IllegalStateException("No APK asset URL found"))

      val updatesDir = getUpdatesDir(context)
      val targetFile = targetApkFile(context, release)
      val tmpFile = File(updatesDir, "${targetFile.name}.part")

      // Clean stale partial downloads or old APKs
      updatesDir.listFiles()?.forEach { existing ->
        if (existing.name != targetFile.name) {
          existing.delete()
        }
      }
      if (tmpFile.exists()) tmpFile.delete()

      val request =
        Request.Builder()
          .url(downloadUrl)
          .cacheControl(noCache)
          .header("User-Agent", "WorldNewsAndroid/${BuildConfig.VERSION_NAME}")
          .build()

      val call = downloadClient.newCall(request)
      activeDownloadCall.set(call)

      try {
        call.execute().use { response ->
          if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
          }
          val body = response.body ?: throw IOException("Empty response body")
          val totalBytes =
            body.contentLength().takeIf { it > 0L } ?: release.apkSizeBytes.coerceAtLeast(0L)

          var downloaded = 0L
          var lastReportedBytes = 0L
          var lastReportedTime = System.currentTimeMillis()
          onProgress(0L, totalBytes)

          body.byteStream().use { input ->
            FileOutputStream(tmpFile).use { output ->
              val buffer = ByteArray(8192)
              while (true) {
                currentCoroutineContext().ensureActive()
                if (call.isCanceled()) {
                  throw CancellationException("Download cancelled")
                }
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                downloaded += read

                val now = System.currentTimeMillis()
                if (downloaded - lastReportedBytes >= 65_536L || now - lastReportedTime >= 80L) {
                  onProgress(downloaded, totalBytes)
                  lastReportedBytes = downloaded
                  lastReportedTime = now
                }
              }
              output.flush()
            }
          }

          onProgress(downloaded, if (totalBytes > 0L) totalBytes else downloaded)

          if (downloaded <= 0L || !hasZipMagicBytes(tmpFile)) {
            tmpFile.delete()
            throw IOException("Downloaded file is not a valid APK package")
          }

          if (targetFile.exists()) targetFile.delete()
          if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
          }
          Result.success(targetFile)
        }
      } catch (ce: CancellationException) {
        tmpFile.delete()
        Result.failure(ce)
      } catch (e: Exception) {
        tmpFile.delete()
        if (call.isCanceled()) {
          Result.failure(CancellationException("Download cancelled"))
        } else {
          Result.failure(e)
        }
      } finally {
        activeDownloadCall.compareAndSet(call, null)
      }
    }

  fun cancelActiveDownload(context: Context) {
    activeDownloadCall.getAndSet(null)?.cancel()
    runCatching {
      getUpdatesDir(context).listFiles()?.forEach { file ->
        if (file.name.endsWith(".part")) {
          file.delete()
        }
      }
    }
  }

  private fun hasZipMagicBytes(file: File): Boolean {
    if (!file.exists() || file.length() < 4L) return false
    return runCatching {
      FileInputStream(file).use { fis ->
        val header = ByteArray(4)
        val count = fis.read(header)
        count == 4 &&
          header[0] == 0x50.toByte() &&
          header[1] == 0x4B.toByte() &&
          header[2] == 0x03.toByte() &&
          header[3] == 0x04.toByte()
      }
    }.getOrDefault(false)
  }

  fun canInstallPackages(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
    } else {
      true
    }
  }

  fun createUnknownSourcesSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
      )
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
  }

  fun createInstallApkIntent(context: Context, apkFile: File): Intent {
    val contentUri =
      FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile,
      )
    return Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(contentUri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }
}
