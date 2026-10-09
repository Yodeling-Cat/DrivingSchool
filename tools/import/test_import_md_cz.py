"""Unit tests for the etesty.md.gov.cz importer. Offline: they only use the fixtures in
tests_fixtures/md_cz/, which are trimmed copies of what the site serves."""
import json
import math
import random
import unittest
from pathlib import Path

import pymupdf

import import_md_cz

FIXTURES = Path(__file__).resolve().parent / "tests_fixtures" / "md_cz"


def raw_question(qid):
    return json.loads((FIXTURES / "questions" / f"{qid}.json").read_text(encoding="utf-8"))


def sample_test():
    return import_md_cz.parse_sample_test((FIXTURES / "sample_test.html").read_text(encoding="utf-8"))


class ParseQuestionIdsTest(unittest.TestCase):
    def test_reads_the_ids_of_the_complete_set_in_order(self):
        html = (FIXTURES / "area.html").read_text(encoding="utf-8")

        self.assertEqual([180, 1022, 1408, 1469, 1863, 105487], import_md_cz.parse_question_ids(html))

    def test_a_page_without_the_list_fails_loudly(self):
        with self.assertRaises(ValueError):
            import_md_cz.parse_question_ids("<html></html>")


class ParseSampleTestTest(unittest.TestCase):
    def test_reads_the_test_the_page_starts_and_not_the_commented_out_one(self):
        sample = sample_test()

        self.assertEqual("B", sample["testType"])
        self.assertEqual(30, sample["testTime"])
        self.assertEqual([9, 11, 12], [s["basketScopeId"] for s in sample["basketScopes"]])

    def test_a_page_without_a_test_fails_loudly(self):
        with self.assertRaises(ValueError):
            import_md_cz.parse_sample_test("<html></html>")


class ExamSectionsTest(unittest.TestCase):
    def test_numbers_the_topics_in_exam_order_with_their_size_and_points(self):
        sections, section_of_scope = import_md_cz.exam_sections(sample_test())

        self.assertEqual(
            [
                {"id": 1, "title": "Znalost pravidel provozu na pozemních komunikacích", "questionsPerTest": 2, "points": 2},
                {"id": 2, "title": "Znalost dopravních značek, světelných a akustických signálů", "questionsPerTest": 1, "points": 1},
                {"id": 3, "title": "Schopnost řešení dopravních situací", "questionsPerTest": 1, "points": 4},
            ],
            sections,
        )
        self.assertEqual({9: 1, 11: 2, 12: 3}, section_of_scope)

    def test_a_topic_whose_questions_differ_in_points_fails_loudly(self):
        sample = sample_test()
        sample["basketScopes"][0]["questions"][1]["pointsCount"] = 3

        with self.assertRaises(ValueError):
            import_md_cz.exam_sections(sample)

    def test_the_same_layout_for_every_group_is_accepted(self):
        sections, _ = import_md_cz.exam_sections(sample_test())

        self.assertEqual(sections, import_md_cz.common_sections([sample_test(), sample_test()])[0])

    def test_groups_with_different_layouts_fail_loudly(self):
        other = sample_test()
        other["basketScopes"].pop()

        with self.assertRaises(ValueError):
            import_md_cz.common_sections([sample_test(), other])


class ParseQuestionTest(unittest.TestCase):
    SECTION_OF_SCOPE = {9: 1, 11: 2, 12: 3}

    def parse(self, raw):
        return import_md_cz.parse_question(raw, self.SECTION_OF_SCOPE)

    def test_a_text_question_keeps_its_answers_in_order_and_the_correct_one(self):
        q = self.parse(raw_question(180))

        self.assertEqual(180, q["id"])
        self.assertEqual(1, q["section"])
        self.assertEqual(2, q["points"])
        self.assertEqual("Nemotorové vozidlo je:", q["text"])
        self.assertEqual(3, len(q["answers"]))
        self.assertTrue(q["answers"][0].startswith("Přípojné vozidlo"))
        self.assertEqual(0, q["correct"])
        self.assertIsNone(q["image"])

    def test_answers_follow_their_sort_order_not_the_order_they_arrive_in(self):
        raw = raw_question(1022)
        raw["questionAnswers"].reverse()

        q = self.parse(raw)

        self.assertTrue(q["answers"][1].startswith("Nemusí dát přednost"))
        self.assertEqual(1, q["correct"])

    def test_answer_whitespace_is_collapsed(self):
        q = self.parse(raw_question(1022))

        self.assertEqual("Musí dát přednost v jízdě červenému vozidlu, protože toto vozidlo přijíždí zprava.", q["answers"][0])

    def test_a_question_with_a_picture_uses_the_picture(self):
        self.assertEqual("Q_W_1863.jpg", self.parse(raw_question(1863))["image"])

    def test_a_video_question_uses_the_still_of_the_paper_test(self):
        self.assertEqual("Q_P_1022.jpg", self.parse(raw_question(1022))["image"])

    def test_a_yes_or_no_question_has_two_answers(self):
        q = self.parse(raw_question(1469))

        self.assertEqual(["Ano.", "Ne."], q["answers"])
        self.assertEqual(0, q["correct"])

    def test_a_question_whose_answers_are_pictures_is_left_out(self):
        self.assertIsNone(self.parse(raw_question(1408)))

    def test_a_question_that_has_to_be_watched_to_the_end_is_left_out(self):
        # The new traffic-situation videos are played once, and their still shows the road before
        # anything happens, so the paper test's still cannot stand in for them.
        self.assertIsNone(self.parse(raw_question(105487)))

    def test_a_question_from_a_topic_the_exam_does_not_have_fails_loudly(self):
        raw = raw_question(180)
        raw["basketScopeId"] = 42

        with self.assertRaises(ValueError):
            self.parse(raw)

    def test_a_correct_answer_that_is_not_among_the_answers_fails_loudly(self):
        raw = raw_question(180)
        raw["correctAnswerId"] = 1

        with self.assertRaises(ValueError):
            self.parse(raw)


