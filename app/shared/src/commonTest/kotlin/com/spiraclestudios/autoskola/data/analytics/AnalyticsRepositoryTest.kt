package com.spiraclestudios.autoskola.data.analytics

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsRepositoryTest {

    /** A repository over a fake sink, with the two answers already given or not. */
    private class Fixture(crashReporting: Boolean = false, analytics: Boolean = false) {
        val sink = RecordingAnalyticsDataSource()
        val preferences = PreferencesRepository(MapSettings()).apply {
            setCrashReportingEnabled(crashReporting)
            setAnalyticsEnabled(analytics)
        }
        val repository = AnalyticsRepository(sink, preferences)
    }

    @Test
    fun sendsNothingUntilOneOfTheTwoQuestionsIsAnswered() {
        val fixture = Fixture()

        fixture.repository.capture(AnalyticsEvent.OnboardingCompleted)
        fixture.repository.screen("Home")

        assertEquals(emptyList(), fixture.sink.events)
        assertEquals(emptyList(), fixture.sink.screens)
        assertEquals(listOf(false to false), fixture.sink.consents)
    }

    @Test
    fun crashReportingAloneSendsNoEvents() {
        val fixture = Fixture(crashReporting = true)

        fixture.repository.capture(AnalyticsEvent.ExamSelected)
        fixture.repository.screen("Home")

        assertEquals(emptyList(), fixture.sink.events)
        assertEquals(emptyList(), fixture.sink.screens)
        // The data source is still told, because crash reports have to keep flowing on their own.
        assertEquals(listOf(true to false), fixture.sink.consents)
    }

    @Test
    fun analyticsAloneSendsEvents() {
        val fixture = Fixture(analytics = true)

        fixture.repository.capture(AnalyticsEvent.ExamSelected, mapOf("bank_id" to "sk-b"))

        assertEquals(listOf(AnalyticsEvent.ExamSelected), fixture.sink.events)
        assertEquals(mapOf("bank_id" to "sk-b"), fixture.sink.properties.single())
        assertEquals(listOf(false to true), fixture.sink.consents)
    }

    @Test
    fun anUnchangedAnswerIsNotPushedAgain() {
        val fixture = Fixture(analytics = true)

        fixture.repository.syncConsent()
        fixture.repository.syncConsent()

        assertEquals(listOf(false to true), fixture.sink.consents)
    }

    @Test
    fun withdrawingAnalyticsStopsEventsAndTellsTheSdk() {
        val fixture = Fixture(crashReporting = true, analytics = true)

        fixture.repository.capture(AnalyticsEvent.ExamSelected)
        assertEquals(1, fixture.sink.events.size)

        // A screen that flips a switch writes the preference first, then asks for the push.
        fixture.preferences.setAnalyticsEnabled(false)
        fixture.repository.syncConsent()

        // Crash reporting is untouched, so the SDK is told the new pair rather than switched off.
        assertEquals(listOf(true to true, true to false), fixture.sink.consents)

        fixture.repository.capture(AnalyticsEvent.ExamSelected)
        assertEquals(1, fixture.sink.events.size)
    }
}
