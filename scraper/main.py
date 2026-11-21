"""CLI runner for Iraq News Hub scraper."""

from __future__ import annotations

import argparse
import logging
import sys
from datetime import datetime, timezone
from pathlib import Path

from scraper import __version__
from scraper.config import SourceConfig, load_config
from scraper.fetcher import SourceFetchError, build_session, fetch_source_html
from scraper.parser import ParsedPost, SourceParseError, parse_channel_html
from scraper.storage import (
    build_sources_summary,
    load_news_store,
    load_status_store,
    merge_posts,
    utc_now_iso,
    write_json_atomic,
)
from scraper.x_list import fetch_and_parse_x_list

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("iraq_news_hub")


def _should_skip_due_to_interval(
    source: SourceConfig,
    prev_status: dict,
    now_dt: datetime,
    force: bool,
) -> bool:
    if force or source.fetch_interval_minutes <= 0:
        return False
    last_success_raw = prev_status.get("last_success_at")
    if not last_success_raw:
        return False
    try:
        last_dt = datetime.fromisoformat(str(last_success_raw).replace("Z", "+00:00"))
        elapsed_minutes = (now_dt - last_dt).total_seconds() / 60.0
        # Small 2-minute tolerance for GitHub Actions cron jitter
        return elapsed_minutes < max(1, source.fetch_interval_minutes - 2)
    except ValueError:
        return False


def run_scrape(
    config_path: Path,
    news_path: Path,
    status_path: Path,
    limit_per_source: int | None = None,
    dry_run: bool = False,
    force: bool = False,
) -> int:
    app_config = load_config(config_path)
    news_store = load_news_store(news_path)
    status_store = load_status_store(status_path)
    sources_status: dict[str, dict] = dict(status_store.get("sources", {}))

    now_dt = datetime.now(timezone.utc)
    run_timestamp = now_dt.isoformat().replace("+00:00", "Z")
    collected_posts: list[ParsedPost] = []
    any_source_succeeded = False

    session = build_session(app_config.defaults)
    try:
        for source in app_config.sources:
            prev_status = dict(sources_status.get(source.id, {}))
            if not source.enabled:
                logger.info("Skipping disabled source: %s (%s)", source.name, source.username)
                sources_status[source.id] = {
                    **prev_status,
                    "id": source.id,
                    "name": source.name,
                    "username": source.username,
                    "url": source.url,
                    "category": source.category,
                    "enabled": False,
                    "status": "disabled",
                }
                continue

            if _should_skip_due_to_interval(source, prev_status, now_dt, force):
                logger.info(
                    "Skipping %s until %d-minute interval elapses (last success: %s)",
                    source.name,
                    source.fetch_interval_minutes,
                    prev_status.get("last_success_at"),
                )
                continue

            max_posts = limit_per_source or source.max_posts or app_config.defaults.max_posts_per_fetch
            try:
                if source.source_type == "x_list":
                    posts = fetch_and_parse_x_list(
                        source, app_config.defaults, session=session, max_posts=max_posts
                    )
                else:
                    html = fetch_source_html(source, app_config.defaults, session=session)
                    posts = parse_channel_html(html, source, max_posts=max_posts)

                collected_posts.extend(posts)
                any_source_succeeded = True
                logger.info(
                    "Parsed %d posts from %s (@%s)",
                    len(posts),
                    source.name,
                    source.username,
                )
                sources_status[source.id] = {
                    "id": source.id,
                    "name": source.name,
                    "username": source.username,
                    "url": source.url,
                    "category": source.category,
                    "enabled": True,
                    "status": "ok",
                    "last_attempt_at": run_timestamp,
                    "last_success_at": run_timestamp,
                    "last_error": None,
                    "last_error_at": prev_status.get("last_error_at"),
                    "posts_extracted": len(posts),
                }
            except (SourceFetchError, SourceParseError, Exception) as exc:
                logger.error("Source %s (@%s) failed: %s", source.name, source.username, exc)
                sources_status[source.id] = {
                    "id": source.id,
                    "name": source.name,
                    "username": source.username,
                    "url": source.url,
                    "category": source.category,
                    "enabled": True,
                    "status": "error",
                    "last_attempt_at": run_timestamp,
                    "last_success_at": prev_status.get("last_success_at"),
                    "last_error": str(exc),
                    "last_error_at": run_timestamp,
                    "posts_extracted": 0,
                }
    finally:
        session.close()

    existing_items = list(news_store.get("items", []))
    merged_items, stats = merge_posts(existing_items, collected_posts, run_timestamp)

    logger.info(
        "Merge summary -> new: %d, updated: %d, unchanged: %d, total stored: %d",
        stats.new_count,
        stats.updated_count,
        stats.unchanged_count,
        len(merged_items),
    )

    if dry_run:
        logger.info("Dry run enabled; skipping disk writes.")
        return 0

    sources_summary = build_sources_summary(app_config.sources)
    last_successful = (
        run_timestamp if any_source_succeeded else news_store.get("last_successful_update", "")
    )

    content_changed = (
        stats.new_count > 0
        or stats.updated_count > 0
        or not news_path.exists()
        or news_store.get("sources") != sources_summary
    )

    if content_changed:
        updated_news_store = {
            "schema_version": "1.0",
            "project": "Iraq News Hub",
            "app_version": __version__,
            "generated_at": run_timestamp,
            "last_successful_update": last_successful,
            "total_count": len(merged_items),
            "sources": sources_summary,
            "items": merged_items,
        }
        write_json_atomic(news_path, updated_news_store)
        logger.info("Updated %s (%d total items)", news_path, len(merged_items))
    else:
        logger.info("No new or edited news items; %s left unchanged.", news_path)

    updated_status_store = {
        "schema_version": "1.0",
        "last_run_at": run_timestamp,
        "last_successful_update": last_successful,
        "total_stored_news": len(merged_items),
        "last_merge": {
            "new": stats.new_count,
            "updated": stats.updated_count,
            "unchanged": stats.unchanged_count,
        },
        "sources": sources_status,
    }
    write_json_atomic(status_path, updated_status_store)
    logger.info("Updated %s", status_path)
    return 0


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Iraq News Hub Scraper")
    parser.add_argument("--config", type=Path, default=Path("config/sources.json"))
    parser.add_argument("--output", type=Path, default=Path("data/news.json"))
    parser.add_argument("--status", type=Path, default=Path("data/sources_status.json"))
    parser.add_argument("--limit-per-source", type=int, default=None)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--force", action="store_true", help="Ignore per-source fetch intervals")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    return run_scrape(
        config_path=args.config,
        news_path=args.output,
        status_path=args.status,
        limit_per_source=args.limit_per_source,
        dry_run=args.dry_run,
        force=args.force,
    )


if __name__ == "__main__":
    sys.exit(main())
