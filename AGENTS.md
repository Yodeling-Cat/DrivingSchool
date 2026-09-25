# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, etc.) when working with code in this repository. `CLAUDE.md` imports it.

## Project overview

NajAutoskola is a Kotlin Multiplatform driving-school app (tests + learning resources for new drivers). It targets Android, iOS, Web (JS + Wasm via Compose Multiplatform), and ships a Ktor JVM server. The codebase is currently a fresh KMP scaffold — the placeholder `Greeting`/`Platform` code is template output, not real domain logic.

Root package is `uno.lux.autoskola` across every module. App/server `applicationId` and Gradle `group` are also `uno.lux.autoskola`. Gradle root project name is `NajAutoskola`.

## Tests first

Tests are the primary consumer of our code; the human user comes second. Every change follows these rules:

1. **Write the test first.** Prefer strict TDD: write one failing test, make it pass with the simplest code, refactor, then write the next test. Run the test and see it fail before you write the implementation.
2. **Design for testability.** Keep decisions and rules in plain Kotlin that a unit test can call:
   - Scoring, what to resume, which questions count as mistakes and which answers are locked all belong in `:core`, in `ExamSession` or in ViewModels, never inline in a composable.
   - Composables should only render state and forward events.
   - Inject what a test needs to control: `Settings` (use `MapSettings` in tests), data sources (fakes like `FakeQuestionBankDataSource`), clocks (`now: () -> Long`) and randomness (`Random`).
3. **Test what matters most, not everything.** Aim at domain logic, persistence, data import and state decisions, where bugs would silently corrupt a user's progress or give wrong answers. Don't chase coverage numbers, and don't test Compose layout details.
4. **Keep tests fast and deterministic.** No network, no real time, no shared state between tests.
5. **A bug fix starts with a test that reproduces it.**

Where tests live:
- `:core`: `core/src/commonTest`. Run with `./gradlew :core:jvmTest`.
- `:app:shared`: `app/shared/src/commonTest`. Run with `./gradlew :app:shared:testAndroidHostTest`.
- Importer: `tools/import/test_*.py`. Run with `./tools/import/test.sh` (standard `unittest`, offline fixtures).

## Module layout

Four Gradle modules are wired via `settings.gradle.kts` with typesafe project accessors (`projects.core`, `projects.app.shared`, etc.):

- **`:core`** — pure-Kotlin shared logic for **all** targets including the server (iosArm64, iosSimulatorArm64, jvm, js, wasmJs, androidLibrary). No Compose dependency. This is the only module the server can depend on alongside `:app:shared`.
- **`:app:shared`** — Compose Multiplatform UI shared across Android/iOS/Web. Targets iOS, JS, Wasm, and `androidLibrary` (no JVM target — server cannot consume it). Exposes `api(projects.core)`. iOS framework is built as a static framework named `Shared`.
- **`:app:androidApp`** — Android `com.android.application` entry. `MainActivity` just calls `App()` from `:app:shared`.
- **`:app:webApp`** — Compose Multiplatform web entry; produces both JS and Wasm executables that mount `App()` via `ComposeViewport`.
- **`:server`** — Ktor Netty server (`uno.lux.autoskola.ApplicationKt`), depends on `:core`.
- **`app/iosApp/`** — Xcode project that links the `Shared` framework from `:app:shared` and calls `MainViewController()`.

Cross-module rule of thumb: shared **business logic** that the server also needs goes in `:core`. Shared **UI** goes in `:app:shared/commonMain`. Anything Compose-related must not leak into `:core`.

## Platform-specific code

`:app:shared` uses the `expect`/`actual` pattern. `Platform.kt` in `commonMain` declares `expect fun getPlatform(): Platform`; each target source set (`androidMain`, `iosMain`, `jsMain`, `wasmJsMain`, `jvmMain`) provides its own `actual`. When adding platform APIs, follow this same pattern rather than introducing per-target abstractions.

Compose resources live in `app/shared/src/commonMain/composeResources/` and are accessed via the generated `najautoskola.app.shared.generated.resources.Res` class (e.g. `Res.drawable.compose_multiplatform`).

## Screen sizes and languages

We support every screen size: phones and tablets in both orientations and desktop browsers via the web target. Every screen must work at all of them:

