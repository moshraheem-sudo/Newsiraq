"""Unit tests for Iraq News Hub parser, config validation, and deduplicating storage."""

from __future__ import annotations

import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from scraper.config import SourceConfig, validate_public_telegram_url
from scraper.parser import SourceParseError, parse_channel_html
from scraper.storage import merge_posts

SAMPLE_HTML = """
<html>
<body>
  <div class="tgme_channel_info_header_title"><span>وكالة تجريبية</span></div>
  <div class="tgme_widget_message_wrap">
    <div class="tgme_widget_message" data-post="testchan/101">
      <div class="tgme_widget_message_photo_wrap" style="background-image:url('https://cdn.example.com/photo1.jpg')"></div>
      <div class="tgme_widget_message_photo_wrap" style="background-image:url('https://cdn.example.com/photo2.jpg')"></div>
      <div class="tgme_widget_message_text">خبر عاجل من بغداد<br>تفاصيل إضافية <a href="https://example.org/details">اقرأ المزيد</a></div>
      <div class="tgme_widget_message_info">
        <span class="tgme_widget_message_views">1.2K</span>
        <a class="tgme_widget_message_date" href="https://t.me/testchan/101">
          <time datetime="2026-11-20T12:00:00+00:00">12:00</time>
        </a>
      </div>
    </div>
  </div>
  <div class="tgme_widget_message_wrap">
    <div class="tgme_widget_message" data-post="testchan/102">
      <div class="tgme_widget_message_video_wrap">
        <i class="tgme_widget_message_video_thumb" style="background-image:url('https://cdn.example.com/vthumb.jpg')"></i>
        <video src="https://cdn.example.com/clip.mp4"></video>
      </div>
      <div class="tgme_widget_message_text">فيديو ميداني</div>
      <div class="tgme_widget_message_info">
        edited
        <a class="tgme_widget_message_date" href="https://t.me/testchan/102">
          <time datetime="2026-11-20T13:30:00+00:00">13:30</time>
        </a>
      </div>
    </div>
  </div>
</body>
</html>
"""


