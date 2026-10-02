package com.spiraclestudios.autoskola

/**
 * The App Store page, which is addressed by the numeric id App Store Connect gives the app's
 * record. The app has no record yet, so nothing can be filled in here and there is no page to
 * open: iOS answers `null`, and the home screen leaves the rating card off rather than sending the
 * driver to an address that shows nothing. Filling in the id is what turns it on.
 */
private const val APP_STORE_ID = ""

actual val storeListing: StoreListing? = APP_STORE_ID.takeIf { it.isNotBlank() }
    ?.let { StoreListing(name = "App Store", url = "https://apps.apple.com/app/id$it") }
