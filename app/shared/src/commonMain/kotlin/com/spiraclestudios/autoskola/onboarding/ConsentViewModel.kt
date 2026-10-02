package com.spiraclestudios.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConsentUiState(
    val language: AppLanguage,
    /** The user ticked "I agree to both documents". The continue button is dead until they do. */
    val documentsAccepted: Boolean,
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
) {
    val canContinue: Boolean get() = documentsAccepted
}

sealed interface ConsentUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ConsentUiEvent

    /** The one required answer: without it there is nothing to agree to. */
    data class SetDocumentsAccepted(val accepted: Boolean) : ConsentUiEvent

    /** Optional, and its own answer: crash reports are not analytics. */
    data class SetCrashReporting(val enabled: Boolean) : ConsentUiEvent

    /** Optional, and its own answer. */
    data class SetAnalytics(val enabled: Boolean) : ConsentUiEvent

    data object Continue : ConsentUiEvent
}

/**
 * The screen where the user agrees to the documents and answers the two sharing questions.
 *
 * The two switches are separate on purpose. Bundling them into "I accept the terms" would not be
 * consent that is specific to what it covers, which is what the law asks for and what the app
 * previously got wrong (`plans/privacy-and-terms.md`).
 */
class ConsentViewModel(private val preferences: PreferencesRepository, private val analytics: AnalyticsRepository) :
    ViewModel() {

    /** Not persisted: reading the page is not the acceptance, ticking the box is. */
    private val documentsAccepted = MutableStateFlow(false)

    val uiState: StateFlow<ConsentUiState> =
        combine(preferences.preferences, documentsAccepted) { preferences, documentsAccepted ->
            ConsentUiState(
                language = preferences.language,
                documentsAccepted = documentsAccepted,
                crashReportingEnabled = preferences.crashReportingEnabled,
                analyticsEnabled = preferences.analyticsEnabled,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ConsentUiState(
                language = preferences.preferences.value.language,
                documentsAccepted = false,
                crashReportingEnabled = preferences.preferences.value.crashReportingEnabled,
                analyticsEnabled = preferences.preferences.value.analyticsEnabled,
            ),
        )

    fun onEvent(event: ConsentUiEvent) {
        when (event) {
            is ConsentUiEvent.SelectLanguage -> preferences.selectLanguage(event.language)

            is ConsentUiEvent.SetDocumentsAccepted -> documentsAccepted.value = event.accepted

            is ConsentUiEvent.SetCrashReporting -> {
                preferences.setCrashReportingEnabled(event.enabled)
                analytics.syncConsent()
            }

            is ConsentUiEvent.SetAnalytics -> {
                preferences.setAnalyticsEnabled(event.enabled)
                analytics.syncConsent()
            }

            ConsentUiEvent.Continue -> {
                // Refused in the ViewModel as well as on the button, so the rule holds whichever
                // screen is driving it.
                if (!uiState.value.canContinue) return
                preferences.acceptLegal(CURRENT_LEGAL_VERSION)
                analytics.capture(AnalyticsEvent.OnboardingCompleted)
            }
        }
    }
}
