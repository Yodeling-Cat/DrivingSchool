package com.spiraclestudios.autoskola

import platform.Foundation.NSBundle

/** The marketing version in the app's `Info.plist`, which the Xcode project sets. */
actual fun appVersion(): String? =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
