"""What the importers share: where each country's content goes, how a source is fetched, and how
a bank, its index entry and its images are written. See docs/importing-questions.md."""
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def content_files(country):
    """The Compose resources of [country]'s content module, which its app alone bundles (see
    AGENTS.md, "Countries"): `banks/`, `images/` and, where it has one, `signs/`."""
    return ROOT / f"app/content/{country}/src/commonMain/composeResources/files"


#: Every country's content, by the code of its module.
COUNTRIES = ["sk", "cz"]


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


def media_files(question):
    """Every file [question] is shown with: its picture, its video and its answers' pictures."""
    files = [question["image"]] if question["image"] else []
    if question.get("video"):
        files.append(question["video"]["file"])
    return files + (question.get("answerImages") or [])


def bank_images(banks_dir, image_dir):
    """Every file in [image_dir] that the bundled banks whose media is there reference.

    Older versions stay bundled, so their files count too.
    """
    images = set()
    for path in banks_dir.glob("*-v*.json"):
        bank = read_json(path)
        if bank["imageDir"] == image_dir:
            images.update(name for q in bank["questions"] for name in media_files(q))
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


def write_bank(document, groups, banks_dir):
    """Writes the bank [document] as `<id>-v<version>.json` in [banks_dir], and returns its index
    entry."""
    banks_dir.mkdir(parents=True, exist_ok=True)
    (banks_dir / f"{document['id']}-v{document['version']}.json").write_text(
        json.dumps(document, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return index_entry(document, groups)


def remove_stale_images(files_dir, image_dir):
    """Deletes the files of `images/<image_dir>/` in [files_dir] that no bank there references.
    Older versions stay bundled, so their images stay too."""
    bundled = bank_images(files_dir / "banks", image_dir)
    images = files_dir / "images" / image_dir
    stale = [p for p in images.rglob("*") if p.is_file() and p.relative_to(images).as_posix() not in bundled]
    for path in stale:
        path.unlink()
    if stale:
        log(f"  removed {len(stale)} images no longer referenced")


def write_index(path, entries):
    """Writes the index at [path] with [entries]. Each country's content module has an index of its
    own, so it lists that country's entries and no other's."""
    path.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
    return path
