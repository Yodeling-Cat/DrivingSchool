# Store screenshots

The store listing's screenshots are taken by an instrumented test on a phone and a tablet emulator,
in Slovak and English, with no ads on screen.

```bash
./tools/screenshots/take-screenshots.sh
```

On Windows, `./tools/screenshots/take-screenshots.ps1` does the same. The files land in
`store-screenshots/` at the root of the repository, which git ignores:

```
store-screenshots/
  phone/
    sk/01-exam-selection.png … 06-results.png
    en/…
  tablet/
    sk/…
    en/…
```

## What the script does

1. Builds the debug app and its instrumented test APK (skip with `--skip-build` / `-SkipBuild`).
2. For each of the two AVDs (`Pixel_10` and `Pixel_Tablet` by default; pick others with
   `--phone`/`--tablet` or `-Phone`/`-Tablet`), it uses the emulator if it is running, or starts it and
   shuts it down again afterwards.
3. Installs both APKs, puts the status bar in demo mode (10:00, full battery, no notifications) and
   switches animations off.
4. Runs `StoreScreenshots` with `am instrument`, then restores the status bar and animations.
5. Pulls the PNGs from the app's external files directory into `store-screenshots/<phone|tablet>/`,
   replacing what was there.

## The test

`app/androidApp/src/androidTest/.../screenshots/StoreScreenshots.kt` opens each screen from a clean
start:

- **Its own data.** Koin is restarted for every screen over a preferences file of its own, so the
  debug app's own data on the emulator is never touched.
- **No ads.** `ScreenshotModule.kt` replaces the ad SDK and Google's consent form with the no-op ones,
  has the store answer that "remove ads" was bought, and silences analytics.
- **Onboarding is skipped.** The documents count as accepted and the disclaimer as read. The exam
  selection screen is the one shot taken with no exam chosen.
- **Progress through the app's own repositories.** Tests 1 and 2 are passed, test 3 is failed and
  test 4 is under way. The practice screen opens on test 4's first question with a picture, and the
  results are test 2's.
- **Navigation from the stored route.** Each screen is opened the way the app reopens itself after
  being killed (`StoredNavigationState`).
- **A finished screen.** The screenshot waits until a text from that screen is showing, plus a short
  pause for the pictures.

Each language comes with its own question bank (`sk-sk`, `sk-en`); `LANGUAGES` in the test lists them.
The road-sign catalog exists only in Slovak, so the English signs screen shows Slovak sign names.

The test is marked `@StoreScreenshot`, which `connectedAndroidTest` leaves out
(`testInstrumentationRunnerArguments["notAnnotation"]` in `app/androidApp/build.gradle.kts`). Only the
script runs it.
