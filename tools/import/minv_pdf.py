"""Parses the minv.sk question-bank PDF: question, correct answer and license groups."""
import re
from dataclasses import dataclass, field

import pymupdf

NUMBER = re.compile(r"^(\d+)\.$")


@dataclass
class PdfQuestion:
    section: int  # 1-based "okruh" (topic)
    number: int  # 1-based position within the section
    question: str = ""
    answer: str = ""
    groups: list = field(default_factory=list)
    images: list = field(default_factory=list)  # (page index, xref)


def _clean(text):
    return re.sub(r"\s+", " ", (text or "").replace("\n", " ")).strip()


def _groups(text):
    return [g for g in re.split(r"[,\s]+", text or "") if g]


def _page_images(page):
    """(bbox, xref) of every question-sized image drawn on the page."""
    return [
        (info["bbox"], info["xref"])
        for info in page.get_image_info(xrefs=True)
        if info["xref"] and info["bbox"][2] - info["bbox"][0] < 400
    ]


def _cell_images(cell, page_index, images):
    """The tracked images whose center falls inside the table [cell]."""
    inside = []
    for bbox, xref in images:
        cx = (bbox[0] + bbox[2]) / 2
        cy = (bbox[1] + bbox[3]) / 2
        if cell[0] <= cx <= cell[2] and cell[1] <= cy <= cell[3]:
            inside.append((page_index, xref))
    return inside


def parse(path):
    """Returns (document, questions in PDF order)."""
    doc = pymupdf.open(path)
    rows: list[PdfQuestion] = []
    section = 0
    # Page 0 is the title page, page 1 the table of contents.
    for page_index in range(2, doc.page_count):
        page = doc[page_index]
        images = _page_images(page)
        for table in page.find_tables().tables:
            for row, cells in zip(table.rows, table.extract()):
                number, question, answer, groups = (cells + [None] * 4)[:4]
                number = _clean(number)
                if number in ("p. č.", "p.č."):
                    continue  # repeated table header
                if m := NUMBER.match(number):
                    n = int(m.group(1))
                    if n == 1:
                        section += 1
                    current = PdfQuestion(section=section, number=n)
                    rows.append(current)
                elif not rows or number:
                    continue
                else:
                    current = rows[-1]  # row continued from the previous page
                current.question = _clean(f"{current.question} {question}")
                current.answer = _clean(f"{current.answer} {answer}")
                current.groups += _groups(groups)
                # Images drawn inside the question cell belong to this row.
                question_cell = row.cells[1]
                if question_cell is not None:
                    current.images += _cell_images(question_cell, page_index, images)
    return doc, rows
