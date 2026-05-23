# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

NajAutoskola is a Kotlin Multiplatform driving-school app (tests + learning resources for new drivers). It targets Android, iOS, Web (JS + Wasm via Compose Multiplatform), and ships a Ktor JVM server. The codebase is currently a fresh KMP scaffold — the placeholder `Greeting`/`Platform` code is template output, not real domain logic.

Root package is `uno.lux.autoskola` across every module. App/server `applicationId` and Gradle `group` are also `uno.lux.autoskola`. Gradle root project name is `NajAutoskola`.

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

## Build and run

The README has the canonical commands; key ones:

- Android debug APK: `./gradlew :app:androidApp:assembleDebug`
- Web (Wasm, dev): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
- Web (JS, dev): `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- Server: `./gradlew :server:run`
- iOS: open `app/iosApp/` in Xcode and run.

On Windows use `gradlew.bat` (e.g. `.\gradlew.bat :server:run`).

## Tests

- Shared (Android host): `./gradlew :app:shared:testAndroidHostTest` — Android resources are enabled in host tests (`withHostTest { isIncludeAndroidResources = true }`).
- Shared (iOS sim): `./gradlew :app:shared:iosSimulatorArm64Test`
- Shared (web): `./gradlew :app:shared:wasmJsTest` or `:app:shared:jsTest`
- Server: `./gradlew :server:test` (uses `ktor-server-test-host` + `kotlin-test-junit`)
- Run a single test: append `--tests "uno.lux.autoskola.SomeClassTest.someMethod"` to the relevant test task.

## Toolchain pinning

Versions are centralized in `gradle/libs.versions.toml` — change versions there, not in module build files. Notable: Kotlin 2.3.21, AGP 9.0.1, Compose Multiplatform 1.11.0, Material3 1.11.0-alpha07, Ktor 3.4.3. Android `compileSdk` 36, `minSdk` 26, JVM target 11. Gradle configuration cache and build cache are both **on** (`gradle.properties`) — if you see odd cache behaviors, `--no-configuration-cache` is the first thing to try.
