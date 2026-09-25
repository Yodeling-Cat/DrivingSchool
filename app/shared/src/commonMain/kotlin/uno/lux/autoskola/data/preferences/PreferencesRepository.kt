package uno.lux.autoskola.data.preferences

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.i18n.AppLanguage

/** The exam the user is preparing for: a question bank (country + language) and a license group. */
data class TestSelection(
    val bankId: String,
    val category: LicenseCategory,
)

data class Preferences(
    val disclaimerAccepted: Boolean,
    /** `null` until the user has picked an exam in onboarding. */
    val selection: TestSelection?,
    /** UI language; follows the device language until the user picks one. */
    val language: AppLanguage,
    /** Reveal the correct answer right after the user picks one, and lock the answer. */
    val checkAnswersImmediately: Boolean,
)

/**
 * The user's choices, stored in [Settings]. Unlike attempts, these are read synchronously
 * ([preferences] is a [StateFlow]), because the first frame needs them (see AGENTS.md).
 */
class PreferencesRepository(private val settings: Settings) {

    private val state = MutableStateFlow(read())
    val preferences: StateFlow<Preferences> = state.asStateFlow()

    fun acceptDisclaimer() {
        settings.putBoolean(KEY_DISCLAIMER, true)
        state.update { it.copy(disclaimerAccepted = true) }
    }

    fun selectLanguage(language: AppLanguage) {
        settings.putString(KEY_LANGUAGE, language.code)
        state.update { it.copy(language = language) }
    }

    fun setCheckAnswersImmediately(enabled: Boolean) {
        settings.putBoolean(KEY_CHECK_IMMEDIATELY, enabled)
        state.update { it.copy(checkAnswersImmediately = enabled) }
    }

    fun select(selection: TestSelection) {
        settings.putString(KEY_BANK, selection.bankId)
        settings.putString(KEY_CATEGORY, selection.category.name)
        state.update { it.copy(selection = selection) }
    }

    private fun read() = Preferences(
        disclaimerAccepted = settings.getBoolean(KEY_DISCLAIMER, false),
        selection = readSelection(),
        language = AppLanguage.fromCode(settings.getStringOrNull(KEY_LANGUAGE)) ?: AppLanguage.system(),
        checkAnswersImmediately = settings.getBoolean(KEY_CHECK_IMMEDIATELY, false),
    )

    private fun readSelection(): TestSelection? {
        val bank = settings.getStringOrNull(KEY_BANK) ?: return null
        val category = LicenseCategory.fromName(settings.getStringOrNull(KEY_CATEGORY)) ?: return null
        return TestSelection(bank, category)
    }

    private companion object {
        const val KEY_DISCLAIMER = "disclaimer_accepted"
        const val KEY_BANK = "question_bank"
        const val KEY_CATEGORY = "license_category"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_CHECK_IMMEDIATELY = "check_answers_immediately"
    }
}
