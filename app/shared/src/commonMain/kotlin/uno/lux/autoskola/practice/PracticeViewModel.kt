package uno.lux.autoskola.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.Section
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.TestAttempt
import uno.lux.autoskola.stats.answersByIndex
import uno.lux.autoskola.stats.resumeIndex

data class PracticeQuestion(
    val question: Question,
    /** For the image loader; `null` for text-only questions. */
    val imageUri: String?,
)

sealed interface PracticeUiState {
    data object Loading : PracticeUiState

    /** The attempt is finished, discarded or gone, so the screen should close. */
    data object Closed : PracticeUiState

    data class Ready(
        val exam: ExamLabel,
        val kind: AttemptKind,
        val testNumber: Int?,
        val sections: List<Section>,
        val questions: List<PracticeQuestion>,
        /** Fixed when the attempt starts, so changing the setting mid-test can't unlock answers. */
        val checkImmediately: Boolean,
        /** Question index → answer index. */
        val answers: Map<Int, Int>,
        /** Where to start: the first unanswered question when the test opened. */
        val initialPage: Int,
    ) : PracticeUiState
}

sealed interface PracticeUiEvent {
    data class Answer(val questionIndex: Int, val answerIndex: Int) : PracticeUiEvent
    data object Finish : PracticeUiEvent
}

class PracticeViewModel(
    private val attemptId: String,
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Set when the attempt first loads; the question order and start page don't change after that. */
    private var questions: List<PracticeQuestion>? = null
    private var initialPage: Int? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PracticeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(PracticeUiState.Loading)
            session.openAttempt(attemptId).map { attempt ->
                if (attempt == null) PracticeUiState.Closed else ready(session, attempt)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeUiState.Loading)

    fun onEvent(event: PracticeUiEvent) {
        val session = session.value ?: return
        when (event) {
            is PracticeUiEvent.Answer -> {
                val question = questions?.getOrNull(event.questionIndex)?.question ?: return
                viewModelScope.launch { session.answer(attemptId, question, event.answerIndex) }
            }
            PracticeUiEvent.Finish -> viewModelScope.launch { session.finish(attemptId) }
        }
    }

    private fun ready(session: ExamSession, attempt: TestAttempt): PracticeUiState.Ready {
        val questions = questions ?: session.questionsOf(attempt)
            .map { PracticeQuestion(it, banks.imageUri(session.bank, it)) }
            .also { questions = it }
        val initialPage = initialPage ?: attempt.resumeIndex().also { initialPage = it }
        return PracticeUiState.Ready(
            exam = ExamLabel(session.category, session.bank.language),
            kind = attempt.kind,
            testNumber = attempt.testNumber,
            sections = session.bank.sections,
            questions = questions,
            checkImmediately = attempt.checkImmediately,
            answers = attempt.answersByIndex(),
            initialPage = initialPage,
        )
    }
}
