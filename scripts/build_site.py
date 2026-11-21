"""Assemble static GitHub Pages output directory (_site) with web UI and JSON feeds."""

from __future__ import annotations

import shutil
from pathlib import Path


def build_site(root: Path = Path(".")) -> Path:
    web_dir = root / "web"
    data_dir = root / "data"
    site_dir = root / "_site"

    if site_dir.exists():
        shutil.rmtree(site_dir)
    site_dir.mkdir(parents=True, exist_ok=True)

    # Copy all web frontend assets to site root
    for item in web_dir.iterdir():
        dest = site_dir / item.name
        if item.is_dir():
            shutil.copytree(item, dest)
        else:
            shutil.copy2(item, dest)

    # Expose news.json and sources_status.json both at root (/news.json) and /data/news.json
    site_data_dir = site_dir / "data"
    site_data_dir.mkdir(parents=True, exist_ok=True)

    for filename in ("news.json", "sources_status.json"):
        src = data_dir / filename
        if src.exists():
            shutil.copy2(src, site_dir / filename)
            shutil.copy2(src, site_data_dir / filename)

    # Disable Jekyll processing on GitHub Pages
    (site_dir / ".nojekyll").write_text("", encoding="utf-8")
    return site_dir


if __name__ == "__main__":
    out = build_site()
    print(f"Built static site at {out.resolve()}")
