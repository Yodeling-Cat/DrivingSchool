"""Builds the app's road-sign catalog from the traffic-sign regulation.

The catalog the app shows is príloha č. 1 of vyhláška MV SR č. 30/2020 Z. z. o dopravnom značení,
the regulation that lists every Slovak road sign with its number, its name and what it means. The
text of a regulation is not a protected work (zákon 185/2015 Z. z., § 5), which is why the app can
ship it; see docs/data-sources.md.

Pictures are not taken from the regulation: the sign images the app already bundles for its question
bank (minv.sk's `obr3/dz` set) are matched to the catalog by their file name, which is the sign's
number. An image whose number the regulation does not list is left out and reported.

Run `python tools/import/import_znacky.py --help`.
"""
import argparse
import json
import re
from dataclasses import asdict
from pathlib import Path

import znacky_pdf
from importer_common import RESOURCES, ROOT, bank_images, download, log, write_index

CACHE = Path(__file__).resolve().parent / "cache" / "znacky"
SIGNS_OUT = RESOURCES / "signs"
BANKS = RESOURCES / "banks"
IMAGES = RESOURCES / "images"

# Zbierka zákonov Slovenskej republiky, the consolidated text of 30/2020 Z. z. with its annexes.
# Its príloha č. 1 is the sign catalog. See docs/data-sources.md.
PDF_URL = "https://www.slov-lex.sk/static/pdf/2020/30/ZZ_2020_30_20200401.pdf"

COUNTRY = "SK"
LANGUAGE = "sk"
IMAGE_DIR = "minv-sk"
# "152-1" is one of the pictures of sign 152: the catalog numbers the sign once and shows its
# variants beside it. "401-409" is a picture of a whole group of signs, and is not one of them.
VARIANT = re.compile(r"^(\d{2,3})-\d$")
RANGE = re.compile(r"^\d{3}-\d{3}$")


def bundled_images():
    """The images the app ships, as (path under the image directory), taken from the banks."""
    return sorted(bank_images(BANKS, image_dir=IMAGE_DIR))


def match(signs, images):
    """Pairs every catalog entry with one picture of it.

    The bundled pictures are minv.sk's, named after the sign's number; where minv.sk ships several
    pictures of one sign (`152-1.jpg`, `321-1.jpg`), the one named exactly after the sign is the one
    the list shows. Returns (signs with an `image`, images no entry claims, extra pictures of a sign
    that is already listed).
    """
    by_number = {}
    for sign in signs:
        by_number.setdefault(sign.number, sign)
    order = {number: index for index, number in enumerate(by_number)}
    chosen, unmatched, extra = {}, [], []
    for image in images:
        stem = Path(image).stem
        variant = VARIANT.match(stem)
        number = stem if stem in by_number else (variant.group(1) if variant and variant.group(1) in by_number else None)
        if number is None or RANGE.match(stem):
            unmatched.append(image)
            continue
        exact = stem == number
        if number not in chosen or (exact and not chosen[number][1]):
            if number in chosen:
                extra.append(chosen[number][0])
            chosen[number] = (image, exact)
        else:
            extra.append(image)
    listed = [{**asdict(by_number[number]), "image": image} for number, (image, _) in chosen.items()]
    # The catalog's own order, which groups the kinds of sign together.
    return sorted(listed, key=lambda s: order[s["number"]]), unmatched, extra


def document(signs_with_images, version):
    return {
        "id": LANGUAGE,
        "version": version,
        "country": COUNTRY,
        "language": LANGUAGE,
        "imageDir": IMAGE_DIR,
        "source": "Vyhláška MV SR č. 30/2020 Z. z. o dopravnom značení, príloha č. 1",
        "signs": signs_with_images,
    }


def write_sign_index(signs, version):
    """Writes `signs/index.json`, keeping other countries' catalogs in it."""
    entry = {
        "id": LANGUAGE,
        "version": version,
        "country": COUNTRY,
        "language": LANGUAGE,
        "signCount": len(signs),
    }
    return write_index(SIGNS_OUT / "index.json", [entry], COUNTRY)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--refresh", action="store_true", help="re-download the regulation even if cached")
    parser.add_argument("--version", type=int, default=1, help="catalog version to write")
    args = parser.parse_args()

    log("Fetching the regulation")
    pdf = download(PDF_URL, CACHE / "vyhlaska-30-2020.pdf", args.refresh, timeout=120)

    log("Parsing the sign catalog")
    all_signs = znacky_pdf.parse(pdf)
    signs = [s for s in all_signs if s.description]
    log(f"  {len(all_signs)} catalog entries, {len(signs)} with a meaning, "
        f"{sum(1 for s in signs if not s.name)} of them nameless (their picture is their name)")

    images = bundled_images()
    matched, unmatched, extra = match(signs, images)
    log(f"  {len(images)} bundled sign images, {len(matched)} matched to an entry, "
        f"{len(extra)} extra pictures of a sign already listed")
    for image in unmatched:
        log(f"    no catalog entry for {image}")

    SIGNS_OUT.mkdir(parents=True, exist_ok=True)
    target = SIGNS_OUT / f"{LANGUAGE}-v{args.version}.json"
    target.write_text(
        json.dumps(document(matched, args.version), ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    log(f"Wrote {target.relative_to(ROOT)} and {write_sign_index(matched, args.version).relative_to(ROOT)}")
    for sign in matched[:5]:
        log(f"  {sign['number']} {sign['name']}: {sign['description'][:60]}")


if __name__ == "__main__":
    main()
