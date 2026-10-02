# Readable stack traces in crash reports

Crash reports arrive from PostHog's error tracking (`data/analytics/PostHogAnalyticsDataSource.kt`).
An unsymbolicated report from a release build is a list of frames nobody can read, so each platform
needs its symbols uploaded. **Neither platform is wired up yet** — this is what each one needs, and
when.

## Android: nothing to do yet, on purpose

**Release builds are not minified.** `app/androidApp/build.gradle.kts` sets
`isMinifyEnabled = false`, so R8 never runs, no `mapping.txt` exists, and Kotlin/JVM frames come
through with their real names. There is nothing to symbolicate and no reason to upload anything.

That also means PostHog's Gradle plugin would do nothing here. From `PostHogAndroidGradlePlugin.kt`
it bails out of the mapping path for any variant where `isMinifyEnabled` is false, and the rest of
its work is NDK `.so` symbols, which this app has none of.

**The moment minification is turned on**, stack traces go obfuscated and this becomes real work:

1. Add the plugin to `app/androidApp/build.gradle.kts` — the **app module**, not the root, or it
   logs a warning and does nothing:

   ```kotlin
   plugins {
       id("com.posthog.android") version "1.7.0"
   }
   ```

2. The plugin's only extension settings are `uploadNativeSymbols` and
   `includeNativeSymbolSources`, both about native code. **Credentials are not configured there.**
   They come from the environment on the machine that runs the release build:

   ```
   POSTHOG_CLI_HOST=https://eu.posthog.com        # note: no "i."
   POSTHOG_CLI_PROJECT_ID=<numeric project id>
   POSTHOG_CLI_API_KEY=phx_<personal api key>     # NOT the phc_ project token
   ```

3. It needs a **personal** API key (`phx_…`) with the *error tracking write* and *organization read*
   scopes, not the `phc_…` project token that ships in the app. That token can write events and
   nothing else, which is deliberate.

4. Host gotcha: ingestion uses `https://eu.i.posthog.com`, the CLI uses
   `https://eu.posthog.com`. Two different hosts; do not share one variable.

The upload itself is always `posthog-cli`, invoked by the plugin. Nothing uploads symbols on its own.

There is no PostHog Gradle-plugin workaround needed on this toolchain. The plugin failed to register
its upload tasks on AGP 9.3.0–9.3.2 — PostHog issue #830, which they closed as an AGP bug, fixed in
AGP 9.3.3 / 9.4.1 / 9.5.0-alpha06. This project is on AGP 9.4.1, so it is in the clear.

## iOS: always needed for a release build

Kotlin/Native and Swift frames both need the dSYMs, minified or not. This is a Mac-side job.

**Install the CLI** on the build machine:

```bash
npm install -g @posthog/cli
```

**Build settings on the app target:**

- `DEBUG_INFORMATION_FORMAT` = `DWARF with dSYM File` for Release
- `ENABLE_USER_SCRIPT_SANDBOXING` = `No` — the script walks directories and runs `posthog-cli`,
  which the sandbox forbids

**A Run Script build phase, last**, after the phases that produce the app and the dSYMs:

```bash
if [[ "${CONFIGURATION:-}" != "Release" ]]; then
    exit 0
fi

export PATH="/opt/homebrew/bin:/usr/local/bin:${HOME}/.posthog:${PATH}"
POSTHOG_CLI_PATH="${POSTHOG_CLI_PATH:-$(command -v posthog-cli)}"
if [[ -z "${POSTHOG_CLI_PATH}" ]]; then
    echo "posthog-cli was not found"
    exit 1
fi

"${POSTHOG_CLI_PATH}" dsym upload \
    --directory "${DWARF_DSYM_FOLDER_PATH}" \
    --main-dsym "${DWARF_DSYM_FILE_NAME}" \
    --release-name "${PRODUCT_BUNDLE_IDENTIFIER}" \
    --release-version "${MARKETING_VERSION}" \
    --build "${CURRENT_PROJECT_VERSION}"
```

With that input file declared, so Xcode waits for the dSYM instead of failing with
`content_hash_mismatch`:

```
$(DWARF_DSYM_FOLDER_PATH)/$(DWARF_DSYM_FILE_NAME)/Contents/Resources/DWARF/$(EXECUTABLE_NAME)
```

The script does not carry credentials. Either run `posthog-cli login` once on the build machine or
set the same three `POSTHOG_CLI_*` variables as Android. `POSTHOG_SKIP_ON_CONFLICT=1` makes a
conflicting symbol set skip rather than fail the build.

Use this script rather than `posthog-ios`'s bundled `upload-symbols.sh`: the KMP SDK pulls
`posthog-ios` in through the generated SwiftPM linkage package, so that path is not predictable.

## What is not verified

Everything above for iOS is **from PostHog's documentation and has not been run** — there is no
macOS gateway available here, so the Xcode build phase, the build settings and an end-to-end upload
are all unexercised. The dSYM phase has deliberately not been hand-written into
`app/iosApp/iosApp.xcodeproj/project.pbxproj`: a malformed project file breaks the iOS build
outright, and it cannot be built or opened to check from Windows. It belongs in Xcode.

The Android behaviour — the plugin bailing out when `isMinifyEnabled` is false — is read from the
plugin's source, not observed, because there is no minified release build to observe it on.

Worth checking on the first real release: that `posthog-kmp` 0.5.2 pulls a `posthog-android` build
that both emits and reads the mapping ID. The KMP changelog never mentions the mapping ID, so confirm
it on PostHog's symbol sets page rather than assuming.

## Where this sits

`plans/privacy-and-terms.md` has the privacy side of crash reporting — what a report contains, who
sees it, and how consent gates it. This file is only the plumbing.
