"""Fetcher and parser for public RSS / Atom news feeds."""

from __future__ import annotations

import logging
import re
import zlib
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from html import unescape
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scraper.config import ScraperDefaults, SourceConfig
from scraper.fetcher import SourceFetchError
from scraper.parser import ParsedPost, SourceParseError, _compute_content_hash, _derive_title

logger = logging.getLogger(__name__)
WHITESPACE_RE = re.compile(r"[ \t\r\f\v]+")
MULTI_NEWLINE_RE = re.compile(r"\n{3,}")
VIDEO_EXT_RE = re.compile(r"\.(mp4|m3u8|webm|mov)(\?|$)", re.IGNORECASE)
IMAGE_EXT_RE = re.compile(r"\.(jpg|jpeg|png|webp|gif)(\?|$)", re.IGNORECASE)


def parse_rss_datetime(raw: str | None) -> str:
    if not raw or not raw.strip():
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    cleaned = raw.strip()
    try:
        dt = parsedate_to_datetime(cleaned)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    except (TypeError, ValueError):
        pass
    try:
        dt = datetime.fromisoformat(cleaned.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    except ValueError:
        return cleaned


def _stable_rss_message_id(guid_or_link: str) -> int:
    digest = zlib.crc32(guid_or_link.encode("utf-8")) & 0x7FFFFFFF
    return digest if digest > 0 else 1


def _strip_html(raw_html: str) -> tuple[str, list[str], list[str]]:
    """Return clean text plus any <img> and <video> URLs embedded inside HTML fragments."""
    if not raw_html:
        return "", [], []
    decoded = unescape(raw_html)
    soup = BeautifulSoup(decoded, "lxml")
    imgs: list[str] = []
    vids: list[str] = []

    for img in soup.find_all("img"):
        src = (img.get("src") or "").strip()
        if src.startswith("//"):
            src = "https:" + src
        if src.startswith(("http://", "https://")) and src not in imgs:
            imgs.append(src)

    for vid in soup.find_all("video"):
        vsrc = (vid.get("src") or "").strip()
        if not vsrc:
            src_tag = vid.find("source")
            vsrc = (src_tag.get("src") or "").strip() if src_tag else ""
        if vsrc.startswith("//"):
            vsrc = "https:" + vsrc
        if vsrc.startswith(("http://", "https://")) and vsrc not in vids:
            vids.append(vsrc)

    for br in soup.find_all("br"):
        br.replace_with("\n")

    lines = [
        WHITESPACE_RE.sub(" ", line).strip()
        for line in soup.get_text("\n").replace("\r\n", "\n").split("\n")
    ]
    cleaned = MULTI_NEWLINE_RE.sub("\n\n", "\n".join(line for line in lines if line).strip())
    return cleaned, imgs, vids


def fetch_rss_xml(
    source: SourceConfig,
    defaults: ScraperDefaults,
    session: Optional[requests.Session] = None,
) -> bytes:
    own_session = session is None
    active = session or requests.Session()
    try:
        logger.info("Fetching RSS %s (%s)", source.name, source.url)
        resp = active.get(
            source.url,
            timeout=defaults.request_timeout_seconds,
            allow_redirects=True,
        )
        if resp.status_code in (403, 429):
            raise SourceFetchError(
                f"RSS access restricted (HTTP {resp.status_code}) for {source.url}"
            )
        if resp.status_code >= 400:
            raise SourceFetchError(f"HTTP {resp.status_code} while fetching RSS {source.url}")
        return resp.content
    except requests.RequestException as exc:
        raise SourceFetchError(f"Network error fetching RSS {source.url}: {exc}") from exc
    finally:
        if own_session:
            active.close()


def parse_rss_xml(xml_bytes: bytes | str, source: SourceConfig, max_posts: int = 50) -> list[ParsedPost]:
    if not xml_bytes:
        raise SourceParseError(f"Empty RSS response from {source.url}")

    soup = BeautifulSoup(xml_bytes, "xml")
    items = soup.find_all("item")
    is_atom = False
    if not items:
        items = soup.find_all("entry")
        is_atom = bool(items)

    if not items:
        raise SourceParseError(f"No RSS/Atom items found in {source.url}")

    posts: list[ParsedPost] = []
    seen_ids: set[int] = set()

    for item in items:
        title_el = item.find("title")
        raw_title = unescape(title_el.get_text(" ", strip=True)) if title_el else ""
        title_clean = WHITESPACE_RE.sub(" ", raw_title).strip()

        link_el = item.find("link")
        link_url = ""
        if link_el:
            link_url = (link_el.get("href") or link_el.get_text(strip=True) or "").strip()

        guid_el = item.find("guid") or item.find("id")
        guid_str = guid_el.get_text(strip=True) if guid_el else ""
        unique_key = guid_str or link_url or title_clean
        if not unique_key:
            continue

        message_id = _stable_rss_message_id(unique_key)
        if message_id in seen_ids:
            continue
        seen_ids.add(message_id)

        desc_el = (
            item.find("encoded")
            or item.find("content")
            or item.find("description")
            or item.find("summary")
        )
        desc_raw = desc_el.get_text() if desc_el else ""
        desc_text, desc_imgs, desc_vids = _strip_html(desc_raw)

        images: list[str] = []
        seen_images: set[str] = set()
        media_items: list[dict[str, str]] = []
        seen_videos: set[str] = set()

        def add_image(u: str) -> None:
            clean = u.strip()
            if clean.startswith("//"):
                clean = "https:" + clean
            if clean.startswith(("http://", "https://")) and clean not in seen_images:
                seen_images.add(clean)
                images.append(clean)

        def add_video(u: str, thumb: str = "") -> None:
            clean = u.strip()
            if clean.startswith("//"):
                clean = "https:" + clean
            if clean.startswith(("http://", "https://")) and clean not in seen_videos:
                seen_videos.add(clean)
                media_items.append(
                    {"type": "video", "url": clean, "thumbnail": thumb, "title": ""}
                )

        # Standard RSS <enclosure> + escaped HTML <img> inside <enclosure> (e.g. Independent Arabia)
        for enc in item.find_all("enclosure"):
            enc_url = (enc.get("url") or "").strip()
            enc_type = (enc.get("type") or "").lower()
            if enc_url:
                if enc_type.startswith("video/") or VIDEO_EXT_RE.search(enc_url):
                    add_video(enc_url)
                elif enc_type.startswith("image/") or IMAGE_EXT_RE.search(enc_url) or not enc_type:
                    add_image(enc_url)
            enc_inner = enc.get_text()
            if enc_inner and "<img" in enc_inner.lower():
                _, inner_imgs, inner_vids = _strip_html(enc_inner)
                for im in inner_imgs:
                    add_image(im)
                for vd in inner_vids:
                    add_video(vd)

        # Media RSS tags (<media:content>, <media:thumbnail>)
        for m_el in item.find_all(["content", "thumbnail"]):
            if m_el.prefix != "media" and m_el.name not in ("thumbnail",):
                continue
            m_url = (m_el.get("url") or "").strip()
            m_type = (m_el.get("type") or "").lower()
            m_medium = (m_el.get("medium") or "").lower()
            if not m_url:
                continue
            if m_type.startswith("video/") or m_medium == "video" or VIDEO_EXT_RE.search(m_url):
                add_video(m_url)
            else:
                add_image(m_url)

        for im in desc_imgs:
            add_image(im)
        for vd in desc_vids:
            add_video(vd)

        # Ensure photo media entries & backfill missing video thumbnails using primary image
        primary_thumb = images[0] if images else ""
        for mv in media_items:
            if mv["type"] == "video" and not mv["thumbnail"] and primary_thumb:
                mv["thumbnail"] = primary_thumb
        for im in images:
            media_items.append({"type": "photo", "url": im, "thumbnail": im, "title": ""})

        pub_el = (
            item.find("pubDate")
            or item.find("published")
            or item.find("updated")
            or item.find("date")
        )
        published_at = parse_rss_datetime(pub_el.get_text(strip=True) if pub_el else None)

        final_title = title_clean or _derive_title(desc_text) or source.name
        final_content = desc_text or final_title
        links = [link_url] if link_url.startswith(("http://", "https://")) else []
        content_hash = _compute_content_hash(
            final_title, final_content, images, media_items, links
        )

        posts.append(
            ParsedPost(
                id=f"{source.username}_{message_id}",
                source_id=source.id,
                source_name=source.name,
                source_username=source.username,
                message_id=message_id,
                category=source.category,
                title=final_title,
                content=final_content,
                published_at=published_at,
                original_url=link_url or source.url,
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


def fetch_and_parse_rss_feed(
    source: SourceConfig,
    defaults: ScraperDefaults,
    session: Optional[requests.Session] = None,
    max_posts: int = 50,
) -> list[ParsedPost]:
    xml_bytes = fetch_rss_xml(source, defaults, session=session)
    return parse_rss_xml(xml_bytes, source, max_posts=max_posts)
