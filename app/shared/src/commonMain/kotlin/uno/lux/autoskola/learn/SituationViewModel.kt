package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.situations.situations

sealed interface SituationUiState {
    data object Loading : SituationUiState

    /**
     * The material has no such situation, which is what a screen restored from before the bank
     * changed finds; the screen closes rather than show a drawing that isn't the one it was opened
     * with.
     */
    data object Closed : SituationUiState

    data class Ready(
        val situation: SituationItem,
        /** What the driver has answered about this situation's questions: question id → option. */
        val answers: Map<Int, Int> = emptyMap(),
        /**
         * An attempt to open, once the driver has asked to drill this situation; the screen
         * navigates to it and then sends [SituationUiEvent.AttemptOpened].
         */
        val openAttemptId: String? = null,
    ) : SituationUiState
}

sealed interface SituationUiEvent {
    /** Records the option the driver picked. Answering again replaces the answer that was there. */
    data class Answer(val questionId: Int, val chosen: Int) : SituationUiEvent

    /** Drills this situation's questions, in the practice screen. */
    data object Drill : SituationUiEvent

    /** The screen opened the attempt [SituationUiState.Ready.openAttemptId] asked for. */
    data object AttemptOpened : SituationUiEvent
}

/** One situation, opened from the list: its drawing, and every question asked about it. */
class SituationViewModel(
    private val number: Int,
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val preferences: PreferencesRepository,
    private val answers: PracticeAnswerRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)

    /** What the driver has answered while reading, kept with the bank version being read. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val given: Flow<PracticeAnswers> = session.filterNotNull()
        .flatMapLatest { answers.answers(it.bank.id, it.bank.version) }

    val uiState: StateFlow<SituationUiState> = combine(session, given, openAttemptId) { session, given, open ->
        if (session == null) {
            SituationUiState.Loading
        } else {
            situationOf(session, number)?.let { situation ->
                SituationUiState.Ready(
                    situation = situation,
                    answers = given.byQuestion,
                    openAttemptId = open,
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

            SituationUiEvent.Drill -> viewModelScope.launch {
                val questions = questionsOf(session, number)
                val checkImmediately = preferences.preferences.value.checkAnswersImmediately
                openAttemptId.value = session.startSituations(questions, checkImmediately)?.id
            }

            SituationUiEvent.AttemptOpened -> openAttemptId.value = null
        }
    }

    /** The questions about situation [number], as the bank holds them. */
    private fun questionsOf(session: ExamSession, number: Int) =
        situations(session.bank, session.category).getOrNull(number - 1)?.questions.orEmpty()

    private fun situationOf(session: ExamSession, number: Int) = situationItems(session, banks).getOrNull(number - 1)
}
