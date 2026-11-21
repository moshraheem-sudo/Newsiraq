"""Persistent JSON storage, deduplication, and edit tracking for Iraq News Hub."""

from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from scraper import __version__
from scraper.config import SourceConfig
from scraper.parser import ParsedPost


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class MergeStats:
    new_count: int = 0
    updated_count: int = 0
    unchanged_count: int = 0


def load_news_store(news_path: Path) -> dict[str, Any]:
    if not news_path.exists():
        return {
            "schema_version": "1.0",
            "project": "World News",
            "app_version": __version__,
            "generated_at": "",
            "last_successful_update": "",
            "total_count": 0,
            "sources": [],
            "items": [],
        }
    data = json.loads(news_path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        return {
            "schema_version": "1.0",
            "project": "World News",
            "app_version": __version__,
            "generated_at": "",
            "last_successful_update": "",
            "total_count": len(data),
            "sources": [],
            "items": data,
        }
    return data


def load_status_store(status_path: Path) -> dict[str, Any]:
    if not status_path.exists():
        return {
            "schema_version": "1.0",
            "last_run_at": "",
            "last_successful_update": "",
            "sources": {},
        }
    return json.loads(status_path.read_text(encoding="utf-8"))


def merge_posts(
    existing_items: list[dict[str, Any]],
    parsed_posts: list[ParsedPost],
    timestamp_iso: str,
    max_items: int = 1600,
) -> tuple[list[dict[str, Any]], MergeStats]:
    """Merge new and edited posts into existing items without deleting recent history."""
    stats = MergeStats()
    by_key: dict[tuple[str, int], dict[str, Any]] = {}

    for item in existing_items:
        username = str(item.get("source_username", "")).strip()
        msg_id = int(item.get("message_id", 0))
        if username and msg_id:
            by_key[(username.lower(), msg_id)] = dict(item)

    for post in parsed_posts:
        key = (post.source_username.lower(), post.message_id)
        existing = by_key.get(key)
        if existing is None:
            by_key[key] = {
                "id": post.id,
                "source_id": post.source_id,
                "source_name": post.source_name,
                "source_username": post.source_username,
                "message_id": post.message_id,
                "category": post.category,
                "title": post.title,
                "content": post.content,
                "published_at": post.published_at,
                "fetched_at": timestamp_iso,
                "updated_at": timestamp_iso,
                "original_url": post.original_url,
                "images": post.images,
                "media": post.media,
                "links": post.links,
                "is_edited": post.is_edited,
                "content_hash": post.content_hash,
            }
            stats.new_count += 1
        elif existing.get("content_hash") != post.content_hash:
            existing.update(
                {
                    "source_id": post.source_id,
                    "source_name": post.source_name,
                    "category": post.category,
                    "title": post.title,
                    "content": post.content,
                    "original_url": post.original_url,
                    "images": post.images,
                    "media": post.media,
                    "links": post.links,
                    "is_edited": True,
                    "content_hash": post.content_hash,
                    "updated_at": timestamp_iso,
                }
            )
            stats.updated_count += 1
        else:
            stats.unchanged_count += 1

    merged = sorted(
        by_key.values(),
        key=lambda x: (str(x.get("published_at", "")), int(x.get("message_id", 0))),
        reverse=True,
    )
    if max_items > 0 and len(merged) > max_items:
        merged = merged[:max_items]
    return merged, stats


def write_json_atomic(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(path.suffix + ".tmp")
    tmp_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp_path.replace(path)


def build_sources_summary(sources: list[SourceConfig]) -> list[dict[str, Any]]:
    return [
        {
            "id": s.id,
            "name": s.name,
            "username": s.username,
            "url": s.url,
            "category": s.category,
            "language": s.language,
            "enabled": s.enabled,
        }
        for s in sources
    ]
