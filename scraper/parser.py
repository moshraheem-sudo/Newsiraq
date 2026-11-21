"""HTML parser for Telegram public channel web pages (https://t.me/s/<username>)."""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from bs4 import BeautifulSoup, Tag

from scraper.config import SourceConfig

BG_URL_RE = re.compile(r"background-image\s*:\s*url\(\s*['\"]?([^'\")]+)['\"]?\s*\)", re.IGNORECASE)
WHITESPACE_RE = re.compile(r"[ \t\r\f\v]+")
MULTI_NEWLINE_RE = re.compile(r"\n{3,}")


class SourceParseError(Exception):
    """Raised when the fetched HTML cannot be parsed as a valid public Telegram channel."""


@dataclass
class ParsedPost:
    id: str
    source_id: str
    source_name: str
    source_username: str
    message_id: int
    category: str
    title: str
    content: str
    published_at: str
    original_url: str
    images: list[str]
    media: list[dict[str, str]]
    links: list[str]
    is_edited: bool
    content_hash: str


def _clean_text(raw: str) -> str:
    lines = [WHITESPACE_RE.sub(" ", line).strip() for line in raw.replace("\r\n", "\n").split("\n")]
    joined = "\n".join(lines).strip()
    return MULTI_NEWLINE_RE.sub("\n\n", joined)


def _derive_title(content: str, max_len: int = 140) -> str:
    """Derive a concise title strictly from the source text without inventing anything."""
    if not content:
        return ""
    for line in content.splitlines():
        candidate = line.strip()
        if candidate:
            if len(candidate) <= max_len:
                return candidate
            clipped = candidate[:max_len].rsplit(" ", 1)[0].strip()
            return (clipped if clipped else candidate[:max_len].strip()) + "…"
    return ""


def _extract_bg_url(style_attr: str | None) -> str | None:
    if not style_attr:
        return None
    match = BG_URL_RE.search(style_attr)
    if match:
        url = match.group(1).strip()
        if url.startswith("//"):
            return "https:" + url
        return url
    return None


def _compute_content_hash(
    title: str,
    content: str,
    images: list[str],
    media: list[dict[str, str]],
    links: list[str],
) -> str:
    payload = {
        "title": title,
        "content": content,
        "images": images,
        "media": media,
        "links": links,
    }
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _extract_text_and_links(message_el: Tag) -> tuple[str, list[str]]:
    text_el = message_el.select_one(".tgme_widget_message_text")
    links: list[str] = []
    seen_links: set[str] = set()

    def add_link(href: str | None) -> None:
        if not href:
            return
        clean = href.strip()
        if clean.startswith(("http://", "https://")) and clean not in seen_links:
            seen_links.add(clean)
            links.append(clean)

    if not text_el:
        return "", links

    # Preserve explicit <br> breaks before reading text
    text_clone = BeautifulSoup(str(text_el), "html.parser")
    for br in text_clone.find_all("br"):
        br.replace_with("\n")
    for anchor in text_clone.find_all("a"):
        add_link(anchor.get("href"))

    content = _clean_text(text_clone.get_text())
    return content, links


