package com.spiraclestudios.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.CONSENT_CHOICE_ACCEPT_ALL
import com.spiraclestudios.autoskola.data.analytics.CONSENT_CHOICE_ACCEPT_CHOSEN
import com.spiraclestudios.autoskola.data.analytics.consentAnsweredProperties
import com.spiraclestudios.autoskola.data.analytics.languageChanged
import com.spiraclestudios.autoskola.data.preferences.CURRENT_LEGAL_VERSION
import com.spiraclestudios.autoskola.data.preferences.ConsentAge
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ConsentUiState(
    val language: AppLanguage,
    /** The user ticked "I agree to both documents". */
    val documentsAccepted: Boolean,
    /** "Accept chosen" was pressed without it, so the box is drawn as the thing that is missing. */
    val agreementError: Boolean,
    /** Which side of 16 the user is on, `null` until they answer. */
    val overConsentAge: Boolean?,
    /** A button was pressed before the age question was answered. */
    val ageError: Boolean,
    /** A parent or guardian has agreed on behalf of a user under 16. */
    val parentalConsent: Boolean,
    /** A button was pressed under 16 without that agreement. */
    val parentalError: Boolean,
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val adsEnabled: Boolean,
) {
    /**
     * Whether the three switches may be answered at all: an adult may, and so may a user under 16 whose
     * parent or guardian has agreed ([ConsentAge]). Unanswered counts as not yet — nothing is turned on
     * before the user has said which side of 16 they are on — and false is what draws them locked.
     */
    val switchesEnabled: Boolean get() = ConsentAge.mayAnswerSwitches(overConsentAge, parentalConsent)

    /** Whether the screen asks for a parent or guardian at all: only under 16 has anything to agree to. */
    val asksForParentalConsent: Boolean get() = overConsentAge == false
}

/**
 * The answers as the switches show them, before either button commits them.
 *
 * One value rather than three flows, because the screen is one question with three answers and
 * [ConsentViewModel.uiState] would otherwise have to combine six sources to draw it.
 */
private data class StagedAnswers(val crashReporting: Boolean, val analytics: Boolean, val ads: Boolean)

/**
 * The age question and what follows from it: a parent or guardian's agreement, and the two marks a
 * press leaves where either is missing. One value for the same reason as [StagedAnswers] — it is one
 * part of the screen, and four flows would each have to be combined on their own.
 */
private data class StagedAge(
    val overConsentAge: Boolean?,
    val parentalConsent: Boolean,
    val ageError: Boolean,
    val parentalError: Boolean,
)

sealed interface ConsentUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ConsentUiEvent

    /** The one required answer: without it there is nothing to agree to. */
    data class SetDocumentsAccepted(val accepted: Boolean) : ConsentUiEvent

    /** Which side of 16 the user is on. Under 16 the three answers are not theirs to give. */
    data class SetOverConsentAge(val overConsentAge: Boolean) : ConsentUiEvent

    /** A parent or guardian agreed on the user's behalf, which is what lets an under-16 answer. */
    data class SetParentalConsent(val given: Boolean) : ConsentUiEvent

    /** Optional, and its own answer: crash reports are not analytics. */
    data class SetCrashReporting(val enabled: Boolean) : ConsentUiEvent

    /** Optional, and its own answer. */
    data class SetAnalytics(val enabled: Boolean) : ConsentUiEvent

    /**
     * Optional, and its own answer. Ads are the one thing here the app *shows* rather than sends,
     * and refusing costs the user nothing but the ads: everything in the app works without them.
     */
    data class SetAds(val enabled: Boolean) : ConsentUiEvent

    /** Agree to the documents and to all three answers in one act. */
    data object AcceptAll : ConsentUiEvent

    /** Agree to the documents, at whatever the switches have been set to. */
    data object AcceptChosen : ConsentUiEvent
}

