# Legal documents

The privacy policy and the terms of use are not in this repository: they are pages on the company's own site (the `tree-among-shrubs/web` repository, served at `autoskola.tree-among-shrubs.com`), and the app links to them from `LegalDocument` (`data/preferences/LegalConsent.kt`). Two things to know when touching anything about them, including the app's own strings that describe them:

- They are published in **English and Slovak**, and the **Slovak text is the authoritative one**; both documents say so. Where the two disagree, Slovak governs, and the app's four `i18n/Strings.kt` tables have to agree with what the documents say.
- **Every locale changes together.** Adding, rewording or removing anything in one of them means the same change in the other, in the same commit: half a translation is either `translation missing` on the page or a silent fallback to English, in a document the user is asked to agree to. The site repo's `AGENTS.md` carries the full rule, and `bin/rails test` there renders both locales of both documents and fails on `translation missing`.

## Running the web repository

The documents live in `tree-among-shrubs/web`, which is a Rails app on this same machine but inside WSL, so nothing in this repository can run its tests. Its Ruby comes from mise, which only `~/.bashrc` activates — a plain `bash -lc` finds **no ruby at all**. Put mise's shims on the path yourself, exactly as an interactive shell would:

```bash
wsl.exe -d Ubuntu -e bash -lc 'export PATH=$HOME/.local/share/mise/shims:$PATH; cd ~/projects/tree-among-shrubs/web && bin/rails test'
```

- **The `cd` is part of the command, not decoration.** The shims resolve per directory, so the same `ruby` shim answers 4.0.2 inside that repository (its `.ruby-version`) and 3.4.4 — mise's global default — anywhere else. Running `ruby` from home is how you see 3.4.4 and conclude, wrongly, that the pinned version is not installed.
- **Two other ways work**, and they are what to use when a command has to see `~/.bashrc` for another reason: `wsl.exe -d Ubuntu -e bash -ic '…'` (interactive, so the file is sourced), or `mise exec -- <command>` (mise is at `~/.local/bin/mise`).
- **From Windows the repository is a UNC path**: `\\wsl.localhost\Ubuntu\` followed by the path the repository has inside WSL — `~/projects/tree-among-shrubs/web`, so `\\wsl.localhost\Ubuntu\home\<you>\projects\tree-among-shrubs\web`. Reading and writing files through it is fine — that is how the document edits are made — but **git is not**: on that path git refuses with a "dubious ownership" error. Git, Rails, Bundler and RuboCop all run inside WSL.
