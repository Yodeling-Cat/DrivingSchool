package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uno.lux.autoskola.answers.PracticeAnswers
import uno.lux.autoskola.data.answers.PracticeAnswerRepository
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession

sealed interface SituationUiState {
    data object Loading : SituationUiState

    /**
     * The material has no such situation, which is what a screen restored from before the bank
     * changed finds; the screen closes rather than show a drawing that isn't the one it was opened
     * with.
     */
    data object Closed : SituationUiState

    data class Ready(
        /** Every situation of the exam, which the screen swipes through. */
        val situations: List<SituationItem>,
        /** Which one to open on: the situation the route named, counted from zero. */
        val initialPage: Int,
        /** What the driver has answered about this situation's questions: question id → option. */
        val answers: Map<Int, Int> = emptyMap(),
    ) : SituationUiState
}

sealed interface SituationUiEvent {
    /** Records the option the driver picked. Answering again replaces the answer that was there. */
    data class Answer(val questionId: Int, val chosen: Int) : SituationUiEvent
}

/**
 * One situation, opened from the list: its drawing, and every question the exam asks about it, with
 * what the driver has answered while reading. Reading and answering are all this screen does -
 * running a situation's questions as a test belongs to the list, which drills every situation at
 * once, in the practice screen.
 */
class SituationViewModel(
    private val number: Int,
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val answers: PracticeAnswerRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** What the driver has answered while reading, which marks the situations they are done with. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val given: Flow<PracticeAnswers> = session.filterNotNull()
        .flatMapLatest { answers.answers(it.bank.id, it.bank.version) }

    val uiState: StateFlow<SituationUiState> = combine(session, given) { session, given ->
        if (session == null) {
            SituationUiState.Loading
        } else {
            val situations = situationItems(session, banks, given)
            // A situation the material doesn't have any more - a route restored from before the bank
            // changed - closes the screen rather than opening another one.
            situations.getOrNull(number - 1)?.let {
                SituationUiState.Ready(
                    situations = situations,
                    initialPage = number - 1,
                    answers = given.byQuestion,
                )
            } ?: SituationUiState.Closed
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SituationUiState.Loading)

    fun onEvent(event: SituationUiEvent) {
        val session = session.value ?: return
        when (event) {
            is SituationUiEvent.Answer -> viewModelScope.launch {
                answers.answer(session.bank.id, session.bank.version, event.questionId, event.chosen)
            }
        }
    }
}
