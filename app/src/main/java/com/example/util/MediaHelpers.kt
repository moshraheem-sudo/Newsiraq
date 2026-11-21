package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.example.data.AppLanguage
import com.example.data.NewsArticle
import com.example.ui.AppStrings

object MediaDownloader {
  fun enqueueDownload(
    context: Context,
    url: String,
    isVideo: Boolean,
    articleTitle: String,
    language: AppLanguage,
  ): Result<Long> {
    return runCatching {
      val cleanUrl = url.trim()
      require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
        "Invalid media URL"
      }
      val uri = Uri.parse(cleanUrl)
      val dm =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
          ?: error("DownloadManager unavailable")

      val urlExt = MimeTypeMap.getFileExtensionFromUrl(cleanUrl).lowercase().takeIf { it.isNotBlank() }
      val ext = urlExt ?: if (isVideo) "mp4" else "jpg"
      val prefix = if (isVideo) "video" else "photo"
      val fileName = "IraqNews_${prefix}_${System.currentTimeMillis()}.$ext"
      val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

      val displayTitle =
        articleTitle.trim().take(60).ifBlank {
          if (language == AppLanguage.ARABIC) "وسائط أخبار العراق" else "Iraq News Media"
        }

      fun buildRequest(publicDir: Boolean): DownloadManager.Request {
        return DownloadManager.Request(uri)
          .setTitle(displayTitle)
          .setDescription(
            if (language == AppLanguage.ARABIC) "تنزيل عبر تطبيق أخبار العراق"
            else "Downloading via Iraq News"
          )
          .setMimeType(mimeType)
          .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
          .setAllowedOverMetered(true)
          .setAllowedOverRoaming(true)
          .apply {
            if (publicDir) {
              setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "IraqNews/$fileName",
              )
            } else {
              setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "IraqNews/$fileName",
              )
            }
          }
      }

      try {
        dm.enqueue(buildRequest(publicDir = true))
      } catch (_: SecurityException) {
        // Fallback on legacy Android <= 8.1 / 9 devices if external public storage isn't writable
        dm.enqueue(buildRequest(publicDir = false))
      }
    }
  }
}

object SafeIntentHelper {
  fun openExternalUrl(context: Context, rawUrl: String): Boolean {
    val cleaned = rawUrl.trim()
    if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) return false
    return runCatching {
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(cleaned)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }
}

object NewsShareHelper {
  fun formatArticleForShare(article: NewsArticle, strings: AppStrings): String {
    val parts = mutableListOf<String>()
    val cleanTitle = article.title.trim()
    val cleanBody = article.content.trim()

    if (cleanTitle.isNotBlank()) {
      parts.add(cleanTitle)
    }
    if (cleanBody.isNotBlank() && cleanBody != cleanTitle) {
      parts.add(cleanBody)
    }
    val metaLine = buildString {
      if (article.sourceName.isNotBlank()) {
        append(article.sourceName.trim())
      }
      if (article.originalUrl.isNotBlank()) {
        if (isNotEmpty()) append("\n")
        append(article.originalUrl.trim())
      }
    }
    if (metaLine.isNotBlank()) {
      parts.add(metaLine)
    }
    parts.add(strings.shareFooter)
    return parts.joinToString("\n\n")
  }
}
