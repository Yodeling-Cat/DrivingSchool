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
     * Whether the user lets us collect crash reports. Deliberately its own answer rather than part
     * of accepting the terms: consent has to be specific to what it covers.
     */
    val crashReportingEnabled: Boolean,
    /** Whether the user lets us collect usage analytics. Independent of [crashReportingEnabled]. */
    val analyticsEnabled: Boolean,
    /** `null` until the user has picked an exam in onboarding. */
    val selection: TestSelection?,
    /** UI language; follows the device language until the user picks one. */
    val language: AppLanguage,
    /** Reveal the correct answer right after the user picks one, and lock the answer. */
    val checkAnswersImmediately: Boolean,
    /** Move on to the next question as soon as the user picks an answer. Off in check-immediately mode. */
    val advanceAfterAnswer: Boolean,
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
        crashReportingEnabled = settings.getBoolean(KEY_CRASH_REPORTING, false),
        analyticsEnabled = settings.getBoolean(KEY_ANALYTICS, false),
        selection = readSelection(),
        language = AppLanguage.fromCode(settings.getStringOrNull(KEY_LANGUAGE)) ?: AppLanguage.system(),
        checkAnswersImmediately = settings.getBoolean(KEY_CHECK_IMMEDIATELY, true),
        advanceAfterAnswer = settings.getBoolean(KEY_ADVANCE_AFTER_ANSWER, false),
    )

    private fun readSelection(): TestSelection? {
        val bank = settings.getStringOrNull(KEY_BANK) ?: return null
        val category = LicenseCategory.fromName(settings.getStringOrNull(KEY_CATEGORY)) ?: return null
        return TestSelection(bank, category)
    }

    private companion object {
        const val KEY_LEGAL_VERSION = "legal_accepted_version"
        const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        const val KEY_ANALYTICS = "analytics_enabled"
        const val KEY_BANK = "question_bank"
        const val KEY_CATEGORY = "license_category"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_CHECK_IMMEDIATELY = "check_answers_immediately"
        const val KEY_ADVANCE_AFTER_ANSWER = "advance_after_answer"
    }
}
