# Data sources

Where the app's question banks and images come from, what each source contains, and what we know about using them. To refresh the data, see [importing-questions.md](importing-questions.md).

| Country | Status | Languages | Source |
|---|---|---|---|
| Slovakia | In the app | Slovak, English, Hungarian | Ministry of Interior (minv.sk) |
| Czechia | Imported, not in the app yet | Czech | Ministry of Transport (etesty.md.gov.cz) |

## Slovakia: Ministry of Interior (minv.sk)

Last imported: **2026-09-25**.

The theory exam is run by the traffic police (Prezídium Policajného zboru, odbor dopravnej polície) under the Ministry of Interior. The exam format is set by vyhláška MV SR č. 9/2009 Z. z., as amended by 273/2022 Z. z. (in force since 1 April 2023):

- 40 questions in 30 minutes, taken from 10 topics ("okruhy").
- Each question is worth 1–4 points, 100 points in total. You pass with 90 or more.
- Each question has 3 answers, and exactly one is correct.
- The questions depend on the license group.
- Topic **4** is *Dopravné situácie na križovatkách* — the situations at intersections, every one of
  them a question about a drawing of a junction with colored vehicles. That topic and nothing else
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

**Summary: no license is published. Using the data is probably fine, but it is not certain.** This is not legal advice.

What we found (checked 2026-09-25):
- minv.sk has no terms-of-use, copyright or license page. The only legal documents on the site are the cookie and accessibility statements.
- The pages that publish the tests say nothing about how the content may be reused.
- Slovak copyright law (zákon 185/2015 Z. z., §5) excludes some works from protection. This includes the text of legal regulations, official decisions and technical standards (§5(b)), and ideas and information as such (§5(a)).
  - Road-sign designs are defined in a regulation, so the sign images are probably not protected.
  - The question wording and the traffic-situation drawings (`obr3/ds`, `obr3/tvv`) are not clearly covered by §5. They may be protected works of the ministry, or the collection may be protected as a database.
- Several third-party apps and websites already republish this bank. Some say they got it through a request under the freedom-of-information law, zákon 211/2000 Z. z. Some show a disclaimer on first launch, like ours does.

Recommended before a public release:
1. Ask the ministry for written permission to reuse the questions and images in the app, by email to the traffic police department of Prezídium PZ or as a formal request under zákon 211/2000.
2. Keep the disclaimer screen (`DisclaimerScreen.kt`). It says the app is independent and names minv.sk as the source.
3. Record the answer here.

## Slovakia: the road-sign catalog (príloha č. 1 of vyhláška 30/2020 Z. z.)

The learning tab's road-sign list is not built from the question bank: it is the catalog in
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
  draw their headings past the meaning column's own left edge, so a heading is recognized by its
  being bold, not by which column it lands in. A kind whose signs carried no heading would leave the
  group empty and be named by the kind alone; no kind does today.
- Pictures are **not** taken from the regulation. The app matches the catalog to the images it
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
    2020 catalog numbers differently.
  - The composite pictures (`401-409.jpg`, `411-419.jpg`, …) that show a whole group of signs at
    once: a group is not a sign, so no row claims them.
  - Signs the catalog lists but minv.sk ships no picture of.

### Terms of use

The catalog text is the **text of a legal regulation**, which zákon 185/2015 Z. z. § 5(b) leaves
out of copyright protection (the same argument as for road-sign designs, above). Reusing it is
therefore safe; what the app adds is only the match between the regulation's numbers and pictures
it already ships. The pictures themselves come from minv.sk and fall under the disclaimer below.

## Czechia: Ministry of Transport (etesty.md.gov.cz)

Last imported: **2026-10-09**, by `tools/import/import_md_cz.py`. The importer is on the
`czech-importer` branch, and its output is not committed yet: the app offers Czechia as soon as
`banks/index.json` lists a Czech bank, and it cannot grade the Czech exam yet
([plans/czech-tests.md](../plans/czech-tests.md)).

The theory exam is set by vyhláška 167/2002 Sb. (as amended by 298/2006 Sb.), and the ministry
publishes every exam question in the Věstník dopravy. Its eTesty site states the format:

- 25 questions in 30 minutes, 50 points in total. You pass with 43 or more.
- Each question has one correct answer: three answers, or two for a yes-or-no question.
- The layout is the same for every group. Seven topics, each worth a fixed number of points per
  question:

