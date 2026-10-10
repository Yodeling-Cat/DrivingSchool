# Dependencies and toolchain

Adding, removing or bumping a dependency touches three things: the version catalog and the `kotlin-stdlib` check, the open source library list, and — for a dependency with SwiftPM dependencies — the iOS linkage package.

## Toolchain pinning

Versions are centralized in `gradle/libs.versions.toml` — change versions there, not in module build files. Notable: Kotlin 2.4.20, AGP 9.4.1, Compose Multiplatform 1.12.0, Material3 1.12.0-alpha03, Ktor 3.4.3, AboutLibraries 15.2.0, PostHog 0.5.2. Android `compileSdk` 37, `minSdk` 26, `targetSdk` 36, JVM target 11. Kotlin 2.4 and Compose 1.12 are what AboutLibraries' current UI needs, and they are why `compileSdk` is 37: every Compose 1.12 artifact asks for it. Lifecycle stays on 2.11.0-beta01 and Coil on 3.4.0 — the compileSdk 37 and Kotlin 2.4 they were waiting for have now arrived, so either can move the next time something needs it. When you add or bump a library, check that it doesn't pull `kotlin-stdlib` above our Kotlin version (`./gradlew :app:shared:dependencies --configuration wasmJsCompileClasspath`). A newer stdlib makes the JS/Wasm compiler crash with a `FirRegularClassSymbol` NullPointerException. Do this **before** integrating an SDK rather than after: finding out later means a crash whose cause is in someone else's published metadata rather than in our code.

That mismatch can be resolved from either end — hold the dependency below our compiler, or move the compiler up to the dependency. PostHog pushed us into the second: `posthog-kmp` 0.5.2 declares `kotlin-stdlib` 2.4.20 outright, so on Kotlin 2.4.10 the wasm classpath carried a stdlib above our compiler. Kotlin's own rule is that `2.x.0` and the following `2.x.20` are the same release line, so a `2.4.20` bump is a tooling change, not a language one, and it is the cheaper of the two when the dependency is worth having (`plans/privacy-and-terms.md`). Gradle configuration cache and build cache are both **on** (`gradle.properties`) — if you see odd cache behaviors, `--no-configuration-cache` is the first thing to try.

## Open source libraries

The About screen lists the app's dependencies with [AboutLibraries](https://github.com/mikepenz/AboutLibraries), whose Gradle plugin writes them to `app/shared/src/commonMain/composeResources/files/aboutlibraries.json`. The file is generated and **committed** — the plugin is not wired into the build, so no build fetches anything — and `./gradlew :app:shared:exportLibraryDefinitions` refreshes it. Run that after adding, removing or bumping a dependency, and commit the result with it: a stale file is exactly what the user reads. `AboutViewModel` reads it from the app's Compose resources and `AboutScreen` draws AboutLibraries' own container (`LibrariesVariant.Refined`, `LibraryActionMode.Chips`), supplying only the words around it.

## SwiftPM linkage package

`posthog-kmp` brings the PostHog iOS SDK in as a SwiftPM dependency, and since Kotlin 2.4 the Xcode project has to link such dependencies itself, through a local Swift package that the Kotlin plugin generates: `app/iosApp/KotlinMultiplatformLinkedPackage/`. The Xcode project references it (a local package, linked in the `iosApp` target's Frameworks phase), and the directory is **committed**: Xcode resolves packages before the "Compile Kotlin Framework" phase runs, and that phase regenerates the package and fails the build if anything in it changed (`checkSyntheticImportProjectIsCorrectlyIntegrated…` and "Synthetic project regenerated" are the two errors).

So after adding, removing or bumping a dependency that declares SwiftPM dependencies — any `posthog-kmp` bump does, since the package names its version — regenerate it and commit the result with the bump. The task only writes files, so it runs on Windows too; give it the Xcode project as an absolute path:

```bash
XCODEPROJ_PATH="$PWD/app/iosApp/iosApp.xcodeproj" ./gradlew :app:shared:generateSyntheticLinkageSwiftPMImportProjectForLinkageForCli
```

On a Mac, `:app:shared:integrateLinkagePackage` with the same variable does that and also adds the package to the Xcode project, which is already done here.
