"""Imports the Czech theory-test question bank published by the Ministry of Transport on
etesty.md.gov.cz.

Sources (see docs/data-sources.md):
  * the complete question set ("Soubor všech otázek") - a page listing the id of every question,
    each of which the site's JSON API serves with all its answers, the correct one, its topic,
    its points and its picture or video.
  * the practice exams ("Cvičná zkouška"), one per license group - they give the exam's layout:
    the topics in order, how many questions each draws and what each is worth.

Nothing the ministry publishes says which license group a question is for, but each group's
practice exam draws its questions from that group's pool. So the importer draws a few hundred
practice exams per group and, topic by topic, tells whether the group's draws could have come
from the whole topic. If they could, every question of the topic is in the group; if not, only
the questions the group's exams drew are. See docs/data-sources.md.
Run `python tools/import/import_md_cz.py --help`.
"""
import argparse
import json
import math
import re
import shutil
import subprocess
import time
from pathlib import Path

import pymupdf

from importer_common import (
    BANKS, IMAGES, clean, download, log, read_json, remove_stale_images, write_bank, write_index,
)

CACHE = Path(__file__).resolve().parent / "cache" / "md-cz"
IMAGES_OUT = IMAGES / "md-cz"

BASE_URL = "https://etesty.md.gov.cz"
#: The practice area that holds every question ("Soubor všech otázek").
ALL_QUESTIONS_URL = BASE_URL + "/ro/DLArea/Index?id=99"
QUESTION_URL = BASE_URL + "/api/v1/PublicWeb/Question/{id}"
SAMPLE_TEST_URL = BASE_URL + "/co/DLTest/SampleTest/{group}"

BANK_ID = "cz-cs"
COUNTRY = "CZ"
LANGUAGE = "cs"
#: The groups the site has a practice exam for, in the app's order.
GROUPS = ["A", "B", "BE", "C", "CE", "D", "DE"]
#: The exam is passed with 43 of its 50 points. The site says so on its home page; its data does
#: not carry it.
PASS_MARK = {"points": 43, "outOf": 50}

#: How many practice exams are drawn per group to learn the group's pool. The topics that draw 3
#: questions an exam need this many: with 300, a group's handful of excluded situations went
#: unnoticed. At 600, the weakest case, group A's 141 situations drawn 1,800 times, misses one
#: of them with a chance of about 1 in 2,500.
EXAMS_PER_GROUP = 600

#: How unlikely a group's misses must be, were it drawing from the whole topic, before the topic
#: counts as split for that group.
SPLIT_CHANCE = 1e-6

#: A JPEG is written again at this quality when that makes it smaller. The site's photos are
#: saved for print, at about 470 KB each; at this quality they look the same at a third of that.
JPEG_QUALITY = 80

#: The widest a picture is bundled. The app never shows one wider, and a few of the site's are
#: over 2,000 pixels.
MAX_WIDTH = 1280

#: The height a situation to watch is bundled at. The site's are 1080p at 8 Mbit/s, about 18 MB
#: each and 1.4 GB for the 77; at 480p they come to about 1 MB each, 83 MB for the 77, and the
#: speedometer and the road users are still clear.
VIDEO_HEIGHT = 480

#: The x264 quality the situations to watch are encoded at: higher is smaller and blurrier. 30 made
#: them 125 MB; 33 takes a third off that and still reads well.
VIDEO_CRF = 33

#: The share of see-through pixels up to which a picture counts as a photo with clipped corners,
#: and is bundled as a JPEG on white. Every transparent PNG on the site is one, at 1.5% or less.
MAX_SEE_THROUGH = 0.02


def parse_question_ids(html):
    """The ids of the complete question set, from the practice page that lists them."""
    match = re.search(r"const\s+questionList\s*=\s*\[([\d,\s]*)\]", html)
    if match is None:
        raise ValueError("the page has no questionList")
    return [int(token) for token in match.group(1).split(",") if token.strip()]


def parse_sample_test(html):
    """The practice exam a SampleTest page starts: the JSON passed to `new SampleTest(`."""
    start = html.find("new SampleTest(")
    if start < 0:
        raise ValueError("the page starts no SampleTest")
    test, _ = json.JSONDecoder().raw_decode(html, html.index("{", start))
    return test


def exam_sections(sample):
    """The exam's topics, numbered from 1 in exam order, and a map from the site's topic id
    ("basketScopeId") to that number."""
    sections, section_of_scope = [], {}
    for number, scope in enumerate(sample["basketScopes"], start=1):
        points = {q["pointsCount"] for q in scope["questions"]}
        if len(points) != 1:
            raise ValueError(f"topic {scope['basketScopeId']} mixes points {sorted(points)}")
        sections.append({
            "id": number,
            "title": clean(scope["basketScopeName"]),
            "questionsPerTest": len(scope["questions"]),
            "points": points.pop(),
        })
        section_of_scope[scope["basketScopeId"]] = number
    return sections, section_of_scope


