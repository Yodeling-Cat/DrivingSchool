"""Imports the Slovak theory-test question bank published by minv.sk.

Sources (see docs/data-sources.md):
  * data5.js  - question data behind the official online sample exam. Has all three answers
                and the correct one, in Slovak, English and Hungarian, but no license groups and
                only part of the bank.
  * PDFs      - the complete bank (SK and HU) with the correct answer and the license groups
                of every question, but without the wrong answers.

Questions from data5.js are matched to PDF rows to learn their license groups. Only questions
with all three answers make it into the app. Run `python tools/import/import_minv.py --help`.
"""
import argparse
import collections
import difflib
import json
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

import minv_pdf

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path(__file__).resolve().parent / "cache" / "minv-sk"
RESOURCES = ROOT / "app/shared/src/commonMain/composeResources/files"
BANKS_OUT = RESOURCES / "banks"
IMAGES_OUT = RESOURCES / "images/minv-sk"

BASE_URL = "https://www.minv.sk/egovinet02/PCPZobrazFile?fileName="
DATA_URL = BASE_URL + "pcpfiles/data5.js"
IMAGE_URL = BASE_URL + "pcpfiles/"
PDF_URLS = {
    "sk": "https://www.minv.sk/?elektronicke-testy&subor=475052",
    "hu": "https://www.minv.sk/?elektronicke-testy&subor=475049",
}

GROUPS = ["A", "B", "BE", "C", "CE", "D", "DE", "T"]
# Order of the language arrays inside data5.js (see runTest() in test_mini.js).
LANGUAGES = ["sk", "en", "hu"]


def log(*args):
    print(*args, file=sys.stderr)


def download(url, target, refresh, quiet=False):
    if target.exists() and not refresh:
        return target
    if not quiet:
        log(f"  downloading {url}")
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "NajAutoskola-importer"})
    with urllib.request.urlopen(request, timeout=60) as response:
        target.write_bytes(response.read())
    return target


def norm(text):
    return re.sub(r"[\W_]+", "", unicodedata.normalize("NFKC", text).lower())


def clean(text):
    return re.sub(r"\s+", " ", text).strip()


def load_data5(path):
    raw = path.read_text(encoding="utf-8")
    data = json.loads(raw[raw.index("["):].rstrip().rstrip(";"))
    banks = {}
    for lang, tests in zip(LANGUAGES, data):
        questions, sections = {}, {}
        for test in tests:
            starts = sorted((int(k), v[0]["zacina"], v[0]["txt"]) for k, v in test["okruhy"].items())
            for i, (section, start, title) in enumerate(starts):
                end = starts[i + 1][1] if i + 1 < len(starts) else test["pocet"] + 1
                sections[section] = {"id": section, "title": clean(title), "questionsPerTest": end - start}
            for position, entries in test["otazky"].items():
                q = entries[0]
                section = max(s for s, start, _ in starts if start <= int(position))
                sections[section]["points"] = q["body"]
                questions[q["id"]] = {
                    "id": q["id"],
                    "section": section,
                    "text": clean(q["text"]),
                    "answers": [clean(a["odpoved"]) for a in test["odpovede"][position]],
                    "correct": q["platna"] - 1,
                    "points": q["body"],
                    "image": q["obrazok"] or None,
                }
        banks[lang] = {"questions": questions, "sections": [sections[k] for k in sorted(sections)]}
    return banks


def pdf_groups(tokens):
    """Normalises the "pre skupinu" cell (e.g. ['B', 'E', 'C,CE']) to known group codes."""
    merged = []
    for token in tokens:
        if token == "E" and merged and merged[-1] in ("B", "C", "D"):
            merged[-1] += "E"
        elif token in GROUPS:
            merged.append(token)
    # An empty cell means the question is for every group.
    return sorted(set(merged), key=GROUPS.index) if merged else list(GROUPS)


