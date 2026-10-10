# Naj Autoškola

A driving-theory exam trainer for Android, iOS and the web, built with **Kotlin Multiplatform** and
**Compose Multiplatform**.

[![Android](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/android.yml/badge.svg)](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/android.yml)
[![iOS](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/ios.yml/badge.svg)](https://github.com/Yodeling-Cat/DrivingSchool/actions/workflows/ios.yml)

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-4285F4?logo=jetpackcompose&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Web-informational)

## Features

- **Adaptive layout.** Phones and tablets in both orientations, and desktop browsers. Mouse and
  keyboard work as well as touch.
- **Three question languages.** Slovak, English and Hungarian banks.
- **Four UI languages.** English, Slovak, Hungarian and Czech.
- **Official exam format.** For every license group.
- **Learning material.** Catalog of road signs and intersection situations from the exam.

## Architecture

The shared app has three layers. Each layer talks only to the layer below it.

- **Screens** are stateless composables. They render an immutable `UiState` and report user actions
  as `UiEvent`s.
- **ViewModels** expose one `StateFlow<UiState>` and one `onEvent` function. They hold the screen's
  logic and resolve no strings, so they can be tested without a UI.
- **Repositories** hold the business rules.
- **Data sources** only store and load. Each one is an interface with one implementation per backend.

Reads return `Flow`s that emit again when data changes. Writes are `suspend` functions that run to
completion even when the screen that started them is gone.

### Tech stack

| Area | Technology |
|---|---|
| UI | Compose Multiplatform, Material 3 |
| Targets | Android, iOS, Web, JVM server |
| Architecture | MVVM with unidirectional data flow, `StateFlow`, Coroutines |
| Navigation | Navigation 3 |
| Dependency injection | Koin |
| Persistence | Multiplatform Settings (planned migration to SQLDelight) |
| Analytics and crash reports | PostHog |
| Ads and billing | Google AdMob, Google Play Billing |
| Server | Ktor 3 on Netty |

## License and data

The source code is source-available under the
[PolyForm Noncommercial License 1.0.0](LICENSE.md). You may read, study and change it, and use it
for any noncommercial purpose. Commercial use, including publishing the app or a derivative of it
with ads or paid features, is not permitted.

The license covers only the code in this repository. It does not cover the question banks,
pictures, videos or road-sign catalog, which are not ours to license.

The Slovak question texts, answers and images are published by the Ministry of Interior of the
Slovak Republic. The road-sign catalog comes from regulation 30/2020 Z. z. The Czech question texts,
answers, pictures and videos are published by the Ministry of Transport of the Czech Republic, and
some of the Czech pictures belong to Asociace autoškol ČR. This project is not affiliated with any
of them. Terms of use for the data are described in [docs/data-sources.md](docs/data-sources.md).
