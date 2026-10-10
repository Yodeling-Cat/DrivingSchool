package com.spiraclestudios.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.languageChanged
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.preferences.TestSelection
import com.spiraclestudios.autoskola.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Countries we know about, in display order. Not all have a question bank yet (docs/data-sources.md). */
private val KNOWN_COUNTRIES = listOf("SK", "CZ")

/** One country to choose from, shown even before it has a question bank (grayed out). */
data class CountryOption(val country: String, val available: Boolean)

/**
 * The exam-selection wizard has three steps, each narrowing the choice made in the last one. A country
 * whose test is in one language alone skips the language step.
 */
sealed interface ExamSelectionStep {
    data object ChooseCountry : ExamSelectionStep
    data class ChooseBankLanguage(val country: String, val banks: List<QuestionBankInfo>) : ExamSelectionStep
    data class ChooseCategory(val bank: QuestionBankInfo) : ExamSelectionStep
}

data class ExamSelectionUiState(
    /** `null` while loading. */
    val banks: List<QuestionBankInfo>?,
    val step: ExamSelectionStep,
    val language: AppLanguage,
    /** An exam was picked, so the screen can close. */
    val done: Boolean = false,
) {
    val countryOptions: List<CountryOption>
        get() = KNOWN_COUNTRIES.map { country ->
            CountryOption(country, available = banks.orEmpty().any { it.country == country })
        }

    /**
     * True while there is a step behind the one on screen. Back then walks the wizard instead of
     * leaving it; on the first step there is nothing to undo, so back means leaving the screen.
     */
    val hasPreviousStep: Boolean
        get() = step != firstStep(banks.orEmpty())
}

/**
 * The step the wizard opens on. An app bundles one country's banks (see AGENTS.md, "Countries"), so
 * it has no country to choose and opens on that country's next step.
 */
internal fun firstStep(banks: List<QuestionBankInfo>): ExamSelectionStep {
    val country = banks.map { it.country }.distinct().singleOrNull() ?: return ExamSelectionStep.ChooseCountry
    return stepAfterCountry(country, banks) ?: ExamSelectionStep.ChooseCountry
}

/** What choosing [country] leads to: its languages, or straight to its categories when it has one. */
private fun stepAfterCountry(country: String, banks: List<QuestionBankInfo>): ExamSelectionStep? =
    banks.filter { it.country == country }.let { banksForCountry ->
        when (banksForCountry.size) {
            0 -> null

            // A test in one language alone leaves no language to choose.
            1 -> ExamSelectionStep.ChooseCategory(banksForCountry.single())

            else -> ExamSelectionStep.ChooseBankLanguage(country, banksForCountry)
        }
    }

sealed interface ExamSelectionUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ExamSelectionUiEvent
    data class ChooseCountry(val country: String) : ExamSelectionUiEvent
    data class ChooseBank(val bank: QuestionBankInfo) : ExamSelectionUiEvent
    data class ChooseCategory(val category: LicenseCategory) : ExamSelectionUiEvent

    /** Back to the previous step of the wizard (not navigation - see [ExamSelectionRoute]). */
    data object StepBack : ExamSelectionUiEvent
}

class ExamSelectionViewModel(
    private val preferences: PreferencesRepository,
    private val banks: QuestionBankRepository,
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private val banksState = MutableStateFlow<List<QuestionBankInfo>?>(null)
    private val step = MutableStateFlow<ExamSelectionStep>(ExamSelectionStep.ChooseCountry)
    private val done = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val index = banks.index()
            banksState.value = index
            step.value = firstStep(index)
        }
    }

    val uiState: StateFlow<ExamSelectionUiState> =
        combine(banksState, preferences.preferences, step, done) { banks, preferences, step, done ->
            ExamSelectionUiState(banks, step, preferences.language, done)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExamSelectionUiState(null, ExamSelectionStep.ChooseCountry, preferences.preferences.value.language),
        )

    fun onEvent(event: ExamSelectionUiEvent) {
        when (event) {
            is ExamSelectionUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
            }

            is ExamSelectionUiEvent.ChooseCountry -> {
                if (step.value != ExamSelectionStep.ChooseCountry) return
                step.value = stepAfterCountry(event.country, banksState.value.orEmpty()) ?: return
            }

            is ExamSelectionUiEvent.ChooseBank -> step.value = ExamSelectionStep.ChooseCategory(event.bank)

            is ExamSelectionUiEvent.ChooseCategory -> {
                val bank = (step.value as? ExamSelectionStep.ChooseCategory)?.bank ?: return
                preferences.select(TestSelection(bank.id, event.category))
                // Which exam people prepare for is the question the whole funnel is about, so it
                // is recorded with the two things that describe it and nothing that identifies
                // the user.
                analytics.capture(
                    AnalyticsEvent.ExamSelected,
                    mapOf("bank_id" to bank.id, "category" to event.category.name),
                )
                done.value = true
            }

            ExamSelectionUiEvent.StepBack -> {
                // Nothing is behind the step the wizard opened on.
                if (step.value != firstStep(banksState.value.orEmpty())) step.value = stepBack(step.value)
            }
        }
    }

    private fun banksOf(country: String) = banksState.value.orEmpty().filter { it.country == country }

    private fun stepBack(current: ExamSelectionStep): ExamSelectionStep = when (current) {
        is ExamSelectionStep.ChooseCategory -> {
            val banksForCountry = banksOf(current.bank.country)
            // Back past a language step that was skipped, as there was only one language.
            if (banksForCountry.size > 1) {
                ExamSelectionStep.ChooseBankLanguage(current.bank.country, banksForCountry)
            } else {
                ExamSelectionStep.ChooseCountry
            }
        }

        is ExamSelectionStep.ChooseBankLanguage -> ExamSelectionStep.ChooseCountry

        ExamSelectionStep.ChooseCountry -> ExamSelectionStep.ChooseCountry
    }
}
