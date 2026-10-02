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
        assertFalse(preferences.disclaimerAccepted)
        assertNull(preferences.selection)
    }

    @Test
    fun onboardingChoicesSurviveRestart() {
        val settings = MapSettings()
        PreferencesRepository(settings).apply {
            acceptDisclaimer()
            select(TestSelection("sk-hu", LicenseCategory.CE))
        }

        val restored = PreferencesRepository(settings).preferences.value
        assertTrue(restored.disclaimerAccepted)
        assertEquals(TestSelection("sk-hu", LicenseCategory.CE), restored.selection)
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
        repository.acceptDisclaimer()
        repository.selectLanguage(AppLanguage.Czech)

        assertTrue(repository.preferences.value.disclaimerAccepted)
        assertEquals(AppLanguage.Czech, repository.preferences.value.language)
    }

    @Test
    fun passThresholdIsNinetyPercent() {
        assertTrue(TestResult(score = 90, maxScore = 100).passed)
        assertFalse(TestResult(score = 89, maxScore = 100).passed)
        assertFalse(TestResult(score = 0, maxScore = 0).passed)
    }
}
