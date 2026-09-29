package uno.lux.autoskola.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
import uno.lux.autoskola.stats.isFullyAnswered
import uno.lux.autoskola.stats.resumeIndex

data class PracticeQuestion(
    val question: Question,
    /** For the image loader; `null` for text-only questions. */
    val imageUri: String?,
)

sealed interface PracticeUiState {
    data object Loading : PracticeUiState

    /** The attempt was discarded or is gone, so the screen should close. */
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
        /** Every question has an answer, so the test can be finished. */
        val allAnswered: Boolean,
        /** The test is finished: every answer is revealed and locked. */
        val finished: Boolean,
        /** The test was just finished here, so the screen should show its results. */
        val openResults: Boolean = false,
        val elapsedSeconds: Long = 0L,
        val showTimeLimitToast: Boolean = false,
    ) : PracticeUiState
}

sealed interface PracticeUiEvent {
    data class Answer(val questionIndex: Int, val answerIndex: Int) : PracticeUiEvent
    data object Finish : PracticeUiEvent

    /** The screen showed the results that [PracticeUiState.Ready.openResults] asked for. */
    data object ResultsOpened : PracticeUiEvent

    /** Whether the screen is currently in the foreground and active. */
    data class Foreground(val inForeground: Boolean) : PracticeUiEvent

    /** The screen displayed the time limit toast that [PracticeUiState.Ready.showTimeLimitToast] asked for. */
    data object TimeLimitToastShown : PracticeUiEvent
}

class PracticeViewModel(
    private val attemptId: String,
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val allottedTimeSeconds: Long = TestAttempt.OFFICIAL_DURATION_SECONDS,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Set when the attempt first loads; the question order and start page don't change after that. */
    private var questions: List<PracticeQuestion>? = null
    private var initialPage: Int? = null

    private var isForeground = false
    private var isFinished = false
    private var timeLimitNotified = false
    private var timerJob: Job? = null

    private val elapsedSeconds = MutableStateFlow<Long?>(null)
    private val showTimeLimitToast = MutableStateFlow(false)
    private val openResults = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PracticeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(PracticeUiState.Loading)
            val attemptFlow = session.attempt(attemptId).onEach { attempt ->
                if (attempt != null) {
                    onAttemptLoaded(attempt)
                }
            }
            combine(attemptFlow, openResults, elapsedSeconds, showTimeLimitToast) {
                    attempt,
                    openResults,
                    elapsed,
                    showToast,
                ->
                if (attempt == null) {
                    PracticeUiState.Closed
                } else {
                    val effectiveElapsed = elapsed ?: attempt.elapsedSeconds
                    ready(session, attempt, openResults, effectiveElapsed, showToast)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeUiState.Loading)

    private fun onAttemptLoaded(attempt: TestAttempt) {
        if (elapsedSeconds.value == null) {
            val initial = attempt.elapsedSeconds
            elapsedSeconds.value = initial
            if (initial >= allottedTimeSeconds) {
                timeLimitNotified = true
            }
        }
        isFinished = attempt.isFinished
        updateTimer()
    }

    private fun updateTimer() {
        val canTick = isForeground && !isFinished && elapsedSeconds.value != null
        if (canTick) {
            if (timerJob == null) {
                timerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1_000)
                        tick()
                    }
                }
            }
        } else {
            stopTimer()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun tick() {
        if (isFinished) return
        val current = (elapsedSeconds.value ?: 0L) + 1
        elapsedSeconds.value = current
        if (!timeLimitNotified && current >= allottedTimeSeconds) {
            timeLimitNotified = true
            showTimeLimitToast.value = true
        }
        if (current % 5 == 0L) {
            saveElapsed(current)
        }
    }

    private fun saveElapsed(seconds: Long) {
        val session = session.value ?: return
        viewModelScope.launch {
            session.updateElapsed(attemptId, seconds)
        }
    }

    fun onEvent(event: PracticeUiEvent) {
        val session = session.value ?: return
        when (event) {
            is PracticeUiEvent.Answer -> {
                val question = questions?.getOrNull(event.questionIndex)?.question ?: return
                viewModelScope.launch {
                    session.answer(attemptId, question, event.answerIndex)
                    elapsedSeconds.value?.let { session.updateElapsed(attemptId, it) }
                }
            }

            PracticeUiEvent.Finish -> viewModelScope.launch {
                isFinished = true
                stopTimer()
                val finalElapsed = elapsedSeconds.value ?: 0L
                session.finish(attemptId, finalElapsed)
                openResults.value = true
            }

            PracticeUiEvent.ResultsOpened -> openResults.value = false

            is PracticeUiEvent.Foreground -> {
                isForeground = event.inForeground
                updateTimer()
                if (!event.inForeground) {
                    elapsedSeconds.value?.let { saveElapsed(it) }
                }
            }

            PracticeUiEvent.TimeLimitToastShown -> showTimeLimitToast.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        elapsedSeconds.value?.let { saveElapsed(it) }
    }

    private fun ready(
        session: ExamSession,
        attempt: TestAttempt,
        openResults: Boolean,
        elapsedSeconds: Long,
        showTimeLimitToast: Boolean,
    ): PracticeUiState.Ready {
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
            allAnswered = attempt.isFullyAnswered,
            finished = attempt.isFinished,
            openResults = openResults && attempt.isFinished,
            elapsedSeconds = elapsedSeconds,
            showTimeLimitToast = showTimeLimitToast,
        )
    }
}