/**
 * The screen where the user agrees to the documents and answers three separate questions.
 *
 * The three answers are separate on purpose. Bundling them into "I accept the terms" would not be
 * consent that is specific to what it covers, which is what the law asks for and what the app
 * previously got wrong (`plans/privacy-and-terms.md`).
 *
 * **They are also not everybody's to give.** The app's questions serve candidates from 15, and in
 * Slovakia only a person of 16 can agree to this kind of processing alone, so this screen asks which
 * side of 16 the user is on before anything can be answered. Under 16 the switches stay locked until a
 * parent or guardian's agreement is recorded, and neither button commits anything until then: an
 * answer to a question the user may not answer is not one the app may keep (`plans/age-gate.md`,
 * [ConsentAge]).
 *
 * Neither button undoes that. "Accept all" is not a pre-ticked box: the user asks for both answers
 * by pressing it, which is an affirmative act, and that is the part the law is about. "Accept
 * chosen" is the same act with the answers left alone, and it is what keeps refusing as easy as
 * accepting. A pre-ticked switch would be neither — it would take the answer from the user not
 * touching it.
 *
 * **Nothing is written until a button is pressed.** The switches and the age answer are held here and
 * only copied into [PreferencesRepository] by [commitAnswers], so a switch left on and the screen
 * abandoned is not an answer the app ever recorded, and the SDK is never told to start under one.
 *
 * "Accept chosen" is deliberately not disabled while the agreement is unticked. A dead button cannot
 * be pressed, so it cannot explain itself; this one is pressed and answers by marking the box that
 * has to be ticked (see [ConsentUiState.agreementError]).
 */
