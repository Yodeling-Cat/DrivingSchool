package com.spiraclestudios.autoskola.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.appVersion
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_ANALYTICS
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_CRASH_REPORTING
import com.spiraclestudios.autoskola.data.analytics.CONSENT_SWITCH_PERSONALIZED_ADS
import com.spiraclestudios.autoskola.data.analytics.consentChangedProperties
import com.spiraclestudios.autoskola.data.analytics.languageChanged
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.ConsentAge
import com.spiraclestudios.autoskola.data.preferences.Preferences
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: AppLanguage,
    val checkAnswersImmediately: Boolean,
    val advanceAfterAnswer: Boolean,
    /** The three answers the consent screen asked for, which the user can change here. */
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val personalizedAdsEnabled: Boolean,
    /**
     * Which age band the user is in, `null` while nothing has asked — which is the state an app
     * updated from a version before the question is in. Correctable here on purpose: somebody answers
     * at 15 and is 16 the year after.
     */
    val ageBand: AgeBand?,
    /** Whether a parent or guardian agreed on behalf of a user under 16. */
    val parentalConsent: Boolean,
    /**
     * `null` when there is no exam to name. The exam cannot be unset once chosen, but
     * `ExamRepository.current` re-resolves on a cold subscription, so this is what the row is drawn
     * from before the selected bank is found — and again if an update removed that bank.
     */
    val exam: ExamLabel?,
    /** What the platform reports, or `null` where it reports none. */
    val appVersion: String?,
    /**
     * The identifier our statistics and crash reports for this device are filed under, or `null`
     * while there is nothing to file anything under — which is every device whose owner has agreed
     * to neither. Published by `AnalyticsRepository`, which has none to publish until the SDK is
     * running, so it is absent rather than stale once the answers behind it are switched off.
     */
    val deviceIdentifier: String?,
    /** The identifier has been put on the clipboard, which is the only way to know that it was. */
    val deviceIdentifierCopied: Boolean,
    /**
     * The band the screen is asking the user to confirm, or `null` when it is not asking. It asks
     * before a *higher* band is applied, because one press unlocks switches and the app cannot check
     * the answer itself.
     */
    val confirmingAge: AgeBand? = null,
    /**
     * Whether to offer a way back into Google's consent form. Only where Google shows one — the EEA,
     * the UK and Switzerland — so a user elsewhere is never shown a row about a form that does not
     * exist for them.
     */
    val adPrivacyOptionsRequired: Boolean = false,
) {
    /**
     * Answers checked as they're picked have to stay on screen, so moving on by itself can only be
     * switched on while that is off.
     */
    val canAdvanceAfterAnswer: Boolean get() = !checkAnswersImmediately

    /**
     * Whether the crash-report and statistics switches may be answered at all: anyone from 16 may, and
     * so may a user under 16 whose parent or guardian has agreed ([ConsentAge]). They are drawn locked
     * where not, and the repository holds the same line against a stored answer whatever the screen
     * does.
     */
    val canAnswerSwitches: Boolean get() = ConsentAge.mayAnswerSwitches(ageBand, parentalConsent)

    /**
     * Whether the personalized-ads switch may be answered: only an adult's to give, whatever a parent
     * or guardian agreed to. Drawn locked, with the reason, where it is false.
     */
    val canPersonalizeAds: Boolean get() = ConsentAge.mayPersonalizeAds(ageBand)

    /**
     * Whether the age question is drawn on this screen at all. Only a user under 18 sees it: it is the
     * one answer here they can still change — somebody answers at 15 and is 16 the year after, and 18
     * the year after that — and to an adult it is a question they cannot answer differently, which is
     * clutter and nothing more.
     *
     * The parent or guardian's agreement is not offered here in either case: for an under-16 it is
     * locked in once given, and for everyone else it does not exist.
     */
    val canCorrectAge: Boolean get() = ageBand == AgeBand.Under16 || ageBand == AgeBand.From16To17

    /**
     * The answers the age row offers: the band the user is in and every band above it, never one
     * below. Somebody who is 16 does not become 15 again, so "under 16" is not offered to them, and
     * [SettingsUiEvent.SetAgeBand] refuses it as well as the row hiding it.
     */
    val ageOptions: List<AgeBand> get() = AgeBand.entries.filter { ageBand == null || it.ordinal >= ageBand.ordinal }

    /** Whether the user is under 16, which is when the reason the switches are locked is drawn. */
    val underConsentAge: Boolean get() = ageBand == AgeBand.Under16
}

