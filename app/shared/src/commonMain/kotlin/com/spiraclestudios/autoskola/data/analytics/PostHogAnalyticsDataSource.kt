package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.ErrorTrackingConfig
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext

/**
 * Sends events and crashes to PostHog Cloud (EU).
 *
 * The SDK is started by the first thing we actually send, not when this object is built: a user
 * who has not accepted the terms must not have a connection opened on their behalf, and error
 * autocapture is part of what `setup` switches on alongside it.
 *
 * [context] is `null` where no platform value can be built — a host test, a Compose preview, the
 * Android host test that never installs an application — and this data source then stays inert
 * instead of failing.
 */
class PostHogAnalyticsDataSource(private val context: PostHogContext?, private val debug: Boolean = false) :
    AnalyticsDataSource {

    private var enabled = false
    private var started = false

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            ensureStarted()
        } else if (started) {
            PostHog.optOut()
        }
    }

    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (!enabled) return
        ensureStarted()
        PostHog.capture(event.eventName, properties)
    }

    override fun screen(name: String) {
        if (!enabled) return
        ensureStarted()
        PostHog.screen(name)
    }

    /** Starts PostHog once, and only when there is something to send with. */
    private fun ensureStarted() {
        if (started || context == null) return
        started = true
        PostHog.setup(
            PostHogConfig(
                apiKey = API_KEY,
                host = PostHogConfig.HOST_EU,
                debug = debug,
                // Consent decides this, not the SDK default, so a run that starts before the
                // terms are accepted queues nothing.
                optOut = !enabled,
                captureApplicationLifecycleEvents = true,
                // Crashes on every target: on the web PostHog records unhandled errors and
                // unhandled promise rejections, console errors stay off (plans/privacy-and-terms.md).
                errorTracking = ErrorTrackingConfig(
                    autoCapture = true,
                    inAppIncludes = listOf(APP_PACKAGE),
                ),
            ),
            context,
        )
    }

    private companion object {
        /**
         * A PostHog project token is a public, write-only ingestion key: it has to ship inside the
         * app for the app to send anything, and it cannot be used to read data back out. This is
         * what PostHog's own setup instructions show, and it is the reason it lives in the source
         * rather than in a build secret.
         */
        const val API_KEY = "phc_n7PQCT2Y6esab4DaF7AYyq3BXtb7RCP7CbS9XdnFivHx"

        const val APP_PACKAGE = "com.spiraclestudios.autoskola"
    }
}