- Read the width class from `LocalWidthClass` (`ui/Adaptive.kt`: Compact < 600dp, Medium < 840dp, Expanded ≥ 840dp) instead of checking platforms.
- Phones in landscape are supported (orientation is not locked on Android or iOS). Check `LocalHeightClass` too: on Compact height (< 480dp), put things side by side and slim down the bars, as `PracticeScreen` does.
- Top-level navigation is a bottom `NavigationBar` on Compact and a `NavigationRail` on Medium/Expanded (see `App.kt`).
- Don't let content stretch: cap it with `ContentWidth` (Narrow for forms/settings, Reading for question text, Wide for multi-pane) and centre it. Use extra width for extra panes or grid columns, not wider rows.
- Everything must be usable with a mouse and keyboard as well as touch (e.g. pagers need prev/next buttons, since mouse drags don't swipe).

UI text is translated into English, Slovak, Hungarian and Czech. Never hardcode user-visible strings in composables; add them to `i18n/Strings.kt` for all four languages and read them via `strings`. Question content is not translated — it comes from the question bank in the bank's language.

## Question data

Question banks and images are generated into `app/shared/src/commonMain/composeResources/files/` by `tools/import/` (run `./tools/import/import.sh`). Never hand-edit them. Sources, coverage and terms of use are in `docs/data-sources.md`; the importer is documented in `docs/importing-questions.md`. The bank model and practice-test generation live in `:core` (`uno.lux.autoskola.bank`).

## Question bank versions

Governments periodically publish updated question sets. Each bank therefore has a `version` (all banks are currently **v1**), and its file is `banks/<bank>-v<version>.json`.

- A new official question set gets a **new version**: run the importer with `--version N+1`. Never overwrite a published version in place, because recorded attempts reference question ids *within a bank version*.
- Keep older version files bundled, along with their images; the importer keeps them. Old attempts then still resolve to the exact questions that were asked.
- `banks/index.json` lists the latest version per bank. The app always starts new tests on the latest version. Statistics, results and mistakes are scoped to one bank version, so a new version starts fresh.
- Open decision for when v2 arrives: whether to map unchanged questions between versions so users keep their progress. We'd match them by text, much as the importer matches the PDF. We also need to decide when to stop bundling very old versions.

## Statistics

Every test run is a `TestAttempt` (`:core`, `uno.lux.autoskola.stats`). It records the bank id and version, the license group, the questions in order, each answer with its timestamp, and the start, finish or discard times. `AttemptRepository` (`data/attempts/`) saves an attempt on every answer, so a test survives the app being killed and can be resumed.

Everything else is derived from attempts by `Statistics`: latest result per test, the resumable attempt, per-question stats and mistakes. Don't store derived numbers separately. Answers count towards statistics only once they're final: either the attempt is finished, or it was taken in "check answers immediately" mode.

## Architecture

`:app:shared` has three layers. Each one only talks to the layer below it.

```
Screen (composable)  --UiEvent-->  ViewModel  --calls-->  Repository  --calls-->  DataSource
                     <--UiState--             <--Flow---              <--Flow---
```

**Screens** render state and send events. Each feature has two composables:
- `XScreen(state, onEvent, …navigation callbacks)` is stateless. It renders a `UiState` and reports user actions as `UiEvent`s. It never calls a ViewModel or repository.
- `XRoute(…)` gets the ViewModel with `koinViewModel()`, collects `uiState` with `collectAsStateWithLifecycle()`, and passes navigation callbacks through.

**ViewModels** (`XViewModel`, next to the screen in the same feature package) hold the screen's logic:
- Each one exposes `uiState: StateFlow<XUiState>` and one public `fun onEvent(event: XUiEvent)`. That's the only way screens talk to them.
- `XUiState` and `XUiEvent` are defined in the ViewModel's file. States are immutable data classes; use a sealed interface when a screen has distinct phases (`Loading`, `Ready`, `Closed`).
- Build `uiState` with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Compute the initial value from synchronous data when you can (for example preferences), so the first frame doesn't flash.
- Navigation that depends on the result of work goes through state: `HomeUiState.Ready.openAttemptId`, `ExamSelectionUiState.done`, `PracticeUiState.Closed`. The Route navigates and, if needed, sends an event to acknowledge it (`HomeUiEvent.AttemptOpened`). Pure navigation (back, "change exam") goes straight from the screen to a navigation callback.
- Don't resolve UI strings in ViewModels. Put the facts in state (`AttemptKind`, test number, `ExamLabel`) and let the screen format them with `strings`.

**Repositories** (`data/<area>/XRepository`) hold the rules and combine data sources. Examples: attempts are locked once closed, banks are cached, and the current exam is derived from preferences and banks. `ExamSession` holds the rules for one exam on top of `AttemptRepository`.

**Data sources** (`data/<area>/XDataSource`) only store and load data. Each one is an interface with one implementation per backend:
- `SettingsAttemptDataSource` stores attempts in `Settings`.
- `ResourceQuestionBankDataSource` reads the bundled banks.
- A database or API would be another implementation. `PreferencesRepository` uses `Settings` directly, because `Settings` is already an interface.

**Navigation** uses Navigation 3 (`navigation/ExamNavigation.kt`):
- The back stack is a list of `@Serializable` `NavKey`s. Every new key must be registered in `navConfiguration`, so the back stack can be saved on iOS and web too.
- Each entry has its own ViewModel store, so a screen's ViewModel is cleared when the screen is popped. Pass arguments through the key, for example `PracticeKey(attemptId)` with `koinViewModel { parametersOf(attemptId) }`.
- Before navigation starts, `AppViewModel` picks the top-level destination: disclaimer, exam choice, loading or the exam.

**Dependency injection** uses Koin (`di/AppModule.kt`):
- Data sources and repositories are `single`s. ViewModels are `viewModel`s.
- `initKoin()` starts Koin once per process. The repositories must outlive activities, because retained ViewModels still use them.
- `AppModuleTest` checks that every ViewModel can be created, so register new ones there too.

**Images** are loaded with Coil 3. The data source turns a question's image path into a URI (`Res.getUri`), the ViewModel puts it in state, and the screen draws it with `rememberAsyncImagePainter`. Coil caches the images and scales them down to the size they're shown at.

**Testing ViewModels:**
- Extend `ViewModelTest` (`commonTest/.../testing/`). It swaps `Dispatchers.Main` for an immediate test dispatcher.
- Create ViewModels lazily, after that swap.
- Use `observe(viewModel.uiState)` to keep the state subscribed like a screen would.
- `TestRepositories` wires the real repositories over `MapSettings` and a fake bank.

## Data access

Attempts are stored in `Settings` today. We write their API as if they lived in a database, so we can move to one (probably SQLDelight, see `plans/sqldelight.md`) by adding an `AttemptDataSource` implementation:

- **Reads return a `Flow`** that emits again when the data changes (`AttemptRepository.statistics`, `ExamSession.progress`, `ExamSession.openAttempt`, `ExamRepository.current`). Don't add synchronous getters, even though `Settings` could answer them instantly.
- **Writes are `suspend` functions.** `AttemptRepository` runs them in `NonCancellable` and one at a time, so a write finishes even when the ViewModel that started it is cleared.
- **Show a loading state until the first value arrives.** Don't assume a flow emits immediately.
- **Tests use `runTest`** and read flows with `.first()`.

This applies to user progress: attempts and anything we derive from them. `PreferencesRepository` stays synchronous (a `StateFlow` with plain setters) on every platform, because it's a few small values the app needs to draw its first frame.

## Build and run

The README has the canonical commands; key ones:

- Android debug APK: `./gradlew :app:androidApp:assembleDebug`
- Web (Wasm, dev): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
- Web (JS, dev): `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- Server: `./gradlew :server:run`
- iOS: open `app/iosApp/` in Xcode and run.

On Windows use `gradlew.bat` (e.g. `.\gradlew.bat :server:run`).

## Tests

See "Tests first" above for how we work. Commands:

- Core logic: `./gradlew :core:jvmTest`
- Shared app code (Android host): `./gradlew :app:shared:testAndroidHostTest`. Android resources are enabled in host tests (`withHostTest { isIncludeAndroidResources = true }`).
- Shared app code on other targets: `./gradlew :app:shared:iosSimulatorArm64Test`, `:app:shared:wasmJsTest` or `:app:shared:jsTest`.
- Server: `./gradlew :server:test` (uses `ktor-server-test-host` + `kotlin-test-junit`).
- Importer: `./tools/import/test.sh` (or `test.ps1`). `test_import_minv.py` covers parsing and matching with offline fixtures. `test_bank_output.py` checks the committed banks and images, so run it after every import.
- To run a single test, append `--tests "uno.lux.autoskola.SomeClassTest.someMethod"` to the Gradle task.

## Toolchain pinning

Versions are centralized in `gradle/libs.versions.toml` — change versions there, not in module build files. Notable: Kotlin 2.3.21, AGP 9.0.1, Compose Multiplatform 1.11.0, Material3 1.11.0-alpha07, Ktor 3.4.3. Android `compileSdk` 36, `minSdk` 26, JVM target 11. Some libraries are held back until we upgrade the toolchain. Lifecycle stays on 2.11.0-beta01, because 2.11.0 needs compileSdk 37 and AGP 9.1. Coil stays on 3.4.0, because 3.5 needs Kotlin 2.4. When you add or bump a library, check that it doesn't pull `kotlin-stdlib` above our Kotlin version (`./gradlew :app:shared:dependencies --configuration wasmJsCompileClasspath`). A newer stdlib makes the JS/Wasm compiler crash with a `FirRegularClassSymbol` NullPointerException. Gradle configuration cache and build cache are both **on** (`gradle.properties`) — if you see odd cache behaviors, `--no-configuration-cache` is the first thing to try.
