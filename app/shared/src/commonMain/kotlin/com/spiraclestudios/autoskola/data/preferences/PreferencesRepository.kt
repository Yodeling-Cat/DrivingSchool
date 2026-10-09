package com.spiraclestudios.autoskola.data.preferences

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The exam the user is preparing for: a question bank (country + language) and a license group. */
data class TestSelection(val bankId: String, val category: LicenseCategory)

data class Preferences(
    /**
     * The privacy policy and terms version the user has accepted, 0 until they accept one. The
     * consent screen writes it, and anything below [CURRENT_LEGAL_VERSION] asks again.
     */
    val legalAcceptedVersion: Int,
    /**
     * Whether the first screen has been read. It agrees to nothing — that is what the consent screen
     * is for — but it is the fact that decides whether the disclaimer or the consent screen is drawn,
     * and it is stored for the same reason [legalAcceptedVersion] is: a rotation, or a process the
     * system killed, has to come back to the screen the user was on rather than to the first one.
     */
    val disclaimerRead: Boolean,
    /**
     * Whether the user lets us collect crash reports. Deliberately its own answer rather than part
     * of accepting the terms: consent has to be specific to what it covers.
     */
    val crashReportingEnabled: Boolean,
    /** Whether the user lets us collect usage analytics. Independent of [crashReportingEnabled]. */
    val analyticsEnabled: Boolean,
    /**
     * Whether the user lets ads be chosen for them, using the device's advertising ID. Off until they
     * say so, and its own answer like the other two: personalizing is a use of personal data, so it
     * has to be asked for rather than assumed.
     *
     * It decides which *kind* of ad is requested, never whether one is: the app shows ads to everyone
     * who has not paid for them to stop, and refusing here means generic ads (`plans/ads.md`).
     */
    val personalizedAdsEnabled: Boolean,
    /** `null` until the user has picked an exam in onboarding. */
    val selection: TestSelection?,
    /** UI language; follows the device language until the user picks one. */
    val language: AppLanguage,
    /** Reveal the correct answer right after the user picks one, and lock the answer. */
    val checkAnswersImmediately: Boolean,
    /** Move on to the next question as soon as the user picks an answer. Off in check-immediately mode. */
    val advanceAfterAnswer: Boolean,
    /**
     * The identifier our analytics files this device's data under, remembered the first time the SDK
     * reports one. It is kept rather than read on demand because the SDK has none while it is stopped:
     * a user who has switched both answers off and restarts the app is exactly the user asking us to
     * find or delete what we hold, and the one thing that lets us do it is this. `null` until
     * something has been sent, which is the only state in which nothing is filed under anything.
     */
    val deviceIdentifier: String?,
    /**
     * Which age band the user is in, `null` until the consent screen asks. 16 is the age at which a
     * user can agree to crash reports and usage statistics alone, and below it only a parent or
     * guardian can, on their behalf; 18 is the age from which personalized ads are theirs to answer
     * (see [ConsentAge]).
     */
    val ageBand: AgeBand?,
    /**
     * Whether a parent or guardian has agreed on behalf of a user under 16. Only meaningful with
     * [ageBand] [AgeBand.Under16], and it is what lets the crash-report and statistics switches be
     * answered at all then. It never unlocks personalized ads.
     */
    val parentalConsent: Boolean,
)

/**
 * The user's choices, stored in [Settings]. Unlike attempts, these are read synchronously
 * ([preferences] is a [StateFlow]), because the first frame needs them (see AGENTS.md).
 */
class PreferencesRepository(private val settings: Settings) {

    private val state = MutableStateFlow(read())
    val preferences: StateFlow<Preferences> = state.asStateFlow()

    /** Records that the user agreed to the documents at [version]. */
    fun acceptLegal(version: Int) {
        settings.putInt(KEY_LEGAL_VERSION, version)
        state.update { it.copy(legalAcceptedVersion = version) }
    }

    /**
     * Records that the first screen has been read, which is what puts the consent screen behind it.
     *
     * Stored rather than kept in the composition: it is about the user, not about this particular
     * frame, and a fact that decides which screen opens has to outlive the activity that read it.
     */
    fun setDisclaimerRead(read: Boolean) {
        settings.putBoolean(KEY_DISCLAIMER_READ, read)
        state.update { it.copy(disclaimerRead = read) }
    }

