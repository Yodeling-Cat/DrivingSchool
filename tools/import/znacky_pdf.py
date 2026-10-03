"""Parses the sign catalog out of the traffic-sign regulation (vyhláška MV SR č. 30/2020 Z. z.).

The regulation's príloha č. 1 lists every sign of the Slovak road-sign catalog as a table: a row
number ("Bod"), the sign's number and picture, its name, and what it means. `pymupdf` hands the
table back line by line, each line with the x of its column, which is what tells a sign's number
and name apart from the meaning beside them:

    | Bod | Číslo, vyobrazenie a názov značky | Význam značky, prípadne použitie |
    | 19. | 140                               | Značka upozorňuje na ...         |
    |     | [picture]                         |                                  |
    |     | Chodci                            |                                  |

`lines()` reads a PDF into `PdfLine`s and `rows()` turns those lines into `SignRow`s. `rows()` is
pure, so the shaping rules are unit-tested without a PDF.
"""
import re
from dataclasses import dataclass

import pymupdf

# A "Bod" number: the row's position in the table.
BOD = re.compile(r"^\d+\.$")
# A sign's catalog number, e.g. "140" or "321-1".
SIGN_NUMBER = re.compile(r"^\d{2,3}(-\d+)?$")
# A group heading: "I. Všeobecné výstražné značky", "I. 4. Pohyb na ceste", "A. SYMBOLY ...".
HEADING = re.compile(r"^([IVX]+\.\s*\d*\.?|[A-Z]\.)\s+\S")
# A document title, set in capitals: "VÝSTRAŽNÉ ZNAČKY", "DOPRAVNÉ ZARIADENIA".
TITLE = re.compile(r"^[A-ZÁÄČĎÉÍĹĽŇÓÔŔŠŤÚÝŽ][A-ZÁÄČĎÉÍĹĽŇÓÔŔŠŤÚÝŽ\s]+$")
# Table headers repeat on every page, and the last page carries the portal's footer. "prípadne
# použitie" is the second line of the meaning column's header ("Význam značky, prípadne použitie").
NOISE = re.compile(
    r"^(Bod|Číslo|a názov|Význam|Vyobrazenie|prípadne použitie|Zbierka zákonov|30/2020|Strana \d"
    r"|DynamicResources|Príloha č|k vyhláške č)"
    r"|portálu Slov-Lex|Slov-Lex dost",
)
# The publisher's imprint closes the regulation, on the last page and in the column a sign's name
# comes from: who publishes the Zbierka zákonov, their address, phone number and e-mail. It belongs
# to the document, not to the sign above it.
IMPRINT = re.compile(r"slov-lex|Vydavateľ|Zbierky zákonov|tel\.:|e-mail|@|spravodlivosti", re.IGNORECASE)
# The meaning of the sign sits in the table's third column, which starts left of the middle.
DESC_X = 262.0
# And its number and name in the second: what is left of this belongs to the table's own bookkeeping
# ("Bod" numbers) or, after the last table, to the regulation's citations and its imprint.
NAME_X = 130.0
# Lines above this y are the page header ("Strana 28", "Zbierka zákonov Slovenskej republiky").
HEADER_Y = 100.0

# File names of the pictograms the catalog calls "symboly" (symbols of vehicles, tourist targets,
# parking, motorway signs and traffic-office boards). They are numbering schemes of their own, laid
# out two to a row, and they are not signs of their own that a driver meets on the road.
SYMBOL_HEADING = re.compile(r"^([IVX]+\.|[AB]\.)\s.*SYMBOL|^[IVX]+\. (Symboly|Parkovacie|Diaľničné)")


@dataclass
class PdfLine:
    """One line of a page's text, with the x of its column and whether it is set in bold."""

    page: int
    y: float
    x: float
    text: str
    #: Whether any span of the line is bold, which is how the tables set their own headings.
    bold: bool = False


