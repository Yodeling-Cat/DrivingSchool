package com.spiraclestudios.autoskola.data.analytics

import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository

/**
 * The rules about what may leave the device. Two questions with two answers — crash reports and
 * usage analytics are asked for separately and both start off (see `plans/privacy-and-terms.md`).
 *
 * The gate reads [PreferencesRepository] on every call rather than caching an answer, so a switch
 * in Settings takes effect on the next event whichever screen changes it. [syncConsent] exists
 * because the data source also has to start or stop the SDK, which is not something an event can
 * trigger on its own.
 *
 * Nothing is derived from analytics and nothing waits on it, so every method is fire-and-forget.
 */
class AnalyticsRepository(private val analytics: AnalyticsDataSource, private val preferences: PreferencesRepository) {

    /** What the data source was last told, so a change is pushed once rather than on every event. */
    private var pushed: Consent? = null

    init {
        // The user may have answered on an earlier run.
        syncConsent()
    }

    /**
     * Hands the current answers to the data source, which starts or stops the SDK with them. The
     * ViewModels that change a consent call this, so a switch stops the SDK straight away rather
     * than at the next event.
     */
    fun syncConsent() {
        val consent = Consent(
            crashReporting = preferences.preferences.value.crashReportingEnabled,
            analytics = preferences.preferences.value.analyticsEnabled,
        )
        if (consent == pushed) return
        pushed = consent
        analytics.setConsent(consent.crashReporting, consent.analytics)
    }

    fun capture(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (!preferences.preferences.value.analyticsEnabled) return
        analytics.capture(event, properties)
    }

    fun screen(name: String) {
        if (!preferences.preferences.value.analyticsEnabled) return
        analytics.screen(name)
    }

    private data class Consent(val crashReporting: Boolean, val analytics: Boolean)
}