class MediaTest(unittest.TestCase):
    def test_a_video_is_replaced_by_its_still(self):
        media = raw_question(1022)["mediaContent"]

        self.assertEqual("/binary_content_storage/Q_P_1022.jpg", import_md_cz.media_path(media))

    def test_a_picture_is_taken_as_shown_on_the_web(self):
        media = raw_question(1863)["mediaContent"]

        self.assertEqual("/binary_content_storage/Q_W_1863.jpg", import_md_cz.media_path(media))

    def test_no_media_is_no_path(self):
        self.assertIsNone(import_md_cz.media_path(None))


class SkipReasonTest(unittest.TestCase):
    def test_names_why_a_question_cannot_be_imported(self):
        self.assertEqual("picture answers", import_md_cz.skip_reason(raw_question(1408)))
        self.assertEqual("video to watch", import_md_cz.skip_reason(raw_question(105487)))
        self.assertIsNone(import_md_cz.skip_reason(raw_question(1022)))
        self.assertIsNone(import_md_cz.skip_reason(raw_question(180)))


class CompressTest(unittest.TestCase):
    @staticmethod
    def jpeg(quality):
        # Noise, like a photo's detail, so the quality a JPEG is saved at changes its size.
        noise = random.Random(1).randbytes(64 * 64 * 3)
        return pymupdf.Pixmap(pymupdf.csRGB, 64, 64, noise, False).tobytes("jpg", jpg_quality=quality)

    def test_a_heavy_jpeg_is_recompressed(self):
        heavy = self.jpeg(100)

        smaller = import_md_cz.compress("x.jpg", heavy)

        self.assertLess(len(smaller), len(heavy))
        self.assertEqual((64, 64), (pymupdf.Pixmap(smaller).width, pymupdf.Pixmap(smaller).height))

    def test_a_jpeg_that_would_not_shrink_is_kept(self):
        light = self.jpeg(10)

        self.assertIs(light, import_md_cz.compress("x.jpg", light))

    def test_other_formats_are_kept_as_they_are(self):
        gif = b"GIF89a not really"

        self.assertIs(gif, import_md_cz.compress("x.gif", gif))


class ExamDrawsTest(unittest.TestCase):
    def test_keeps_only_what_an_exam_drew_and_its_layout(self):
        exam = import_md_cz.trim_exam(sample_test())

        self.assertEqual("B", exam["testType"])
        self.assertEqual({9: [1468, 1566], 11: [1863], 12: [2300]}, import_md_cz.exam_draws(exam))
        # A trimmed exam still gives the layout.
        self.assertEqual(import_md_cz.exam_sections(sample_test()), import_md_cz.exam_sections(exam))

    def test_adds_up_the_draws_of_many_exams_by_topic(self):
        exam = import_md_cz.trim_exam(sample_test())

        self.assertEqual(
            {9: [1468, 1566, 1468, 1566], 11: [1863, 1863], 12: [2300, 2300]},
            import_md_cz.draws_by_scope([exam, exam]),
        )


class WholePoolTest(unittest.TestCase):
    def test_draws_that_reach_every_question_are_the_whole_pool(self):
        self.assertTrue(import_md_cz.draws_whole_pool(10, list(range(10)) * 5))

    def test_a_question_or_two_missed_by_chance_is_still_the_whole_pool(self):
        # 174 draws from the whole of 100 questions miss about 17 of them; these miss 13.
        draws = [i for i in range(100) if i % 8] * 2
        self.assertTrue(import_md_cz.draws_whole_pool(100, draws))

    def test_many_draws_that_keep_to_half_the_questions_are_a_part_of_it(self):
        self.assertFalse(import_md_cz.draws_whole_pool(100, list(range(50)) * 20))

    def test_poisson_tail(self):
        self.assertAlmostEqual(1.0, import_md_cz.poisson_tail(2.0, 0))
        self.assertAlmostEqual(1 - math.exp(-2.0), import_md_cz.poisson_tail(2.0, 1))
        self.assertAlmostEqual(1 - 3 * math.exp(-2.0), import_md_cz.poisson_tail(2.0, 2))