def common_sections(samples):
    """The layout every group's practice exam shares. The bank has one layout for all groups, so
    an exam that differs fails the import rather than being imported wrong."""
    layouts = [exam_sections(sample) for sample in samples]
    for layout, sample in zip(layouts, samples):
        if layout != layouts[0]:
            raise ValueError(f"the {sample['testType']} exam is laid out differently from the {samples[0]['testType']} one")
    return layouts[0]


def trim_exam(exam):
    """What the importer keeps of one practice exam: its group, and the questions and points it
    drew in each topic."""
    return {
        "testType": exam["testType"],
        "basketScopes": [
            {
                "basketScopeId": scope["basketScopeId"],
                "basketScopeName": scope["basketScopeName"],
                "questions": [{"id": q["id"], "pointsCount": q["pointsCount"]} for q in scope["questions"]],
            }
            for scope in exam["basketScopes"]
        ],
    }


def draws_by_scope(exams):
    """Every id [exams] drew, by the site's topic id, as often as it was drawn."""
    result = {}
    for exam in exams:
        for scope in exam["basketScopes"]:
            result.setdefault(scope["basketScopeId"], []).extend(q["id"] for q in scope["questions"])
    return result


def poisson_tail(lam, k):
    """The chance that a Poisson count with mean [lam] is [k] or more."""
    return max(0.0, 1.0 - sum(math.exp(-lam) * lam ** i / math.factorial(i) for i in range(k)))


def draws_whole_pool(pool_size, draws):
    """Whether [draws] could have come from all [pool_size] questions of a topic.

    Drawing d times from n questions leaves each one undrawn with a chance of (1 - 1/n)^d, so the
    number never drawn is close to a Poisson count with mean n(1 - 1/n)^d. Missing many more than
    that means the draws keep to part of the topic.
    """
    missed = pool_size - len(set(draws))
    expected = pool_size * (1 - 1 / pool_size) ** len(draws)
    return poisson_tail(expected, missed) >= SPLIT_CHANCE


def assign_groups(pools, draws):
    """Maps question id -> the groups whose exams can ask it, in the order of [draws].

    [pools] is every question of the complete set by the site's topic id, and [draws] each group's
    draws by topic id. Also returns the topics a group draws only part of, as
    (group, topic id) -> the number of questions in that part.
    """
    result, parts = {}, {}
    for group, by_scope in draws.items():
        for scope, ids in by_scope.items():
            unknown = set(ids) - pools.get(scope, set())
            if unknown:
                raise ValueError(f"the {group} exams drew {sorted(unknown)}, which topic {scope} of the complete set lacks")
        for scope, pool in pools.items():
            drawn = by_scope.get(scope, [])
            if not drawn:
                continue
            if draws_whole_pool(len(pool), drawn):
                members = pool
            else:
                members = set(drawn)
                parts[(group, scope)] = len(members)
            for qid in members:
                result.setdefault(qid, []).append(group)
    return result, parts


def file_name(url):
    """The file a media URL of the site names, which is the name the app bundles it under."""
    return url.rsplit("/", 1)[1]


def parse_question(raw, section_of_scope):
    """One question from the API, in the shape the bank file uses.

    A video is kept with the still the paper test prints in its place, which the app shows until the
    video plays. Answers that are pictures have no text, and their pictures go in `answerImages`.
    """
    if raw["basketScopeId"] not in section_of_scope:
        raise ValueError(f"question {raw['id']} is in topic {raw['basketScopeId']}, which the exam does not have")
    answers = sorted(raw["questionAnswers"], key=lambda answer: answer["sortOrder"])
    ids = [answer["answerId"] for answer in answers]
    if raw["correctAnswerId"] not in ids:
        raise ValueError(f"question {raw['id']}: the correct answer is not among its answers")
    question = {
        "id": raw["id"],
        "section": section_of_scope[raw["basketScopeId"]],
        "text": clean(raw["questionText"]),
        "answers": [clean(answer["multilingualAnswerTexts"][LANGUAGE]) for answer in answers],
        "correct": ids.index(raw["correctAnswerId"]),
        "points": raw["pointsCount"],
        "image": None,
    }
    media = raw["mediaContent"]
    if media and media["mediaUrl"].endswith(".mp4"):
        question["image"] = media["printMediaName"]
        # The site plays a clip on a loop beside the question, and a situation to watch once, full
        # screen, when asked to.
        question["video"] = {"file": file_name(media["mediaUrl"]), "loops": not media.get("isFullScreen")}
    elif media:
        question["image"] = file_name(media["mediaUrl"])
    if any(answer["mediaContent"] for answer in answers):
        # The site gives a picture answer a "." for its text.
        question["answers"] = ["" for _ in answers]
        question["answerImages"] = [file_name(answer["mediaContent"]["mediaUrl"]) for answer in answers]
    return question


