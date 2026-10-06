package com.spiraclestudios.autoskola.data.analytics

import com.spiraclestudios.autoskola.data.preferences.ConsentAge
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
    private val identifier = MutableStateFlow<String?>(preferences.preferences.value.deviceIdentifier)

    /**
     * The identifier our events are filed under, or `null` while there is none.
     *
     * A flow rather than a call, because its value arrives from two places: the SDK, which has one
     * only while it is running, and the preferences, where it is remembered so the row in Settings
     * outlives a restart with both answers off. Declared after the backing flow, which it reads.
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
            crashReporting = maySend(preferences.preferences.value.crashReportingEnabled),
            analytics = maySend(preferences.preferences.value.analyticsEnabled),
        )
        if (consent == pushed) return
        pushed = consent
        analytics.setConsent(consent.crashReporting, consent.analytics)
        refreshDeviceIdentifier()
    }

    fun capture(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (!maySend(preferences.preferences.value.analyticsEnabled)) return
        analytics.capture(event, properties)
    }

    fun screen(name: String) {
        if (!maySend(preferences.preferences.value.analyticsEnabled)) return
        analytics.screen(name)
    }

    /**
     * A stored answer as it takes effect: what the user asked for, and nothing more than their age
     * answer allows (see [ConsentAge]). Under 16 without a parent or guardian agreeing, the switches
     * are not theirs to answer, and this is what holds that against a value already stored — from the
     * version before the app asked, or from anything that wrote one without asking the user.
     */
    private fun maySend(requested: Boolean): Boolean {
        val current = preferences.preferences.value
        return ConsentAge.effective(requested, current.overConsentAge, current.parentalConsent)
    }

    /**
     * Asks the data source what the identifier is now, remembers it, and publishes it.
     *
     * Three cases, and the remembered value is what makes the second and third one work:
     *
     *  - the SDK is running and reports one: it is stored (only ever while an answer allows sending,
     *    since that is the only state in which the SDK has one to report) and published;
     *  - the SDK is running and reports none, e.g. this device has never sent anything: whatever was
     *    remembered stands, because nothing has said it is wrong;
     *  - the SDK is stopped, which is a restart with both answers off: the remembered one is
     *    published, so the row in Settings still shows the user what their earlier data is filed
     *    under — the state in which they are most likely to be asking us about it.
     *
     * Reading the data source never starts anything (see [AnalyticsDataSource.deviceIdentifier]), so
     * this is safe to call on a screen that is only being looked at.
     */
    fun refreshDeviceIdentifier() {
        val live = analytics.deviceIdentifier()
        if (live != null && live != preferences.preferences.value.deviceIdentifier) {
            preferences.setDeviceIdentifier(live)
        }
        val current = live ?: preferences.preferences.value.deviceIdentifier
        if (identifier.value != current) identifier.value = current
    }

    private data class Consent(val crashReporting: Boolean, val analytics: Boolean)
}