@dataclass
class SignRow:
    """
    One catalog entry: a sign's number, its name, and what it means.

    The annex groups its signs twice over. [kind] is the part of the annex the sign is listed in -
    "VÝSTRAŽNÉ ZNAČKY", "REGULAČNÉ ZNAČKY", "INFORMAČNÉ ZNAČKY", "DODATKOVÉ TABUĽKY",
    "VODOROVNÉ ZNAČKY" or "DOPRAVNÉ ZARIADENIA" - and [group] the heading inside it, such as
    "II. 3. Zákazy vjazdu" or "I. 4. Pohyb na ceste".
    """

    number: str
    name: str
    description: str
    kind: str
    group: str


def lines(path):
    """Every piece of text in the regulation's príloha č. 1, in reading order."""
    doc = pymupdf.open(path)
    out = []
    for index, page in enumerate(doc):
        for block in page.get_text("dict")["blocks"]:
            if block["type"] != 0:
                continue
            for line in block["lines"]:
                text = _clean("".join(span["text"] for span in line["spans"]))
                if not text or line["bbox"][1] < HEADER_Y:
                    continue
                # A heading of a table is set in bold - the groups of the road markings and of the
                # markings' symbols sit past the meaning column's left edge, which is what put
                # "II. Priečne čiary" at the end of a sign's meaning.
                bold = any("Bold" in span["font"] for span in line["spans"])
                out.append(PdfLine(page=index, y=line["bbox"][1], x=line["bbox"][0], text=text, bold=bold))
    return sorted(out, key=lambda line: (line.page, line.y, line.x))


def _clean(text):
    """One line of a cell, with the line break's spaces taken out of the middle of a sentence."""
    collapsed = re.sub(r"\s+", " ", text.replace("\xa0", " ").replace("\xad", "")).strip()
    # A line ends where the next one starts, which can leave a space before its punctuation.
    return re.sub(r"\s+([.,;:!?])", r"\1", collapsed)


def rows(lines):
    """The catalog entries among [lines], in document order."""
    out = []
    kind = ""
    group = ""
    current = None

    def close():
        nonlocal current
        if current is not None:
            out.append(current)
            current = None

    # A row's cells are drawn at slightly different heights (260.3 against 260.4), so a row is read
    # in `y` order rounded to the point, and left to right within that.
    for line in sorted(lines, key=lambda line: (line.page, round(line.y), line.x)):
        if NOISE.match(line.text) or IMPRINT.search(line.text):
            # A table header repeated on the next page, the portal's footer, or the publisher's
            # imprint. None carries text of a row, and a row's meaning may continue below it, so the
            # row stays open.
            continue
        if TITLE.match(line.text):
            close()
            kind, group = line.text, ""
            continue
        if HEADING.match(line.text) and line.bold:
            # A heading of the table itself. It is bold throughout, wherever the table puts it:
            # the road markings draw theirs past the meaning column's own left edge, which is what
            # put "II. Priečne čiary" at the end of a sign's meaning.
            close()
            group = line.text
            continue
        if BOD.match(line.text):
            close()
            current = {"number": None, "names": [], "description": [], "kind": kind, "group": group}
            continue
        if current is None:
            continue
        if line.x >= DESC_X:
            current["description"].append(line.text)
        elif line.x < NAME_X:
            # Left of the table's second column: a "Bod" number, which was taken above, or - after
            # the regulation's last table - the citations and the imprint of the document, which
            # belong to no sign.
            continue
        elif SIGN_NUMBER.match(line.text):
            current["number"] = line.text
        elif line.text != " ":
            current["names"].append(line.text)
    close()
    return [row for row in (_entry(item) for item in out) if row is not None]


def _entry(item):
    """One parsed row as a [SignRow], or `None` for a row that is not a sign (a symbol table)."""
    if not item["number"]:
        return None
    return SignRow(
        number=item["number"],
        name=_clean(" ".join(item["names"])),
        description=_clean(" ".join(item["description"])),
        kind=item["kind"],
        group=item["group"],
    )


def parse(path):
    return rows(lines(path))
