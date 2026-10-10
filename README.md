# NajAutoskola

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

## Getting started

### Run the apps

| Target | Command |
|---|---|
| Android (Slovak / Czech) | `./gradlew :app:androidApp:assembleSkDebug` / `assembleCzDebug` |
| Web (Wasm) | `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`, with `-Pcountry=cz` for the Czech app |
| Web (JS, for older browsers) | `./gradlew :app:webApp:jsBrowserDevelopmentRun` |
| Server | `./gradlew :server:run` |
| iOS | Open [`app/iosApp`](app/iosApp) in Xcode and run. |

On Windows, use `gradlew.bat`. Each country is its own app with only its own questions, pictures and
videos (see [docs/countries.md](docs/countries.md)).

### Testing

We prefer strict TDD, and write the tests first.

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

## Data and license

The question texts, answers and images are published by the Ministry of Interior of the Slovak
Republic. The road-sign catalog comes from regulation 30/2020 Z. z. This project is not affiliated
with either. Terms of use for the data are described in [docs/data-sources.md](docs/data-sources.md).

This repository has no open-source license. All rights reserved.
