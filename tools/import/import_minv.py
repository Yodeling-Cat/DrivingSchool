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
import unicodedata
from pathlib import Path

import minv_pdf
from importer_common import RESOURCES, ROOT, bank_images, download, log, write_index

CACHE = Path(__file__).resolve().parent / "cache" / "minv-sk"
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
# Order the PDFs are matched in: Slovak is the reference, so it overrides Hungarian.
PDF_MATCH_ORDER = ["hu", "sk"]


def norm(text):
    return re.sub(r"[\W_]+", "", unicodedata.normalize("NFKC", text).lower())


def clean(text):
    return re.sub(r"\s+", " ", text).strip()


def section_starts(test):
    """(section id, position of its first question, title) of every "okruh", in exam order."""
    return sorted((int(key), value[0]["zacina"], value[0]["txt"]) for key, value in test["okruhy"].items())


def section_of(starts, position):
    """The section of the question asked at [position]."""
    return max(section for section, start, _ in starts if start <= int(position))


def parse_question(data5_question, answers, position, starts):
    """One question of data5.js, in the shape the bank file uses."""
    return {
        "id": data5_question["id"],
        "section": section_of(starts, position),
        "text": clean(data5_question["text"]),
        "answers": [clean(answer["odpoved"]) for answer in answers],
        "correct": data5_question["platna"] - 1,
        "points": data5_question["body"],
        "image": data5_question["obrazok"] or None,
    }


def parse_test(test):
    """The sections and the questions of one test in data5.js."""
    starts = section_starts(test)
    sections = {}
    for index, (section, start, title) in enumerate(starts):
        end = starts[index + 1][1] if index + 1 < len(starts) else test["pocet"] + 1
        sections[section] = {"id": section, "title": clean(title), "questionsPerTest": end - start}
    questions = {}
    for position, entries in test["otazky"].items():
        question = parse_question(entries[0], test["odpovede"][position], position, starts)
        # A section's points are those of its questions.
        sections[question["section"]]["points"] = question["points"]
        questions[question["id"]] = question
    return sections, questions


def load_data5(path):
    raw = path.read_text(encoding="utf-8")
    data = json.loads(raw[raw.index("["):].rstrip().rstrip(";"))
    banks = {}
    for lang, tests in zip(LANGUAGES, data):
        questions, sections = {}, {}
        for test in tests:
            test_sections, test_questions = parse_test(test)
            sections.update(test_sections)
            questions.update(test_questions)
        banks[lang] = {"questions": questions, "sections": [sections[k] for k in sorted(sections)]}
    return banks


def pdf_groups(tokens):
    """Normalizes the "pre skupinu" cell (e.g. ['B', 'E', 'C,CE']) to known group codes."""
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


def fetch_sources(refresh):
    """Downloads data5.js and the PDFs unless they're cached, and returns their paths."""
    log("Fetching sources")
    data_path = download(DATA_URL, CACHE / "data5.js", refresh)
    pdf_paths = {lang: download(url, CACHE / f"otazky-{lang}.pdf", refresh) for lang, url in PDF_URLS.items()}
    return data_path, pdf_paths


def learn_groups(banks, pdf_paths):
    """Maps data5 question id -> license groups, matching every PDF against its own language."""
    groups_by_id = {}
    for lang in PDF_MATCH_ORDER:
        _, rows = minv_pdf.parse(pdf_paths[lang])
        matched, fuzzy, missing = match_groups(banks[lang]["questions"], rows)
        log(f"  {lang}: {len(rows)} PDF rows, matched {len(matched)} "
            f"({fuzzy} fuzzy), unmatched {len(missing)}")
        for q in missing:
            log(f"    unmatched {q['id']}: {q['text'][:70]}")
        groups_by_id.update(matched)
    return groups_by_id


def bank_document(lang, bank, groups_by_id, version):
    """The contents of one bank file: its sections, and the questions that learned their groups."""
    questions = [
        {**q, "groups": groups_by_id[q["id"]]}
        for q in sorted(bank["questions"].values(), key=lambda q: (q["section"], q["id"]))
        if q["id"] in groups_by_id
    ]
    return {
        "id": f"sk-{lang}",
        "version": version,
        "country": "SK",
        "language": lang,
        "imageDir": IMAGES_OUT.name,
        "sections": bank["sections"],
        "questions": questions,
    }


def write_banks(banks, groups_by_id, version):
    """Writes one bank file per language and refreshes the shared index.

    Returns the images the newly written banks reference.
    """
    BANKS_OUT.mkdir(parents=True, exist_ok=True)
    manifest, images = [], set()
    for lang in LANGUAGES:
        bank = banks[lang]
        document = bank_document(lang, bank, groups_by_id, version)
        (BANKS_OUT / f"{document['id']}-v{version}.json").write_text(
            json.dumps(document, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        questions = document["questions"]
        images.update(q["image"] for q in questions if q["image"])
        counts = {group: sum(1 for q in questions if group in q["groups"]) for group in GROUPS}
        manifest.append({"id": document["id"], "version": version, "country": "SK", "language": lang,
                         "questionCounts": counts})
        log(f"  {document['id']}: {len(questions)} questions "
            f"({len(bank['questions']) - len(questions)} without groups dropped), {counts}")
    # Keep other importers' banks in the shared index.
    write_index(BANKS_OUT / "index.json", manifest, "SK")
    return images


def sync_images(new_images, refresh):
    """Downloads the images the new banks need, and deletes the files no bundled bank references."""
    to_download = [image for image in sorted(new_images) if refresh or not (IMAGES_OUT / image).exists()]
    bundled = bank_images(BANKS_OUT, IMAGES_OUT.name)
    log(f"Fetching images: {len(bundled)} referenced, {len(to_download)} to download")
    for image in to_download:
        download(IMAGE_URL + image, IMAGES_OUT / image, refresh, quiet=True)
    stale = [p for p in IMAGES_OUT.rglob("*")
             if p.is_file() and p.relative_to(IMAGES_OUT).as_posix() not in bundled]
    for path in stale:
        path.unlink()
    if stale:
        log(f"  removed {len(stale)} images no longer referenced")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--refresh", action="store_true", help="re-download sources and images even if cached")
    parser.add_argument("--version", type=int, default=1,
                        help="bank version to write (bump when minv.sk publishes a new question set; "
                             "see docs/importing-questions.md)")
    args = parser.parse_args()

    data_path, pdf_paths = fetch_sources(args.refresh)

    log("Parsing")
    banks = load_data5(data_path)
    groups_by_id = learn_groups(banks, pdf_paths)

    sync_images(write_banks(banks, groups_by_id, args.version), args.refresh)
    log("Done")


if __name__ == "__main__":
    main()
