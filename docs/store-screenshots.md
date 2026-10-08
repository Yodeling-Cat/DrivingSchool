# Store screenshots

The screenshots for the store listing are taken automatically. A script runs an instrumented test on
a phone emulator and a tablet emulator. The test opens each screen in every listing language and saves
what the device shows. Ads are switched off for the run.

## Taking them

```bash
./tools/screenshots/take-screenshots.sh
```

On Windows, use the PowerShell version:

```powershell
./tools/screenshots/take-screenshots.ps1
```

One run builds the app, takes 12 screenshots on each emulator and pulls them to the computer. When the
build is already done, this takes about 2 minutes.

### Requirements

- The Android SDK, found from `sdk.dir` in `local.properties` or from `ANDROID_HOME`. The script uses
  `adb` and `emulator` from it.
- An AVD for the phone and an AVD for the tablet. The defaults are `Pixel_10` and `Pixel_Tablet`.
  `emulator -list-avds` shows the AVDs on the machine.

You do not have to start the emulators first. If an emulator is already running, the script uses it.
If it is not running, the script starts it and shuts it down at the end. A physical device that is
also connected is not used.

### Options

| `.sh`               | `.ps1`            | What it does                                     |
|---------------------|-------------------|--------------------------------------------------|
| `--phone <AVD>`     | `-Phone <AVD>`    | The phone AVD to use. Default: `Pixel_10`.       |
| `--tablet <AVD>`    | `-Tablet <AVD>`   | The tablet AVD to use. Default: `Pixel_Tablet`.  |
| `--skip-build`      | `-SkipBuild`      | Use the APKs from the last build.                |

### Output

The screenshots go to `store-screenshots/` at the root of the repository. Git ignores that directory.
Each run replaces the screenshots of the devices it ran on.

```
store-screenshots/
  phone/
    sk/
      01-exam-selection.png
      02-signs.png
      03-situation.png
      04-home.png
      05-practice.png
      06-results.png
    en/
      …
  tablet/
    sk/
      …
    en/
      …
```

The images are at the emulator's full resolution: 1080x2424 for `Pixel_10` and 2560x1600 (landscape)
for `Pixel_Tablet`.

## What is in each screenshot

| File                    | Screen                                                                        |
|-------------------------|-------------------------------------------------------------------------------|
| `01-exam-selection.png` | The last page of the exam selection, where the license group is picked.       |
| `02-signs.png`          | The road signs.                                                               |
| `03-situation.png`      | The first intersection situation.                                             |
| `04-home.png`           | The home screen of a user who has passed two tests, failed one and is doing a fourth. |
| `05-practice.png`       | The fourth test, open on its first question with a picture from question 6 on. |
| `06-results.png`        | The results of the second test: passed, with two wrong answers.             |

### The exam's language

Each language's screenshots use an exam in that same language. The test goes through the exam
selection the way a user does: it picks Slovakia, then the exam in the screenshot's language. It takes
`01-exam-selection.png` on the license group page, then picks Car (B). Screenshots 02 to 06 use the
exam that the app saved from that choice, so the app text and the questions are in the same language.

If a language in the list has no Slovak exam in that language, the run stops with an error. When the
app supports the same language for both the app and the exam, adding the language to the list is
enough.

The road sign catalog exists only in Slovak, so the sign names in `02-signs.png` are in Slovak in
every language.

## How it works

### The script

`tools/screenshots/take-screenshots.sh` and `.ps1` do the same steps:

1. Build the debug app and its instrumented test APK with Gradle.
2. For each emulator, install both APKs.
3. Put the status bar in demo mode: the clock shows 10:00, the battery is full and no notifications
   are shown. Switch off the system animations.
4. Run the `StoreScreenshots` test with `adb shell am instrument`.
5. Put the status bar and the animation settings back the way they were. This also happens when the
   test fails.
6. Pull the PNGs from the app's external files directory
   (`/sdcard/Android/data/com.spiraclestudios.autoskola.dev/files/store-screenshots/`) to
   `store-screenshots/<phone|tablet>/`.

If the test fails, the script prints the test's output and stops. The screenshots that were already
on the computer stay as they were.

### The test

The test is in `app/androidApp/src/androidTest/kotlin/com/spiraclestudios/autoskola/screenshots/`:

- `StoreScreenshots.kt` takes the screenshots.
- `ScreenshotModule.kt` has the Koin definitions that replace the app's own for the run.

For each screenshot, the test does these steps:

1. **Starts from empty data.** It restarts Koin with a preferences file of its own
   (`store_screenshots`) and clears that file. The debug app's own data on the emulator is not
   changed.
2. **Removes the ads.** `ScreenshotModule.kt` replaces the ad SDK and Google's consent form with
   versions that do nothing. The billing data source says that "remove ads" was bought. Analytics send
   nothing.
3. **Skips onboarding.** It marks the documents as accepted and the disclaimer as read. It also
   marks the app as rated, so the rating card does not cover the home screen.
4. **Writes the data the screen needs** through the app's own repositories: the exam, and the test
   attempts for screens 04 to 06.
5. **Opens the screen from the stored route.** This is how the app reopens after the system kills
   it (`StoredNavigationState`). For example, the signs screen is `home/learn/signs`.
6. **Waits for the screen.** It waits until a text from that screen is visible, then for the device
   to be idle, then 1.5 seconds more for the pictures to load.
7. **Saves the screenshot** of the whole display, status bar included.

### Not part of the normal test runs

The test class has the `@StoreScreenshot` annotation. `app/androidApp/build.gradle.kts` sets
`testInstrumentationRunnerArguments["notAnnotation"]` to that annotation, so `connectedAndroidTest`
and Android Studio's test runs skip the test. The script runs the class by name with
`am instrument`, which does not use that filter.

## Changing the screenshots

### Add a screen

Add a `capture` call to `takeStoreScreenshots()` in `StoreScreenshots.kt`. Give it:

- a file name with the next number, for example `"07-settings"`;
- a `Target` with the route of the screen and a text that is on the screen when it has loaded. Take
  the text from `language.strings`, so that the wait works in every language.

If the screen needs data, write it in the `capture` block through the repositories, as
`seedProgress` does.

### Add a language

Add the `AppLanguage` to `LANGUAGES` in `StoreScreenshots.kt`. There must be a Slovak exam in that
language (see [The exam's language](#the-exams-language)).

### Change the exam

`COUNTRY` and `CATEGORY` in `StoreScreenshots.kt` set the country and the license group that the test
picks.

## Troubleshooting

- **`"…" never appeared on screen`.** The test did not find the text it waits for within 30 seconds.
  The screen did not open, or its text changed. Make the `Target`'s text match the screen again.
- **`There is no SK exam in …`.** A language in `LANGUAGES` has no exam in that language.
- **`<AVD> did not boot within 5 minutes`.** Start the emulator yourself and run the script again. The
  script uses an emulator that is already running.
- **`Test … was meant to pass` (or `fail`).** The seeded test results no longer give the result the
  home screen needs, possibly after a new question bank version. Change the number of wrong answers in
  `seedProgress`.
