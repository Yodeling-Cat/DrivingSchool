package com.spiraclestudios.autoskola.data.analytics

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRepositoryTest {

    /** A repository over a fake sink, with the disclaimer already accepted or not. */
    private fun repository(accepted: Boolean = false): Pair<AnalyticsRepository, RecordingAnalyticsDataSource> {
        val analytics = RecordingAnalyticsDataSource()
        val preferences = PreferencesRepository(MapSettings()).apply { if (accepted) acceptDisclaimer() }
        return AnalyticsRepository(analytics, preferences) to analytics
    }

    @Test
    fun recordsNothingBeforeConsent() {
        val (analytics, sink) = repository(accepted = false)

        analytics.capture(AnalyticsEvent.OnboardingCompleted)
        analytics.screen("Home")

        assertEquals(emptyList(), sink.events)
        assertEquals(emptyList(), sink.screens)
    }

    @Test
    fun recordsAfterConsent() {
        val (analytics, sink) = repository(accepted = true)

        analytics.capture(AnalyticsEvent.ExamSelected, mapOf("bank_id" to "sk-b"))

        assertEquals(listOf(AnalyticsEvent.ExamSelected), sink.events)
        assertEquals(mapOf("bank_id" to "sk-b"), sink.properties.single())
    }

    @Test
    fun anEarlierAcceptanceStartsRecording() {
        val (analytics, sink) = repository(accepted = true)

        assertTrue(analytics.isEnabled.value)
        assertEquals(listOf(true), sink.enableCalls)
    }

    @Test
    fun acceptingTurnsRecordingOn() {
        val (analytics, sink) = repository(accepted = false)

        analytics.setEnabled(true)
        analytics.capture(AnalyticsEvent.OnboardingCompleted)

        assertEquals(listOf(false, true), sink.enableCalls)
        assertEquals(listOf(AnalyticsEvent.OnboardingCompleted), sink.events)
    }

    @Test
    fun withdrawingConsentStopsRecording() {
        val (analytics, sink) = repository(accepted = true)

        analytics.setEnabled(false)
        analytics.capture(AnalyticsEvent.ExamSelected)
        analytics.screen("Home")

        assertEquals(listOf(true, false), sink.enableCalls)
        assertEquals(emptyList(), sink.events)
        assertEquals(emptyList(), sink.screens)
    }
}
