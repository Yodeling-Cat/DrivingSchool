# Store screenshots

The screenshots for the store listing are taken automatically. A script runs an instrumented test on
three emulators: a phone, a 7-inch tablet and a 10-inch tablet. The test opens each screen in every
listing language and saves what the device shows. Ads are switched off for the run. At the end, the
script checks that the tablet screenshots meet the store's requirements.

## Taking them

```bash
./tools/screenshots/take-screenshots.sh
```

On Windows, use the PowerShell version:

```powershell
./tools/screenshots/take-screenshots.ps1
```

One run builds the app, takes 14 screenshots on the phone and on the 7-inch tablet and 12 on the 10-inch tablet, and pulls them to the
computer. When the build is already done, this takes about 5 minutes, most of it booting the tablets.

To take only some of them:

```powershell
./tools/screenshots/take-screenshots.ps1 -Only phone
./tools/screenshots/take-screenshots.ps1 -Only tablet
```

### Requirements

- The Android SDK, found from `sdk.dir` in `local.properties` or from `ANDROID_HOME`. The script uses
  `adb` and `emulator` from it.
- The three AVDs below. `emulator -list-avds` shows the AVDs on the machine.

| Device         | AVD name               | Name in Android Studio | Screen                              |
|----------------|------------------------|------------------------|-------------------------------------|
| Phone          | `Pixel_10`             | Pixel 10               | 1080x2424, 420 dpi, portrait        |
| 7-inch tablet  | `Pixel_Tablet_7-inch`  | Pixel Tablet 7-inch    | 1080x1920 (9:16), 280 dpi, portrait |
| 10-inch tablet | `Pixel_Tablet_10-inch` | Pixel Tablet 10-inch   | 2560x1440 (16:9), 280 dpi, landscape |

Both tablets use the Android 13 (API 33) Google Play system image.

#### How the tablets are set up

Android Studio has no device profile with a 16:9 screen in either tablet size. So both tablet AVDs
start from the Pixel Tablet profile, with a different screen. To make them again:

```bash
avdmanager create avd -n Pixel_Tablet_7-inch -k "system-images;android-33;google_apis_playstore;x86_64" -d pixel_tablet
avdmanager create avd -n Pixel_Tablet_10-inch -k "system-images;android-33;google_apis_playstore;x86_64" -d pixel_tablet
```

Then set these lines in each AVD's `config.ini` (`~/.android/avd/<AVD name>.avd/config.ini`):

| Key                     | 7-inch                | 10-inch                |
|-------------------------|-----------------------|------------------------|
| `avd.ini.displayname`   | `Pixel Tablet 7-inch` | `Pixel Tablet 10-inch` |
| `hw.lcd.width`          | `1080`                | `2560`                 |
| `hw.lcd.height`         | `1920`                | `1440`                 |
| `hw.lcd.density`        | `280`                 | `280`                  |
| `hw.initialOrientation` | `portrait`            | `landscape`            |
| `skin.name`             | `1080x1920`           | `2560x1440`            |
| `skin.path`             | `_no_skin`            | `_no_skin`             |
| `showDeviceFrame`       | `no`                  | `no`                   |

The density decides which layout the app draws. At 280 dpi, the 7-inch tablet is 617 dp wide, so the
app uses its tablet layout, which starts at 600 dp. A 16:9 screen with an exact 7-inch diagonal is
only 549 dp wide and gets the phone layout. The emulators' nominal sizes are about 7.9 and 10.5
inches. The store checks only the pixels, not the inches.

### Running emulators

- **An emulator that is already running**, for example one you started, is used as it is. The script
  leaves it running at the end.
- **An emulator that is not running** is started by the script and shut down when its screenshots are
  taken. The script waits until it has stopped before it starts the next one. So only one emulator
  that the script started runs at a time. Each one needs about 3 GB of memory.
- **A physical device** that is also connected is not used.

### Options

| `.sh`                 | `.ps1`               | What it does                                                    |
|-----------------------|----------------------|-----------------------------------------------------------------|
| `--only phone`        | `-Only phone`        | Take only the phone screenshots.                                |
| `--only tablet`       | `-Only tablet`       | Take only the two tablets' screenshots, and check them.         |
| `--phone <AVD>`       | `-Phone <AVD>`       | The phone AVD. Default: `Pixel_10`.                             |
| `--tablet-7 <AVD>`    | `-Tablet7 <AVD>`     | The 7-inch tablet AVD. Default: `Pixel_Tablet_7-inch`.          |
| `--tablet-10 <AVD>`   | `-Tablet10 <AVD>`    | The 10-inch tablet AVD. Default: `Pixel_Tablet_10-inch`.        |
| `--skip-build`        | `-SkipBuild`         | Use the APKs from the last build.                               |

### Output

The screenshots go to `store-screenshots/` at the root of the repository. Git ignores that directory.
Each run replaces the screenshots of the devices it ran on.

```
store-screenshots/
  phone/
    sk/
      01-exam-selection.png
      02-home.png
      03-practice.png
      04-situation.png
      05-results.png
      06-practice-questions.png   (phone and 7-inch tablet)
      07-signs.png
    en/
      …
  tablet/
    7-inch/
      sk/
        …
      en/
        …
    10-inch/
      sk/
        …
      en/
        …
```

The images are at the emulator's full resolution: 1080x2424 for the phone, 1080x1920 for the 7-inch
tablet and 2560x1440 for the 10-inch tablet.

### The store's requirements

After it takes the tablet screenshots, the script checks each one against what Google Play accepts:

| Folder            | File size  | Aspect ratio | Each side            |
|-------------------|------------|--------------|----------------------|
| `tablet/7-inch/`  | Up to 8 MB | 16:9 or 9:16 | 320 px to 3,840 px   |
| `tablet/10-inch/` | Up to 8 MB | 16:9 or 9:16 | 1,080 px to 7,680 px |

