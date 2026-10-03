package com.spiraclestudios.autoskola.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.appVersion
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_ADS
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_ANALYTICS
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_CRASH_REPORTING
import com.spiraclestudios.autoskola.data.analytics.consentChangedProperties
import com.spiraclestudios.autoskola.data.analytics.languageChanged
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.Preferences
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val language: AppLanguage,
    val checkAnswersImmediately: Boolean,
    val advanceAfterAnswer: Boolean,
    /** The three answers the consent screen asked for, which the user can change here. */
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val adsEnabled: Boolean,
    /**
     * `null` when there is no exam to name. The exam cannot be unset once chosen, but
     * `ExamRepository.current` re-resolves on a cold subscription, so this is what the row is drawn
     * from before the selected bank is found — and again if an update removed that bank.
     */
    val exam: ExamLabel?,
    /** What the platform reports, or `null` where it reports none. */
    val appVersion: String?,
) {
    /**
     * Answers checked as they're picked have to stay on screen, so moving on by itself can only be
     * switched on while that is off.
     */
    val canAdvanceAfterAnswer: Boolean get() = !checkAnswersImmediately
}

sealed interface SettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : SettingsUiEvent
    data class SetCheckAnswersImmediately(val enabled: Boolean) : SettingsUiEvent
    data class SetAdvanceAfterAnswer(val enabled: Boolean) : SettingsUiEvent
    data class SetCrashReporting(val enabled: Boolean) : SettingsUiEvent
    data class SetAnalytics(val enabled: Boolean) : SettingsUiEvent
    data class SetAds(val enabled: Boolean) : SettingsUiEvent
}

class SettingsViewModel(
    private val preferences: PreferencesRepository,
    exams: ExamRepository,
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    /** Read once: on Android the platform call is reflective, and the answer cannot change. */
    private val version = appVersion()

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences.preferences, exams.current, ::state)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(preferences.preferences.value, CurrentExam.Loading),
            )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
            }

            is SettingsUiEvent.SetCheckAnswersImmediately -> preferences.setCheckAnswersImmediately(event.enabled)

            is SettingsUiEvent.SetAdvanceAfterAnswer -> preferences.setAdvanceAfterAnswer(event.enabled)

            is SettingsUiEvent.SetCrashReporting -> {
                if (preferences.preferences.value.crashReportingEnabled != event.enabled) {
                    // Recorded before the write, so the answer the gate reads is the one that was
                    // in force when the user changed it - the event is the record of the change.
                    analytics.capture(
                        AnalyticsEvent.ConsentChanged,
                        consentChangedProperties(CONSENT_SWITCH_CRASH_REPORTING, event.enabled),
                    )
                }
                preferences.setCrashReportingEnabled(event.enabled)
                analytics.syncConsent()
            }

            is SettingsUiEvent.SetAnalytics -> {
                if (preferences.preferences.value.analyticsEnabled != event.enabled) {
                    analytics.capture(
                        AnalyticsEvent.ConsentChanged,
                        consentChangedProperties(CONSENT_SWITCH_ANALYTICS, event.enabled),
                    )
                }
                preferences.setAnalyticsEnabled(event.enabled)
                analytics.syncConsent()
            }

            is SettingsUiEvent.SetAds -> {
                if (preferences.preferences.value.adsEnabled != event.enabled) {
                    analytics.capture(
                        AnalyticsEvent.ConsentChanged,
                        consentChangedProperties(CONSENT_SWITCH_ADS, event.enabled),
                    )
                }
                // No syncConsent after this one: the ads answer is not something PostHog is told
                // about. The ad SDK reads the preference itself, so the write is the whole of it.
                preferences.setAdsEnabled(event.enabled)
            }
        }
    }

    private fun state(preferences: Preferences, exam: CurrentExam) = SettingsUiState(
        language = preferences.language,
        checkAnswersImmediately = preferences.checkAnswersImmediately,
        advanceAfterAnswer = preferences.advanceAfterAnswer,
        crashReportingEnabled = preferences.crashReportingEnabled,
        analyticsEnabled = preferences.analyticsEnabled,
        adsEnabled = preferences.adsEnabled,
        exam = (exam as? CurrentExam.Ready)?.session?.let { ExamLabel(it.category, it.bank.language) },
        appVersion = version,
    )
}
