# Data sources

Where the app's question banks and images come from, what each source contains, and what we know about using them. To refresh the data, see [importing-questions.md](importing-questions.md).

| Country | Status | Languages | Source |
|---|---|---|---|
| Slovakia | In the app | Slovak, English, Hungarian | Ministry of Interior (minv.sk) |
| Czechia | Planned | Czech | Ministry of Transport (etesty.md.gov.cz) |

## Slovakia: Ministry of Interior (minv.sk)

Last imported: **2026-09-25**.

The theory exam is run by the traffic police (Prezídium Policajného zboru, odbor dopravnej polície) under the Ministry of Interior. The exam format is set by vyhláška MV SR č. 9/2009 Z. z., as amended by 273/2022 Z. z. (in force since 1 April 2023):

- 40 questions in 30 minutes, taken from 10 topics ("okruhy").
- Each question is worth 1–4 points, 100 points in total. You pass with 90 or more.
- Each question has 3 answers, and exactly one is correct.
- The questions depend on the license group.
- Topic **4** is *Dopravné situácie na križovatkách* — the situations at intersections, every one of
  them a question about a drawing of a junction with coloured vehicles. That topic and nothing else
  is the app's intersections material (`SITUATIONS_SECTION` in `:core`'s `situations/Situations.kt`):
  88 questions over 40 drawings in the Slovak bank, 89 over 41 in the Hungarian one. It is a topic
  rather than the folder of drawings, because two of its questions carry a drawing the bank keeps
  outside `obr3/ds/`. `tools/import/test_bank_output.py` holds both ends of that: every question of
  topic 4 has a drawing, and every drawing of `obr3/ds/` is asked in topic 4.

### Source 1: the online sample exam (`data5.js`)

- Page: <https://www.minv.sk/?testy> links to the sample exam at <https://www.minv.sk/egovinet02/PCPZobrazFile?fileName=test2.html>.
- Data: <https://www.minv.sk/egovinet02/PCPZobrazFile?fileName=pcpfiles/data5.js>, about 4.7 MB. It is a JavaScript file with one assignment, `var data=[...]`.
- Images: `https://www.minv.sk/egovinet02/PCPZobrazFile?fileName=pcpfiles/<obrazok>`, for example `pcpfiles/obr3/dz/343.jpg`. The same paths are used in all three languages.
- Player script: `lib/test_mini.js`, which shows how the page reads `data`.

**Structure.** `data` holds three arrays, in the order Slovak, English, Hungarian (see `runTest()` in `test_mini.js`). Each array holds 100 complete sample exams:

```text
{ cislo, pocet: 40, cas: 1800, minbody: 90, maxbody: 100,
  okruhy:   { "1": [{ txt: "<topic title>", zacina: <first position> }], ... },
  otazky:   { "1": [{ id, text, body: <points>, obrazok: "<image path>", platna: <1-based correct answer> }], ... },
  odpovede: { "1": [{ odpoved }, { odpoved }, { odpoved }], ... } }
```

**Good:** it has all three answers and the correct one, in all three languages.

**Missing:**
- License groups.
- Most of the question bank. The Slovak exams contain only 956 unique questions, and every one of them is valid for group B.

### Source 2: the question-bank PDFs

- Page: <https://www.minv.sk/?elektronicke-testy>
- Slovak: <https://www.minv.sk/?elektronicke-testy&subor=475052>, about 5.9 MB, 1,416 questions.
- Hungarian: <https://www.minv.sk/?elektronicke-testy&subor=475049>, about 5.9 MB, 1,415 questions.
- There is no English PDF.

Each PDF is a 4-column table: number, question, **correct answer only**, and the license groups ("pre skupinu"). An empty group cell means the question is for every group. The PDFs are exported from PowerPoint and dated 2023-03-15.

**Good:** it is the complete bank, and it lists the license groups.

**Missing:** the wrong answers, so questions that appear only in the PDF can't be used as multiple-choice questions.

### How the two are combined

The importer matches each `data5.js` question to a PDF row by topic, question text and correct-answer text. The match gives the question its license groups. Only questions with all three answers go into the app. For each language:

| Bank | Questions | A | B | B+E | C | C+E | D | D+E | T |
|---|---|---|---|---|---|---|---|---|---|
| `sk-sk` | 956 | 805 | 956 | 932 | 886 | 884 | 885 | 883 | 827 |
| `sk-en` | 941 | 798 | 940 | 916 | 875 | 873 | 874 | 872 | 822 |
| `sk-hu` | 960 | 808 | 959 | 935 | 892 | 890 | 891 | 889 | 833 |

**Known gaps:**
- About 460 of the 1,416 questions are in the PDF only, without wrong answers. They are mostly questions for groups other than B, so the non-B groups miss their group-specific questions. Watch for a new data file on minv.sk, or other official sources, that contains them.
- English has no PDF. English questions get their groups through the shared question `id`, from the Slovak or Hungarian match. 11 English questions have no match in either language and are dropped.
- The app builds its numbered practice tests itself (`QuestionBank.practiceTests` in `:core`), following the official 40-question layout. It does not use the 100 sample exams from `data5.js`.

