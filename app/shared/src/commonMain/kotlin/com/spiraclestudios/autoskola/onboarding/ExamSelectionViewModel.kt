package com.spiraclestudios.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
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

/** The exam-selection wizard has three steps, each narrowing the choice made in the last one. */
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
) : ViewModel() {

    private val banksState = MutableStateFlow<List<QuestionBankInfo>?>(null)
    private val step = MutableStateFlow<ExamSelectionStep>(ExamSelectionStep.ChooseCountry)
    private val done = MutableStateFlow(false)

    init {
        viewModelScope.launch { banksState.value = banks.index() }
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
            is ExamSelectionUiEvent.SelectLanguage -> preferences.selectLanguage(event.language)

            is ExamSelectionUiEvent.ChooseCountry -> {
                val banksForCountry = banksState.value.orEmpty().filter { it.country == event.country }
                if (banksForCountry.isNotEmpty()) {
                    step.value = ExamSelectionStep.ChooseBankLanguage(event.country, banksForCountry)
                }
            }

            is ExamSelectionUiEvent.ChooseBank -> step.value = ExamSelectionStep.ChooseCategory(event.bank)

            is ExamSelectionUiEvent.ChooseCategory -> {
                val bank = (step.value as? ExamSelectionStep.ChooseCategory)?.bank ?: return
                preferences.select(TestSelection(bank.id, event.category))
                done.value = true
            }

            ExamSelectionUiEvent.StepBack -> step.value = stepBack(step.value)
        }
    }

    private fun stepBack(current: ExamSelectionStep): ExamSelectionStep = when (current) {
        is ExamSelectionStep.ChooseCategory ->
            ExamSelectionStep.ChooseBankLanguage(
                current.bank.country,
                banksState.value.orEmpty().filter { it.country == current.bank.country },
            )

        is ExamSelectionStep.ChooseBankLanguage -> ExamSelectionStep.ChooseCountry

        ExamSelectionStep.ChooseCountry -> ExamSelectionStep.ChooseCountry
    }
}