The aspect ratio must be correct to within one pixel. If a screenshot breaks a rule, the script lists
each problem and ends with an error. The screenshots stay in the folder, so you can look at them.

The script does not check the phone screenshots.

## What is in each screenshot

| File                          | Screen                                                                  |
|-------------------------------|-------------------------------------------------------------------------|
| `01-exam-selection.png`       | The last page of the exam selection, where the license group is picked. |
| `02-home.png`                 | The home screen, with the test history below.                           |
| `03-practice.png`             | Test 23 with the correct answer picked: question 18 on the phone, question 19 on both tablets. |
| `04-situation.png`            | Intersection situation 27, with its questions answered correctly.       |
| `05-results.png`              | The results of test 3: passed, with three wrong answers.                |
| `06-practice-questions.png`   | Phone and 7-inch tablet. Test 23 with the question list open.          |
| `07-signs.png`                | The road signs.                                                         |

The 10-inch tablet has no `06-practice-questions.png`. The app shows the question list as a panel
beside the question when the screen is at least 840 dp wide and 480 dp high, so on the 10-inch tablet
in landscape `03-practice.png` already shows it. On the phone and on the 7-inch tablet in portrait, the
list is a sheet that opens over the question, so it gets a screenshot of its own. The test uses the same
rule to decide.

### The test history

The data behind screenshots 02 to 06 is the same in every run:

- Tests 1 to 34 are finished, except test 23.
- Test 2 is failed with about 45 points: the questions worth the most points are answered wrongly
  until no more than 45 points are left.
- Tests 5, 11, 18 and 29 are failed with 7 wrong answers, on the questions worth the most points.
- All the other finished tests are passed, with 0 to 3 wrong answers on questions worth 1 point.
- Test 23 is started and not finished. Questions 1 to 19 are answered: questions 4, 9, 13 and 16
  wrongly, all others correctly. The home screen offers this test to resume.
- A wrong answer is always the answer after the correct one, so the same questions get the same
  answers in every run.
- In intersection situation 27, every question has the correct answer.

The practice screen opens on question 20, the first question with no answer, as the app does. The test
then taps the previous-question button until it shows the question for the device. It counts a device
as a tablet when its smallest width is 600 dp or more, which is the app's own limit.

The constants at the end of `StoreScreenshots.kt` set all of these numbers: `LAST_FINISHED`,
`FAILED_TESTS`, `BADLY_FAILED_TEST`, `BADLY_FAILED_SCORE`, `UNDER_WAY_TEST`, `ANSWERED_UNDER_WAY`,
`WRONG_UNDER_WAY`, `SHOWN_ON_PHONE`, `SHOWN_ON_TABLET`, `RESULTS_TEST` and `SHOWN_SITUATION`.

### The exam's language

Each language's screenshots use an exam in that same language. The test goes through the exam
selection the way a user does: it picks Slovakia, then the exam in the screenshot's language. It takes
`01-exam-selection.png` on the license group page, then picks Car (B). Screenshots 02 to 07 use the
exam that the app saved from that choice, so the app text and the questions are in the same language.

If a language in the list has no Slovak exam in that language, the run stops with an error. When the
app supports the same language for both the app and the exam, adding the language to the list is
enough.

The road sign catalog exists only in Slovak, so the sign names in `07-signs.png` are in Slovak in
every language.

## How it works

### The script

`tools/screenshots/take-screenshots.sh` and `.ps1` do the same steps:

1. Build the debug app and its instrumented test APK with Gradle.
2. For each emulator, one at a time: use it if it is running, or start it and wait until it has
   booted. Then install both APKs.
3. Put the status bar in demo mode: the clock shows 10:00, the battery is full and no notifications
   are shown. Switch off the system animations.
4. Run the `StoreScreenshots` test with `adb shell am instrument`.
5. Put the status bar and the animation settings back the way they were. This also happens when the
   test fails.
6. Pull the PNGs from the app's external files directory
   (`/sdcard/Android/data/com.spiraclestudios.autoskola.dev/files/store-screenshots/`) to
   `store-screenshots/phone/`, `store-screenshots/tablet/7-inch/` or
   `store-screenshots/tablet/10-inch/`.
7. If the script started the emulator, shut it down and wait until it has stopped.
8. After the tablets, check their screenshots against the store's requirements.

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
   attempts and answers for screens 02 to 06 (see [The test history](#the-test-history)).
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

- a file name with the next number, for example `"08-settings"`;
- a `Target` with the route of the screen and a text that is on the screen when it has loaded. Take
  the text from `language.strings`, so that the wait works in every language;
- if the screen has to change before the screenshot, a `beforeShot` that does it. `tap` taps a text
  and `tapDescribed` taps an icon button by its description, as the question list screenshot does.

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
- **`<AVD> did not boot within 5 minutes. Is it an AVD that 'emulator -list-avds' lists?`** Check the
  AVD name with `emulator -list-avds`, or give the correct name with the options above. If the name is
  correct, start the emulator yourself and run the script again. The script uses an emulator that is
  already running. Before it stops, the script shuts down the emulator that did not boot.
- **`… tablet screenshot problem(s): the store will not accept them as they are.`** A tablet AVD's
  screen is not 16:9, or its sides are outside the store's limits. Compare its `config.ini` with
  [How the tablets are set up](#how-the-tablets-are-set-up).
- **`Test … was meant to pass` (or `fail`).** The seeded test results no longer give the result the
  home screen needs, possibly after a new question bank version. Change the number of wrong answers in
  `seedProgress`.
- **`No "…" button appeared on screen to be tapped`.** The description of an icon button changed in
  `i18n/Strings.kt`, or the button is not on that layout.
