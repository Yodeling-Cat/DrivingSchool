"""What the importers share: where the app's resources are, and how a source is fetched and an
index is written. See docs/importing-questions.md."""
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "app/shared/src/commonMain/composeResources/files"


def log(*args):
    print(*args, file=sys.stderr)


def download(url, target, refresh, quiet=False, timeout=60):
    """Fetches [url] into [target], unless it is cached there already and [refresh] is not asked for."""
    if target.exists() and not refresh:
        return target
    if not quiet:
        log(f"  downloading {url}")
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "NajAutoskola-importer"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        target.write_bytes(response.read())
    return target


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def bank_images(banks_dir, image_dir):
    """Every image the bundled banks whose pictures are in [image_dir] reference.

    Older versions stay bundled, so their images count too.
    """
    images = set()
    for path in banks_dir.glob("*-v*.json"):
        bank = read_json(path)
        if bank["imageDir"] != image_dir:
            continue
        images.update(q["image"] for q in bank["questions"] if q["image"])
    return images


def write_index(path, entries, country):
    """Writes the index at [path] with [entries] for [country], keeping the other countries' entries."""
    others = [e for e in read_json(path) if e["country"] != country] if path.exists() else []
    path.write_text(json.dumps(others + entries, ensure_ascii=False, indent=2), encoding="utf-8")
    return path
