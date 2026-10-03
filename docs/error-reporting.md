# Readable stack traces in crash reports

Crash reports arrive from PostHog's error tracking (`data/analytics/PostHogAnalyticsDataSource.kt`).
An unsymbolicated report from a release build is a list of frames nobody can read, so each platform
needs its symbols uploaded. **Neither platform is wired up yet** — this is what each one needs, and
when.

## Android: the mapping now exists, the upload does not

**Release builds are minified.** `app/androidApp/build.gradle.kts` sets `isMinifyEnabled = true`
with `proguard-rules.pro` beside it, so R8 runs, `app/androidApp/build/outputs/mapping/release/mapping.txt`
is produced, and the app's own frames come through obfuscated. Stack traces from a release build
are unreadable until that mapping reaches PostHog.

`proguard-rules.pro` keeps the one thing the app reaches reflectively — the application's
`BuildConfig` fields, which the shared module reads by name because it generates none of its own.
Compose, Koin, kotlinx.serialization, Coil and AndroidX ship their own consumer rules.

**Wired up, and optional.** PostHog's Gradle plugin is applied by `app/androidApp/build.gradle.kts`
**only when this machine has both the credentials and `posthog-cli` on PATH**. A machine with
neither — every developer's, and any CI job that is not a release — builds a minified release and
uploads nothing, instead of failing. The build prints what it skipped and why.

Both conditions are needed. The plugin shells out to `posthog-cli` and a missing binary fails the
whole release build rather than skipping the upload, so credentials alone are not enough.

Credentials are read from the environment first, then from a `.env` at the repo root. `.env` is
gitignored; `.env.example` names the three variables. They come from:

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

**First, iOS is not wired up at all yet.** The Kotlin Multiplatform SDK links the native
`posthog-ios` package through Kotlin's SwiftPM integration, which has to be generated and added to
the Xcode project before anything on iOS reports:

```
./gradlew :app:shared:integrateLinkagePackage
```

That writes a `KotlinMultiplatformLinkedPackage` directory and adds it to `app/iosApp`; the
generated package and the Xcode project changes are both committed. **It has not been run**, so
today the iOS app links no PostHog: no events and no crash reports leave it, and the dSYM upload
below would have nothing to symbolicate. Everything after that in this section is only reachable
once that step is done.

[plans/posthog-ios.md](./../plans/posthog-ios.md) is the plan for finishing it, including the risk
that running the task rewrites `project.pbxproj` and takes the build phase below with it.

**Install the CLI** on the build machine:

```bash
npm install -g @posthog/cli
```

**Build settings on the app target:**

- `DEBUG_INFORMATION_FORMAT` = `DWARF with dSYM File` for Release
- `ENABLE_USER_SCRIPT_SANDBOXING` = `No` — the script walks directories and runs `posthog-cli`,
  which the sandbox forbids

**This is already wired up.** `iosApp.xcodeproj` has an **Upload PostHog Debug Symbols** phase,
placed last, which runs [app/iosApp/upload-symbols.sh](./../app/iosApp/upload-symbols.sh). That
script is the source of truth. It exits 0 rather than failing the build when the configuration is
not Release, when `posthog-cli` is missing, or when there are no credentials — the same
"optional, and say so" rule the Gradle side follows.

It reads the bundle id and version from the built `Info.plist` with `PlistBuddy`, because this
project defines no `MARKETING_VERSION` or `CURRENT_PROJECT_VERSION` build settings for a script to
read.

The rest of this section describes what that script does, so the setup reads in one place:

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

The Android behavior — the plugin bailing out when `isMinifyEnabled` is false — is read from the
plugin's source, not observed, because there is no minified release build to observe it on.

Worth checking on the first real release: that `posthog-kmp` 0.5.2 pulls a `posthog-android` build
that both emits and reads the mapping ID. The KMP changelog never mentions the mapping ID, so confirm
it on PostHog's symbol sets page rather than assuming.

## Where this sits

`plans/privacy-and-terms.md` has the privacy side of crash reporting — what a report contains, who
sees it, and how consent gates it. This file is only the plumbing.
