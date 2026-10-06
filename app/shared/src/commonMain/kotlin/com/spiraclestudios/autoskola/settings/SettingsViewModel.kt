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

data class SettingsUiState(
    val language: AppLanguage,
    val checkAnswersImmediately: Boolean,
    val advanceAfterAnswer: Boolean,
    /** The three answers the consent screen asked for, which the user can change here. */
    val crashReportingEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val personalizedAdsEnabled: Boolean,
    /**
     * Which side of 16 the user is on, `null` while nothing has asked — which is the state an app
     * updated from a version before the question is in. Correctable here on purpose: somebody answers
     * at 15 and is 16 the year after.
     */
    val overConsentAge: Boolean?,
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
     * Whether the screen is asking the user to confirm that they have turned 16. It asks before that
     * answer is applied, because one press unlocks three switches and the app cannot check the answer
     * itself.
     */
    val confirmingAge: Boolean = false,
) {
    /**
     * Answers checked as they're picked have to stay on screen, so moving on by itself can only be
     * switched on while that is off.
     */
    val canAdvanceAfterAnswer: Boolean get() = !checkAnswersImmediately

    /**
     * Whether the three consent switches may be answered at all: an adult may, and so may a user under
     * 16 whose parent or guardian has agreed ([ConsentAge]). They are drawn locked where not, and the
     * repository holds the same line against a stored answer whatever the screen does.
     */
    val canAnswerSwitches: Boolean get() = ConsentAge.mayAnswerSwitches(overConsentAge, parentalConsent)

    /**
     * Whether the age question is drawn on this screen at all. Only a user under 16 sees it: it is the
     * one answer here they can still change — somebody answers at 15 and is 16 the year after — and to
     * everyone else it is a question they cannot answer differently, which is clutter and nothing more.
     *
     * The parent or guardian's agreement is not offered here in either case: for an under-16 it is
     * locked in once given, and for everyone else it does not exist.
     */
    val underConsentAge: Boolean get() = overConsentAge == false
}

sealed interface SettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : SettingsUiEvent
    data class SetCheckAnswersImmediately(val enabled: Boolean) : SettingsUiEvent
    data class SetAdvanceAfterAnswer(val enabled: Boolean) : SettingsUiEvent
    data class SetCrashReporting(val enabled: Boolean) : SettingsUiEvent
    data class SetAnalytics(val enabled: Boolean) : SettingsUiEvent
    data class SetPersonalizedAds(val enabled: Boolean) : SettingsUiEvent

    /** Which side of 16 the user is on, changed here when they turn 16 or answer wrongly. */
    data class SetOverConsentAge(val overConsentAge: Boolean) : SettingsUiEvent

    /** The user says they have turned 16; the screen asks them to be sure before it counts. */
    data object ConfirmOverConsentAge : SettingsUiEvent

    /** Backing out of that question: the answer is not given and nothing changes. */
    data object DismissAgeConfirmation : SettingsUiEvent

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

    /** Whether the screen is asking the user to confirm that they have turned 16. */
    private val ageConfirmation = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            preferences.preferences,
            exams.current,
            identifierCopied,
            analytics.deviceIdentifier,
            ageConfirmation,
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
                    confirmingAge = false,
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
                if (!canAnswerSwitches()) return
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

            is SettingsUiEvent.SetOverConsentAge -> {
                // Turning 16 unlocks three switches in one press and the app has no way to check the
                // answer, so it asks first. Turning under 16 takes answers away and needs no question.
                if (event.overConsentAge) {
                    ageConfirmation.value = true
                } else {
                    answerTheAgeQuestion(overConsentAge = false)
                }
            }

            SettingsUiEvent.ConfirmOverConsentAge -> {
                ageConfirmation.value = false
                answerTheAgeQuestion(overConsentAge = true)
            }

            SettingsUiEvent.DismissAgeConfirmation -> ageConfirmation.value = false

            SettingsUiEvent.DeviceIdentifierCopied -> identifierCopied.value = true
        }
    }

    /**
     * Records the age answer and everything that follows from it.
     *
     * No analytics event: the age band is not something the app sends, and the documents list what is
     * sent. Under 16 withdraws the three answers exactly as it does on the consent screen — they are
     * not that user's to give, and leaving them on behind a locked row would keep answers the app may
     * not act on. Turning 16 again changes none of them.
     */
    private fun answerTheAgeQuestion(overConsentAge: Boolean) {
        preferences.setOverConsentAge(overConsentAge)
        if (!overConsentAge) {
            preferences.setCrashReportingEnabled(false)
            preferences.setAnalyticsEnabled(false)
            preferences.setPersonalizedAdsEnabled(false)
        }
        syncGates()
    }

    /**
     * Both things that act on an answer are told, because changing the age answer can change what the
     * three answers are worth: the SDK stops or starts, and the ad SDK learns whether this user is
     * under the age of consent and whether their ads may be personalized — which has to be right before
     * the next banner, not just before the next fullscreen request.
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
        ConsentAge.mayAnswerSwitches(it.overConsentAge, it.parentalConsent)
    }

    private fun state(
        preferences: Preferences,
        exam: CurrentExam,
        identifierCopied: Boolean,
        deviceIdentifier: String?,
        confirmingAge: Boolean,
    ) = SettingsUiState(
        language = preferences.language,
        checkAnswersImmediately = preferences.checkAnswersImmediately,
        advanceAfterAnswer = preferences.advanceAfterAnswer,
        crashReportingEnabled = preferences.crashReportingEnabled,
        analyticsEnabled = preferences.analyticsEnabled,
        personalizedAdsEnabled = preferences.personalizedAdsEnabled,
        overConsentAge = preferences.overConsentAge,
        parentalConsent = preferences.parentalConsent,
        exam = (exam as? CurrentExam.Ready)?.session?.let { ExamLabel(it.category, it.bank.language) },
        appVersion = version,
        deviceIdentifier = deviceIdentifier,
        deviceIdentifierCopied = identifierCopied,
        confirmingAge = confirmingAge,
    )
}
