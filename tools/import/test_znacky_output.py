"""Checks the committed sign catalog that ships in the app. Run after every import."""
import collections
import unittest

import import_znacky
from importer_common import read_json

SIGNS = import_znacky.SIGNS_OUT
IMAGES = import_znacky.IMAGES

# The number of signs the current import produced. It moves when the catalog does, which is the
# point: a re-import that silently loses rows has to be looked at.
SIGN_COUNT = 120


class SignCatalogOutputTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = read_json(SIGNS / "sk-v1.json")
        cls.signs = cls.catalog["signs"]

    def test_lists_every_sign_of_the_current_import(self):
        self.assertEqual(SIGN_COUNT, len(self.signs))

    def test_signs_have_a_number_a_meaning_and_a_picture(self):
        for sign in self.signs:
            where = f"sign {sign.get('number')}"
            self.assertTrue(sign["number"].strip(), where)
            self.assertTrue(sign["description"].strip(), where)
            self.assertTrue(sign["image"].strip(), where)
            self.assertTrue((IMAGES / self.catalog["imageDir"] / sign["image"]).is_file(), where)

    def test_numbers_are_unique(self):
        numbers = [sign["number"] for sign in self.signs]
        self.assertEqual(len(numbers), len(set(numbers)))

    def test_signs_keep_the_regulations_own_order(self):
        # The catalog lists warning signs first, then the prohibitory, informative and so on, so
        # the numbers rise in blocks rather than strictly.
        self.assertEqual("110", self.signs[0]["number"])
        self.assertEqual("731", self.signs[-1]["number"])

    def test_every_sign_knows_the_part_of_the_catalog_it_is_from(self):
        kinds = {
            "VÝSTRAŽNÉ ZNAČKY",
            "REGULAČNÉ ZNAČKY",
            "INFORMAČNÉ ZNAČKY",
            "DODATKOVÉ TABUĽKY",
            "VODOROVNÉ ZNAČKY",
            "DOPRAVNÉ ZARIADENIA",
        }
        for sign in self.signs:
            where = f"sign {sign['number']}"
            self.assertIn(sign["kind"], kinds, where)
            self.assertTrue(sign["group"].strip() or sign["kind"], where)
            # A group heading must not have run on into a sign's meaning.
            self.assertNotIn(sign["group"], sign["description"], where)

    def test_the_signs_come_in_the_six_kinds_of_the_regulation(self):
        counts = collections.Counter(sign["kind"] for sign in self.signs)

        self.assertEqual(
            {
                "VÝSTRAŽNÉ ZNAČKY": 25,
                "REGULAČNÉ ZNAČKY": 31,
                "INFORMAČNÉ ZNAČKY": 44,
                "DODATKOVÉ TABUĽKY": 5,
                "VODOROVNÉ ZNAČKY": 10,
                "DOPRAVNÉ ZARIADENIA": 5,
            },
            dict(counts),
        )

    def test_the_regulation_is_named_as_the_source(self):
        self.assertIn("30/2020", self.catalog["source"])

    def test_no_sign_carries_text_from_the_documents_around_it(self):
        # The regulation's last page ends with the citations of its last sign and the publisher's
        # imprint, drawn in the same columns as a row: one sign shipped with all of it as its name.
        # Only words no sign's own text can hold are listed here.
        forbidden = ("@", "tel.:", "e-mail", "helpdesk", "Vydavateľ", "Zbierky zákonov", "slov-lex", "Račianska")
        for sign in self.signs:
            text = f"{sign['name']} {sign['description']}"
            for word in forbidden:
                self.assertNotIn(word, text, f"sign {sign['number']}")

    def test_a_signs_name_is_a_name(self):
        # The check that catches a name a column of the regulation ran into: a name is a few words
        # on one line, so a citation or an imprint block is not one however it got there.
        for sign in self.signs:
            where = f"sign {sign['number']}: {sign['name']!r}"
            self.assertLessEqual(len(sign["name"]), 60, where)
            self.assertNotIn("\n", sign["name"], where)
            self.assertFalse(sign["name"].endswith("."), where)
            self.assertFalse(any(char.isdigit() for char in sign["name"]), where)

    def test_the_last_sign_of_the_regulation_keeps_its_name(self):
        last = next(s for s in self.signs if s["number"] == "731")

        self.assertEqual("Čierna šípka", last["name"])

    def test_the_index_lists_the_catalog_and_its_count(self):
        index = read_json(SIGNS / "index.json")
        entry = next(c for c in index if c["id"] == self.catalog["id"])

        self.assertEqual(self.catalog["version"], entry["version"])
        self.assertEqual(len(self.signs), entry["signCount"])


if __name__ == "__main__":
    unittest.main()
