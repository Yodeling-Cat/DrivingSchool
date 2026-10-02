This is a Kotlin Multiplatform project targeting Android, iOS, Web, Server.

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Setting up your machine

Nothing here is needed to build, run or test the app locally. It covers where crash reports and
usage analytics go.

**Analytics and crash reporting need no setup to work in a debug build** on Android and on the web:
they report to the project the app ships with, and start as soon as you accept the consent screen
and switch one of the two answers on. Until you do, the app sends nothing — so if you see no events,
check the switches before anything else. iOS is not wired up yet: the native `posthog-ios` package
still has to be linked into the Xcode project, which [docs/error-reporting.md](./docs/error-reporting.md)
describes.

**A `.env` is about release builds, not about seeing your events.** A minified release rewrites the
app's own stack frames, so the mapping file R8 produces has to be uploaded to PostHog or that
build's crash reports arrive as a list of obfuscated frames. The upload needs a secret, read from
the environment or from a `.env` at the repo root (gitignored — copy [.env.example](./.env.example)):

- `POSTHOG_CLI_API_KEY` — a **personal** API key (`phx_…`), *not* the project token the app ships
  with. Create it under Settings → Personal API keys, with the **error tracking write** and
  **organization read** scopes.
- `POSTHOG_CLI_PROJECT_ID` — the numeric project id, from Project settings.
- `POSTHOG_CLI_HOST` — `https://eu.posthog.com` for the EU cloud. Note there is no `i.`: the app
  ingests events to `eu.i.posthog.com`, while the CLI talks to `eu.posthog.com`.

`posthog-cli` must also be on your PATH (`npm install -g @posthog/cli`).

- **Only running debug builds?** Nothing to do.
- **Building a release on your machine?** Then this is mandatory, or that build's crash reports are
  unreadable. The build still succeeds, and says that it skipped the upload.
- **CI builds your releases?** The same three values belong in the release job's secrets, with
  `posthog-cli` installed on the runner. That is where they are supposed to live — a release build
  should not depend on secrets sitting on a laptop.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Server: `./gradlew :server:run`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

### Open source libraries

The About screen's list of open source libraries is generated and committed, like the question
banks. Refresh it after adding, removing or bumping a dependency:

- `./gradlew :app:shared:exportLibraryDefinitions`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:shared:testAndroidHostTest`
- Server tests: `./gradlew :server:test`
- Web tests:
  - Wasm target: `./gradlew :app:shared:wasmJsTest`
  - JS target: `./gradlew :app:shared:jsTest`
- iOS tests: `./gradlew :app:shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).