# NajAutoskola

A driving-theory exam trainer for Android, iOS and the web, built with **Kotlin Multiplatform** and
**Compose Multiplatform**. One shared codebase drives all three platforms. The app runs the Slovak
theory test as the official exam runs it, with the official question bank, and adds learning
material for road signs and intersections.

[![Android](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/android.yml/badge.svg)](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/android.yml)
[![iOS](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/ios.yml/badge.svg)](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/ios.yml)

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-4285F4?logo=jetpackcompose&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Web-informational)

## Features

- **Official exam format.** 40 questions in 30 minutes, 1–4 points per question, 90 of 100 points
  to pass, as set by Slovak regulation 9/2009 Z. z.
- **Every license group.** A, B, B+E, C, C+E, D, D+E and T, each with its own question pool.
- **Three question languages.** Slovak, English and Hungarian banks with about 950 questions each.
- **Learning material.** A searchable catalog of 120 road signs and a walkthrough of the intersection
  situations from the exam.
- **Progress that survives anything.** Each answer is saved as it is given. A test that is
  interrupted, even by the system killing the app, resumes on the same question.
- **Statistics and mistakes.** Results per test, per-question history, and a practice mode for the questions
  the user answered wrongly.
- **Four UI languages.** English, Slovak, Hungarian and Czech.
- **Adaptive layout.** Phones and tablets in both orientations, and desktop browsers. Mouse and
  keyboard work as well as touch.
- **Privacy-first.** Analytics, crash reporting and personalized ads stay off until the user opts
  in. An age question restricts personalized ads to adults.

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.4 (Multiplatform) |
| UI | Compose Multiplatform 1.12, Material 3 |
| Targets | Android, iOS (arm64 and simulator), Web (Wasm and JS), JVM server |
| Architecture | MVVM with unidirectional data flow, `StateFlow`, Coroutines |
| Navigation | Navigation 3, with a serializable back stack on every platform |
| Dependency injection | Koin |
| Persistence | Multiplatform Settings (planned migration to SQLDelight) |
| Images | Coil 3 |
| Serialization | kotlinx.serialization |
| Analytics and crash reports | PostHog (opt-in) |
| Ads and billing | Google AdMob with the UMP consent form, Google Play Billing |
| Server | Ktor 3 on Netty |
| Data pipeline | Python 3 (PDF and JavaScript parsing) |
| Quality | ktlint, Android Lint, SwiftLint, GitHub Actions |

## Architecture

### Modules

| Module | Contents |
|---|---|
| [`core`](core/src/commonMain/kotlin) | Pure Kotlin domain logic for every target, including the server: the question bank model, practice-test generation, scoring, attempts and statistics. No UI dependencies. |
| [`app/shared`](app/shared/src/commonMain/kotlin) | Compose Multiplatform UI, ViewModels, repositories and data sources shared by Android, iOS and the web. Platform APIs use `expect`/`actual`. |
| [`app/androidApp`](app/androidApp) | Android application entry point. |
| [`app/iosApp`](app/iosApp) | Xcode project that hosts the shared UI. |
| [`app/webApp`](app/webApp) | Web entry point, built for both Wasm and JS. |
| [`server`](server/src/main/kotlin) | Ktor server that shares `core` with the apps. |
| [`tools/import`](tools/import) | Python importer that builds the question banks and the road-sign catalog from official sources. |

### Layers

The shared app has three layers. Each layer talks only to the layer below it.

- **Screens** are stateless composables. They render an immutable `UiState` and report user actions
  as `UiEvent`s.
- **ViewModels** expose one `StateFlow<UiState>` and one `onEvent` function. They hold the screen's
  logic and resolve no strings, so they can be tested without a UI.
- **Repositories** hold the business rules: an attempt is locked once it is finished, and statistics
  are derived from attempts, never stored separately.
- **Data sources** only store and load. Each one is an interface with one implementation per backend,
  so moving attempts from key-value storage to a database means adding one class.

Reads return `Flow`s that emit again when data changes. Writes are `suspend` functions that run to
completion even when the screen that started them is gone.

### Design decisions

- **Attempts are the source of truth.** Each test run is recorded as a `TestAttempt` with every
  answer and its timestamp. Results, the resumable test, per-question stats and mistakes are all
  computed from attempts.