def media_files(question):
    """Every file [question] is shown with: its picture, its video and its answers' pictures."""
    files = [question["image"]] if question["image"] else []
    if "video" in question:
        files.append(question["video"]["file"])
    return files + question.get("answerImages", [])


def encode_video_args(source, target):
    """The ffmpeg command that makes a situation to watch small enough to bundle: VIDEO_HEIGHT
    tall, H.264 that every platform's player reads, no sound, and the index at the start of the file
    so it starts playing at once."""
    return [
        "ffmpeg", "-v", "error", "-y", "-i", str(source),
        "-vf", f"scale=-2:{VIDEO_HEIGHT}",
        "-c:v", "libx264", "-preset", "slow", "-crf", str(VIDEO_CRF), "-profile:v", "main", "-pix_fmt", "yuv420p",
        "-an", "-movflags", "+faststart",
        str(target),
    ]


def flatten_on_white(pixmap, alpha):
    """[pixmap] without its [alpha] channel: a pixel less than half opaque turns white, the others
    keep their color. Only used on photos with a few see-through pixels at their corners."""
    rgb = bytearray(pymupdf.Pixmap(pixmap, 0).samples)
    for index, value in enumerate(alpha):
        if value < 128:
            rgb[index * 3:index * 3 + 3] = b"\xff\xff\xff"
    return pymupdf.Pixmap(pymupdf.csRGB, pixmap.width, pixmap.height, bytes(rgb), False)


def compress(name, data):
    """The name and the bytes to bundle the picture [name], whose file is [data], under.

    A photo is bundled as a JPEG at JPEG_QUALITY and at most MAX_WIDTH wide, when that makes it
    smaller: the site saves its JPEGs for print, and many photos as PNGs. A PNG has to halve to be
    turned into a JPEG, because line art is smaller and sharper as a PNG. A photo whose corners are
    see-through is put on white; a picture that is really transparent, and a GIF, is kept as it is.
    """
    stem, extension = name.rsplit(".", 1)
    if extension.lower() not in ("jpg", "jpeg", "png"):
        return name, data
    pixmap = pymupdf.Pixmap(data)
    if pixmap.alpha:
        alpha = pixmap.samples[pixmap.n - 1::pixmap.n]
        if sum(1 for value in alpha if value < 255) > MAX_SEE_THROUGH * len(alpha):
            return name, data
        pixmap = flatten_on_white(pixmap, alpha)
    if pixmap.n != 3:
        return name, data
    while pixmap.width > MAX_WIDTH:
        pixmap.shrink(1)
    jpeg = pixmap.tobytes("jpg", jpg_quality=JPEG_QUALITY)
    if extension.lower() == "png":
        return (f"{stem}.jpg", jpeg) if len(jpeg) < len(data) / 2 else (name, data)
    return (name, jpeg) if len(jpeg) < len(data) else (name, data)


def bank_document(questions, sections, groups_by_id, version):
    """The contents of the bank file, with the questions that are in at least one group."""
    return {
        "id": BANK_ID,
        "version": version,
        "country": COUNTRY,
        "language": LANGUAGE,
        "imageDir": IMAGES_OUT.name,
        "sections": sections,
        "questions": [
            {**q, "groups": groups_by_id[q["id"]]}
            for q in sorted(questions, key=lambda q: (q["section"], q["id"]))
            if groups_by_id.get(q["id"])
        ],
        "passMark": PASS_MARK,
    }


def fetch_sources(refresh, exams_per_group):
    """Downloads the question list, every question and [exams_per_group] practice exams of every
    group, unless they are cached, and returns the questions and the exams by group."""
    log("Fetching sources")
    area = download(ALL_QUESTIONS_URL, CACHE / "all-questions.html", refresh)
    ids = parse_question_ids(area.read_text(encoding="utf-8"))
    to_fetch = [qid for qid in ids if refresh or not (CACHE / "questions" / f"{qid}.json").exists()]
    log(f"  {len(ids)} questions, {len(to_fetch)} to download")
    for qid in to_fetch:
        download(QUESTION_URL.format(id=qid), CACHE / "questions" / f"{qid}.json", refresh, quiet=True)
        # One request at a time, gently: it is the ministry's server.
        time.sleep(0.1)
    questions = [read_json(CACHE / "questions" / f"{qid}.json") for qid in ids]
    return questions, {group: fetch_exams(group, exams_per_group, refresh) for group in GROUPS}


