package com.spiraclestudios.autoskola.data.analytics

import android.app.Application
import com.posthog.kmp.PostHogContext

private var installedApplication: Application? = null

/**
 * Hands the shared module the application PostHog's Android context needs, and is called from the
 * Android entry point before the first frame. There is no other way to get one here: the shared
 * module is a library with no activity of its own, and it registers no `ContentProvider`.
 *
 * Until it is called — in a host test, or in a Compose preview rendered without a screen around
 * it — [postHogContext] answers `null` and analytics stays off.
 */
fun provideApplication(application: Application) {
    installedApplication = application
}

actual fun postHogContext(): PostHogContext? = installedApplication?.let { PostHogContext(it) }
