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

/**
 * The exam-selection wizard: the language to take the test in, then the license category. Each app
 * bundles one country's banks (see docs/countries.md), so there is no country to choose, and a
 * test in one language alone has no language to choose either: it opens on the categories.
 */
sealed interface ExamSelectionStep {
    data class ChooseBankLanguage(val banks: List<QuestionBankInfo>) : ExamSelectionStep
    data class ChooseCategory(val bank: QuestionBankInfo) : ExamSelectionStep
}

/** The step the wizard opens on for [banks]: their languages, or the categories of the only one. */
internal fun firstStep(banks: List<QuestionBankInfo>): ExamSelectionStep =
    banks.singleOrNull()?.let(ExamSelectionStep::ChooseCategory) ?: ExamSelectionStep.ChooseBankLanguage(banks)

data class ExamSelectionUiState(
    /** `null` while loading. */
    val banks: List<QuestionBankInfo>?,
    val step: ExamSelectionStep,
    val language: AppLanguage,
    /** An exam was picked, so the screen can close. */
    val done: Boolean = false,
) {
    /**
     * True while there is a step behind the one on screen. Back then walks the wizard instead of
     * leaving it; on the first step there is nothing to undo, so back means leaving the screen.
     */
    val hasPreviousStep: Boolean
        get() = step != firstStep(banks.orEmpty())

    /**
     * How far through the wizard the step on screen is: two steps when there is a language to
     * choose, and one when there is not.
     */
    val progress: StepProgress
        get() {
            val total = if (banks.orEmpty().size > 1) 2 else 1
            return StepProgress(current = if (step is ExamSelectionStep.ChooseCategory) total else 1, total = total)
        }
}

/** The step on screen, counted from 1, out of the [total] steps the wizard walks. */
data class StepProgress(val current: Int, val total: Int)

sealed interface ExamSelectionUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ExamSelectionUiEvent
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
    private val step = MutableStateFlow(firstStep(emptyList()))
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
            ExamSelectionUiState(null, step.value, preferences.preferences.value.language),
        )

    fun onEvent(event: ExamSelectionUiEvent) {
        when (event) {
            is ExamSelectionUiEvent.SelectLanguage -> {
                analytics.languageChanged(preferences.preferences.value.language, event.language)
                preferences.selectLanguage(event.language)
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

            // The only step that can be behind another is the first one.
            ExamSelectionUiEvent.StepBack -> step.value = firstStep(banksState.value.orEmpty())
        }
    }
}