### Terms of use

**Summary: no licence is published. Using the data is probably fine, but it is not certain.** This is not legal advice.

What we found (checked 2026-09-25):
- minv.sk has no terms-of-use, copyright or licence page. The only legal documents on the site are the cookie and accessibility statements.
- The pages that publish the tests say nothing about how the content may be reused.
- Slovak copyright law (zákon 185/2015 Z. z., §5) excludes some works from protection. This includes the text of legal regulations, official decisions and technical standards (§5(b)), and ideas and information as such (§5(a)).
  - Road-sign designs are defined in a regulation, so the sign images are probably not protected.
  - The question wording and the traffic-situation drawings (`obr3/ds`, `obr3/tvv`) are not clearly covered by §5. They may be protected works of the ministry, or the collection may be protected as a database.
- Several third-party apps and websites already republish this bank. Some say they got it through a request under the freedom-of-information law, zákon 211/2000 Z. z. Some show a disclaimer on first launch, like ours does.

Recommended before a public release:
1. Ask the ministry for written permission to reuse the questions and images in the app, by email to the traffic police department of Prezídium PZ or as a formal request under zákon 211/2000.
2. Keep the disclaimer screen (`DisclaimerScreen.kt`). It says the app is independent and names minv.sk as the source.
3. Record the answer here.

## Slovakia: the road-sign catalogue (príloha č. 1 of vyhláška 30/2020 Z. z.)

The learning tab's road-sign list is not built from the question bank: it is the catalogue in
príloha č. 1 of **vyhláška MV SR č. 30/2020 Z. z. o dopravnom značení** (the traffic-sign
regulation, in force since 1 April 2020), read by `tools/import/import_znacky.py`.

- Source: <https://www.slov-lex.sk/static/pdf/2020/30/ZZ_2020_30_20200401.pdf> (Zbierka zákonov,
  5.9 MB). The annex holds one table row per sign: its row number, its number and picture, its
  name, and what it means. `znacky_pdf.py` reads those rows back by the x of each table column.
- Grouping: each sign records the part of the annex it is listed in (its **kind**) and the heading
  inside it (its **group**), which is what the list's sticky headings are drawn from. There are six
  kinds — VÝSTRAŽNÉ ZNAČKY (25), REGULAČNÉ ZNAČKY (31), INFORMAČNÉ ZNAČKY (44), DODATKOVÉ TABUĽKY
  (5), VODOROVNÉ ZNAČKY (10) and DOPRAVNÉ ZARIADENIA (5) — and the 120 signs fall into 27 headings,
  such as "I. 4. Pohyb na ceste", "II. 3. Zákazy vjazdu" or "III. Doplnkové zariadenia k svetelným
  signálom". The kind and the group are read from the text the tables set in bold: the road markings
  draw their headings past the meaning column's own left edge, so a heading is recognised by its
  being bold, not by which column it lands in. A kind whose signs carried no heading would leave the
  group empty and be named by the kind alone; no kind does today.
- Pictures are **not** taken from the regulation. The app matches the catalogue to the images it
  already bundles for the question bank (minv.sk's `obr3/dz` set, see above), whose file names are
  the signs' numbers: `140.jpg` is sign 140.
- Language: Slovak only. The regulation is published in Slovak and there is no official English or
  Hungarian wording, so the list is in Slovak whatever language the app's UI is in — as question
  content follows its bank. The UI around it (titles, search, counts) is translated as usual.
- Coverage (2026-09-30): **120 signs**, every one with a bundled picture. What the list does not
  have:
  - The pictograms the regulation calls *symboly* (`1-1.png` … `5-2.png` in the bank's images):
    symbols for additional panels rather than signs a driver meets on the road. They are laid out
    two to a row in the regulation and are skipped by the importer.
  - Signs 323 and 324, whose numbers are from the previous regulation (9/2009 Z. z.) and which the
    2020 catalogue numbers differently.
  - The composite pictures (`401-409.jpg`, `411-419.jpg`, …) that show a whole group of signs at
    once: a group is not a sign, so no row claims them.
  - Signs the catalogue lists but minv.sk ships no picture of.

### Terms of use

The catalogue text is the **text of a legal regulation**, which zákon 185/2015 Z. z. § 5(b) leaves
out of copyright protection (the same argument as for road-sign designs, above). Reusing it is
therefore safe; what the app adds is only the match between the regulation's numbers and pictures
it already ships. The pictures themselves come from minv.sk and fall under the disclaimer below.

## Czechia: Ministry of Transport (etesty.md.gov.cz)

Status: **not imported yet.**

- Official online tests: <https://etesty.md.gov.cz/ro/Home>
- The Czech ministry publishes tests per license group. Check the structure, languages, images and terms of use when we add this source, and document them here in the same way as Slovakia.
