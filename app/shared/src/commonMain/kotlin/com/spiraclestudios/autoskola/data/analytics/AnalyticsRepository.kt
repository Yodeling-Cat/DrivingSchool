package com.spiraclestudios.autoskola.data.analytics

import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /**
     * The identifier our events are filed under, or `null` while there is none. The backing flow is
     * declared first, because the public one reads it where it is built.
     */
    private val identifier = MutableStateFlow<String?>(null)

    /**
     * The identifier our events are filed under, or `null` while there is none.
     *
     * A flow rather than a call, because the identifier exists only while the SDK is running: it
     * appears when one of the two answers is given, whether or not a screen is open at that moment.
     * [refreshDeviceIdentifier] is for the screen that is about to show it.
     */
    val deviceIdentifier: StateFlow<String?> = identifier.asStateFlow()

    init {
        // The user may have answered on an earlier run.
        syncConsent()
        refreshDeviceIdentifier()
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
        refreshDeviceIdentifier()
    }

    fun capture(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (!preferences.preferences.value.analyticsEnabled) return
        analytics.capture(event, properties)
    }

    fun screen(name: String) {
        if (!preferences.preferences.value.analyticsEnabled) return
        analytics.screen(name)
    }

    /**
     * Asks the data source what the identifier is now, and publishes it. Reading it never starts
     * anything (see [AnalyticsDataSource.deviceIdentifier]), so this is safe to call on a screen that
     * is only being looked at — and it is what picks the identifier up when the SDK has one to
     * report that it did not a moment ago.
     */
    fun refreshDeviceIdentifier() {
        val current = analytics.deviceIdentifier()
        if (identifier.value != current) identifier.value = current
    }

    private data class Consent(val crashReporting: Boolean, val analytics: Boolean)
}