def match_groups(questions, rows):
    """Maps data5 question id -> license groups by matching question + correct answer to PDF rows."""
    exact = collections.defaultdict(list)
    for row in rows:
        exact[(row.section, norm(row.question), norm(row.answer))].append(row)
    used, result, fuzzy, missing = set(), {}, 0, []
    for q in questions.values():
        key = (q["section"], norm(q["text"]), norm(q["answers"][q["correct"]]))
        candidates = exact.get(key) or []
        row = next((r for r in candidates if id(r) not in used), candidates[0] if candidates else None)
        if row is None:
            # Tolerate small wording differences between the PDF and the online test.
            target = key[1] + "|" + key[2]
            scored = [
                (difflib.SequenceMatcher(None, target, norm(r.question) + "|" + norm(r.answer)).ratio(), r)
                for r in rows if r.section == q["section"]
            ]
            score, best = max(scored, key=lambda s: s[0], default=(0, None))
            if score < 0.85:
                missing.append(q)
                continue
            row, fuzzy = best, fuzzy + 1
        used.add(id(row))
        result[q["id"]] = pdf_groups(row.groups)
    return result, fuzzy, missing


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--refresh", action="store_true", help="re-download sources and images even if cached")
    parser.add_argument("--version", type=int, default=1,
                        help="bank version to write (bump when minv.sk publishes a new question set; "
                             "see docs/importing-questions.md)")
    args = parser.parse_args()

    log("Fetching sources")
    data_path = download(DATA_URL, CACHE / "data5.js", args.refresh)
    pdf_paths = {lang: download(url, CACHE / f"otazky-{lang}.pdf", args.refresh) for lang, url in PDF_URLS.items()}

    log("Parsing")
    banks = load_data5(data_path)
    groups_by_id = {}
    for lang in ("hu", "sk"):  # Slovak is the reference, so it overrides Hungarian.
        _, rows = minv_pdf.parse(pdf_paths[lang])
        matched, fuzzy, missing = match_groups(banks[lang]["questions"], rows)
        log(f"  {lang}: {len(rows)} PDF rows, matched {len(matched)} "
            f"({fuzzy} fuzzy), unmatched {len(missing)}")
        for q in missing:
            log(f"    unmatched {q['id']}: {q['text'][:70]}")
        groups_by_id.update(matched)

    BANKS_OUT.mkdir(parents=True, exist_ok=True)
    manifest, images = [], set()
    for lang in LANGUAGES:
        bank = banks[lang]
        questions = []
        for q in sorted(bank["questions"].values(), key=lambda q: (q["section"], q["id"])):
            if q["id"] not in groups_by_id:
                continue
            questions.append({**q, "groups": groups_by_id[q["id"]]})
            if q["image"]:
                images.add(q["image"])
        bank_id = f"sk-{lang}"
        out = {"id": bank_id, "version": args.version, "country": "SK", "language": lang, "imageDir": IMAGES_OUT.name, "sections": bank["sections"], "questions": questions}
        (BANKS_OUT / f"{bank_id}-v{args.version}.json").write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        counts = {g: sum(1 for q in questions if g in q["groups"]) for g in GROUPS}
        manifest.append({"id": bank_id, "version": args.version, "country": "SK", "language": lang, "questionCounts": counts})
        log(f"  {bank_id}: {len(questions)} questions ({len(bank['questions']) - len(questions)} without groups dropped), {counts}")
    # Keep other importers' banks in the shared index.
    index_path = BANKS_OUT / "index.json"
    others = [b for b in json.loads(index_path.read_text(encoding="utf-8")) if b["country"] != "SK"] if index_path.exists() else []
    index_path.write_text(json.dumps(others + manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    missing = [i for i in sorted(images) if args.refresh or not (IMAGES_OUT / i).exists()]
    # Older bank versions stay bundled (see docs/importing-questions.md), so keep their images too.
    for older in BANKS_OUT.glob("sk-*-v*.json"):
        bank = json.loads(older.read_text(encoding="utf-8"))
        images.update(q["image"] for q in bank["questions"] if q["image"])
    log(f"Fetching images: {len(images)} referenced, {len(missing)} to download")
    for image in missing:
        download(IMAGE_URL + image, IMAGES_OUT / image, args.refresh, quiet=True)
    stale = [p for p in IMAGES_OUT.rglob("*") if p.is_file() and p.relative_to(IMAGES_OUT).as_posix() not in images]
    for path in stale:
        path.unlink()
    if stale:
        log(f"  removed {len(stale)} images no longer referenced")
    log("Done")


if __name__ == "__main__":
    main()