| # | Topic (`basketScopeId`) | Questions per exam | Points each |
|---|---|---|---|
| 1 | Znalost pravidel provozu na pozemních komunikacích (9) | 10 | 2 |
| 2 | Znalost zásad bezpečné jízdy a ovládání vozidla (10) | 4 | 2 |
| 3 | Znalost dopravních značek, světelných a akustických signálů (11) | 3 | 1 |
| 4 | Schopnost řešení dopravních situací (12) | 3 | 4 |
| 5 | Znalost předpisů o podmínkách provozu vozidel (13) | 2 | 1 |
| 6 | Znalost předpisů souvisejících s provozem (14) | 2 | 2 |
| 7 | Znalost zdravotnické přípravy (15) | 1 | 1 |

- Groups with a practice exam on the site: A, B, B+E, C, C+E, D and D+E. There is no T and no AM.
- Language: Czech only. The API's answers are `multilingualAnswerTexts`, but they hold only `cs`.

### Source 1: the complete set and the question API

- Page: <https://etesty.md.gov.cz/ro/DLArea/Index?id=99> ("Soubor všech otázek"). Its script holds
  `const questionList=[…]`, the ids of all **1,136** questions.
- Each question: `https://etesty.md.gov.cz/api/v1/PublicWeb/Question/<id>`, JSON with the question
  text, its topic (`basketScopeId`), its points (`pointsCount`), the answers in `sortOrder`, the
  `correctAnswerId` and the media (`mediaContent`).
- Media: `https://etesty.md.gov.cz/binary_content_storage/<file>`. A question can have:
  - a picture (`Q_W_<id>.jpg|png|gif`, 302 questions). The site saves its photos for print, at
    about 470 KB a JPEG, and many as PNGs: bundled as they are, the pictures would take 58 MB. The
    importer bundles each photo as a JPEG at quality 80 and at most 1,280 pixels wide, which looks
    the same, and keeps line art, transparency and GIFs as they are: about 25 MB in all.
  - a video (`.mp4`, 202 questions), in two kinds:
    - **looping clips** (125 questions): the site plays them on a loop beside the question. Each one
      names the still that the paper test prints in its place (`printMediaName`, `Q_P_<id>.jpg`),
      and that still is what the app shows.
    - **dynamic situations** (77 questions, 75 of them in topic 4, new since 2025-09-21): the site plays
      them once, full screen (`isFullScreen`), and the question is about what happens in them. The
      still shows the road before anything happens, so these are **left out** until the app can
      play video.
  - pictures as answers (25 questions, such as "which of these signs…"). The bank format has text
    answers only, so these are **left out** too.
