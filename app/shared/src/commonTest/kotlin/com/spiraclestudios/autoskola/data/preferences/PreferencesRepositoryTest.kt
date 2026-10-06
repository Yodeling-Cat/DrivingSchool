package com.spiraclestudios.autoskola.data.preferences

import com.russhwolf.settings.MapSettings
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencesRepositoryTest {

    @Test
    fun onboardingStateIsEmptyOnFirstLaunch() {
        val preferences = PreferencesRepository(MapSettings()).preferences.value
        assertEquals(0, preferences.legalAcceptedVersion)
        assertFalse(preferences.crashReportingEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertNull(preferences.selection)
    }

    @Test
    fun onboardingChoicesSurviveRestart() {
        val settings = MapSettings()
        PreferencesRepository(settings).apply {
            acceptLegal(CURRENT_LEGAL_VERSION)
            select(TestSelection("sk-hu", LicenseCategory.CE))
        }

        val restored = PreferencesRepository(settings).preferences.value
        assertEquals(CURRENT_LEGAL_VERSION, restored.legalAcceptedVersion)
        assertEquals(TestSelection("sk-hu", LicenseCategory.CE), restored.selection)
    }

    @Test
    fun theTwoSharingAnswersAreSeparateAndSurviveRestart() {
        val settings = MapSettings()
        PreferencesRepository(settings).setCrashReportingEnabled(true)

        val restored = PreferencesRepository(settings).preferences.value
        assertTrue(restored.crashReportingEnabled)
        assertFalse(restored.analyticsEnabled)
    }

    @Test
    fun settingsSurviveRestart() {
        val settings = MapSettings()
        PreferencesRepository(settings).apply {
            assertTrue(preferences.value.checkAnswersImmediately)
            selectLanguage(AppLanguage.Hungarian)
            setCheckAnswersImmediately(false)
        }

        val restored = PreferencesRepository(settings).preferences.value
        assertEquals(AppLanguage.Hungarian, restored.language)
        assertFalse(restored.checkAnswersImmediately)
    }

    @Test
    fun autoAdvanceIsOffWhileAnswersAreCheckedImmediately() {
        val settings = MapSettings()
        val repository = PreferencesRepository(settings)

        // Checking answers as they're picked is the default, and it rules auto-advance out.
        repository.setAdvanceAfterAnswer(true)
        assertFalse(repository.preferences.value.advanceAfterAnswer)
        assertFalse(PreferencesRepository(settings).preferences.value.advanceAfterAnswer)

        repository.setCheckAnswersImmediately(false)
        repository.setAdvanceAfterAnswer(true)
        assertTrue(repository.preferences.value.advanceAfterAnswer)
        assertTrue(PreferencesRepository(settings).preferences.value.advanceAfterAnswer)

        // Turning checking back on forces it off, and off is what's stored.
        repository.setCheckAnswersImmediately(true)
        assertFalse(repository.preferences.value.advanceAfterAnswer)
        assertFalse(PreferencesRepository(settings).preferences.value.advanceAfterAnswer)
    }

    @Test
    fun changesAreEmittedImmediately() {
        val repository = PreferencesRepository(MapSettings())
        repository.acceptLegal(CURRENT_LEGAL_VERSION)
        repository.selectLanguage(AppLanguage.Czech)

        assertEquals(CURRENT_LEGAL_VERSION, repository.preferences.value.legalAcceptedVersion)
        assertEquals(AppLanguage.Czech, repository.preferences.value.language)
    }

    @Test
    fun adsAreGenericUntilTheUserSaysOtherwise() {
        val settings = MapSettings()
        val repository = PreferencesRepository(settings)

        // Personalizing is a use of personal data, so this answer starts as no and has to be given -
        // it is not a default the user was never asked about. It is its own answer, like the other
        // two consents.
        assertFalse(repository.preferences.value.personalizedAdsEnabled)

        repository.setPersonalizedAdsEnabled(true)
        assertTrue(repository.preferences.value.personalizedAdsEnabled)
        assertTrue(PreferencesRepository(settings).preferences.value.personalizedAdsEnabled)

        // And it can be taken back at any time, which is what the Settings switch does.
        repository.setPersonalizedAdsEnabled(false)
        assertFalse(repository.preferences.value.personalizedAdsEnabled)
        assertFalse(PreferencesRepository(settings).preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun anOldYesToShowingAdsIsNotAYesToPersonalizingThem() {
        // Stored by the version in which this switch decided whether ads were shown at all. A yes to
        // that question was never an answer to this one, so it is left unread rather than reinterpreted.
        val settings = MapSettings("ads_enabled" to true)

        assertFalse(PreferencesRepository(settings).preferences.value.personalizedAdsEnabled)
    }

    @Test
    fun thePersonalizedAdsAnswerIsSeparateFromTheOtherTwo() {
        val repository = PreferencesRepository(MapSettings())
        repository.setCrashReportingEnabled(false)
        repository.setAnalyticsEnabled(false)
        repository.setPersonalizedAdsEnabled(true)

        val preferences = repository.preferences.value
        assertTrue(preferences.personalizedAdsEnabled)
        assertFalse(preferences.analyticsEnabled)
        assertFalse(preferences.crashReportingEnabled)
    }

    @Test
    fun passThresholdIsNinetyPercent() {
        assertTrue(TestResult(score = 90, maxScore = 100).passed)
        assertFalse(TestResult(score = 89, maxScore = 100).passed)
        assertFalse(TestResult(score = 0, maxScore = 0).passed)
    }

    @Test
    fun theDeviceIdentifierSurvivesRestart() {
        val settings = MapSettings()
        // Nothing is filed under an identifier until something has been sent, so there is none.
        assertNull(PreferencesRepository(settings).preferences.value.deviceIdentifier)

        PreferencesRepository(settings).setDeviceIdentifier("ident-1")

        // And it stays after the switches that produced it are off: it is the user's only way to ask
        // us about what was already filed under it.
        assertEquals("ident-1", PreferencesRepository(settings).preferences.value.deviceIdentifier)
    }

    // ── The age answer ──

    @Test
    fun theAgeAnswerAndTheAgreementSurviveRestart() {
        val settings = MapSettings()
        assertNull(PreferencesRepository(settings).preferences.value.overConsentAge)
        assertFalse(PreferencesRepository(settings).preferences.value.parentalConsent)

        PreferencesRepository(settings).apply {
            setParentalConsent(true)
            setOverConsentAge(false)
        }

        val restarted = PreferencesRepository(settings).preferences.value
        assertEquals(false, restarted.overConsentAge)
        assertTrue(restarted.parentalConsent)
    }

    @Test
    fun answeringTheAgeQuestionLeavesTheThreeStoredAnswersAlone() {
        val preferences = PreferencesRepository(MapSettings())
        preferences.setCrashReportingEnabled(true)
        preferences.setAnalyticsEnabled(true)
        preferences.setPersonalizedAdsEnabled(true)

        preferences.setOverConsentAge(false)

        // The answers are the user's and the age answer is not one of them: it decides whether they may
        // be given and whether they count (`ConsentAge`), never what they say.
        assertTrue(preferences.preferences.value.crashReportingEnabled)
        assertTrue(preferences.preferences.value.analyticsEnabled)
        assertTrue(preferences.preferences.value.personalizedAdsEnabled)
        assertFalse(ConsentAge.mayAnswerSwitches(overConsentAge = false, parentalConsent = false))
    }

    @Test
    fun anAgreementWrittenAfterwardsDoesNotTurnThemOn() {
        val preferences = PreferencesRepository(MapSettings())
        preferences.setOverConsentAge(false)

        preferences.setParentalConsent(true)

        // The agreement is what lets the user answer; it is not an answer itself.
        assertFalse(preferences.preferences.value.personalizedAdsEnabled)
        assertTrue(ConsentAge.mayAnswerSwitches(overConsentAge = false, parentalConsent = true))
    }

    @Test
    fun theAnswerCanBeRemovedAgain() {
        val preferences = PreferencesRepository(MapSettings())
        preferences.setOverConsentAge(true)

        preferences.setOverConsentAge(null)

        assertNull(preferences.preferences.value.overConsentAge)
        assertFalse(ConsentAge.mayAnswerSwitches(overConsentAge = null, parentalConsent = false))
    }
}
