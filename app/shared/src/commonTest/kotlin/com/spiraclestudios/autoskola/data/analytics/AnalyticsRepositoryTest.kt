package com.spiraclestudios.autoskola.data.analytics

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun aNewEventIsGatedByTheAnalyticsAnswerToo() {
        val fixture = Fixture()

        fixture.repository.capture(AnalyticsEvent.TestCompleted, mapOf("kind" to "practice_test"))
        fixture.repository.capture(AnalyticsEvent.QuestionImageFailed)

        assertEquals(emptyList(), fixture.sink.events)
        assertEquals(emptyList(), fixture.sink.properties)
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

    @Test
    fun theIdentifierIsThereOnlyWhileSomethingMayBeSent() {
        val fixture = Fixture()
        fixture.sink.identifier = "ident-1"

        // Neither answer has been given, so there is nothing for an identifier to belong to, and
        // asking for one is not a way of starting anything: the sink is still stopped.
        assertNull(fixture.repository.deviceIdentifier.value)
        assertFalse(fixture.sink.started)
        assertEquals(listOf(false to false), fixture.sink.consents)

        fixture.preferences.setAnalyticsEnabled(true)
        fixture.repository.syncConsent()

        assertEquals("ident-1", fixture.repository.deviceIdentifier.value)
        assertTrue(fixture.sink.started)
    }

    @Test
    fun crashReportingAloneIsEnoughForThereToBeAnIdentifier() {
        // One identifier covers both: crash reports and events are filed under the same one, so a
        // user who let the crashes through has something to quote either way.
        val fixture = Fixture(crashReporting = true)
        fixture.sink.identifier = "ident-2"

        fixture.repository.refreshDeviceIdentifier()

        assertEquals("ident-2", fixture.repository.deviceIdentifier.value)
    }

    @Test
    fun theIdentifierIsRememberedSoItOutlivesTheAnswersThatMadeIt() {
        val fixture = Fixture(analytics = true)
        fixture.sink.identifier = "ident-1"
        fixture.repository.refreshDeviceIdentifier()
        assertEquals("ident-1", fixture.repository.deviceIdentifier.value)

        // Both answers off and a new process: the SDK is stopped and reports nothing, so what the
        // user is shown is what was remembered rather than nothing at all. This is the state a user
        // asking us to delete what we hold is most likely to be in.
        fixture.preferences.setAnalyticsEnabled(false)
        fixture.repository.syncConsent()
        fixture.sink.identifier = null
        val restarted = AnalyticsRepository(fixture.sink, fixture.preferences)

        assertEquals("ident-1", restarted.deviceIdentifier.value)
        assertEquals("ident-1", fixture.preferences.preferences.value.deviceIdentifier)
    }

    @Test
    fun aNewIdentifierReplacesTheOneThatWasRemembered() {
        // What a reset looks like from here: the SDK has a different one, and the remembered value
        // follows it rather than keeping the old identifier on screen.
        val fixture = Fixture(analytics = true)
        fixture.sink.identifier = "ident-1"
        fixture.repository.refreshDeviceIdentifier()

        fixture.sink.identifier = "ident-2"
        fixture.repository.refreshDeviceIdentifier()

        assertEquals("ident-2", fixture.repository.deviceIdentifier.value)
        assertEquals("ident-2", fixture.preferences.preferences.value.deviceIdentifier)
    }
}
