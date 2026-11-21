"""HTTP fetcher for public Telegram web preview pages (t.me/s/)."""

from __future__ import annotations

import logging
from typing import Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from scraper.config import ScraperDefaults, SourceConfig

logger = logging.getLogger(__name__)


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
) -> str:
    """Fetch public web preview HTML for a single Telegram channel."""
    own_session = session is None
    active_session = session or build_session(defaults)
    try:
        logger.info("Fetching %s (%s)", source.name, source.url)
        resp = active_session.get(
            source.url,
            timeout=defaults.request_timeout_seconds,
            allow_redirects=True,
        )
        if resp.status_code in (403, 429):
            raise SourceFetchError(
                f"Access restricted (HTTP {resp.status_code}) for {source.url}; skipping without bypass."
            )
        if resp.status_code >= 400:
            raise SourceFetchError(f"HTTP {resp.status_code} while fetching {source.url}")

        body = resp.text
        lowered = body.lower()
        if "captcha" in lowered and "tgme_widget_message" not in lowered:
            raise SourceFetchError(
                f"Challenge/CAPTCHA detected at {source.url}; skipping without bypass."
            )
        return body
    except requests.RequestException as exc:
        raise SourceFetchError(f"Network error fetching {source.url}: {exc}") from exc
    finally:
        if own_session:
            active_session.close()
