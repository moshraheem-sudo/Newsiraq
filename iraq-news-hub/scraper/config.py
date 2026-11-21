"""Configuration loader and validator for news sources."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

VALID_USERNAME_RE = re.compile(r"^[A-Za-z0-9_]{4,64}$")
VALID_X_LIST_RE = re.compile(r"^/i/lists/(\d{6,})/?$")


@dataclass(frozen=True)
class SourceConfig:
    id: str
    name: str
    username: str
    url: str
    category: str
    language: str
    enabled: bool
    source_type: str = "telegram_web"
    max_posts: int | None = None
    fetch_interval_minutes: int = 0
    notes: str = ""


@dataclass(frozen=True)
class ScraperDefaults:
    request_timeout_seconds: int = 20
    max_posts_per_fetch: int = 30
    user_agent: str = "IraqNewsHubBot/1.0 (+https://github.com/moshraheem-sudo/Newsiraq)"


@dataclass(frozen=True)
class AppConfig:
    defaults: ScraperDefaults
    sources: list[SourceConfig]


def validate_public_telegram_url(url: str, username: str) -> str:
    """Ensure only public Telegram web preview URLs (https://t.me/s/<username>) are used."""
    parsed = urlparse(url.strip())
    if parsed.scheme != "https" or parsed.netloc.lower() not in {"t.me", "telegram.me"}:
        raise ValueError(f"Unsupported domain/scheme for source '{username}': {url}")
    expected_prefix = f"/s/{username}".lower()
    if parsed.path.rstrip("/").lower() != expected_prefix:
        raise ValueError(
            f"Source URL must use public web preview format https://t.me/s/{username}, got: {url}"
        )
    return f"https://t.me/s/{username}"


def validate_x_list_url(url: str) -> str:
    """Ensure X list URL matches https://x.com/i/lists/<numeric_id>."""
    parsed = urlparse(url.strip())
    if parsed.scheme != "https" or parsed.netloc.lower() not in {"x.com", "twitter.com"}:
        raise ValueError(f"Unsupported X list domain/scheme: {url}")
    match = VALID_X_LIST_RE.match(parsed.path)
    if not match:
        raise ValueError(f"X list URL must match https://x.com/i/lists/<list_id>, got: {url}")
    return f"https://x.com/i/lists/{match.group(1)}"


def validate_rss_url(url: str) -> str:
    """Ensure RSS feed URL uses HTTPS and a valid host."""
    cleaned = url.strip()
    parsed = urlparse(cleaned)
    if parsed.scheme != "https" or not parsed.netloc:
        raise ValueError(f"RSS URL must use https:// with valid domain, got: {url}")
    return cleaned


def load_config(config_path: Path) -> AppConfig:
    if not config_path.exists():
        raise FileNotFoundError(f"Sources configuration file not found: {config_path}")

    raw: dict[str, Any] = json.loads(config_path.read_text(encoding="utf-8"))
    raw_defaults = raw.get("defaults", {})
    defaults = ScraperDefaults(
        request_timeout_seconds=int(raw_defaults.get("request_timeout_seconds", 20)),
        max_posts_per_fetch=int(raw_defaults.get("max_posts_per_fetch", 30)),
        user_agent=str(
            raw_defaults.get(
                "user_agent",
                "IraqNewsHubBot/1.0 (+https://github.com/moshraheem-sudo/Newsiraq)",
            )
        ),
    )

    sources: list[SourceConfig] = []
    seen_ids: set[str] = set()
    seen_usernames: set[str] = set()

    for item in raw.get("sources", []):
        source_type = str(item.get("type") or "telegram_web").strip().lower()
        username = str(item.get("username", "")).strip().lstrip("@")
        if not VALID_USERNAME_RE.match(username):
            raise ValueError(f"Invalid source username/key: {username!r}")

        source_id = str(item.get("id") or username.lower()).strip()
        if source_id in seen_ids:
            raise ValueError(f"Duplicate source id in config: {source_id}")
        if username.lower() in seen_usernames:
            raise ValueError(f"Duplicate source username in config: {username}")

        if source_type == "telegram_web":
            raw_url = str(item.get("url") or f"https://t.me/s/{username}").strip()
            normalized_url = validate_public_telegram_url(raw_url, username)
        elif source_type == "x_list":
            raw_url = str(item.get("url") or "").strip()
            normalized_url = validate_x_list_url(raw_url)
        elif source_type == "rss":
            raw_url = str(item.get("url") or "").strip()
            normalized_url = validate_rss_url(raw_url)
        else:
            raise ValueError(f"Unsupported source type {source_type!r} for {source_id}")

        seen_ids.add(source_id)
        seen_usernames.add(username.lower())

        max_posts_raw = item.get("max_posts")
        sources.append(
            SourceConfig(
                id=source_id,
                name=str(item.get("name") or username).strip(),
                username=username,
                url=normalized_url,
                category=str(item.get("category") or "أخبار عامة").strip(),
                language=str(item.get("language") or "ar").strip(),
                enabled=bool(item.get("enabled", True)),
                source_type=source_type,
                max_posts=int(max_posts_raw) if max_posts_raw is not None else None,
                fetch_interval_minutes=int(item.get("fetch_interval_minutes") or 0),
                notes=str(item.get("notes") or "").strip(),
            )
        )

    return AppConfig(defaults=defaults, sources=sources)
