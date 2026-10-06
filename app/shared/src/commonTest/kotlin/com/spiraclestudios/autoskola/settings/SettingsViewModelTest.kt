package com.spiraclestudios.autoskola.settings

import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories().apply {
        preferences.select(TestSelection("sk-sk", LicenseCategory.C))
        // An adult, because the three switches can only be answered by one — or by an under-16 whose
        // parent or guardian agreed. The locked state has its own tests below.
        preferences.setOverConsentAge(true)
    }
    private val viewModel by lazy {
        SettingsViewModel(repositories.preferences, repositories.exams, repositories.analytics, repositories.ads)
    }

    @Test
    fun theThreeAnswersCanBeChangedHere() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))

        assertEquals(true, state.value.crashReportingEnabled)
        assertEquals(false, state.value.analyticsEnabled)
        assertEquals(false, state.value.adsEnabled)
        assertEquals(true, repositories.preferences.preferences.value.crashReportingEnabled)
    }

    @Test
    fun theAdsAnswerBelongsToSettingsToo() = runTest {
        val state = observe(viewModel.uiState)

        // Off, because it starts off and this is a fresh install. The row is what lets a user who
        // said no in onboarding change their mind without reinstalling.
        assertFalse(state.value.adsEnabled)

        viewModel.onEvent(SettingsUiEvent.SetAds(true))

        assertTrue(state.value.adsEnabled)
        assertTrue(repositories.preferences.preferences.value.adsEnabled)

        viewModel.onEvent(SettingsUiEvent.SetAds(false))

        assertFalse(state.value.adsEnabled)
        assertFalse(repositories.preferences.preferences.value.adsEnabled)
    }

    @Test
    fun changingTheAdsSwitchRecordsTheChange() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()

        viewModel.onEvent(SettingsUiEvent.SetAds(true))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "ads", "enabled" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun theAdsAnswerIsNotOnePostHogIsTold() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        val before = repositories.analyticsSink.consents.size

        viewModel.onEvent(SettingsUiEvent.SetAds(true))

        // `syncConsent` is what tells PostHog which of its own switches are on, so it is called for
        // the two answers that gate what PostHog may collect and not for this one: the ad SDK reads
        // the preference itself.
        assertEquals(before, repositories.analyticsSink.consents.size)
    }

    @Test
    fun showsTheCurrentChoices() = runTest {
        val state = observe(viewModel.uiState).value

        assertEquals(ExamLabel(LicenseCategory.C, "sk"), state.exam)
        assertTrue(state.checkAnswersImmediately)
    }

    @Test
    fun changesAreSaved() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(AppLanguage.Hungarian))
        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(false))

        assertEquals(AppLanguage.Hungarian, state.value.language)
        assertFalse(state.value.checkAnswersImmediately)
        assertFalse(repositories.preferences.preferences.value.checkAnswersImmediately)
    }

    @Test
    fun autoAdvanceIsSavedAndOnlyWhileAnswersArentCheckedImmediately() = runTest {
        val state = observe(viewModel.uiState)

        // Checked answers are the default, and they rule auto-advance out.
        assertFalse(state.value.advanceAfterAnswer)
        assertFalse(state.value.canAdvanceAfterAnswer)

        viewModel.onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(true))
        assertFalse(state.value.advanceAfterAnswer)

        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(false))
        assertTrue(state.value.canAdvanceAfterAnswer)
        viewModel.onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(true))
        assertTrue(state.value.advanceAfterAnswer)
        assertTrue(repositories.preferences.preferences.value.advanceAfterAnswer)

        // Checking answers as they're picked turns it back off.
        viewModel.onEvent(SettingsUiEvent.SetCheckAnswersImmediately(true))
        assertFalse(state.value.advanceAfterAnswer)
        assertFalse(repositories.preferences.preferences.value.advanceAfterAnswer)
        assertFalse(state.value.canAdvanceAfterAnswer)
    }

    @Test
    fun changingAConsentSwitchRecordsTheChange() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetCrashReporting(true))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "crash_reporting", "enabled" to true),
            repositories.analyticsSink.properties.single(),
        )
    }

    @Test
    fun turningAnalyticsOffRecordsTheChangeTheUserJustMade() = runTest {
        // The answer in force when the switch is pressed is still "on", so the withdrawal is
        // recorded; the preference is then written, and nothing more is sent from then on.
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()
        repositories.analyticsSink.properties.clear()

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(false))

        assertEquals(listOf(AnalyticsEvent.ConsentChanged), repositories.analyticsSink.events)
        assertEquals(
            mapOf("switch" to "analytics", "enabled" to false),
            repositories.analyticsSink.properties.single(),
        )
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun settingASwitchToWhatItAlreadyIsRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        repositories.analyticsSink.events.clear()

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun pickingADifferentLanguageRecordsIt() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(AppLanguage.Hungarian))

        assertEquals(listOf(AnalyticsEvent.LanguageChanged), repositories.analyticsSink.events)
        assertEquals(mapOf("to" to "hu"), repositories.analyticsSink.properties.single())
    }

    @Test
    fun pickingTheLanguageAlreadyInUseRecordsNothing() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        observe(viewModel.uiState)
        val current = repositories.preferences.preferences.value.language

        viewModel.onEvent(SettingsUiEvent.SelectLanguage(current))

        assertEquals(emptyList(), repositories.analyticsSink.events)
    }

    @Test
    fun showsTheIdentifierOurStatisticsAreFiledUnder() = runTest {
        repositories.analyticsSink.identifier = "ident-9"
        val state = observe(viewModel.uiState)

        // A fresh install has agreed to nothing, so there is nothing to file anything under yet.
        assertNull(state.value.deviceIdentifier)

        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertEquals("ident-9", state.value.deviceIdentifier)
    }

    @Test
    fun showsTheIdentifierThatWasRememberedWithBothAnswersOff() = runTest {
        repositories.analyticsSink.identifier = "ident-9"
        repositories.preferences.setAnalyticsEnabled(true)
        repositories.analytics.syncConsent()
        repositories.analytics.refreshDeviceIdentifier()

        // What a restart with both answers off looks like: the SDK reports nothing, and the row still
        // shows the identifier the earlier data is filed under.
        repositories.preferences.setAnalyticsEnabled(false)
        repositories.analytics.syncConsent()
        repositories.analyticsSink.identifier = null
        val state = observe(viewModel.uiState)

        assertFalse(state.value.analyticsEnabled)
        assertEquals("ident-9", state.value.deviceIdentifier)
    }

    @Test
    fun copyingTheIdentifierIsRecordedSoTheRowCanSaySo() = runTest {
        // The clipboard belongs to the platform: the press is the screen's own side effect, and this
        // flag is the only part of it that exists here.
        val state = observe(viewModel.uiState)

        assertFalse(state.value.deviceIdentifierCopied)

        viewModel.onEvent(SettingsUiEvent.DeviceIdentifierCopied)

        assertTrue(state.value.deviceIdentifierCopied)
    }

    // ── The age question ──
    //
    // The one thing on this screen a user changes because they changed: somebody answers at 15 and is
    // 16 the year after. Under 16 the three switches are drawn locked, and refused here as well.

    @Test
    fun theAgeAnswerCanBeCorrectedHere() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetOverConsentAge(false))

        assertEquals(false, state.value.overConsentAge)
        assertEquals(false, repositories.preferences.preferences.value.overConsentAge)
        assertFalse(state.value.canAnswerSwitches)
        assertTrue(state.value.asksForParentalConsent)
    }

    @Test
    fun under16LocksTheThreeSwitchesAndRefusesChanges() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetOverConsentAge(false))
        viewModel.onEvent(SettingsUiEvent.SetAnalytics(true))

        assertFalse(state.value.canAnswerSwitches)
        assertFalse(state.value.analyticsEnabled)
        assertFalse(repositories.preferences.preferences.value.analyticsEnabled)
    }

    @Test
    fun anUnder16WhoTurns16GetsTheSwitchesBack() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetOverConsentAge(false))
        viewModel.onEvent(SettingsUiEvent.SetOverConsentAge(true))

        assertTrue(state.value.canAnswerSwitches)
        assertFalse(state.value.asksForParentalConsent)
    }

    @Test
    fun theGuardiansAgreementUnlocksThem() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(SettingsUiEvent.SetOverConsentAge(false))
        viewModel.onEvent(SettingsUiEvent.SetParentalConsent(true))
        viewModel.onEvent(SettingsUiEvent.SetAds(true))

        assertTrue(state.value.canAnswerSwitches)
        assertTrue(state.value.adsEnabled)
        assertTrue(repositories.preferences.preferences.value.adsEnabled)
    }

    @Test
    fun anUnansweredQuestionLeavesThemLocked() = runTest {
        val fresh = TestRepositories(overConsentAge = null).apply {
            preferences.select(TestSelection("sk-sk", LicenseCategory.C))
        }
        val model = SettingsViewModel(fresh.preferences, fresh.exams, fresh.analytics, fresh.ads)

        val state = observe(model.uiState)

        assertFalse(state.value.canAnswerSwitches)
        assertFalse(state.value.asksForParentalConsent)
    }
}