sealed interface SettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : SettingsUiEvent
    data class SetCheckAnswersImmediately(val enabled: Boolean) : SettingsUiEvent
    data class SetAdvanceAfterAnswer(val enabled: Boolean) : SettingsUiEvent
    data class SetCrashReporting(val enabled: Boolean) : SettingsUiEvent
    data class SetAnalytics(val enabled: Boolean) : SettingsUiEvent
    data class SetPersonalizedAds(val enabled: Boolean) : SettingsUiEvent

    /**
     * Which age band the user is in, changed here when they have a birthday. A higher band is asked
     * about before it counts. A lower one is refused: nobody gets younger, and an answer that took
     * switches away would be a way to withdraw them by mistake.
     */
    data class SetAgeBand(val band: AgeBand) : SettingsUiEvent

    /** The user confirms the higher band the screen asked about, and it is applied. */
    data object ConfirmAgeBand : SettingsUiEvent

    /** Backing out of that question: the answer is not given and nothing changes. */
    data object DismissAgeConfirmation : SettingsUiEvent

    /** The user wants to review what they agreed to in Google's consent form, and the form is shown. */
    data object OpenAdPrivacyOptions : SettingsUiEvent

    /**
     * The identifier was put on the clipboard, which the screen did itself: the clipboard belongs to
     * the platform, so the press is a side effect the screen performs and this only records it.
     */
    data object DeviceIdentifierCopied : SettingsUiEvent
}

