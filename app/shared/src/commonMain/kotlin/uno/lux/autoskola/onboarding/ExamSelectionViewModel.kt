package uno.lux.autoskola.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.bank.QuestionBankInfo
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.i18n.AppLanguage

data class ExamSelectionUiState(
    /** `null` while loading. */
    val banks: List<QuestionBankInfo>?,
    val current: TestSelection?,
    val language: AppLanguage,
    /** An exam was picked, so the screen can close. */
    val done: Boolean = false,
)

sealed interface ExamSelectionUiEvent {
    data class SelectLanguage(val language: AppLanguage) : ExamSelectionUiEvent
    data class Select(val selection: TestSelection) : ExamSelectionUiEvent
}

class ExamSelectionViewModel(private val preferences: PreferencesRepository, banks: QuestionBankRepository) :
    ViewModel() {

    private val done = MutableStateFlow(false)

    val uiState: StateFlow<ExamSelectionUiState> =
        combine(
            flow { emit(banks.index()) }.onStart<List<QuestionBankInfo>?> { emit(null) },
            preferences.preferences,
            done,
        ) { banks, preferences, done ->
            ExamSelectionUiState(banks, preferences.selection, preferences.language, done)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            preferences.preferences.value.let { ExamSelectionUiState(null, it.selection, it.language) },
        )

    fun onEvent(event: ExamSelectionUiEvent) {
        when (event) {
            is ExamSelectionUiEvent.SelectLanguage -> preferences.selectLanguage(event.language)

            is ExamSelectionUiEvent.Select -> {
                preferences.select(event.selection)
                done.value = true
            }
        }
    }
}
