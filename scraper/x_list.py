"""Fetcher and parser for public X (Twitter) Lists via official syndication embed timeline."""

from __future__ import annotations

import json
import logging
import re
from datetime import datetime, timezone
from typing import Any

import requests

from scraper.config import ScraperDefaults, SourceConfig
from scraper.fetcher import SourceFetchError
from scraper.parser import ParsedPost, SourceParseError, _compute_content_hash, _derive_title

logger = logging.getLogger(__name__)

NEXT_DATA_RE = re.compile(
    r'<script\s+id="__NEXT_DATA__"\s+type="application/json">(.*?)</script>',
    re.DOTALL,
)
WHITESPACE_RE = re.compile(r"[ \t\r\f\v]+")
MULTI_NEWLINE_RE = re.compile(r"\n{3,}")


def parse_twitter_datetime(raw: str | None) -> str:
    if not raw:
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    try:
        dt = datetime.strptime(raw.strip(), "%a %b %d %H:%M:%S %z %Y")
        return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    except ValueError:
        return raw.strip()


def _extract_list_id(source: SourceConfig) -> str:
    match = re.search(r"(\d{6,})", source.url) or re.search(r"(\d{6,})", source.username)
    if not match:
        raise ValueError(f"Could not determine numeric X list ID from {source.url}")
    return match.group(1)


def fetch_x_list_html(
    list_id: str,
    defaults: ScraperDefaults,
    session: requests.Session,
) -> str:
    syndication_url = f"https://syndication.twitter.com/srv/timeline-list/list-id/{list_id}"
    logger.info("Fetching X list %s (%s)", list_id, syndication_url)
    try:
        resp = session.get(
            syndication_url,
            timeout=defaults.request_timeout_seconds,
            allow_redirects=True,
        )
        if resp.status_code in (403, 429):
            raise SourceFetchError(
                f"X syndication rate-limited/restricted (HTTP {resp.status_code}) for list {list_id}."
            )
        if resp.status_code >= 400:
            raise SourceFetchError(f"HTTP {resp.status_code} while fetching X list {list_id}")
        return resp.text
    except requests.RequestException as exc:
        raise SourceFetchError(f"Network error fetching X list {list_id}: {exc}") from exc


def parse_x_list_html(html: str, source: SourceConfig, max_posts: int = 50) -> list[ParsedPost]:
    match = NEXT_DATA_RE.search(html or "")
    if not match:
        raise SourceParseError(
            f"Missing __NEXT_DATA__ payload in X syndication response for {source.username}"
        )

    try:
        payload: dict[str, Any] = json.loads(match.group(1))
    except json.JSONDecodeError as exc:
        raise SourceParseError(f"Invalid JSON in X syndication payload: {exc}") from exc

    entries = (
        payload.get("props", {})
        .get("pageProps", {})
        .get("timeline", {})
        .get("entries", [])
    )
    if not entries:
        return []

    posts: list[ParsedPost] = []
    for entry in entries:
        tweet = entry.get("content", {}).get("tweet")
        if not isinstance(tweet, dict):
            continue

        id_str = str(tweet.get("id_str") or "").strip()
        if not id_str.isdigit():
            continue
        message_id = int(id_str)

        user = tweet.get("user") or {}
        screen_name = str(user.get("screen_name") or "x").strip()
        author_name = str(user.get("name") or screen_name).strip()

        raw_text = str(tweet.get("full_text") or tweet.get("text") or "").strip()
        entities = tweet.get("entities") or {}

        links: list[str] = []
        seen_links: set[str] = set()
        for url_obj in entities.get("urls", []):
            short_url = str(url_obj.get("url") or "")
            expanded_url = str(url_obj.get("expanded_url") or short_url).strip()
            if short_url and expanded_url:
                raw_text = raw_text.replace(short_url, expanded_url)
            if expanded_url.startswith(("http://", "https://")) and expanded_url not in seen_links:
                seen_links.add(expanded_url)
                links.append(expanded_url)

        ext_media = (tweet.get("extended_entities") or {}).get("media") or entities.get("media") or []
        images: list[str] = []
        seen_images: set[str] = set()
        media_items: list[dict[str, str]] = []

        for m in ext_media:
            short_media_url = str(m.get("url") or "")
            if short_media_url:
                raw_text = raw_text.replace(short_media_url, "").strip()

            m_type = str(m.get("type") or "photo")
            thumb_url = str(m.get("media_url_https") or m.get("media_url") or "").strip()

            if m_type == "photo":
                if thumb_url and thumb_url not in seen_images:
                    seen_images.add(thumb_url)
                    images.append(thumb_url)
                    media_items.append(
                        {"type": "photo", "url": thumb_url, "thumbnail": thumb_url, "title": ""}
                    )
            elif m_type in ("video", "animated_gif"):
                variants = (m.get("video_info") or {}).get("variants") or []
                mp4_variants = [
                    v for v in variants if v.get("content_type") == "video/mp4" and v.get("url")
                ]
                mp4_variants.sort(key=lambda v: int(v.get("bitrate") or 0), reverse=True)
                best_video = str(mp4_variants[0]["url"]) if mp4_variants else ""
                if thumb_url and thumb_url not in seen_images:
                    seen_images.add(thumb_url)
                    images.append(thumb_url)
                if best_video or thumb_url:
                    media_items.append(
                        {
                            "type": "video",
                            "url": best_video,
                            "thumbnail": thumb_url,
                            "title": "",
                        }
                    )

        cleaned_lines = [
            WHITESPACE_RE.sub(" ", line).strip()
            for line in raw_text.replace("\r\n", "\n").split("\n")
        ]
        body_text = MULTI_NEWLINE_RE.sub("\n\n", "\n".join(cleaned_lines).strip())

        if not body_text and not images and not media_items:
            continue

        # Include real account attribution without inventing headlines
        attributed_content = f"@{screen_name} ({author_name})\n{body_text}".strip()
        title = _derive_title(body_text) or f"{author_name} (@{screen_name})"
        published_at = parse_twitter_datetime(tweet.get("created_at"))
        original_url = f"https://x.com/{screen_name}/status/{id_str}"
        content_hash = _compute_content_hash(
            title, attributed_content, images, media_items, links
        )

        posts.append(
            ParsedPost(
                id=f"{source.username}_{message_id}",
                source_id=source.id,
                source_name=source.name,
                source_username=source.username,
                message_id=message_id,
                category=source.category,
                title=title,
                content=attributed_content,
                published_at=published_at,
                original_url=original_url,
                images=images,
                media=media_items,
                links=links,
                is_edited=False,
                content_hash=content_hash,
            )
        )

        if max_posts > 0 and len(posts) >= max_posts:
            break

    return posts


def fetch_and_parse_x_list(
    source: SourceConfig,
    defaults: ScraperDefaults,
    session: requests.Session,
    max_posts: int,
) -> list[ParsedPost]:
    list_id = _extract_list_id(source)
    html = fetch_x_list_html(list_id, defaults, session)
    return parse_x_list_html(html, source, max_posts=max_posts)
