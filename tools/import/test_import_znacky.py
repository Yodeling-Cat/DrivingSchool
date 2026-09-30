"""Unit tests for the sign-catalogue importer. Offline: they build the parser's input by hand."""
import unittest

import import_znacky
import znacky_pdf
from znacky_pdf import PdfLine, SignRow

DESC_X = znacky_pdf.DESC_X


def line(text, x=191.5, y=None, page=0, bold=False):
    """A line of the table: the column is [x], and [y] defaults to 'the next one down'."""
    return PdfLine(page=page, y=y if y is not None else 1.0, x=x, text=text, bold=bold)


def body(number, name, description, y=10.0):
    """One catalogue row: the number, the picture's name, then the meaning beside them."""
    return [
        line("19.", x=112.2, y=y),
        line(number, y=y),
        line(" ", x=232.0, y=y + 6),
        line(name, x=170.0, y=y + 12),
        line(description, x=265.4, y=y),
    ]


class RowsTest(unittest.TestCase):
    def test_reads_a_number_a_name_and_a_meaning(self):
        rows = znacky_pdf.rows(body("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov."))

        self.assertEqual(
            [SignRow("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov.", "")], rows
        )

    def test_a_wrapped_name_and_meaning_are_joined(self):
        lines = [
            line("21.", x=112.2, y=10.0),
            line("142", y=10.0),
            line("Priechod pre", x=170.0, y=16.0),
            line("chodcov", x=170.0, y=22.0),
            line("Značka upozorňuje na", x=265.4, y=10.0),
            line("priechod pre chodcov.", x=265.4, y=20.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual("Priechod pre chodcov", rows[0].name)
        self.assertEqual("Značka upozorňuje na priechod pre chodcov.", rows[0].description)

    def test_the_number_is_read_wherever_the_column_puts_it(self):
        # In the príloha on traffic equipment the name is drawn above the sign's number.
        lines = [
            line("6.", x=118.0, y=10.0),
            line("Vodiaca tabuľa", x=173.6, y=16.0),
            line("707", y=10.0),
            line("Vodiaca tabuľa označuje trvalú prekážku.", x=273.6, y=10.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(SignRow("707", "Vodiaca tabuľa", "Vodiaca tabuľa označuje trvalú prekážku.", ""), rows[0])

    def test_a_group_heading_is_not_a_name(self):
        lines = [
            line("I. 4. Pohyb na ceste", x=254.4, y=2.0),
            *body("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov."),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual("Chodci", rows[0].name)
        self.assertEqual("I. 4. Pohyb na ceste", rows[0].section)

    def test_a_document_title_becomes_the_section(self):
        lines = [
            line("DOPRAVNÉ ZARIADENIA", x=233.4, y=2.0),
            *body("701", "Zábrana", "Zábrana vyznačuje uzávierku."),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual("DOPRAVNÉ ZARIADENIA", rows[0].section)

    def test_a_repeated_table_header_does_not_leak_into_the_text(self):
        lines = [
            *body("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov."),
            line("Bod", x=112.6, y=40.0),
            line("Číslo, vyobrazenie a názov značky", x=159.5, y=40.0),
            line("Význam značky, prípadne použitie", x=338.2, y=40.0),
            line("30/2020 Z. z.", x=468.8, y=6.0),
            line("Strana 28", x=55.3, y=6.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual("Chodci", rows[0].name)
        self.assertEqual("Značka upozorňuje na zvýšený pohyb chodcov.", rows[0].description)

    def test_a_header_whose_second_line_is_on_its_own_does_not_leak_either(self):
        # "Význam značky, prípadne použitie" is drawn as two lines, and the second one carries no
        # word that says it is a header.
        lines = [
            *body("113", "Stúpanie", "Značka upozorňuje na nebezpečné stúpanie."),
            line("Význam značky,", x=338.2, y=40.0),
            line("prípadne použitie", x=335.8, y=46.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual("Značka upozorňuje na nebezpečné stúpanie.", rows[0].description)

    def test_a_space_left_before_a_full_stop_is_closed_up(self):
        lines = [
            line("19.", x=112.2, y=10.0),
            line("263", y=10.0),
            line("Koniec najvyššej dovolenej rýchlosti", x=170.0, y=16.0),
            line("Značka ukončuje zákaz predchádzajúcej značky Najvyššia dovolená rýchlosť", x=265.4, y=10.0),
            line(".", x=265.4, y=16.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(
            "Značka ukončuje zákaz predchádzajúcej značky Najvyššia dovolená rýchlosť.",
            rows[0].description,
        )

    def test_a_meaning_continued_on_the_next_page_stays_with_its_row(self):
        lines = [
            *body("145", "Zver", "Značka upozorňuje na neočakávaný pohyb zveri."),
            line("Bod", x=112.6, y=60.0),
            line("Ak je symbol nahradený symbolom obojživelníka, ide o drobné živočíchy.", x=265.4, y=70.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(1, len(rows))
        self.assertEqual(
            "Značka upozorňuje na neočakávaný pohyb zveri. "
            "Ak je symbol nahradený symbolom obojživelníka, ide o drobné živočíchy.",
            rows[0].description,
        )

    def test_symbol_tables_are_not_signs(self):
        # The annex also lists the symbols used on additional panels: no sign number, a name only.
        lines = [
            line("A. SYMBOLY VOZIDIEL A ÚČASTNÍKOV CESTNEJ PREMÁVKY", x=141.9, y=10.0),
            line("10.", x=112.2, y=20.0),
            line("Chodci", x=266.2, y=20.0),
        ]

        self.assertEqual([], znacky_pdf.rows(lines))

    def test_a_row_without_a_name_is_kept(self):
        # Sign 350 is a number plate: its "name" column holds the picture only.
        lines = [
            line("12.", x=112.2, y=10.0),
            line("350", y=10.0),
            line("Značka informuje o čísle cesty I. triedy.", x=265.4, y=10.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(SignRow("350", "", "Značka informuje o čísle cesty I. triedy.", ""), rows[0])

    def test_text_before_the_first_row_number_is_ignored(self):
        lines = [
            line("Značka upozorňuje na", x=265.4, y=4.0),
            *body("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov."),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(1, len(rows))
        self.assertEqual("Chodci", rows[0].name)

    def test_the_citations_and_imprint_after_the_last_sign_are_not_its_name(self):
        # The regulation ends with the sources of its last sign and with the publisher's imprint,
        # written in the same columns as a row's own text.
        lines = [
            line("2.", x=118.0, y=10.0),
            line("731", y=10.0),
            line("Čierna šípka", x=179.3, y=16.0),
            line("Čierna šípka umožňuje vodičovi otočiť sa", x=273.6, y=10.0),
            line("Slovenskej republiky v znení neskorších predpisov.", x=55.3, y=30.0),
            line("§ 2 ods. 1, § 4 ods. 1, 2 a 6 zákona č. 184/1999 Z. z.", x=55.3, y=42.0),
            line("Vydavateľ Zbierky zákonov Slovenskej republiky", x=58.3, y=54.0),
            line("Vydavateľ Zbierky zákonov Slovenskej republiky", x=152.2, y=60.0),
            line("tel.: +421 2 209 25 267, e-mail: helpdesk@slov-lex.sk.", x=178.3, y=66.0),
        ]

        rows = znacky_pdf.rows(lines)

        self.assertEqual(1, len(rows))
        self.assertEqual("Čierna šípka", rows[0].name)
        self.assertEqual("Čierna šípka umožňuje vodičovi otočiť sa", rows[0].description)

    def test_a_row_that_is_not_a_sign_keeps_no_text_from_its_neighbours(self):
        # A symbol table has no sign numbers; whatever sits around it must not become a sign either.
        lines = [
            line("A. SYMBOLY VOZIDIEL A ÚČASTNÍKOV CESTNEJ PREMÁVKY", x=141.9, y=2.0),
            line("10.", x=112.2, y=10.0),
            line("Chodci", x=266.2, y=10.0),
        ]

        self.assertEqual([], znacky_pdf.rows(lines))


class MatchTest(unittest.TestCase):
    @staticmethod
    def sign(number, name="Chodci"):
        return SignRow(number, name, "Značka.", "I. Výstražné značky")

    def test_an_image_finds_the_entry_with_its_number(self):
        listed, unmatched, extra = import_znacky.match([self.sign("140")], ["obr3/dz/140.jpg"])

        self.assertEqual([("140", "obr3/dz/140.jpg")], [(s["number"], s["image"]) for s in listed])
        self.assertEqual([], unmatched)
        self.assertEqual([], extra)

    def test_the_picture_named_exactly_after_the_sign_wins(self):
        listed, _, extra = import_znacky.match([self.sign("321")], ["obr3/dz/321-1.jpg", "obr3/dz/321.jpg"])

        self.assertEqual("obr3/dz/321.jpg", listed[0]["image"])
        self.assertEqual(["obr3/dz/321-1.jpg"], extra)

    def test_a_variant_is_used_when_the_sign_has_no_picture_of_its_own(self):
        listed, unmatched, _ = import_znacky.match([self.sign("152")], ["obr3/dz/152-1.jpg"])

        self.assertEqual("obr3/dz/152-1.jpg", listed[0]["image"])
        self.assertEqual([], unmatched)

    def test_a_picture_of_a_whole_group_of_signs_is_not_a_sign(self):
        listed, unmatched, _ = import_znacky.match([self.sign("401")], ["obr3/dz/401-409.jpg"])

        self.assertEqual([], listed)
        self.assertEqual(["obr3/dz/401-409.jpg"], unmatched)

    def test_an_image_no_entry_claims_is_reported(self):
        listed, unmatched, _ = import_znacky.match([self.sign("140")], ["obr3/dz/1-10a.png"])

        self.assertEqual([], listed)
        self.assertEqual(["obr3/dz/1-10a.png"], unmatched)

    def test_the_catalogue_order_is_kept(self):
        signs = [self.sign("110"), self.sign("201"), self.sign("140")]

        listed, _, _ = import_znacky.match(signs, ["obr3/dz/201.jpg", "obr3/dz/140.jpg", "obr3/dz/110.jpg"])

        self.assertEqual(["110", "201", "140"], [s["number"] for s in listed])


if __name__ == "__main__":
    unittest.main()
