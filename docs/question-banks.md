# Question banks

## Generated data

Question banks, pictures and videos are generated into each country's content module, `app/content/<country>/src/commonMain/composeResources/files/`, by `tools/import/` (run `./tools/import/import.sh`; see [countries.md](countries.md)). Never hand-edit them. Sources, coverage and terms of use are in [data-sources.md](data-sources.md); the importer is documented in [importing-questions.md](importing-questions.md).

**Never delete `tools/import/cache/`.** It is gitignored, but it is not disposable: besides the downloads it holds the Czech importer's 4,200 drawn practice exams (which decide each question's license groups) and its encoded videos, about two hours of work against the ministry's server and ffmpeg. With it, an import is a re-run of about a minute; `--refresh` is what fetches again. The Czech import needs ffmpeg on the `PATH`. The bank model and practice-test generation live in `:core` (`com.spiraclestudios.autoskola.bank`).

## Versions

Governments periodically publish updated question sets. Each bank therefore has a `version` (all banks are currently **v1**), and its file is `banks/<bank>-v<version>.json`.

- A new official question set gets a **new version**: run the importer with `--version N+1`. Never overwrite a published version in place, because recorded attempts reference question ids *within a bank version*.
- Keep older version files bundled, along with their images; the importer keeps them. Old attempts then still resolve to the exact questions that were asked.
- `banks/index.json` lists the latest version per bank. The app always starts new tests on the latest version. Statistics, results and mistakes are scoped to one bank version, so a new version starts fresh.
- Open decision for when v2 arrives: whether to map unchanged questions between versions so users keep their progress. We'd match them by text, much as the importer matches the PDF. We also need to decide when to stop bundling very old versions.
