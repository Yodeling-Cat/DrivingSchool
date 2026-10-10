# Platform-specific code

`:app:shared` uses the `expect`/`actual` pattern: the declaration goes in `commonMain`, and each target source set provides its own `actual`. The targets are `android`, `iosArm64`, `iosSimulatorArm64`, `js` and `wasmJs`, so those source sets are `androidMain`, `iosMain` and `webMain`, the last shared by `js` and `wasmJs` (a `jsMain` or `wasmJsMain` file is only for something the two cannot share); there is no `jvmMain`, because the module has no desktop target. Examples: `WindowTitle.kt` (`expect fun setWindowTitle(title: String)`), `DebugBuild.kt` (`expect fun isDebugBuild(): Boolean`) and `NavigationState.kt`. Each keeps one file per target, named after the common one (`WindowTitle.ios.kt`, `DebugBuild.web.kt`), and every target has one even where the function does nothing — the apps have no window title, so `setWindowTitle` is empty there. When adding platform APIs, follow this same pattern rather than introducing per-target abstractions.

## Resources

Compose resources every country uses live in `app/shared/src/commonMain/composeResources/` and are accessed via the generated `najautoskola.app.shared.generated.resources.Res` class (e.g. `Res.drawable.grain`). A question's pictures and videos are a country's content, read through `BundledContent.uri` instead (see [countries.md](countries.md)).

## iOS on Windows

**iOS code cannot be compiled on Windows.** Kotlin/Native builds for Apple targets only on a Mac, so an `iosMain` change made here is unverified until the iOS workflow (`.github/workflows/ios.yml`) or a Mac builds it. Write it against the Objective-C headers with care: a method declared in a class's own header is a member in Kotlin, not an extension, so importing it from `platform.<Framework>` breaks the build. Say in the commit or the summary that the iOS side was not built.
