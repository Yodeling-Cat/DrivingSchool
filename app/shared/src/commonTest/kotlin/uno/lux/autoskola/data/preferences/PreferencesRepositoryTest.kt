package uno.lux.autoskola.data.preferences

import com.russhwolf.settings.MapSettings
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.i18n.AppLanguage
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
            assertFalse(preferences.value.checkAnswersImmediately)
            selectLanguage(AppLanguage.Hungarian)
            setCheckAnswersImmediately(true)
        }

        val restored = PreferencesRepository(settings).preferences.value
        assertEquals(AppLanguage.Hungarian, restored.language)
        assertTrue(restored.checkAnswersImmediately)
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
