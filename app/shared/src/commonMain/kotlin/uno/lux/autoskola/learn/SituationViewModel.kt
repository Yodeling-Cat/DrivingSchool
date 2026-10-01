package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository

sealed interface SituationUiState {
    data object Loading : SituationUiState

    /**
     * The material has no such situation, which is what a screen restored from before the bank
     * changed finds; the screen closes rather than show a drawing that isn't the one it was opened
     * with.
     */
    data object Closed : SituationUiState

    data class Ready(val situation: SituationItem) : SituationUiState
}

/** One situation, opened from the list: its drawing, and every question asked about it. */
class SituationViewModel(private val number: Int, exams: ExamRepository, private val banks: QuestionBankRepository) :
    ViewModel() {

    val uiState: StateFlow<SituationUiState> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .map { session ->
            if (session == null) {
                SituationUiState.Loading
            } else {
                situationItems(session, banks).getOrNull(number - 1)
                    ?.let { SituationUiState.Ready(it) }
                    ?: SituationUiState.Closed
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SituationUiState.Loading)
}