class ScraperTests(unittest.TestCase):
    def setUp(self) -> None:
        self.source = SourceConfig(
            id="test",
            name="قناة الاختبار",
            username="testchan",
            url="https://t.me/s/testchan",
            category="أخبار عامة",
            language="ar",
            enabled=True,
        )

    def test_validate_public_telegram_url(self) -> None:
        self.assertEqual(
            validate_public_telegram_url("https://t.me/s/inainaiq/", "inainaiq"),
            "https://t.me/s/inainaiq",
        )
        with self.assertRaises(ValueError):
            validate_public_telegram_url("https://t.me/inainaiq", "inainaiq")
        with self.assertRaises(ValueError):
            validate_public_telegram_url("https://api.telegram.org/bot123/getUpdates", "inainaiq")

    def test_parse_multimedia_and_edited_posts(self) -> None:
        posts = parse_channel_html(SAMPLE_HTML, self.source)
        self.assertEqual(len(posts), 2)

        first = posts[0]
        self.assertEqual(first.id, "testchan_101")
        self.assertEqual(first.message_id, 101)
        self.assertEqual(first.title, "خبر عاجل من بغداد")
        self.assertIn("تفاصيل إضافية", first.content)
        self.assertEqual(first.links, ["https://example.org/details"])
        self.assertEqual(len(first.images), 2)
        self.assertFalse(first.is_edited)

        second = posts[1]
        self.assertEqual(second.id, "testchan_102")
        self.assertTrue(second.is_edited)
        self.assertEqual(second.media[0]["type"], "video")
        self.assertEqual(second.media[0]["url"], "https://cdn.example.com/clip.mp4")

    def test_deduplication_preserves_history_and_tracks_edits(self) -> None:
        posts = parse_channel_html(SAMPLE_HTML, self.source)
        old_item = {
            "id": "testchan_50",
            "source_id": "test",
            "source_name": "قناة الاختبار",
            "source_username": "testchan",
            "message_id": 50,
            "category": "أخبار عامة",
            "title": "خبر قديم محفوظ",
            "content": "خبر قديم محفوظ",
            "published_at": "2026-11-19T10:00:00Z",
            "fetched_at": "2026-11-19T10:05:00Z",
            "updated_at": "2026-11-19T10:05:00Z",
            "original_url": "https://t.me/testchan/50",
            "images": [],
            "media": [],
            "links": [],
            "is_edited": False,
            "content_hash": "oldhash",
        }
        merged1, stats1 = merge_posts([old_item], posts, "2026-11-20T14:00:00Z")
        self.assertEqual(stats1.new_count, 2)
        self.assertEqual(len(merged1), 3)  # Old post preserved!

        # Re-run unchanged -> 0 new, 0 updated
        merged2, stats2 = merge_posts(merged1, posts, "2026-11-20T14:10:00Z")
        self.assertEqual(stats2.new_count, 0)
        self.assertEqual(stats2.updated_count, 0)
        self.assertEqual(stats2.unchanged_count, 2)

        # Simulate edited post
        edited_html = SAMPLE_HTML.replace("خبر عاجل من بغداد", "خبر عاجل ومحدث من بغداد")
        edited_posts = parse_channel_html(edited_html, self.source)
        merged3, stats3 = merge_posts(merged2, edited_posts, "2026-11-20T14:20:00Z")
        self.assertEqual(stats3.updated_count, 1)
        updated_101 = next(item for item in merged3 if item["id"] == "testchan_101")
        self.assertEqual(updated_101["title"], "خبر عاجل ومحدث من بغداد")
        self.assertTrue(updated_101["is_edited"])
        self.assertEqual(updated_101["fetched_at"], "2026-11-20T14:00:00Z")
        self.assertEqual(updated_101["updated_at"], "2026-11-20T14:20:00Z")

    def test_error_page_raises(self) -> None:
        with self.assertRaises(SourceParseError):
            parse_channel_html("<html><body><div class='tgme_page_error'>Not found</div></body></html>", self.source)

    def test_x_list_validation_and_parsing(self) -> None:
        import json
        from scraper.config import validate_x_list_url
        from scraper.x_list import parse_x_list_html

        self.assertEqual(
            validate_x_list_url("https://x.com/i/lists/1763642433328279733/"),
            "https://x.com/i/lists/1763642433328279733",
        )
        with self.assertRaises(ValueError):
            validate_x_list_url("https://x.com/someuser")

        x_source = SourceConfig(
            id="x_list_test",
            name="قائمة أخبار X",
            username="list_1763642433328279733",
            url="https://x.com/i/lists/1763642433328279733",
            category="أخبار منوعة (X)",
            language="ar",
            enabled=True,
            source_type="x_list",
            max_posts=50,
            fetch_interval_minutes=60,
        )
        sample_next_data = {
            "props": {
                "pageProps": {
                    "timeline": {
                        "entries": [
                            {
                                "content": {
                                    "tweet": {
                                        "id_str": "2124007673322471645",
                                        "created_at": "Sat Nov 21 05:11:06 +0000 2026",
                                        "full_text": "خبر عاجل من العراق https://t.co/abc123",
                                        "user": {"name": "قناة إخبارية", "screen_name": "NewsChannel"},
                                        "entities": {"urls": []},
                                        "extended_entities": {
                                            "media": [
                                                {
                                                    "url": "https://t.co/abc123",
                                                    "type": "video",
                                                    "media_url_https": "https://pbs.twimg.com/thumb.jpg",
                                                    "video_info": {
                                                        "variants": [
                                                            {"content_type": "video/mp4", "bitrate": 832000, "url": "https://video.twimg.com/clip.mp4"}
                                                        ]
                                                    },
                                                }
                                            ]
                                        },
                                    }
                                }
                            }
                        ]
                    }
                }
            }
        }
        html = f'<script id="__NEXT_DATA__" type="application/json">{json.dumps(sample_next_data)}</script>'
        posts = parse_x_list_html(html, x_source, max_posts=50)
        self.assertEqual(len(posts), 1)
        self.assertEqual(posts[0].id, "list_1763642433328279733_2124007673322471645")
        self.assertEqual(posts[0].title, "خبر عاجل من العراق")
        self.assertEqual(posts[0].images, ["https://pbs.twimg.com/thumb.jpg"])
        self.assertEqual(posts[0].media[0]["type"], "video")
        self.assertEqual(posts[0].media[0]["url"], "https://video.twimg.com/clip.mp4")

    def test_telegram_pagination_dedup_and_limit(self) -> None:
        from scraper.fetcher import extract_before_cursor
        from scraper.parser import parse_channel_pages

        page1 = """
        <html><body>
          <div class="tgme_channel_info_header_title"><span>قناة الاختبار</span></div>
          <a class="tme_messages_more" href="/s/testchan?before=101" data-before="101">المزيد</a>
          <div class="tgme_widget_message" data-post="testchan/102">
            <div class="tgme_widget_message_text">الخبر الثاني</div>
            <a class="tgme_widget_message_date" href="https://t.me/testchan/102"><time datetime="2026-11-21T10:00:00+00:00"></time></a>
          </div>
          <div class="tgme_widget_message" data-post="testchan/103">
            <div class="tgme_widget_message_text">الخبر الثالث</div>
            <a class="tgme_widget_message_date" href="https://t.me/testchan/103"><time datetime="2026-11-21T10:05:00+00:00"></time></a>
          </div>
        </body></html>
        """
        page2 = """
        <html><body>
          <div class="tgme_channel_info_header_title"><span>قناة الاختبار</span></div>
          <div class="tgme_widget_message" data-post="testchan/100">
            <div class="tgme_widget_message_text">الخبر الأول</div>
            <a class="tgme_widget_message_date" href="https://t.me/testchan/100"><time datetime="2026-11-21T09:50:00+00:00"></time></a>
          </div>
          <div class="tgme_widget_message" data-post="testchan/102">
            <div class="tgme_widget_message_text">الخبر الثاني</div>
            <a class="tgme_widget_message_date" href="https://t.me/testchan/102"><time datetime="2026-11-21T10:00:00+00:00"></time></a>
          </div>
        </body></html>
        """
        self.assertEqual(extract_before_cursor(page1, "testchan"), 101)
        posts = parse_channel_pages([page1, page2], self.source, max_posts=2)
        self.assertEqual([p.message_id for p in posts], [102, 103])


if __name__ == "__main__":
    unittest.main()