- **Question banks are versioned.** A new official question set becomes a new bank version, and old
  versions stay bundled. A past attempt always resolves to the exact questions that were asked.
- **Screen state is persisted on every platform.** A custom `SaveableStateRegistry` writes
  `rememberSaveable` state to storage. Scroll positions, pager pages and open tabs come back after
  the app is killed on Android, iOS and the web.
- **The data pipeline is reproducible.** The official sources are a JavaScript sample exam (with all
  answers but no license groups) and PDFs (with license groups but only the correct answer). The
  importer matches the two by topic, question text and answer text, then generates the banks and
  images the app bundles. Generated data is never edited by hand.

## Testing

Tests come first in this project: a change starts with a failing test, and a bug fix starts with a
test that reproduces the bug. Tests target what would silently corrupt a user's progress or show a
wrong answer: scoring, persistence, data import and state decisions. Clocks, randomness, storage and
data sources are injected, so every test is fast and deterministic, with no network and no real time.

```bash
./gradlew :core:jvmTest                     # Domain logic
./gradlew :app:shared:testAndroidHostTest   # ViewModels, repositories, persistence
./gradlew :app:shared:iosSimulatorArm64Test # Shared code on iOS
./gradlew :app:shared:wasmJsTest            # Shared code on Wasm
./gradlew :app:shared:jsTest                # Shared code on JS
./gradlew :server:test                      # Server
./tools/import/test.sh                      # Importer, with offline fixtures
./gradlew ktlintCheck                       # Code style
```

Every screen and component also has Compose previews for each of its states and for phone, tablet,
desktop and landscape sizes.

## Getting started

### Requirements

- JDK 17 or newer (CI uses JDK 25)
- Android SDK with platform 37
- Xcode 16.2 or newer, on macOS, for iOS
- Python 3.10 or newer, only to re-run the question importer

### Run the apps

| Target | Command |
|---|---|
| Android | `./gradlew :app:androidApp:assembleDebug` |
| Web (Wasm) | `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` |
| Web (JS, for older browsers) | `./gradlew :app:webApp:jsBrowserDevelopmentRun` |
| Server | `./gradlew :server:run` |
| iOS | Open [`app/iosApp`](app/iosApp) in Xcode and run. |

On Windows, use `gradlew.bat`. A debug build of the Android app has the application ID suffix
`.dev`, so it installs next to a release build.

### Release builds

Debug builds need no configuration. A release build is minified with R8, and its mapping file is
uploaded to PostHog so that crash reports stay readable. The upload needs `posthog-cli` on the
`PATH` and three values in the environment or in a `.env` file at the repository root (copy
[`.env.example`](.env.example)):

- `POSTHOG_CLI_API_KEY`: a personal API key with the error tracking write and organization read
  scopes.
- `POSTHOG_CLI_PROJECT_ID`: the numeric project ID.
- `POSTHOG_CLI_HOST`: `https://eu.posthog.com`.

Without them, the release build still succeeds and reports that it skipped the upload. Details are in
[docs/error-reporting.md](docs/error-reporting.md).

## Continuous integration

GitHub Actions runs on every push to `main` and on every pull request:

- **Android:** ktlint and Android Lint, then a full debug build.
- **iOS:** SwiftLint in strict mode, then an Xcode build with warnings treated as errors.

## Documentation

- [Data sources](docs/data-sources.md): where the questions, images and road signs come from, and
  their coverage.
- [Importing questions](docs/importing-questions.md): how to run the importer and publish a new bank
  version.
- [Error reporting](docs/error-reporting.md): crash-report symbolication on each platform.
- [Store screenshots](docs/store-screenshots.md): taking the store listing's screenshots on a phone
  and two tablet emulators.
- [AGENTS.md](AGENTS.md): the full engineering conventions for this repository.

## Roadmap

- Czech question bank (the Czech UI translation is already done).
- Legislation section in the learning material.
- Migration of attempts from key-value storage to SQLDelight.
- Premium tier with Google Play Billing.

## Data and license

The question texts, answers and images are published by the Ministry of Interior of the Slovak
Republic. The road-sign catalog comes from regulation 30/2020 Z. z. This project is not affiliated
with either. Terms of use for the data are described in [docs/data-sources.md](docs/data-sources.md).

This repository has no open-source license. All rights reserved.