class SettingsViewModel(
    private val preferences: PreferencesRepository,
    exams: ExamRepository,
    private val analytics: AnalyticsRepository,
    private val ads: AdRepository,
) : ViewModel() {

    /** Read once: on Android the platform call is reflective, and the answer cannot change. */
    private val version = appVersion()

    /** Whether the identifier has been copied. Nothing but the user's press changes it. */
    private val identifierCopied = MutableStateFlow(false)

    /** The band the screen is asking the user to confirm, if it is asking. */
    private val ageConfirmation = MutableStateFlow<AgeBand?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            preferences.preferences,
            exams.current,
            identifierCopied,
            analytics.deviceIdentifier,
            combine(ageConfirmation, ads.privacyOptionsRequired, ::Extras),
            ::state,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(
                    preferences.preferences.value,
                    CurrentExam.Loading,
                    identifierCopied = false,
                    deviceIdentifier = analytics.deviceIdentifier.value,
                    extras = Extras(confirmingAge = null, adPrivacyOptionsRequired = ads.privacyOptionsRequired.value),
                ),
            )

    init {
        // The identifier is there only while the SDK is running and has one to report, so the screen
        // asks for the current one instead of trusting what was read when something else last
        // changed. Reading it starts nothing.
        analytics.refreshDeviceIdentifier()
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
            }

            is SettingsUiEvent.SetCheckAnswersImmediately -> preferences.setCheckAnswersImmediately(event.enabled)

            is SettingsUiEvent.SetAdvanceAfterAnswer -> preferences.setAdvanceAfterAnswer(event.enabled)

            is SettingsUiEvent.SetCrashReporting -> {
                // The row is drawn locked where the age answer does not allow it; refused here too, so
                // the rule holds whichever screen is driving this and whatever else changes.
                if (!canAnswerSwitches()) return
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
                if (!canAnswerSwitches()) return
                if (preferences.preferences.value.analyticsEnabled != event.enabled) {
                    analytics.capture(
                        AnalyticsEvent.ConsentChanged,
                        consentChangedProperties(CONSENT_SWITCH_ANALYTICS, event.enabled),
                    )
                }
                preferences.setAnalyticsEnabled(event.enabled)
                analytics.syncConsent()
            }

            is SettingsUiEvent.SetPersonalizedAds -> {
                // Refused for anyone under 18 even with the other two unlocked: no agreement opens it.
                if (!canPersonalizeAds()) return
                if (preferences.preferences.value.personalizedAdsEnabled != event.enabled) {
                    analytics.capture(
                        AnalyticsEvent.ConsentChanged,
                        consentChangedProperties(CONSENT_SWITCH_PERSONALIZED_ADS, event.enabled),
                    )
                }
                preferences.setPersonalizedAdsEnabled(event.enabled)
                // No syncConsent after this one: the answer is not something PostHog is told about. The
                // ad SDK is, and at once - a banner that is loaded next should already ask for the kind
                // of ad the user has just chosen.
                ads.pushConsent()
            }

            is SettingsUiEvent.SetAgeBand -> {
                val current = preferences.preferences.value.ageBand
                when {
                    // Nothing to change, and nothing to ask: the same band, or a question that has not
                    // been answered, which the consent screen makes sure cannot be reached from here.
                    current == null || event.band == current -> Unit

                    // A higher band unlocks switches in one press and the app has no way to check the
                    // answer, so it asks first. Nothing is written while the question is on screen.
                    current.rises(event.band) -> ageConfirmation.value = event.band

                    // A lower band is refused. The row does not offer it either; this is the rule.
                    else -> Unit
                }
            }

            SettingsUiEvent.ConfirmAgeBand -> {
                val confirmed = ageConfirmation.value ?: return
                ageConfirmation.value = null
                answerTheAgeQuestion(confirmed)
            }

            SettingsUiEvent.DismissAgeConfirmation -> ageConfirmation.value = null

            SettingsUiEvent.OpenAdPrivacyOptions -> viewModelScope.launch { ads.showPrivacyOptions() }

            SettingsUiEvent.DeviceIdentifierCopied -> identifierCopied.value = true
        }
    }

    /**
     * Records the age answer and everything that follows from it.
     *
     * No analytics event: the age band is not something the app sends, and the documents list what is
     * sent. Only ever a *higher* band, so it takes no answer away: moving up changes none of them, and
     * the app never fills one in either.
     */
    private fun answerTheAgeQuestion(band: AgeBand) {
        preferences.setAgeBand(band)
        syncGates()
        // The form asks different things of different ages, so it is asked again: somebody who has just
        // turned 16 is no longer one it leaves alone, and is shown it now rather than at the next launch.
        viewModelScope.launch { ads.refreshConsent() }
    }

    /**
     * Both things that act on an answer are told, because changing the age answer can change what the
     * answers are worth: the SDK stops or starts, and the ad SDK learns who the ads are for and whether
     * they may be personalized — which has to be right before the next banner, not just before the
     * next fullscreen request.
     */
    private fun syncGates() {
        analytics.syncConsent()
        ads.pushConsent()
    }

    /**
     * The same rule the state draws from, read where an event has to refuse rather than mark: the row
     * is locked on screen, and this is the lock.
     */
    private fun canAnswerSwitches(): Boolean = preferences.preferences.value.let {
        ConsentAge.mayAnswerSwitches(it.ageBand, it.parentalConsent)
    }

    /** The personalized-ads switch's lock, which is the age band alone. */
    private fun canPersonalizeAds(): Boolean = ConsentAge.mayPersonalizeAds(preferences.preferences.value.ageBand)

    private fun state(
        preferences: Preferences,
        exam: CurrentExam,
        identifierCopied: Boolean,
        deviceIdentifier: String?,
        extras: Extras,
    ) = SettingsUiState(
        language = preferences.language,
        checkAnswersImmediately = preferences.checkAnswersImmediately,
        advanceAfterAnswer = preferences.advanceAfterAnswer,
        crashReportingEnabled = preferences.crashReportingEnabled,
        analyticsEnabled = preferences.analyticsEnabled,
        personalizedAdsEnabled = preferences.personalizedAdsEnabled,
        ageBand = preferences.ageBand,
        parentalConsent = preferences.parentalConsent,
        exam = (exam as? CurrentExam.Ready)?.session?.let { ExamLabel(it.category, it.bank.language) },
        appVersion = version,
        deviceIdentifier = deviceIdentifier,
        deviceIdentifierCopied = identifierCopied,
        confirmingAge = extras.confirmingAge,
        adPrivacyOptionsRequired = extras.adPrivacyOptionsRequired,
    )
}

/**
 * The two answers about a dialog and a row that are not preferences, held together so the state is
 * built from one value rather than from a flow each.
 */
private data class Extras(val confirmingAge: AgeBand?, val adPrivacyOptionsRequired: Boolean)
