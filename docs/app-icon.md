# App icon

`assets/design/app-icon/` holds the design sources for the launcher icons, one directory per target: the Android adaptive set (including the 512 the Play listing uses), an iOS `AppIcon` set and the web icons. Nothing in the build reads it — each platform has its own copy in its own format, under `app/androidApp/src/main/res/`, `app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` and `app/webApp/src/webMain/resources/` (with `manifest.webmanifest`), plus `in-app/rail_mark.png` as `composeResources/drawable/rail_mark.png`, the book without its tile at the top of the navigation rail — so it is not a resource to wire up: it is where an icon change starts.

The icon follows the system's colors where the system offers it: Android 13+ themed icons draw the monochrome layer in the wallpaper's colors, and iOS 18 swaps in a dark and a tinted icon. Those variants are derived, not exported: after replacing the sources, run `python tools/app-icon/derive_variants.py`, which rewrites the Android `ic_launcher_monochrome.png`s (the export's are a full-color copy, which a themed icon draws as one flat shape) and writes `ios/AppIcon-dark.png`, `ios/AppIcon-tinted.png` and `in-app/rail_mark.png`. Then copy across by hand:

- **Android:** `android/res/` into `app/androidApp/src/main/res/` as it is.
- **iOS:** the app uses the single-size set (iOS 18.2 is the deployment target, and Xcode makes every size from it), not the export's per-size files: `AppIcon~ios-marketing.png` becomes `AppIcon.png`, flattened to RGB, because App Store Connect rejects an icon with an alpha channel even when every pixel is opaque; the dark and tinted files go in beside it.
- **Web:** the files in `web/`. A browser has no way to recolor a site's icon.
