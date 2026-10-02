package com.spiraclestudios.autoskola

import android.app.Application

private var installedApplication: Application? = null

/**
 * Hands the shared module the application, and is called from the Android entry point before the
 * first frame. There is no other way to get one here: this module is a library with no activity of
 * its own, and it registers no `ContentProvider`.
 *
 * Until it is called — in a host test, or in a Compose preview rendered without a screen around it
 * — [applicationContext] answers `null`.
 */
fun provideApplication(application: Application) {
    installedApplication = application
}

/** The application, or `null` where one was never installed. Nothing here may assume it exists. */
fun applicationContext(): Application? = installedApplication
