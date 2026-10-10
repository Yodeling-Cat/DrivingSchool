# Contributing to Naj Autoškola

Bug reports, fixes and improvements are welcome. Please read this page before you open an issue or a
pull request.

## Before you start

- **Small fixes** (typos, an obvious bug): open a pull request directly.
- **Anything larger** (a new feature, a new screen, a refactor, a new dependency): open an issue
  first and describe what you want to change. Wait for a reply before you write the code. A pull
  request that has not been discussed may be declined, however good it is.

## Reporting a bug

Open an issue and include:

1. What you did, step by step.
2. What you expected, and what happened instead.
3. The platform (Android, iOS or web), the device or browser, and the app version.
4. The country of the app and the question and UI languages.
5. A screenshot or a screen recording, if it helps.

## Mistakes in the questions

The questions, answers, pictures and videos are the ministries' official exam content. They are
imported automatically and are **never edited by hand**, so a pull request that changes them will
not be accepted. See [docs/question-banks.md](docs/question-banks.md).

If a question looks wrong, open an issue with its number and say what is wrong. If the mistake is in
the official text itself, it has to be fixed by the ministry. We can report it to them.

## Security issues

Do **not** open a public issue for a security problem. Report it privately with
[GitHub's private vulnerability reporting](https://github.com/Yodeling-Cat/DrivingSchool/security/advisories/new) (the **Security** tab of
this repository, then **Report a vulnerability**).

## Setting up

You need JDK 21. Android Studio is the easiest way to work on the project. iOS builds need a Mac
with Xcode.

The build, run and test commands are in [AGENTS.md](AGENTS.md), under "Build, run and test". Each
area of the code has a guide in [docs/](docs/). Read the guide for the area you change before you
start; [AGENTS.md](AGENTS.md) lists which guide applies when.

## Rules for code changes

These rules are explained in [AGENTS.md](AGENTS.md) and the guides. A pull request must follow them:

1. **Write the test first.** A bug fix starts with a test that reproduces the bug. Keep logic in
   plain Kotlin that a unit test can call, not in composables.
2. **Never hardcode user-visible text.** Add every string to `i18n/Strings.kt` in all four UI
   languages: English, Slovak, Hungarian and Czech. If you cannot write one of them, say so in the
   pull request.
3. **Give every new or changed composable a `@Preview`** in the same change.
4. **Write American English** in code, comments, docs and commit messages.
5. **Change dependency versions only in `gradle/libs.versions.toml`.**

Before you open the pull request, run:

```bash
./gradlew ktlintCheck :core:jvmTest :app:shared:testAndroidHostTest
```

On Windows, use `gradlew.bat`. iOS code cannot be compiled on Windows or Linux. If you could not
build the iOS side, say so in the pull request.

## Commits and pull requests

- Keep each pull request to one change. Split unrelated changes into separate pull requests.
- Write commit subjects in the imperative mood and in sentence case, with no prefix and no period,
  for example `Show the remaining time in the exam's top bar`.
- Fill in the pull request template, including the contributor agreement.
- The Android and iOS checks must pass before a pull request is merged.

## Contributor License Agreement

Before we can merge your pull request, you must accept the
[Contributor License Agreement](CLA.md). You accept it by ticking its box in the pull request
template.

In short: you keep the copyright in your work, but you give the project owner a permanent, broad
license to use it in any way, including under a different license or commercially. This keeps the
owner free to change how the project is licensed. Read [CLA.md](CLA.md) for the full terms.

## License

The project is source-available under the
[PolyForm Noncommercial License 1.0.0](LICENSE.md), not an open-source license. You may read, study
and change the code for noncommercial purposes. Commercial use is not permitted.
