"""Configuration loader and validator for news sources."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

VALID_USERNAME_RE = re.compile(r"^[A-Za-z0-9_]{4,64}$")


@dataclass(frozen=True)
class SourceConfig:
    id: str
    name: str
    username: str
    url: str
    category: str
    language: str
    enabled: bool
    notes: str = ""


@dataclass(frozen=True)
class ScraperDefaults:
    request_timeout_seconds: int = 20
    max_posts_per_fetch: int = 30
    user_agent: str = "IraqNewsHubBot/1.0 (+https://github.com/iraq-news-hub)"


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
                "IraqNewsHubBot/1.0 (+https://github.com/iraq-news-hub)",
            )
        ),
    )

    sources: list[SourceConfig] = []
    seen_ids: set[str] = set()
    seen_usernames: set[str] = set()

    for item in raw.get("sources", []):
        username = str(item.get("username", "")).strip().lstrip("@")
        if not VALID_USERNAME_RE.match(username):
            raise ValueError(f"Invalid Telegram channel username: {username!r}")

        source_id = str(item.get("id") or username.lower()).strip()
        if source_id in seen_ids:
            raise ValueError(f"Duplicate source id in config: {source_id}")
        if username.lower() in seen_usernames:
            raise ValueError(f"Duplicate source username in config: {username}")

        raw_url = str(item.get("url") or f"https://t.me/s/{username}").strip()
        normalized_url = validate_public_telegram_url(raw_url, username)

        seen_ids.add(source_id)
        seen_usernames.add(username.lower())

        sources.append(
            SourceConfig(
                id=source_id,
                name=str(item.get("name") or username).strip(),
                username=username,
                url=normalized_url,
                category=str(item.get("category") or "أخبار عامة").strip(),
                language=str(item.get("language") or "ar").strip(),
                enabled=bool(item.get("enabled", True)),
                notes=str(item.get("notes") or "").strip(),
            )
        )

    return AppConfig(defaults=defaults, sources=sources)