def _extract_media(message_el: Tag, links: list[str]) -> tuple[list[str], list[dict[str, str]], list[str]]:
    images: list[str] = []
    seen_images: set[str] = set()
    media: list[dict[str, str]] = []
    extra_links = list(links)
    seen_links = set(links)

    def add_image(url: str | None) -> None:
        if not url:
            return
        clean = url.strip()
        if clean.startswith(("http://", "https://")) and clean not in seen_images:
            seen_images.add(clean)
            images.append(clean)

    # Photos (including grouped albums)
    for photo_el in message_el.select(".tgme_widget_message_photo_wrap"):
        img_url = _extract_bg_url(photo_el.get("style"))
        if img_url:
            add_image(img_url)
            media.append({"type": "photo", "url": img_url, "thumbnail": img_url, "title": ""})

    # Videos & video thumbnails
    seen_video_urls: set[str] = set()
    for video_wrap in message_el.select(".tgme_widget_message_video_wrap, .tgme_widget_message_roundvideo_wrap"):
        thumb_el = video_wrap.select_one(".tgme_widget_message_video_thumb, .tgme_widget_message_roundvideo_thumb")
        thumb_url = _extract_bg_url(thumb_el.get("style") if thumb_el else None) or ""
        video_el = video_wrap.select_one("video")
        video_src = (video_el.get("src") or "").strip() if video_el else ""
        if video_src.startswith("//"):
            video_src = "https:" + video_src
        key = video_src or thumb_url
        if key and key not in seen_video_urls:
            seen_video_urls.add(key)
            media.append(
                {
                    "type": "video",
                    "url": video_src,
                    "thumbnail": thumb_url,
                    "title": "",
                }
            )
            if thumb_url:
                add_image(thumb_url)

    # Standalone <video> tags not inside video_wrap
    for video_el in message_el.select("video"):
        video_src = (video_el.get("src") or "").strip()
        if video_src.startswith("//"):
            video_src = "https:" + video_src
        if video_src and video_src not in seen_video_urls:
            seen_video_urls.add(video_src)
            media.append({"type": "video", "url": video_src, "thumbnail": "", "title": ""})

    # Audio / Voice
    for audio_el in message_el.select("audio"):
        audio_src = (audio_el.get("src") or "").strip()
        if audio_src:
            media.append({"type": "audio", "url": audio_src, "thumbnail": "", "title": ""})

    # Documents
    for doc_el in message_el.select(".tgme_widget_message_document_wrap"):
        doc_title_el = doc_el.select_one(".tgme_widget_message_document_title")
        doc_title = doc_title_el.get_text(" ", strip=True) if doc_title_el else ""
        media.append({"type": "document", "url": "", "thumbnail": "", "title": doc_title})

    # Link previews
    for preview_el in message_el.select("a.tgme_widget_message_link_preview"):
        href = (preview_el.get("href") or "").strip()
        if href.startswith(("http://", "https://")) and href not in seen_links:
            seen_links.add(href)
            extra_links.append(href)
        title_el = preview_el.select_one(".link_preview_title")
        preview_title = title_el.get_text(" ", strip=True) if title_el else ""
        preview_img_el = preview_el.select_one(".link_preview_image")
        preview_img = _extract_bg_url(preview_img_el.get("style") if preview_img_el else None) or ""
        if preview_img:
            add_image(preview_img)
        if href or preview_title:
            media.append(
                {
                    "type": "link_preview",
                    "url": href,
                    "thumbnail": preview_img,
                    "title": preview_title,
                }
            )

    return images, media, extra_links


def _normalize_iso_datetime(dt_raw: str | None) -> str:
    if not dt_raw:
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    try:
        dt = datetime.fromisoformat(dt_raw.strip())
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    except ValueError:
        return dt_raw.strip()


def parse_channel_html(html: str, source: SourceConfig, max_posts: int = 30) -> list[ParsedPost]:
    """Parse a Telegram public channel web preview HTML page into structured posts."""
    if not html or len(html.strip()) < 200:
        raise SourceParseError(f"Empty or truncated HTML received for {source.username}")

    soup = BeautifulSoup(html, "lxml")

    error_el = soup.select_one(".tgme_page_error")
    if error_el:
        raise SourceParseError(
            f"Telegram returned page error for {source.username}: {error_el.get_text(' ', strip=True)}"
        )

    channel_title_el = soup.select_one(".tgme_channel_info_header_title")
    if not channel_title_el and not soup.select(".tgme_widget_message"):
        raise SourceParseError(
            f"Page does not appear to be a public Telegram channel preview for {source.username}"
        )

    resolved_source_name = source.name or (
        channel_title_el.get_text(" ", strip=True) if channel_title_el else source.username
    )

    message_nodes = soup.select(".tgme_widget_message")
    if max_posts > 0 and len(message_nodes) > max_posts:
        message_nodes = message_nodes[-max_posts:]

    posts: list[ParsedPost] = []
    for msg in message_nodes:
        data_post = (msg.get("data-post") or "").strip()
        if "/" not in data_post:
            continue

        post_channel, post_id_str = data_post.rsplit("/", 1)
        if not post_id_str.isdigit():
            continue
        message_id = int(post_id_str)

        date_anchor = msg.select_one("a.tgme_widget_message_date")
        time_el = date_anchor.select_one("time") if date_anchor else None
        published_at = _normalize_iso_datetime(time_el.get("datetime") if time_el else None)
        original_url = (
            date_anchor.get("href", "").strip()
            if date_anchor and date_anchor.get("href")
            else f"https://t.me/{source.username}/{message_id}"
        )

        content, links = _extract_text_and_links(msg)
        images, media, links = _extract_media(msg, links)

        # Skip pure service placeholders that carry neither text nor media
        if not content and not images and not media:
            continue

        info_el = msg.select_one(".tgme_widget_message_info")
        info_text = info_el.get_text(" ", strip=True).lower() if info_el else ""
        is_edited = "edited" in info_text

        title = _derive_title(content)
        content_hash = _compute_content_hash(title, content, images, media, links)

        posts.append(
            ParsedPost(
                id=f"{source.username}_{message_id}",
                source_id=source.id,
                source_name=resolved_source_name,
                source_username=post_channel or source.username,
                message_id=message_id,
                category=source.category,
                title=title,
                content=content,
                published_at=published_at,
                original_url=original_url,
                images=images,
                media=media,
                links=links,
                is_edited=is_edited,
                content_hash=content_hash,
            )
        )

    return posts