class AssignGroupsTest(unittest.TestCase):
    POOLS = {9: set(range(1, 41)), 10: set(range(101, 161))}

    def exams(self, scope_pools, count=200, seed=1):
        """[count] exams that draw 4 questions from each topic's pool in [scope_pools]."""
        rng = random.Random(seed)
        return [{scope: rng.sample(sorted(pool), 4) for scope, pool in scope_pools.items()} for _ in range(count)]

    def draws(self, exams):
        result = {}
        for exam in exams:
            for scope, ids in exam.items():
                result.setdefault(scope, []).extend(ids)
        return result

    def test_a_topic_every_group_draws_from_whole_goes_to_every_group(self):
        draws = {group: self.draws(self.exams(self.POOLS, seed=n)) for n, group in enumerate(["A", "B"])}

        groups, parts = import_md_cz.assign_groups(self.POOLS, draws, ["A", "B"])

        self.assertEqual(["A", "B"], groups[1])
        self.assertEqual(["A", "B"], groups[160])
        self.assertEqual({}, parts)

    def test_a_topic_a_group_draws_only_part_of_gives_that_group_only_that_part(self):
        motorcycles, cars = set(range(101, 131)), set(range(131, 161))
        draws = {
            "A": self.draws(self.exams({9: self.POOLS[9], 10: motorcycles}, seed=1)),
            "B": self.draws(self.exams({9: self.POOLS[9], 10: cars}, seed=2)),
        }

        groups, parts = import_md_cz.assign_groups(self.POOLS, draws, ["A", "B"])

        self.assertEqual(["A"], groups[101])
        self.assertEqual(["B"], groups[160])
        self.assertEqual(["A", "B"], groups[1])
        self.assertEqual({("A", 10): 30, ("B", 10): 30}, parts)

    def test_a_question_no_group_drew_from_a_split_topic_has_no_group(self):
        draws = {
            "A": self.draws(self.exams({9: self.POOLS[9], 10: set(range(101, 131))}, seed=1)),
            "B": self.draws(self.exams({9: self.POOLS[9], 10: set(range(131, 151))}, seed=2)),
        }

        groups, _ = import_md_cz.assign_groups(self.POOLS, draws, ["A", "B"])

        self.assertNotIn(155, groups)

    def test_a_drawn_question_the_complete_set_does_not_have_fails_loudly(self):
        draws = {"A": {9: [1, 2, 999]}}

        with self.assertRaises(ValueError):
            import_md_cz.assign_groups(self.POOLS, draws, ["A"])


class BankDocumentTest(unittest.TestCase):
    @staticmethod
    def questions():
        _, section_of_scope = import_md_cz.exam_sections(sample_test())
        return [import_md_cz.parse_question(raw_question(i), section_of_scope) for i in (1863, 180, 1469)]

    def test_writes_the_bank_format_with_the_groups_each_question_was_assigned(self):
        sections, _ = import_md_cz.exam_sections(sample_test())

        document = import_md_cz.bank_document(self.questions(), sections, {180: ["A", "B"], 1863: ["C"], 1469: ["A"]}, version=2)

        self.assertEqual(
            {"id": "cz-cs", "version": 2, "country": "CZ", "language": "cs", "imageDir": "md-cz"},
            {k: document[k] for k in ("id", "version", "country", "language", "imageDir")},
        )
        self.assertEqual(sections, document["sections"])
        # Sorted by section, then id.
        self.assertEqual([180, 1469, 1863], [q["id"] for q in document["questions"]])
        self.assertEqual([["A", "B"], ["A"], ["C"]], [q["groups"] for q in document["questions"]])

    def test_a_question_with_no_group_is_left_out(self):
        sections, _ = import_md_cz.exam_sections(sample_test())

        document = import_md_cz.bank_document(self.questions(), sections, {180: ["A"]}, version=1)

        self.assertEqual([180], [q["id"] for q in document["questions"]])

    def test_the_index_entry_counts_the_questions_of_every_group(self):
        sections, _ = import_md_cz.exam_sections(sample_test())
        document = import_md_cz.bank_document(self.questions(), sections, {180: ["A", "B"], 1863: ["B"], 1469: ["A"]}, version=1)

        entry = import_md_cz.index_entry(document)

        self.assertEqual(
            {"id": "cz-cs", "version": 1, "country": "CZ", "language": "cs",
             "questionCounts": {"A": 2, "B": 2, "BE": 0, "C": 0, "CE": 0, "D": 0, "DE": 0}},
            entry,
        )


if __name__ == "__main__":
    unittest.main()
