"""Unit tests for the minv.sk importer. Offline: they only use the fixtures in tests_fixtures/."""
import unittest
from pathlib import Path

import import_minv
from minv_pdf import PdfQuestion

FIXTURES = Path(__file__).resolve().parent / "tests_fixtures"


class PdfGroupsTest(unittest.TestCase):
    def test_empty_cell_means_every_group(self):
        self.assertEqual(import_minv.GROUPS, import_minv.pdf_groups([]))

    def test_split_trailer_groups_are_joined(self):
        # The PDF wraps "BE" as "B" + "E" in narrow cells.
        self.assertEqual(["BE", "C", "CE"], import_minv.pdf_groups(["B", "E", "C", "CE"]))

    def test_page_numbers_leaking_into_the_cell_are_dropped(self):
        self.assertEqual(["D", "DE"], import_minv.pdf_groups(["D", "DE", "12."]))

    def test_groups_are_deduplicated_in_canonical_order(self):
        self.assertEqual(["A", "T"], import_minv.pdf_groups(["T", "A", "T"]))


class NormTest(unittest.TestCase):
    def test_ignores_case_whitespace_and_punctuation(self):
        self.assertEqual(import_minv.norm("Vodič  je, povinný!"), import_minv.norm("vodič je povinný"))

    def test_keeps_diacritics(self):
        self.assertNotEqual(import_minv.norm("stáť"), import_minv.norm("stat"))


class LoadData5Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.banks = import_minv.load_data5(FIXTURES / "data5_sample.js")

    def test_reads_every_language_in_order(self):
        self.assertEqual(["sk", "en", "hu"], list(self.banks))

    def test_questions_are_assigned_to_sections_by_position(self):
        questions = self.banks["sk"]["questions"]
        self.assertEqual(1, questions[101]["section"])
        self.assertEqual(1, questions[102]["section"])
        self.assertEqual(2, questions[103]["section"])

    def test_correct_answer_is_zero_based_and_images_are_optional(self):
        q = self.banks["sk"]["questions"][103]
        self.assertEqual(2, q["correct"])
        self.assertEqual(["a3", "b3", "c3"], q["answers"])
        self.assertEqual("obr3/dz/1.png", q["image"])
        self.assertIsNone(self.banks["sk"]["questions"][101]["image"])

    def test_sections_record_questions_per_test_and_points(self):
        self.assertEqual(
            [
                {"id": 1, "title": "Pravidlá", "questionsPerTest": 2, "points": 3},
                {"id": 2, "title": "Značky", "questionsPerTest": 1, "points": 2},
            ],
            self.banks["sk"]["sections"],
        )

    def test_text_whitespace_is_collapsed(self):
        self.assertEqual("Otázka jedna", self.banks["sk"]["questions"][101]["text"])


class MatchGroupsTest(unittest.TestCase):
    @staticmethod
    def question(qid, text, answer, section=1):
        return {"id": qid, "section": section, "text": text, "answers": [answer, "x", "y"], "correct": 0}

    @staticmethod
    def row(text, answer, groups=(), section=1, number=1):
        return PdfQuestion(section=section, number=number, question=text, answer=answer, groups=list(groups))

    def test_exact_match_takes_the_rows_groups(self):
        questions = {1: self.question(1, "Autobusom sa rozumie", "vozidlo na dopravu osôb")}
        rows = [self.row("Autobusom sa rozumie", "vozidlo na dopravu osôb", ["D", "DE"])]

        groups, fuzzy, missing = import_minv.match_groups(questions, rows)

        self.assertEqual({1: ["D", "DE"]}, groups)
        self.assertEqual((0, []), (fuzzy, missing))

    def test_small_wording_differences_match_fuzzily(self):
        questions = {1: self.question(1, "Ak na ukazovateli so symbolom olejničky zasvieti", "porucha mazania motora.")}
        rows = [self.row("Ak na ukazovateli so symbolom olejničky zasvieti svetlo", "porucha mazania motora")]

        groups, fuzzy, missing = import_minv.match_groups(questions, rows)

        self.assertEqual({1: import_minv.GROUPS}, groups)
        self.assertEqual(1, fuzzy)

    def test_same_text_in_another_section_does_not_match(self):
        questions = {1: self.question(1, "Táto značka", "prikazuje", section=3)}
        rows = [self.row("Táto značka", "prikazuje", section=1)]

        groups, _, missing = import_minv.match_groups(questions, rows)

        self.assertEqual({}, groups)
        self.assertEqual([1], [q["id"] for q in missing])

    def test_duplicate_rows_are_used_once_each(self):
        questions = {
            1: self.question(1, "Táto značka", "prikazuje"),
            2: self.question(2, "Táto značka", "prikazuje"),
        }
        rows = [
            self.row("Táto značka", "prikazuje", ["A"], number=1),
            self.row("Táto značka", "prikazuje", ["T"], number=2),
        ]

        groups, _, _ = import_minv.match_groups(questions, rows)

        self.assertEqual({1: ["A"], 2: ["T"]}, groups)


if __name__ == "__main__":
    unittest.main()
