"""Checks the committed sign catalogue that ships in the app. Run after every import."""
import json
import unittest

import import_znacky

SIGNS = import_znacky.SIGNS_OUT
IMAGES = import_znacky.RESOURCES / "images"

# The number of signs the current import produced. It moves when the catalogue does, which is the
# point: a re-import that silently loses rows has to be looked at.
SIGN_COUNT = 120


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


class SignCatalogueOutputTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalogue = load(SIGNS / "sk-v1.json")
        cls.signs = cls.catalogue["signs"]

    def test_lists_every_sign_of_the_current_import(self):
        self.assertEqual(SIGN_COUNT, len(self.signs))

    def test_signs_have_a_number_a_meaning_and_a_picture(self):
        for sign in self.signs:
            where = f"sign {sign.get('number')}"
            self.assertTrue(sign["number"].strip(), where)
            self.assertTrue(sign["description"].strip(), where)
            self.assertTrue(sign["image"].strip(), where)
            self.assertTrue((IMAGES / self.catalogue["imageDir"] / sign["image"]).is_file(), where)

    def test_numbers_are_unique(self):
        numbers = [sign["number"] for sign in self.signs]
        self.assertEqual(len(numbers), len(set(numbers)))

    def test_signs_keep_the_regulations_own_order(self):
        # The catalogue lists warning signs first, then the prohibitory, informative and so on, so
        # the numbers rise in blocks rather than strictly.
        self.assertEqual("110", self.signs[0]["number"])
        self.assertEqual("731", self.signs[-1]["number"])

    def test_every_sign_knows_the_part_of_the_catalogue_it_is_from(self):
        for sign in self.signs:
            self.assertTrue(sign["section"].strip(), f"sign {sign['number']}")

    def test_the_regulation_is_named_as_the_source(self):
        self.assertIn("30/2020", self.catalogue["source"])

    def test_the_index_lists_the_catalogue_and_its_count(self):
        index = load(SIGNS / "index.json")
        entry = next(c for c in index if c["id"] == self.catalogue["id"])

        self.assertEqual(self.catalogue["version"], entry["version"])
        self.assertEqual(len(self.signs), entry["signCount"])


if __name__ == "__main__":
    unittest.main()
