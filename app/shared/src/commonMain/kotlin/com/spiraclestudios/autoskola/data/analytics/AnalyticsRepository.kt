package com.spiraclestudios.autoskola.data.analytics

import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The rule about what may leave the device: nothing, until the user has accepted the disclaimer
 * and the terms. See `plans/privacy-and-terms.md`.
 *
 * The gate sits here and not only inside the SDK, so that the answer is the same for a fake as it
 * is for PostHog, and so a test can assert it: `AnalyticsRepositoryTest` checks that no event is
 * recorded before consent, whatever [AnalyticsDataSource] is behind this.
 *
 * Nothing is derived from analytics and nothing waits on it, so every method is fire-and-forget.
 */
class AnalyticsRepository(private val analytics: AnalyticsDataSource, private val preferences: PreferencesRepository) {
    private val enabled = MutableStateFlow(preferences.preferences.value.disclaimerAccepted)

    /** Whether events are currently being recorded, for a settings screen to show. */
    val isEnabled: StateFlow<Boolean> = enabled.asStateFlow()

    init {
        // The user may already have accepted on an earlier run.
        analytics.setEnabled(enabled.value)
    }

    /** Called when the user accepts the terms, or later withdraws consent. */
    fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
        analytics.setEnabled(enabled)
    }

    fun capture(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (!enabled.value) return
        analytics.capture(event, properties)
    }

    fun screen(name: String) {
        if (!enabled.value) return
        analytics.screen(name)
    }
}
