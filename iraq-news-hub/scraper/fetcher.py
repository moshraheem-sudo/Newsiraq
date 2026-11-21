"""HTTP fetcher for public Telegram web preview pages (t.me/s/)."""

from __future__ import annotations

import logging
import re
from typing import Optional

import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from scraper.config import ScraperDefaults, SourceConfig

logger = logging.getLogger(__name__)
BEFORE_PARAM_RE = re.compile(r"[?&]before=(\d+)")


class SourceFetchError(Exception):
    """Raised when a source cannot be fetched reliably without bypassing restrictions."""


def build_session(defaults: ScraperDefaults) -> requests.Session:
    session = requests.Session()
    retry = Retry(
        total=2,
        backoff_factor=0.8,
        status_forcelist=[500, 502, 503, 504],
        allowed_methods=["GET"],
        raise_on_status=False,
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("https://", adapter)
    session.headers.update(
        {
            "User-Agent": defaults.user_agent,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
        }
    )
    return session


def fetch_source_html(
    source: SourceConfig,
    defaults: ScraperDefaults,
    session: Optional[requests.Session] = None,
    url_override: Optional[str] = None,
) -> str:
    """Fetch public web preview HTML for a single Telegram channel page."""
    target_url = url_override or source.url
    own_session = session is None
    active_session = session or build_session(defaults)
    try:
        logger.info("Fetching %s (%s)", source.name, target_url)
        resp = active_session.get(
            target_url,
            timeout=defaults.request_timeout_seconds,
            allow_redirects=True,
        )
        if resp.status_code in (403, 429):
            raise SourceFetchError(
                f"Access restricted (HTTP {resp.status_code}) for {target_url}; skipping without bypass."
            )
        if resp.status_code >= 400:
            raise SourceFetchError(f"HTTP {resp.status_code} while fetching {target_url}")

        body = resp.text
        lowered = body.lower()
        if "captcha" in lowered and "tgme_widget_message" not in lowered:
            raise SourceFetchError(
                f"Challenge/CAPTCHA detected at {target_url}; skipping without bypass."
            )
        return body
    except requests.RequestException as exc:
        raise SourceFetchError(f"Network error fetching {target_url}: {exc}") from exc
    finally:
        if own_session:
            active_session.close()


def extract_before_cursor(html: str, username: str) -> Optional[int]:
    """Extract the older-page `before` message ID cursor from a t.me/s/ page."""
    soup = BeautifulSoup(html, "lxml")
    more_link = soup.select_one("a.tme_messages_more[data-before], a.tme_messages_more[href]")
    if more_link:
        raw_before = (more_link.get("data-before") or "").strip()
        if raw_before.isdigit():
            return int(raw_before)
        href = str(more_link.get("href") or "")
        match = BEFORE_PARAM_RE.search(href)
        if match:
            return int(match.group(1))

    min_id: Optional[int] = None
    expected_prefix = f"{username.lower()}/"
    for node in soup.select(".tgme_widget_message[data-post]"):
        data_post = str(node.get("data-post") or "").strip()
        if "/" in data_post and data_post.lower().startswith(expected_prefix):
            _, post_id_str = data_post.rsplit("/", 1)
            if post_id_str.isdigit():
                val = int(post_id_str)
                if min_id is None or val < min_id:
                    min_id = val
    return min_id


def fetch_source_pages_html(
    source: SourceConfig,
    defaults: ScraperDefaults,
    session: Optional[requests.Session] = None,
    max_posts: int = 50,
    max_pages: int = 6,
) -> list[str]:
    """Fetch one or more paginated t.me/s/ pages until ~max_posts messages are collected."""
    own_session = session is None
    active_session = session or build_session(defaults)
    pages: list[str] = []
    seen_post_ids: set[str] = set()
    seen_cursors: set[int] = set()
    next_url: Optional[str] = source.url

    try:
        for page_index in range(max_pages):
            if not next_url:
                break
            html = fetch_source_html(
                source=source,
                defaults=defaults,
                session=active_session,
                url_override=next_url,
            )
            pages.append(html)

            soup = BeautifulSoup(html, "lxml")
            new_in_page = 0
            for node in soup.select(".tgme_widget_message[data-post]"):
                dp = str(node.get("data-post") or "").strip()
                if dp and dp not in seen_post_ids:
                    seen_post_ids.add(dp)
                    new_in_page += 1

            if max_posts <= 0 or len(seen_post_ids) >= max_posts or new_in_page == 0:
                break

            cursor = extract_before_cursor(html, source.username)
            if cursor is None or cursor <= 1 or cursor in seen_cursors:
                break
            seen_cursors.add(cursor)
            next_url = f"https://t.me/s/{source.username}?before={cursor}"
            logger.debug("Paginating %s page %d -> %s", source.username, page_index + 2, next_url)
    finally:
        if own_session:
            active_session.close()

    return pages