class ConsentViewModel(
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsRepository,
    private val ads: AdRepository,
) : ViewModel() {

    /** Not persisted: reading the page is not the acceptance, ticking the box or pressing is. */
    private val documentsAccepted = MutableStateFlow(false)

    /** Set by "Accept chosen" without the agreement, cleared as soon as it is ticked. */
    private val agreementError = MutableStateFlow(false)

    /** The age question, staged like the switches: a button is what writes it. */
    private val age = MutableStateFlow(
        StagedAge(
            overConsentAge = preferences.preferences.value.overConsentAge,
            parentalConsent = preferences.preferences.value.parentalConsent,
            ageError = false,
            parentalError = false,
        ),
    )

    /**
     * The answers as the switches show them, starting from whatever was answered last time — a
     * re-consent after a policy change opens on the old answers rather than on nothing.
     */
    private val staged = MutableStateFlow(
        StagedAnswers(
            crashReporting = preferences.preferences.value.crashReportingEnabled,
            analytics = preferences.preferences.value.analyticsEnabled,
            ads = preferences.preferences.value.adsEnabled,
        ),
    )

    val uiState: StateFlow<ConsentUiState> = combine(
        preferences.preferences,
        documentsAccepted,
        agreementError,
        staged,
        age,
    ) { preferences, documentsAccepted, agreementError, staged, age ->
        ConsentUiState(
            language = preferences.language,
            documentsAccepted = documentsAccepted,
            agreementError = agreementError,
            overConsentAge = age.overConsentAge,
            ageError = age.ageError,
            parentalConsent = age.parentalConsent,
            parentalError = age.parentalError,
            crashReportingEnabled = staged.crashReporting,
            analyticsEnabled = staged.analytics,
            adsEnabled = staged.ads,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConsentUiState(
            language = preferences.preferences.value.language,
            documentsAccepted = false,
            agreementError = false,
            overConsentAge = preferences.preferences.value.overConsentAge,
            ageError = false,
            parentalConsent = preferences.preferences.value.parentalConsent,
            parentalError = false,
            crashReportingEnabled = preferences.preferences.value.crashReportingEnabled,
            analyticsEnabled = preferences.preferences.value.analyticsEnabled,
            adsEnabled = preferences.preferences.value.adsEnabled,
        ),
    )

    fun onEvent(event: ConsentUiEvent) {
        when (event) {
            is ConsentUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
            }

            is ConsentUiEvent.SetDocumentsAccepted -> {
                documentsAccepted.value = event.accepted
                // Ticking it is the answer the mark was asking for, so it goes as soon as it is
                // given. Unticking is not an answer either way, so it leaves the mark alone.
                if (event.accepted) agreementError.value = false
            }

            is ConsentUiEvent.SetOverConsentAge -> {
                age.update { it.copy(overConsentAge = event.overConsentAge, ageError = false) }
                // Under 16 the three answers are not the user's to give, so they go off until a parent
                // or guardian agrees — and stay off if the user answers that way again. Saying "16 or
                // older" puts back whatever was stored, which is what a re-consent opens on.
                staged.value = if (event.overConsentAge) storedAnswers() else noAnswers()
            }

            is ConsentUiEvent.SetParentalConsent -> {
                age.update { it.copy(parentalConsent = event.given, parentalError = false) }
                // Withdrawing it takes the three answers with it: that agreement was the whole of what
                // they rested on.
                if (!event.given) staged.value = noAnswers()
            }

            // Held, not written. These are what the buttons commit.
            is ConsentUiEvent.SetCrashReporting -> staged.update { it.copy(crashReporting = event.enabled) }

            is ConsentUiEvent.SetAnalytics -> staged.update { it.copy(analytics = event.enabled) }

            is ConsentUiEvent.SetAds -> staged.update { it.copy(ads = event.enabled) }

            ConsentUiEvent.AcceptAll -> {
                if (!ageAllowsAnswering()) return
                staged.value = StagedAnswers(crashReporting = true, analytics = true, ads = true)
                documentsAccepted.value = true
                agreementError.value = false
                commitAnswers(CONSENT_CHOICE_ACCEPT_ALL)
            }

            ConsentUiEvent.AcceptChosen -> {
                // Refused in the ViewModel as well as on the button, so the rule holds whichever
                // screen is driving it. The mark is the whole of the refusal: nothing is committed.
                if (!ageAllowsAnswering()) return
                if (!documentsAccepted.value) {
                    agreementError.value = true
                    return
                }
                commitAnswers(CONSENT_CHOICE_ACCEPT_CHOSEN)
            }
        }
    }

    private fun noAnswers() = StagedAnswers(crashReporting = false, analytics = false, ads = false)

    private fun storedAnswers() = StagedAnswers(
        crashReporting = preferences.preferences.value.crashReportingEnabled,
        analytics = preferences.preferences.value.analyticsEnabled,
        ads = preferences.preferences.value.adsEnabled,
    )

    /**
     * The two requirements that come before either button, marked where they are unmet: the age
     * question, and — under 16 — a parent or guardian's agreement. Everything else on this screen is an
     * answer the user gives; these two decide whether they may answer at all.
     *
     * Marked rather than enforced by a dead button, for the reason the documents' box is: a button
     * that cannot be pressed cannot explain itself, and this screen is where the app has to explain
     * this.
     */
    private fun ageAllowsAnswering(): Boolean {
        val asked = age.value
        val answered = asked.overConsentAge != null
        val guardianAgreed = answered && (asked.overConsentAge == true || asked.parentalConsent)
        age.value = asked.copy(ageError = !answered, parentalError = !guardianAgreed)
        return answered && guardianAgreed
    }

    /**
     * The only place this screen writes anything, and both buttons end here: the answers become the
     * user's, the SDK is told, and the onboarding is recorded.
     *
     * The order matters. [PreferencesRepository.setOverConsentAge] is what pins the three answers to
     * the age answer — under 16 without a parent or guardian's agreement they are all off — so it is
     * written *last*, after the answers whose fate it decides, and the agreement is written before it
     * for the same reason.
     *
     * The write comes before the capture on purpose — [AnalyticsRepository] reads the answer on
     * every event, so this is what lets the onboarding event be sent at all, and an under-16 without a
     * guardian's agreement sends nothing, which is the point of the gate. [choice] is the button that
     * was pressed, which is how the two ways through are told apart in the data.
     */
    private fun commitAnswers(choice: String) {
        val answers = staged.value
        val asked = age.value
        preferences.setParentalConsent(asked.parentalConsent)
        preferences.setCrashReportingEnabled(answers.crashReporting)
        preferences.setAnalyticsEnabled(answers.analytics)
        preferences.setAdsEnabled(answers.ads)
        preferences.setOverConsentAge(asked.overConsentAge)
        analytics.syncConsent()
        // The ad SDK is told here as well as on the repository's own construction: this screen is
        // where the answer first exists, and a banner can be drawn before the next request path runs.
        ads.pushAgeGate()
        preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        analytics.capture(
            AnalyticsEvent.ConsentAnswered,
            consentAnsweredProperties(answers.crashReporting, answers.analytics, answers.ads, choice),
        )
        analytics.capture(AnalyticsEvent.OnboardingCompleted)
    }
}
