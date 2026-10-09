"""Checks the committed importer output that ships in the app. Run after every import."""
import unittest

import import_minv
from importer_common import read_json

BANKS = import_minv.BANKS_OUT
IMAGES = import_minv.RESOURCES / "images"

#: The bank's topic of situations at intersections. The app reads its intersections material out of
#: it and nothing else (`SITUATIONS_SECTION` in :core's situations/Situations.kt).
SITUATIONS_SECTION = 4

#: Where the bank keeps its drawings of those situations.
SITUATION_DIR = "obr3/ds/"


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
            for q in bank["questions"]:
                where = f"{name} question {q['id']}"
                self.assertEqual(3, len(q["answers"]), where)
                self.assertTrue(all(a.strip() for a in q["answers"]), where)
                self.assertIn(q["correct"], range(3), where)
                self.assertTrue(q["text"].strip(), where)
                self.assertIn(q["section"], sections, where)
                self.assertEqual(sections[q["section"]]["points"], q["points"], where)
                self.assertTrue(q["groups"], where)
                self.assertTrue(set(q["groups"]) <= set(import_minv.GROUPS), where)

    def test_a_full_exam_fits_in_every_group(self):
        # Every group needs enough questions per section for at least one complete test.
        for name, bank in self.banks.items():
            for group in import_minv.GROUPS:
                for section in bank["sections"]:
                    available = sum(1 for q in bank["questions"] if q["section"] == section["id"] and group in q["groups"])
                    self.assertGreaterEqual(available, section["questionsPerTest"], (name, group, section["id"]))

    def test_exam_is_worth_100_points(self):
        for name, bank in self.banks.items():
            total = sum(s["questionsPerTest"] * s["points"] for s in bank["sections"])
            self.assertEqual(100, total, name)

    def test_every_referenced_image_is_bundled(self):
        for name, bank in self.banks.items():
            for q in bank["questions"]:
                if q["image"]:
                    self.assertTrue((IMAGES / bank["imageDir"] / q["image"]).is_file(), f"{name}: {q['image']}")

    def test_the_situations_the_app_reads_are_the_topic_of_that_name(self):
        # The app builds its intersections material from one topic and nothing else, so the topic
        # has to be the situations one: every question of it is about a drawing, and every drawing
        # of the situations folder is asked there rather than in some other topic.
        for name, bank in self.banks.items():
            situations = [q for q in bank["questions"] if q["section"] == SITUATIONS_SECTION]
            self.assertTrue(situations, f"{name}: no situations topic")
            for q in situations:
                self.assertTrue(q["image"], f"{name} question {q['id']}: in the situations topic with no drawing")
            for q in bank["questions"]:
                if q["image"] and SITUATION_DIR in f"{q['image']}/":
                    self.assertEqual(
                        SITUATIONS_SECTION,
                        q["section"],
                        f"{name}: {q['image']} is asked in topic {q['section']}",
                    )


if __name__ == "__main__":
    unittest.main()
