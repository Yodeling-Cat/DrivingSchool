"""What the importers share: where the app's resources are, how a source is fetched, and how a
bank, its index entry and its images are written. See docs/importing-questions.md."""
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "app/shared/src/commonMain/composeResources/files"
BANKS = RESOURCES / "banks"
IMAGES = RESOURCES / "images"


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


def clean(text):
    """[text] with its runs of whitespace, line breaks included, collapsed to one space."""
    return re.sub(r"\s+", " ", text).strip()


def bank_images(banks_dir, image_dir):
    """Every file in [image_dir] that the bundled banks whose media is there reference: pictures,
    videos and the pictures that answers are.

    Older versions stay bundled, so their files count too.
    """
    images = set()
    for path in banks_dir.glob("*-v*.json"):
        bank = read_json(path)
        if bank["imageDir"] != image_dir:
            continue
        for q in bank["questions"]:
            if q["image"]:
                images.add(q["image"])
            if q.get("video"):
                images.add(q["video"]["file"])
            images.update(q.get("answerImages") or [])
    return images


def index_entry(document, groups):
    """The entry of `banks/index.json` for the bank [document], counting its questions per group."""
    questions = document["questions"]
    return {
        "id": document["id"],
        "version": document["version"],
        "country": document["country"],
        "language": document["language"],
        "questionCounts": {group: sum(1 for q in questions if group in q["groups"]) for group in groups},
    }


def write_bank(document, groups):
    """Writes the bank [document] as `banks/<id>-v<version>.json`, and returns its index entry."""
    BANKS.mkdir(parents=True, exist_ok=True)
    (BANKS / f"{document['id']}-v{document['version']}.json").write_text(
        json.dumps(document, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return index_entry(document, groups)


def remove_stale_images(image_dir):
    """Deletes the files of `images/<image_dir>/` that no bundled bank references. Older versions
    stay bundled, so their images stay too."""
    bundled = bank_images(BANKS, image_dir)
    images = IMAGES / image_dir
    stale = [p for p in images.rglob("*") if p.is_file() and p.relative_to(images).as_posix() not in bundled]
    for path in stale:
        path.unlink()
    if stale:
        log(f"  removed {len(stale)} images no longer referenced")


def write_index(path, entries, country):
    """Writes the index at [path] with [entries] for [country], keeping the other countries' entries."""
    others = [e for e in read_json(path) if e["country"] != country] if path.exists() else []
    path.write_text(json.dumps(others + entries, ensure_ascii=False, indent=2), encoding="utf-8")
    return path
