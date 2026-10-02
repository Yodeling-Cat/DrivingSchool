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
    /** The user ticked "I agree to both documents". Only the narrower button waits for it. */
    val documentsAccepted: Boolean,
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
) {
    /** "Accept all" is always live; "Accept chosen" waits for the documents to be agreed to. */
    val canAcceptChosen: Boolean get() = documentsAccepted
}

sealed interface ConsentUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ConsentUiEvent

    /** The one required answer: without it there is nothing to agree to. */
    data class SetDocumentsAccepted(val accepted: Boolean) : ConsentUiEvent

    /** Optional, and its own answer: crash reports are not analytics. */
    data class SetCrashReporting(val enabled: Boolean) : ConsentUiEvent

    /** Optional, and its own answer. */
    data class SetAnalytics(val enabled: Boolean) : ConsentUiEvent

    /** Agree to the documents and to both sharing questions in one act. */
    data object AcceptAll : ConsentUiEvent

    /** Agree to the documents, at whatever the switches have been set to. */
    data object AcceptChosen : ConsentUiEvent
}

/**
 * The screen where the user agrees to the documents and answers the two sharing questions.
 *
 * The two switches are separate on purpose. Bundling them into "I accept the terms" would not be
 * consent that is specific to what it covers, which is what the law asks for and what the app
 * previously got wrong (`plans/privacy-and-terms.md`).
 *
 * Neither button undoes that. "Accept all" is not a pre-ticked box: the user asks for both answers
 * by pressing it, which is an affirmative act, and that is the part the law is about. "Accept
 * chosen" is the same act with the answers left alone, and it is what keeps refusing as easy as
 * accepting. A pre-ticked switch would be neither — it would take the answer from the user not
 * touching it.
 *
 * **Nothing is written until a button is pressed.** The switches are held here and only copied into
 * [PreferencesRepository] by [commitAnswers], so a switch left on and the screen abandoned is not an
 * answer the app ever recorded, and the SDK is never told to start under one.
 */
class ConsentViewModel(private val preferences: PreferencesRepository, private val analytics: AnalyticsRepository) :
    ViewModel() {

    /** Not persisted: reading the page is not the acceptance, ticking the box or pressing is. */
    private val documentsAccepted = MutableStateFlow(false)

    /**
     * The answers as the switches show them, starting from whatever was answered last time — a
     * re-consent after a policy change opens on the old answers rather than on nothing.
     */
    private val stagedCrashReporting = MutableStateFlow(preferences.preferences.value.crashReportingEnabled)
    private val stagedAnalytics = MutableStateFlow(preferences.preferences.value.analyticsEnabled)

    val uiState: StateFlow<ConsentUiState> = combine(
        preferences.preferences,
        documentsAccepted,
        stagedCrashReporting,
        stagedAnalytics,
    ) { preferences, documentsAccepted, crashReporting, analytics ->
        ConsentUiState(
            language = preferences.language,
            documentsAccepted = documentsAccepted,
            crashReportingEnabled = crashReporting,
            analyticsEnabled = analytics,
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

            // Held, not written. These are what the buttons commit.
            is ConsentUiEvent.SetCrashReporting -> stagedCrashReporting.value = event.enabled

            is ConsentUiEvent.SetAnalytics -> stagedAnalytics.value = event.enabled

            ConsentUiEvent.AcceptAll -> {
                stagedCrashReporting.value = true
                stagedAnalytics.value = true
                documentsAccepted.value = true
                commitAnswers()
            }

            ConsentUiEvent.AcceptChosen -> {
                // Refused in the ViewModel as well as on the button, so the rule holds whichever
                // screen is driving it.
                if (!uiState.value.canAcceptChosen) return
                commitAnswers()
            }
        }
    }

    /**
     * The only place this screen writes anything, and both buttons end here: the answers become the
     * user's, the SDK is told, and the onboarding is recorded.
     *
     * The write comes before the capture on purpose — [AnalyticsRepository] reads the answer on
     * every event, so this is what lets the onboarding event be sent at all.
     */
    private fun commitAnswers() {
        preferences.setCrashReportingEnabled(stagedCrashReporting.value)
        preferences.setAnalyticsEnabled(stagedAnalytics.value)
        analytics.syncConsent()
        preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        analytics.capture(AnalyticsEvent.OnboardingCompleted)
    }
}
