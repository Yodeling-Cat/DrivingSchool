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
import com.spiraclestudios.autoskola.data.preferences.AgeBand
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
    /** Which age band the user is in, `null` until they answer. */
    val ageBand: AgeBand?,
    /** A parent or guardian has agreed on behalf of a user under 16. */
    val parentalConsent: Boolean,
    /** A button was pressed under 16 without that agreement. */
    val parentalError: Boolean,
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val personalizedAdsEnabled: Boolean,
) {
    /**
     * Whether the crash-report and statistics switches may be answered at all: anyone from 16 may, and
     * so may a user under 16 whose parent or guardian has agreed ([ConsentAge]). Unanswered counts as
     * not yet — nothing is turned on before the user has said which band they are in — and false is
     * what draws them locked.
     */
    val switchesEnabled: Boolean get() = ConsentAge.mayAnswerSwitches(ageBand, parentalConsent)

    /**
     * Whether the personalized-ads switch may be answered: only an adult's to give, and no agreement
     * unlocks it for anyone else. Drawn locked, with the reason, where it is false.
     */
    val personalizedAdsAvailable: Boolean get() = ConsentAge.mayPersonalizeAds(ageBand)

    /** Whether the screen asks for a parent or guardian at all: only under 16 has anything to agree to. */
    val asksForParentalConsent: Boolean get() = ageBand == AgeBand.Under16

    /**
     * Whether the age question has been answered, which is what the card is drawn from: closed while it
     * has not, and the choices revealed once it has. The two buttons are dead until then, since nothing
     * here is answerable before it.
     */
    val ageAnswered: Boolean get() = ageBand != null
}

/**
 * The answers as the switches show them, before either button commits them.
 *
 * One value rather than three flows, because the screen is one question with three answers and
 * [ConsentViewModel.uiState] would otherwise have to combine six sources to draw it.
 */
private data class StagedAnswers(val crashReporting: Boolean, val analytics: Boolean, val personalizedAds: Boolean)

/**
 * The age question and what follows from it: a parent or guardian's agreement, and the mark a press
 * leaves where it is missing. One value for the same reason as [StagedAnswers] — it is one part of the
 * screen, and three flows would each have to be combined on their own.
 */
private data class StagedAge(val ageBand: AgeBand?, val parentalConsent: Boolean, val parentalError: Boolean)

