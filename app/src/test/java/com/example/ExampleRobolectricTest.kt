package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.GitHubUpdateManager
import com.example.data.LiveFeedFetcher
import com.example.data.NewsSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("أخبار العالم", appName)
  }

  @Test
  fun `loads bundled 25 live source configs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val raw = context.assets.open("sources.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    val configs = LiveFeedFetcher.parseSourcesConfig(raw)
    assertEquals(25, configs.size)
  }

  @Test
  fun `parses rss feed xml cleanly without html glyphs`() {
    val sampleXml =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
        <channel>
          <item>
            <title>عاجل | خبر تجريبي</title>
            <link>https://example.com/news/1</link>
            <guid>https://example.com/news/1</guid>
            <pubDate>Sat, 21 Nov 2026 07:30:00 GMT</pubDate>
            <description><![CDATA[<img src="https://example.com/pic.jpg" />تفاصيل الخبر العاجل]]></description>
            <media:content url="https://example.com/hd.jpg" width="1200" medium="image" />
          </item>
        </channel>
      </rss>
      """.trimIndent()
    val src =
      NewsSourceConfig(
        id = "test_rss",
        type = "rss",
        name = "مصدر تجريبي",
        username = "test_rss",
        url = "https://example.com/rss",
        category = "أخبار العراق",
      )
    val parsed =
      LiveFeedFetcher.javaClass
        .getDeclaredMethod(
          "parseRssXml",
          String::class.java,
          NewsSourceConfig::class.java,
          String::class.java,
        )
        .apply { isAccessible = true }
        .invoke(LiveFeedFetcher, sampleXml, src, "2026-11-21T07:45:00Z") as List<*>
    assertEquals(1, parsed.size)
    val article = parsed.first() as com.example.data.NewsArticle
    assertEquals("عاجل | خبر تجريبي", article.title)
    assertEquals("تفاصيل الخبر العاجل", article.content)
    assertTrue(article.images.contains("https://example.com/hd.jpg"))
    assertEquals("2026-11-21T07:30:00Z", article.publishedAt)
  }

  @Test
  fun `parses rss feed with bare ampersands and split date`() {
    val malformedXml =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <rss version="2.0">
        <channel>
          <item>
            <title>أخبار الاقتصاد & الطاقة</title>
            <link>https://example.com/news/2?a=1&b=2</link>
            <guid>https://example.com/news/2</guid>
            <pubDate>Sat, 21 Nov 2026 08:15:00 GMT</pubDate>
            <description>تفاصيل التقرير الاقتصادي</description>
          </item>
        </channel>
      </rss>
      """.trimIndent()
    val src =
      NewsSourceConfig(
        id = "test_rss_amp",
        type = "rss",
        name = "مصدر اقتصادي",
        username = "test_rss_amp",
        url = "https://example.com/rss",
        category = "أخبار عربية وعالمية",
      )
    val parsed =
      LiveFeedFetcher.javaClass
        .getDeclaredMethod(
          "parseRssXml",
          String::class.java,
          NewsSourceConfig::class.java,
          String::class.java,
        )
        .apply { isAccessible = true }
        .invoke(LiveFeedFetcher, malformedXml, src, "2026-11-21T08:30:00Z") as List<*>
    assertEquals(1, parsed.size)
    val article = parsed.first() as com.example.data.NewsArticle
    assertEquals("أخبار الاقتصاد & الطاقة", article.title)
    assertEquals("2026-11-21T08:15:00Z", article.publishedAt)
  }

  @Test
  fun `parses telegram reply post using main text and preserving source username casing`() {
    val telegramHtml =
      """
      <div class="tgme_widget_message_wrap js-widget_message_wrap">
        <div class="tgme_widget_message" data-post="rudawarabic/9988">
          <a class="tgme_widget_message_reply">
            <div class="tgme_widget_message_text js-message_text">نص مقتبس قديم</div>
          </a>
          <div class="tgme_widget_message_text js-message_text">الخبر الرئيسي المحدث من بغداد</div>
          <div class="tgme_widget_message_info">
            <a class="tgme_widget_message_date" href="https://t.me/rudawarabic/9988">
              <time datetime="2026-11-21T09:00:00+00:00"></time>
            </a>
          </div>
        </div>
      </div>
      """.trimIndent()
    val src =
      NewsSourceConfig(
        id = "rudaw_ar",
        type = "telegram_web",
        name = "رووداو عربية",
        username = "RudawArabic",
        url = "https://t.me/s/RudawArabic",
        category = "أخبار العراق",
      )
    val parsed =
      LiveFeedFetcher.javaClass
        .getDeclaredMethod(
          "parseTelegramHtml",
          String::class.java,
          NewsSourceConfig::class.java,
          String::class.java,
        )
        .apply { isAccessible = true }
        .invoke(LiveFeedFetcher, telegramHtml, src, "2026-11-21T09:10:00Z") as List<*>
    assertEquals(1, parsed.size)
    val article = parsed.first() as com.example.data.NewsArticle
    assertEquals("RudawArabic", article.sourceUsername)
    assertEquals("الخبر الرئيسي المحدث من بغداد", article.title)
    assertEquals("الخبر الرئيسي المحدث من بغداد", article.content)
  }

  @Test
  fun `localizes category names cleanly for english UI`() {
    val enStrings = com.example.ui.stringsFor(com.example.data.AppLanguage.ENGLISH)
    assertEquals("Iraq News", enStrings.localizeCategory("أخبار العراق"))
    assertEquals("Arab & World", enStrings.localizeCategory("أخبار عربية وعالمية"))
    assertEquals("World News", enStrings.localizeCategory("أخبار عالمية"))
    assertEquals("X Feed", enStrings.localizeCategory("أخبار منوعة (X)"))
  }

  @Test
  fun `compares semantic versions accurately for github updates`() {
    assertTrue(GitHubUpdateManager.isNewerVersion("v1.8.0", "1.7.0"))
    assertTrue(GitHubUpdateManager.isNewerVersion("2.0", "1.9.10"))
    assertFalse(GitHubUpdateManager.isNewerVersion("v1.7.0", "1.7.0"))
    assertFalse(GitHubUpdateManager.isNewerVersion("v1.6.5", "1.7.0"))
  }

  @Test
  fun `parses github latest release json with apk asset`() {
    val releaseJson =
      """
      {
        "tag_name": "v1.8.0",
        "name": "World News v1.8.0",
        "body": "Performance & feed updates",
        "draft": false,
        "published_at": "2026-11-22T10:00:00Z",
        "html_url": "https://github.com/moshraheem-sudo/Newsiraq/releases/tag/v1.8.0",
        "assets": [
          {
            "name": "checksums.txt",
            "content_type": "text/plain",
            "size": 256,
            "browser_download_url": "https://github.com/moshraheem-sudo/Newsiraq/releases/download/v1.8.0/checksums.txt"
          },
          {
            "name": "WorldNews-v1.8.0.apk",
            "content_type": "application/vnd.android.package-archive",
            "size": 26214400,
            "browser_download_url": "https://github.com/moshraheem-sudo/Newsiraq/releases/download/v1.8.0/WorldNews-v1.8.0.apk"
          }
        ]
      }
      """.trimIndent()
    val parsed = GitHubUpdateManager.parseGitHubReleaseJson(releaseJson)
    assertNotNull(parsed)
    assertEquals("1.8.0", parsed!!.versionName)
    assertEquals("WorldNews-v1.8.0.apk", parsed.apkFileName)
    assertEquals(26_214_400L, parsed.apkSizeBytes)
    assertEquals(
      "https://github.com/moshraheem-sudo/Newsiraq/releases/download/v1.8.0/WorldNews-v1.8.0.apk",
      parsed.apkDownloadUrl,
    )
    assertEquals("25.0 MB", GitHubUpdateManager.formatMegabytes(parsed.apkSizeBytes))
  }

  @Test
  fun `creates valid FileProvider install intent for downloaded apk`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val apk = File(GitHubUpdateManager.getUpdatesDir(context), "WorldNews-test.apk")
    apk.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
    val intent = GitHubUpdateManager.createInstallApkIntent(context, apk)
    assertEquals(Intent.ACTION_VIEW, intent.action)
    assertEquals("application/vnd.android.package-archive", intent.type)
    assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    assertNotNull(intent.data)
    assertTrue(intent.data.toString().contains(".fileprovider"))
    apk.delete()
  }
}