def fetch_exams(group, count, refresh):
    """[count] practice exams of [group]. Each draws anew, so only what it drew is cached."""
    paths = [CACHE / "exams" / group / f"{n}.json" for n in range(count)]
    to_fetch = [path for path in paths if refresh or not path.exists()]
    if to_fetch:
        log(f"  {group}: drawing {len(to_fetch)} practice exams")
    for path in to_fetch:
        page = download(SAMPLE_TEST_URL.format(group=group), CACHE / "exam.html", refresh=True, quiet=True)
        exam = trim_exam(parse_sample_test(page.read_text(encoding="utf-8")))
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(exam, ensure_ascii=False), encoding="utf-8")
        time.sleep(0.1)
    return [read_json(path) for path in paths]


def build_bank(raw_questions, exams, version):
    """The bank document, logging which topics are split by group and what was left out."""
    sections, section_of_scope = common_sections([exam for group_exams in exams.values() for exam in group_exams])
    pools = {}
    for raw in raw_questions:
        pools.setdefault(raw["basketScopeId"], set()).add(raw["id"])
    groups_by_id, parts = assign_groups(pools, {g: draws_by_scope(e) for g, e in exams.items()})
    for (group, scope), size in sorted(parts.items(), key=lambda p: (p[0][1], GROUPS.index(p[0][0]))):
        log(f"  topic {section_of_scope[scope]}: {group} draws {size} of its {len(pools[scope])} questions")
    questions = [parse_question(raw, section_of_scope) for raw in raw_questions]
    document = bank_document(questions, sections, groups_by_id, version)
    unused = sorted({q["id"] for q in questions} - {q["id"] for q in document["questions"]})
    if unused:
        log(f"  left out {len(unused)} questions no group's exams drew: {unused}")
    return document

def write_media(document, refresh):
    """Writes the pictures and videos [document] references, and points its questions at the names
    they were written under (a photo saved as a PNG is bundled as a JPEG).

    A clip that loops is small and bundled as it is; a situation to watch is encoded smaller first,
    once, into the cache, which needs ffmpeg on the PATH.
    """
    questions = document["questions"]
    plays_once = {q["video"]["file"] for q in questions if "video" in q and not q["video"]["loops"]}
    files = sorted({name for q in questions for name in media_files(q)})
    log(f"Writing media: {len(files)} files, {len(plays_once)} of them videos to encode")
    IMAGES_OUT.mkdir(parents=True, exist_ok=True)
    bundled_as = {}
    for name in files:
        source = download(f"{BASE_URL}/binary_content_storage/{name}", CACHE / "media" / name, refresh, quiet=True)
        if name in plays_once:
            encoded = CACHE / "encoded" / name
            if refresh or not encoded.exists():
                encoded.parent.mkdir(parents=True, exist_ok=True)
                subprocess.run(encode_video_args(source, encoded), check=True)
            shutil.copyfile(encoded, IMAGES_OUT / name)
            bundled_as[name] = name
        elif name.endswith(".mp4"):
            shutil.copyfile(source, IMAGES_OUT / name)
            bundled_as[name] = name
        else:
            bundled, data = compress(name, source.read_bytes())
            (IMAGES_OUT / bundled).write_bytes(data)
            bundled_as[name] = bundled
    for q in questions:
        if q["image"]:
            q["image"] = bundled_as[q["image"]]
        if "answerImages" in q:
            q["answerImages"] = [bundled_as[name] for name in q["answerImages"]]

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--refresh", action="store_true", help="re-download sources and images even if cached")
    parser.add_argument("--exams", type=int, default=EXAMS_PER_GROUP,
                        help="practice exams to draw per group to learn its pool (default %(default)s)")
    parser.add_argument("--version", type=int, default=1,
                        help="bank version to write (bump when the ministry publishes a new question set; "
                             "see docs/importing-questions.md)")
    args = parser.parse_args()

    raw_questions, exams = fetch_sources(args.refresh, args.exams)

    log("Parsing")
    document = build_bank(raw_questions, exams, args.version)
    write_media(document, args.refresh)
    entry = write_bank(document, GROUPS)
    log(f"  {document['id']}: {len(document['questions'])} questions, {entry['questionCounts']}")
    # Keep other importers' banks in the shared index.
    write_index(BANKS / "index.json", [entry], COUNTRY)
    remove_stale_images(IMAGES_OUT.name)
    log("Done")


if __name__ == "__main__":
    main()