    /** The answer to "send crash reports?", which the user can change at any time in Settings. */
    fun setCrashReportingEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_CRASH_REPORTING, enabled)
        state.update { it.copy(crashReportingEnabled = enabled) }
    }

    /** The answer to "send usage analytics?", which the user can change at any time in Settings. */
    fun setAnalyticsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_ANALYTICS, enabled)
        state.update { it.copy(analyticsEnabled = enabled) }
    }

    /** The answer to "personalize your ads?", which the user can change at any time in Settings. */
    fun setPersonalizedAdsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_PERSONALIZED_ADS, enabled)
        state.update { it.copy(personalizedAdsEnabled = enabled) }
    }

    /**
     * Remembers [identifier], the analytics service's own name for this device. Written when the SDK
     * reports one (see `AnalyticsRepository.refreshDeviceIdentifier`) and never cleared by a switch:
     * it is the only thing that lets the user ask us about what was already filed under it, and the
     * one thing they cannot find anywhere else.
     */
    fun setDeviceIdentifier(identifier: String) {
        // Nothing clears it: a reset replaces the identifier rather than removing the record of it,
        // and an identifier that was never created is simply absent.
        settings.putString(KEY_DEVICE_IDENTIFIER, identifier)
        state.update { it.copy(deviceIdentifier = identifier) }
    }

    /**
     * The answer to "how old are you?", which the consent screen asks and Settings lets the user
     * correct later — somebody answers at 15 and is 16 the year after.
     *
     * It records this answer and nothing else. In particular it does not touch the three consent
     * answers: they are the user's, given by their own presses, and what the age answer decides is
     * whether they may be answered and whether they count at all ([ConsentAge]). An app that filled
     * one in — or cleared one — because the user answered a different question would be answering a
     * question it is supposed to ask.
     */
    fun setAgeBand(band: AgeBand?) {
        if (band == null) {
            settings.remove(KEY_AGE_BAND)
        } else {
            settings.putString(KEY_AGE_BAND, band.name)
        }
        state.update { it.copy(ageBand = band) }
    }

    /**
     * Whether a parent or guardian agreed on behalf of a user under 16: what lets the three switches
     * be answered at all, and nothing more. Written and withdrawn without touching the answers.
     */
    fun setParentalConsent(given: Boolean) {
        settings.putBoolean(KEY_PARENTAL_CONSENT, given)
        state.update { it.copy(parentalConsent = given) }
    }

    fun selectLanguage(language: AppLanguage) {
        settings.putString(KEY_LANGUAGE, language.code)
        state.update { it.copy(language = language) }
    }

    /**
     * An answer that is checked as it's picked has to stay on screen, so the two settings can't both
     * be on: checking answers turns auto-advance off, and keeps it off.
     */
    fun setCheckAnswersImmediately(enabled: Boolean) {
        settings.putBoolean(KEY_CHECK_IMMEDIATELY, enabled)
        settings.putBoolean(KEY_ADVANCE_AFTER_ANSWER, false)
        state.update { it.copy(checkAnswersImmediately = enabled, advanceAfterAnswer = false) }
    }

    /** Refused while answers are checked immediately: a checked answer stays on its own question. */
    fun setAdvanceAfterAnswer(enabled: Boolean) {
        val advanceAfterAnswer = enabled && !state.value.checkAnswersImmediately
        settings.putBoolean(KEY_ADVANCE_AFTER_ANSWER, advanceAfterAnswer)
        state.update { it.copy(advanceAfterAnswer = advanceAfterAnswer) }
    }

    fun select(selection: TestSelection) {
        settings.putString(KEY_BANK, selection.bankId)
        settings.putString(KEY_CATEGORY, selection.category.name)
        state.update { it.copy(selection = selection) }
    }

    private fun read() = Preferences(
        legalAcceptedVersion = settings.getInt(KEY_LEGAL_VERSION, 0),
        disclaimerRead = settings.getBoolean(KEY_DISCLAIMER_READ, false),
        crashReportingEnabled = settings.getBoolean(KEY_CRASH_REPORTING, false),
        analyticsEnabled = settings.getBoolean(KEY_ANALYTICS, false),
        personalizedAdsEnabled = settings.getBoolean(KEY_PERSONALIZED_ADS, false),
        selection = readSelection(),
        language = AppLanguage.fromCode(settings.getStringOrNull(KEY_LANGUAGE)) ?: AppLanguage.system(),
        checkAnswersImmediately = settings.getBoolean(KEY_CHECK_IMMEDIATELY, true),
        advanceAfterAnswer = settings.getBoolean(KEY_ADVANCE_AFTER_ANSWER, false),
        deviceIdentifier = settings.getStringOrNull(KEY_DEVICE_IDENTIFIER),
        ageBand = AgeBand.fromName(settings.getStringOrNull(KEY_AGE_BAND)),
        parentalConsent = settings.getBoolean(KEY_PARENTAL_CONSENT, false),
    )

    private fun readSelection(): TestSelection? {
        val bank = settings.getStringOrNull(KEY_BANK) ?: return null
        val category = LicenseCategory.fromName(settings.getStringOrNull(KEY_CATEGORY)) ?: return null
        return TestSelection(bank, category)
    }

    private companion object {
        const val KEY_LEGAL_VERSION = "legal_accepted_version"
        const val KEY_DISCLAIMER_READ = "disclaimer_read"
        const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        const val KEY_ANALYTICS = "analytics_enabled"

        /**
         * Not `ads_enabled`, which an earlier version stored for "show me ads". A yes to that was never a
         * yes to personalizing them, so the old key is left unread rather than reinterpreted.
         */
        const val KEY_PERSONALIZED_ADS = "personalized_ads_enabled"
        const val KEY_BANK = "question_bank"
        const val KEY_CATEGORY = "license_category"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_CHECK_IMMEDIATELY = "check_answers_immediately"
        const val KEY_ADVANCE_AFTER_ANSWER = "advance_after_answer"
        const val KEY_DEVICE_IDENTIFIER = "device_identifier"
        const val KEY_AGE_BAND = "age_band"
        const val KEY_PARENTAL_CONSENT = "parental_consent"
    }
}
