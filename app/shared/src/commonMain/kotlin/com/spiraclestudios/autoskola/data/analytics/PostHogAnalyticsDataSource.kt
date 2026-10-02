package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.ErrorTrackingConfig
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogBeforeSend
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import com.spiraclestudios.autoskola.appVersion

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

    private var crashReporting = false
    private var analyticsEnabled = false
    private var started = false

    private val anyConsent: Boolean get() = crashReporting || analyticsEnabled

    override fun setConsent(crashReporting: Boolean, analytics: Boolean) {
        this.crashReporting = crashReporting
        this.analyticsEnabled = analytics
        if (anyConsent) {
            ensureStarted()
            // PostHog has one off switch for everything, so it is only thrown when neither answer
            // allows anything to leave. Which events actually go is decided per event, in
            // beforeSend below.
            if (started) PostHog.optIn()
        } else if (started) {
            PostHog.optOut()
        }
    }

    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (!analyticsEnabled) return
        ensureStarted()
        PostHog.capture(event.eventName, properties)
    }

    override fun screen(name: String) {
        if (!analyticsEnabled) return
        ensureStarted()
        PostHog.screen(name)
    }

    /** Starts PostHog once, and only when there is something to send with. */
    private fun ensureStarted() {
        if (started || context == null || !anyConsent) return
        started = true
        PostHog.setup(
            PostHogConfig(
                apiKey = API_KEY,
                host = PostHogConfig.HOST_EU,
                debug = debug,
                optOut = !anyConsent,
                // Nothing the SDK would send on its own: lifecycle events and page views are
                // product analytics, and they would go without passing the analytics answer.
                captureApplicationLifecycleEvents = false,
                captureScreenViews = false,
                // Crashes on every target: on the web PostHog records unhandled errors and
                // unhandled promise rejections, console errors stay off (plans/privacy-and-terms.md).
                errorTracking = ErrorTrackingConfig(
                    autoCapture = true,
                    inAppIncludes = listOf(APP_PACKAGE),
                ),
                // The consent gate, read at send time rather than at setup time. This is the only
                // way to honour a switch flipped later: PostHog.setup cannot be called twice, so
                // the crash-reporting answer cannot be baked into ErrorTrackingConfig.
                beforeSend = listOf(
                    PostHogBeforeSend { event ->
                        if (isEventAllowed(event.event, crashReporting, analyticsEnabled)) event else null
                    },
                ),
            ),
            context,
        )
        // Every event carries which build it came from, so a developer's own traffic can be
        // filtered out of the real users' insights. There is one PostHog project for both builds,
        // because the free plan allows only one.
        PostHog.register(ANALYTICS_ENVIRONMENT_PROPERTY, analyticsEnvironment(debug))
        // The platform, always, and the app version wherever the platform reports one - where it
        // does not, the property is left off rather than sent empty.
        PostHog.register(ANALYTICS_PLATFORM_PROPERTY, platformName())
        appVersion()?.let { PostHog.register(ANALYTICS_APP_VERSION_PROPERTY, it) }
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

/**
 * Every event name the app sends on purpose, named from [AnalyticsEvent] so a new event cannot be
 * added without the analytics answer covering it.
 */
internal val ANALYTICS_EVENT_NAMES: Set<String> =
    AnalyticsEvent.entries.mapTo(mutableSetOf()) { it.eventName }

/** The event name PostHog's error tracking sends for a captured exception. */
private const val EXCEPTION_EVENT = "\$exception"

/**
 * The event name PostHog sends for [AnalyticsDataSource.screen] - its own name for a screen view,
 * not one of ours, so it is listed here rather than in [ANALYTICS_EVENT_NAMES].
 */
private const val SCREEN_EVENT = "\$screen"

/**
 * Whether an event of this name is covered by one of the two answers the user gave. Every event
 * the app sends on purpose, and the screen view with them, is covered by the analytics answer; a
 * crash report is covered by the crash-reporting answer instead. Anything else is dropped: nothing
 * the SDK might send on its own may leave without the answer that covers it.
 */
internal fun isEventAllowed(event: String, crashReporting: Boolean, analytics: Boolean): Boolean = when (event) {
    EXCEPTION_EVENT -> crashReporting
    SCREEN_EVENT -> analytics
    in ANALYTICS_EVENT_NAMES -> analytics
    else -> false
}