- The online bulletin (<https://etesty.md.gov.cz/ro/Bulletin/List?id=99>) and the Věstník PDF
  (`binary_content_storage/Vestnik\etesty_za_<date>.pdf`, 265 MB, 470 pages) list the same
  questions with their answers and points, by topic.

### Source 2: the practice exams

- `https://etesty.md.gov.cz/co/DLTest/SampleTest/<group>` starts a practice exam. The page passes
  the whole exam as JSON to `new SampleTest(…)`: its topics in order, their names, and the
  questions drawn for each with their points. That gives the layout above.

### License groups

**Nothing the ministry publishes says which group a question is for.** Neither the API, the
bulletin, the Věstník PDF nor the practice areas have it. The site does list "Zásady bezpečné jízdy
[A]", "[B]" and "[C,D]" as separate areas, but all three hold the same 157 questions, and so did the
old site (etesty2.mdcr.cz, archived in December 2024).

Yet each group's practice exam does draw from its own pool: in topic 2, group A's exams ask about
motorcycles and C's about lorries and buses. So the importer learns the pools from the exams
themselves:

1. It draws 600 practice exams per group (`--exams`), and caches what each one drew.
2. For each group and topic it asks whether the draws could have come from the whole topic. Drawing
   d times from n questions leaves about n(1 − 1/n)^d of them never drawn; if far more were never
   drawn (a Poisson tail below 10⁻⁶), the group draws from part of the topic.
3. A group that draws from the whole topic gets every question of it; otherwise it gets the
   questions its exams drew. A question no group drew is left out.

With 600 exams a question of a group's pool is all but never missed: the weakest case, group A's
141 situations drawn 1,800 times, misses one with a chance of about 1 in 2,500. 300 exams were not
enough: the 3 situations the trailer groups never get went unnoticed. It stays a statistical answer,
though, so ask the ministry for the real assignment (see below).

What the 4,200 exams showed (2026-10-09), as the number of questions of each topic a group's exams
can ask. A number below the topic's size means the topic is split for that group:

| # | Topic | Questions | A | B | B+E | C | C+E | D | D+E |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Rules | 444 | 422 | 427 | 417 | 425 | 417 | 417 | 417 |
| 2 | Safe driving | 157 | 93 | 77 | 88 | 107 | 74 | 93 | 74 |
| 3 | Signs | 222 | 222 | 222 | 222 | 222 | 222 | 222 | 222 |
| 4 | Situations | 154 | 141 | 154 | 151 | 154 | 151 | 141 | 151 |
| 5 | Vehicle conditions | 45 | 32 | 42 | 30 | 41 | 30 | 42 | 30 |
| 6 | Related regulations | 79 | 50 | 54 | 50 | 59 | 51 | 57 | 51 |
| 7 | First aid | 35 | 35 | 35 | 35 | 35 | 35 | 35 | 35 |

The splits make sense where they can be read: the rules questions about buses, lorries and tractors
are not asked of the groups that drive none of them, C is not asked about motorcycle passengers, and
B+E, C+E and D+E are left exactly the same questions in topics 1 and 5. Within a group's part of a
topic, each question was drawn about equally often, as uniform drawing gives, and none rarely, so
these are pools rather than a weighting. One question (2885, a tractor trailer's load bed) was drawn
by no group, so it is left out.

### Coverage

What the bank holds, per topic and group, once the questions it cannot show are left out (25 with
picture answers, 77 dynamic videos, and question 2885):

| # | Questions | A | B | B+E | C | C+E | D | D+E |
|---|---|---|---|---|---|---|---|---|
| 1 | 444 | 422 | 427 | 417 | 425 | 417 | 417 | 417 |
| 2 | 148 | 91 | 71 | 87 | 103 | 74 | 92 | 74 |
| 3 | 203 | 203 | 203 | 203 | 203 | 203 | 203 | 203 |
| 4 | 79 | 66 | 79 | 76 | 79 | 76 | 66 | 76 |
| 5 | 45 | 32 | 42 | 30 | 41 | 30 | 42 | 30 |
| 6 | 79 | 50 | 54 | 50 | 59 | 51 | 57 | 51 |
| 7 | 35 | 35 | 35 | 35 | 35 | 35 | 35 | 35 |
| **`cz-cs`** | **1,033** | **899** | **911** | **898** | **945** | **886** | **912** | **886** |

427 of the questions have a picture, and 48 are yes-or-no questions with two answers.

**Known gaps:**
- The 77 dynamic videos and the 25 picture-answer questions, until the app can show them. Topic 4
  loses half its questions to the first: every group still has more than enough for its 3 a test,
  but a real exam asks dynamic situations too.
- The groups are inferred, not published.

### Terms of use

**Summary: no license is published, and the pictures carry the ministry's copyright notice.** This
is not legal advice.

What we found (checked 2026-10-09):
- etesty.md.gov.cz has no terms-of-use, license or copyright page. Its footer says
  "© 2026 Ministerstvo dopravy".
- The photos and stills carry "© Česká republika - Ministerstvo dopravy" in the corner, and some name
  their maker ("Design GARRY Production").
- Some of the newer photos (for example `Q_W_5374` and `Q_W_5561`) carry "© Asociace autoškol ČR" instead, the
  Czech driving-schools association, and "Financováno z fondu zábrany škod ČKP". Those are a third
  party's pictures, which the ministry's permission may not cover.
- The site says the exam may only use approved questions published in the Věstník dopravy, as the
  law requires, so the questions themselves are published officially.
- Czech copyright law (zákon 121/2000 Sb., § 3(a)) excludes official works ("úřední dílo") from
  protection: legal regulations, decisions and other official documents, and other works "u něhož
  je veřejný zájem na vyloučení z právní ochrany". Whether a question bank published in the Věstník
  falls under that is not clear. The pictures, with their explicit notice, probably do not.

Recommended before a public release, as for Slovakia: ask the ministry (posta@md.gov.cz, odbor
řidičů a evidence vozidel) for written permission to reuse the questions and pictures, and in the
same request for the group of every question. Record the answer here.
