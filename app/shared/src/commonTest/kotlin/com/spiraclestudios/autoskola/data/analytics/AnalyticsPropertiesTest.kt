package com.spiraclestudios.autoskola.data.analytics

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.stats.AttemptKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsPropertiesTest {

    @Test
    fun everyAttemptKindHasAWireName() {
        assertEquals("practice_test", AttemptKind.PracticeTest.wireName())
        assertEquals("mistakes", AttemptKind.Mistakes.wireName())
        assertEquals("situations", AttemptKind.Situations.wireName())
    }

    @Test
    fun aCompletedPracticeTestCarriesWhetherItPassed() {
        val properties = testCompletedProperties(AttemptKind.PracticeTest, 300L, wrongCount = 4, passed = false)

        assertEquals(
            mapOf(
                "kind" to "practice_test",
                "duration_seconds" to 300L,
                "wrong_count" to 4,
                "passed" to false,
            ),
            properties,
        )
    }

    @Test
    fun aCompletedMistakesRetestCarriesWhetherItPassed() {
        val properties = testCompletedProperties(AttemptKind.Mistakes, 120L, wrongCount = 0, passed = true)

        assertEquals(true, properties["passed"])
        assertEquals("mistakes", properties["kind"])
    }

    @Test
    fun aCompletedSituationsDrillCarriesNoVerdictAtAll() {
        // A drill is neither passed nor failed. Sending passed = false would read in the dashboard
        // as a drill that always fails, so the property is left out rather than sent false.
        val properties = testCompletedProperties(AttemptKind.Situations, 90L, wrongCount = 2, passed = false)

        assertFalse("passed" in properties)
        assertEquals(
            mapOf(
                "kind" to "situations",
                "duration_seconds" to 90L,
                "wrong_count" to 2,
            ),
            properties,
        )
    }

    @Test
    fun aStartedTestCarriesItsKindAndWhetherItWasContinued() {
        assertEquals(
            mapOf("kind" to "mistakes", "resumed" to true),
            testStartedProperties(AttemptKind.Mistakes, resumed = true),
        )
    }

    @Test
    fun anAbandonedTestCarriesWhatWasDoneInHowLong() {
        assertEquals(
            mapOf("kind" to "practice_test", "answers_given" to 7, "duration_seconds" to 45L),
            testAbandonedProperties(AttemptKind.PracticeTest, answersGiven = 7, durationSeconds = 45L),
        )
    }

    @Test
    fun theConsentEventsCarryTheirChoicesAndAnswers() {
        assertEquals(
            mapOf(
                "crash_reporting" to true,
                "analytics" to true,
                "personalized_ads" to true,
                "choice" to "accept_all",
            ),
            consentAnsweredProperties(true, true, personalizedAds = true, choice = CONSENT_CHOICE_ACCEPT_ALL),
        )
        assertEquals(
            mapOf("switch" to "analytics", "enabled" to false),
            consentChangedProperties(CONSENT_SWITCH_ANALYTICS, enabled = false),
        )
    }

    @Test
    fun everyEventTheAppSendsIsCoveredByTheAnalyticsAnswer() {
        // isEventAllowed drops anything it does not recognize, so an event added without being
        // listed here would silently stop being sent. This is that check.
        AnalyticsEvent.entries.forEach { event ->
            assertTrue(
                isEventAllowed(event.eventName, crashReporting = false, analytics = true),
                "${event.eventName} is not covered by the analytics answer",
            )
            assertFalse(
                isEventAllowed(event.eventName, crashReporting = false, analytics = false),
                "${event.eventName} is sent with the analytics answer off",
            )
        }
    }

    @Test
    fun aCrashReportGoesByTheCrashReportingAnswerAndAScreenViewByAnalytics() {
        assertTrue(isEventAllowed("\$exception", crashReporting = true, analytics = false))
        assertFalse(isEventAllowed("\$exception", crashReporting = false, analytics = true))
        assertTrue(isEventAllowed("\$screen", crashReporting = false, analytics = true))
        assertFalse(isEventAllowed("\$screen", crashReporting = true, analytics = false))
    }

    @Test
    fun anUnknownEventIsNeverSent() {
        assertFalse(isEventAllowed("something_the_sdk_might_send", crashReporting = true, analytics = true))
    }

    /** A repository over a recording sink, with analytics answered. */
    private fun answeredRepository(): Pair<RecordingAnalyticsDataSource, AnalyticsRepository> {
        val sink = RecordingAnalyticsDataSource()
        val preferences = PreferencesRepository(MapSettings()).apply {
            // An adult: the age question decides whether the analytics switch may be answered at all.
            setOverConsentAge(true)
            setAnalyticsEnabled(true)
        }
        return sink to AnalyticsRepository(sink, preferences)
    }

    @Test
    fun pickingADifferentLanguageIsRecordedWithItsCode() {
        val (sink, repository) = answeredRepository()

        repository.languageChanged(from = AppLanguage.English, to = AppLanguage.Slovak)

        assertEquals(listOf(AnalyticsEvent.LanguageChanged), sink.events)
        assertEquals(mapOf("to" to "sk"), sink.properties.single())
    }

    @Test
    fun pickingTheLanguageAlreadyInUseIsNotAChange() {
        val (sink, repository) = answeredRepository()

        repository.languageChanged(from = AppLanguage.Czech, to = AppLanguage.Czech)

        assertEquals(emptyList(), sink.events)
    }
}
