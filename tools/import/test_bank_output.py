"""Checks the committed importer output that ships in the app. Run after every import."""
import unittest

import import_md_cz
import import_minv
from importer_common import BANKS, IMAGES, read_json

#: The topic of traffic situations. The app reads its intersections material out of it and nothing
#: else, in every bank (`SITUATIONS_SECTION` in :core's situations/Situations.kt).
SITUATIONS_SECTION = 4

#: What each country's exam looks like: its license groups, how many answers a question offers
#: (the Czech bank has yes-or-no questions), and the folder its situation drawings are kept in, when
#: it keeps them in one (the Czech pictures are named by question).
EXAMS = {
    "SK": {"groups": import_minv.GROUPS, "answers": {3}, "situation_dir": "obr3/ds/"},
    "CZ": {"groups": import_md_cz.GROUPS, "answers": {2, 3}, "situation_dir": None},
}


class BankOutputTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.index = read_json(BANKS / "index.json")
        cls.banks = {path.stem: read_json(path) for path in BANKS.glob("*-v*.json")}

    def test_every_index_entry_has_its_bank_file(self):
        for entry in self.index:
            self.assertIn(f"{entry['id']}-v{entry['version']}", self.banks)

    def test_index_lists_the_latest_version_of_each_bank(self):
        for entry in self.index:
            versions = [b["version"] for b in self.banks.values() if b["id"] == entry["id"]]
            self.assertEqual(max(versions), entry["version"], entry["id"])

    def test_index_counts_match_the_bank(self):
        for entry in self.index:
            bank = self.banks[f"{entry['id']}-v{entry['version']}"]
            for group, count in entry["questionCounts"].items():
                self.assertEqual(count, sum(1 for q in bank["questions"] if group in q["groups"]), (entry["id"], group))

    def test_questions_are_well_formed(self):
        for name, bank in self.banks.items():
            sections = {s["id"]: s for s in bank["sections"]}
            ids = [q["id"] for q in bank["questions"]]
            self.assertEqual(len(ids), len(set(ids)), f"{name}: duplicate question ids")
            exam = EXAMS[bank["country"]]
            for q in bank["questions"]:
                where = f"{name} question {q['id']}"
                self.assertIn(len(q["answers"]), exam["answers"], where)
                pictures = q.get("answerImages")
                if pictures is None:
                    self.assertTrue(all(a.strip() for a in q["answers"]), where)
                else:
                    # Answers that are pictures: one picture each, and no text beside it.
                    self.assertEqual(len(q["answers"]), len(pictures), where)
                    self.assertTrue(all(p.strip() for p in pictures), where)
                if q.get("video"):
                    # A video always has a still to stand for it until it plays.
                    self.assertTrue(q["image"], where)
                self.assertIn(q["correct"], range(len(q["answers"])), where)
                self.assertTrue(q["text"].strip(), where)
                self.assertIn(q["section"], sections, where)
                self.assertEqual(sections[q["section"]]["points"], q["points"], where)
                self.assertTrue(q["groups"], where)
                self.assertTrue(set(q["groups"]) <= set(exam["groups"]), where)

    def test_a_full_exam_fits_in_every_group(self):
        # Every group needs enough questions per section for at least one complete test.
        for name, bank in self.banks.items():
            for group in EXAMS[bank["country"]]["groups"]:
                for section in bank["sections"]:
                    available = sum(1 for q in bank["questions"] if q["section"] == section["id"] and group in q["groups"])
                    self.assertGreaterEqual(available, section["questionsPerTest"], (name, group, section["id"]))

    def test_an_exam_is_worth_the_points_its_pass_mark_is_out_of(self):
        for name, bank in self.banks.items():
            total = sum(s["questionsPerTest"] * s["points"] for s in bank["sections"])
            self.assertEqual(bank["passMark"]["outOf"], total, name)
            self.assertLess(bank["passMark"]["points"], total, name)

    def test_every_referenced_picture_and_video_is_bundled(self):
        for name, bank in self.banks.items():
            for q in bank["questions"]:
                files = [q["image"]] if q["image"] else []
                if q.get("video"):
                    files.append(q["video"]["file"])
                files += q.get("answerImages") or []
                for file in files:
                    self.assertTrue((IMAGES / bank["imageDir"] / file).is_file(), f"{name}: {file}")

    def test_the_situations_the_app_reads_are_the_topic_of_that_name(self):
        # The app builds its intersections material from one topic and nothing else, so the topic
        # has to be the situations one: every question of it is about a picture, and every drawing
        # of a situations folder is asked there rather than in some other topic.
        for name, bank in self.banks.items():
            situations = [q for q in bank["questions"] if q["section"] == SITUATIONS_SECTION]
            self.assertTrue(situations, f"{name}: no situations topic")
            for q in situations:
                self.assertTrue(q["image"], f"{name} question {q['id']}: in the situations topic with no picture")
            situation_dir = EXAMS[bank["country"]]["situation_dir"]
            for q in bank["questions"]:
                if situation_dir and q["image"] and situation_dir in f"{q['image']}/":
                    self.assertEqual(
                        SITUATIONS_SECTION,
                        q["section"],
                        f"{name}: {q['image']} is asked in topic {q['section']}",
                    )


if __name__ == "__main__":
    unittest.main()