sealed interface ConsentUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ConsentUiEvent

    /** The one required answer: without it there is nothing to agree to. */
    data class SetDocumentsAccepted(val accepted: Boolean) : ConsentUiEvent

    /**
     * Which age band the user is in. Under 16 the crash-report and statistics answers are not theirs
     * to give, and under 18 the personalized-ads answer is not.
     */
    data class SetAgeBand(val band: AgeBand) : ConsentUiEvent

    /**
     * A parent or guardian agreed on the user's behalf, which is what lets an under-16 answer the
     * crash-report and statistics switches. It does not unlock personalized ads.
     */
    data class SetParentalConsent(val given: Boolean) : ConsentUiEvent

    /** Optional, and its own answer: crash reports are not analytics. */
    data class SetCrashReporting(val enabled: Boolean) : ConsentUiEvent

    /** Optional, and its own answer. */
    data class SetAnalytics(val enabled: Boolean) : ConsentUiEvent

    /**
     * Optional, and its own answer. It decides whether the ads the user sees are chosen for them, never
     * whether there are any: refusing costs the user nothing but the tailoring, and the app works the
     * same either way.
     */
    data class SetPersonalizedAds(val enabled: Boolean) : ConsentUiEvent

    /** Agree to the documents and to every answer the user's age lets them give, in one act. */
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
 * Slovakia only a person of 16 can agree to crash reports and statistics alone, so this screen asks which
 * age band the user is in before anything can be answered. Under 16 those switches stay locked until a
 * parent or guardian's agreement is recorded, and neither button commits anything until then: an
 * answer to a question the user may not answer is not one the app may keep (`plans/age-gate.md`,
 * [ConsentAge]). **Personalized ads are an adult's alone**: under 18 that switch stays locked whatever
 * a parent or guardian agrees to.
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
            ageBand = preferences.preferences.value.ageBand,
            parentalConsent = preferences.preferences.value.parentalConsent,
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
            personalizedAds = preferences.preferences.value.personalizedAdsEnabled,
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
            ageBand = age.ageBand,
            parentalConsent = age.parentalConsent,
            parentalError = age.parentalError,
            crashReportingEnabled = staged.crashReporting,
            analyticsEnabled = staged.analytics,
            personalizedAdsEnabled = staged.personalizedAds,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConsentUiState(
            language = preferences.preferences.value.language,
            documentsAccepted = false,
            agreementError = false,
            ageBand = preferences.preferences.value.ageBand,
            parentalConsent = preferences.preferences.value.parentalConsent,
            parentalError = false,
            crashReportingEnabled = preferences.preferences.value.crashReportingEnabled,
            analyticsEnabled = preferences.preferences.value.analyticsEnabled,
            personalizedAdsEnabled = preferences.preferences.value.personalizedAdsEnabled,
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

            is ConsentUiEvent.SetAgeBand -> {
                age.update { it.copy(ageBand = event.band) }
                // Answering "under 16" withdraws all three answers: they are not this user's to give,
                // so they are turned off rather than left sitting behind locked switches as answers the
                // app holds for somebody who cannot have given them. Answering 16 to 17 withdraws the
                // personalized-ads answer alone, for the same reason. Answering "18 or older" touches
                // nothing — filling one in is the direction that would be the app answering a question
                // it is here to ask.
                when (event.band) {
                    AgeBand.Under16 -> withdrawTheAnswers()
                    AgeBand.From16To17 -> withdrawPersonalizedAds()
                    AgeBand.Adult -> Unit
                }
                // What this answer also decides, in both directions, is whether the switches may be
                // answered at all ([ConsentUiState.switchesEnabled], [ConsentUiState.personalizedAdsAvailable])
                // and whether what they hold counts (the gates read the same rules).
            }

            is ConsentUiEvent.SetParentalConsent -> {
                age.update { it.copy(parentalConsent = event.given, parentalError = false) }
                // Giving the agreement unlocks the switches and leaves their values alone. Taking it
                // back takes the three answers with it, exactly as answering "under 16" does: it was
                // the agreement that let them be given, so without it they are turned off rather than
                // left on behind locked switches.
                if (!event.given) withdrawTheAnswers()
            }

            // Held, not written. These are what the buttons commit.
            is ConsentUiEvent.SetCrashReporting -> staged.update { it.copy(crashReporting = event.enabled) }

            is ConsentUiEvent.SetAnalytics -> staged.update { it.copy(analytics = event.enabled) }

            is ConsentUiEvent.SetPersonalizedAds -> staged.update { it.copy(personalizedAds = event.enabled) }

            ConsentUiEvent.AcceptAll -> {
                if (!ageAllowsAnswering()) return
                // Every answer the user's age lets them give, and no more: under 18 the ads switch is
                // locked, so pressing this must not be the thing that turns it on.
                staged.value = StagedAnswers(
                    crashReporting = true,
                    analytics = true,
                    personalizedAds = ConsentAge.mayPersonalizeAds(age.value.ageBand),
                )
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

    /**
     * The two requirements that come before either button, and the one mark a press can leave: under 16
     * without a parent or guardian's agreement. The age question has no mark of its own — the buttons
     * are drawn dead until it is answered, and a dead button says that without a red word.
     *
     * Marked rather than enforced by a dead button for the guardian's agreement, because the buttons
     * are live by then and a press has to answer somehow.
     */
    private fun ageAllowsAnswering(): Boolean {
        val asked = age.value
        val allowed = ConsentAge.mayAnswerSwitches(asked.ageBand, asked.parentalConsent)
        age.value = asked.copy(parentalError = !allowed)
        return allowed
    }

    /**
     * The only place this screen writes anything, and both buttons end here: the answers become the
     * user's, the SDK is told, and the onboarding is recorded.
     *
     * The order matters. [PreferencesRepository.setAgeBand] is what pins the answers to the age answer
     * — under 16 without a parent or guardian's agreement they are all off, and under 18 the ads one
     * is — so it is written *last*, after the answers whose fate it decides, and the agreement is
     * written before it for the same reason.
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
        // Checked again here, because a switch can be left on and the age answer changed after it:
        // nothing the age does not allow is ever written as a yes.
        preferences.setPersonalizedAdsEnabled(
            ConsentAge.effectivePersonalizedAds(answers.personalizedAds, asked.ageBand),
        )
        preferences.setAgeBand(asked.ageBand)
        analytics.syncConsent()
        // The ad SDK is told here as well as on the repository's own construction: this screen is
        // where the answers first exist, and a banner can be drawn before the next request path runs.
        ads.pushConsent()
        preferences.acceptLegal(CURRENT_LEGAL_VERSION)
        analytics.capture(
            AnalyticsEvent.ConsentAnswered,
            consentAnsweredProperties(answers.crashReporting, answers.analytics, answers.personalizedAds, choice),
        )
        analytics.capture(AnalyticsEvent.OnboardingCompleted)
    }

    /**
     * Turns the personalized-ads answer off, in the staged copy and in the store, and leaves the other
     * two as they are: what answering 16 to 17 withdraws, because that answer is not theirs to give.
     */
    private fun withdrawPersonalizedAds() {
        staged.update { it.copy(personalizedAds = false) }
        preferences.setPersonalizedAdsEnabled(false)
        ads.pushConsent()
    }

    /**
     * Turns all three answers off, in the staged copy and in the store.
     *
     * The only place the consent screen writes a switch nobody pressed, and it only ever goes
     * downwards: saying you are under 16 withdraws answers, where putting one on would be giving
     * consent on the user's behalf.
     */
    private fun withdrawTheAnswers() {
        staged.value = StagedAnswers(crashReporting = false, analytics = false, personalizedAds = false)
        preferences.setCrashReportingEnabled(false)
        preferences.setAnalyticsEnabled(false)
        preferences.setPersonalizedAdsEnabled(false)
        // Told now rather than at the next commit: the SDKs follow the stored answers, and these have
        // just changed without a button being pressed.
        analytics.syncConsent()
        ads.pushConsent()
    }
}
