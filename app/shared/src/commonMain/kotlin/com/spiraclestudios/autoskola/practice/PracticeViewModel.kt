package com.spiraclestudios.autoskola.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.Section
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.stats.answersByIndex
import com.spiraclestudios.autoskola.stats.isFullyAnswered
import com.spiraclestudios.autoskola.stats.resumeIndex
import com.spiraclestudios.autoskola.stats.wasAnswered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

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
        /** The driver left the test, so the screen should close. */
        val close: Boolean = false,
        /** Page the test moves on to after an answer, once the screen has reported the move. */
        val advanceToPage: Int? = null,
        val elapsedSeconds: Long = 0L,
        /**
         * How long this test is given, so the screen can say when it has run out - or `null` when it
         * isn't timed at all, which is a situations drill.
         */
        val allottedSeconds: Long? = null,
        val showTimeLimitToast: Boolean = false,
    ) : PracticeUiState
}

sealed interface PracticeUiEvent {
    data class Answer(val questionIndex: Int, val answerIndex: Int) : PracticeUiEvent
    data object Finish : PracticeUiEvent

    /**
     * The driver is leaving the test. What they answered stays as it is - and a test they answered
     * nothing in goes: there is no work in it to come back to.
     */
    data object Leave : PracticeUiEvent

    /** The screen showed the results that [PracticeUiState.Ready.openResults] asked for. */
    data object ResultsOpened : PracticeUiEvent

    /** The screen moved to the question [PracticeUiState.Ready.advanceToPage] asked for. */
    data object PageAdvanced : PracticeUiEvent

    /** Whether the screen is currently in the foreground and active. */
    data class Foreground(val inForeground: Boolean) : PracticeUiEvent

    /** The screen displayed the time limit toast that [PracticeUiState.Ready.showTimeLimitToast] asked for. */
    data object TimeLimitToastShown : PracticeUiEvent

    /** Debug builds only: fill in every question that is still unanswered, with a random option. */
    data object AnswerAllRandomly : PracticeUiEvent

    /** Debug builds only: fill in every question that is still unanswered, with the right option. */
    data object AnswerAllCorrectly : PracticeUiEvent
}

class PracticeViewModel(
    private val attemptId: String,
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val preferences: PreferencesRepository,
    private val practiceAnswers: PracticeAnswerRepository,
    private val allottedTimeSeconds: Long = TestAttempt.OFFICIAL_DURATION_SECONDS,
    /** Which option the debug shortcut picks, injected so a test can pin it down. */
    private val random: Random = Random.Default,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Set when the attempt first loads; the question order and start page don't change after that. */
    private var questions: List<PracticeQuestion>? = null

    /** The questions already answered, so the debug shortcut can fill in only the rest. */
    private var answered: Set<Int> = emptySet()
    private var initialPage: Int? = null

    private var isForeground = false
    private var isFinished = false
    private var checksImmediately = false
    private var allAnswered = false
    private var kind: AttemptKind = AttemptKind.PracticeTest

    /** How long the attempt being read is given; `null` when it isn't timed at all. */
    private var timeLimitSeconds: Long? = null
    private var timeLimitNotified = false
    private var timerJob: Job? = null

    private val elapsedSeconds = MutableStateFlow<Long?>(null)
    private val showTimeLimitToast = MutableStateFlow(false)
    private val openResults = MutableStateFlow(false)
    private val advanceToPage = MutableStateFlow<Int?>(null)
    private val close = MutableStateFlow(false)

    /** What the screen is asked to do once the work behind it is done: results, or closing. */
    private val screenRequests = combine(openResults, close) { openResults, close -> openResults to close }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PracticeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(PracticeUiState.Loading)
            val attemptFlow = session.attempt(attemptId).onEach { attempt ->
                if (attempt != null) {
                    onAttemptLoaded(attempt)
                }
            }
            combine(attemptFlow, screenRequests, advanceToPage, elapsedSeconds, showTimeLimitToast) {
                    attempt,
                    requests,
                    advanceTo,
                    elapsed,
                    showToast,
                ->
                val (openResults, close) = requests
                if (attempt == null) {
                    PracticeUiState.Closed
                } else {
                    val effectiveElapsed = elapsed ?: attempt.elapsedSeconds
                    ready(session, attempt, openResults, close, advanceTo, effectiveElapsed, showToast)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeUiState.Loading)

    private fun onAttemptLoaded(attempt: TestAttempt) {
        kind = attempt.kind
        // An exam test is given the official thirty minutes; a situations drill is reading practice
        // with no exam behind it, so its clock only counts how long the driver has been at it.
        timeLimitSeconds = if (attempt.kind == AttemptKind.Situations) null else allottedTimeSeconds
        if (elapsedSeconds.value == null) {
            val initial = attempt.elapsedSeconds
            elapsedSeconds.value = initial
            if (isOverTime(initial, timeLimitSeconds)) {
                timeLimitNotified = true
            }
        }
        isFinished = attempt.isFinished
        checksImmediately = attempt.checkImmediately
        allAnswered = attempt.isFullyAnswered
        answered = attempt.answersByIndex().keys
        updateTimer()
    }

    private fun updateTimer() {
        val canTick = isForeground && !isFinished && elapsedSeconds.value != null &&
            !stopsClockAtEnd(allAnswered, checksImmediately)
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
        if (!timeLimitNotified && isOverTime(current, timeLimitSeconds)) {
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
                val nextPage = nextPageAfter(event.questionIndex)
                viewModelScope.launch {
                    session.answer(attemptId, question, event.answerIndex)
                    recordSituationAnswer(session, question, event.answerIndex)
                    // The saved attempt comes back through the flow, which re-reads whether every
                    // question is answered and stops the clock on the answer that completes it.
                    elapsedSeconds.value?.let { session.updateElapsed(attemptId, it) }
                    // Move on only once the answer is saved, so no question is passed over unseen.
                    nextPage?.let { advanceToPage.value = it }
                }
            }

            PracticeUiEvent.Finish -> viewModelScope.launch {
                isFinished = true
                stopTimer()
                val finalElapsed = elapsedSeconds.value ?: 0L
                session.finish(attemptId, finalElapsed)
                openResults.value = true
            }

            // Leaving a test nobody answered a question of: there is nothing in it to come back to,
            // so the attempt goes rather than waiting to be picked up again. One that was answered
            // stays as it is, and the screen closes.
            PracticeUiEvent.Leave -> viewModelScope.launch {
                if (session.attempt(attemptId).first()?.wasAnswered == true) {
                    close.value = true
                } else {
                    session.discard(attemptId)
                }
            }

            PracticeUiEvent.ResultsOpened -> openResults.value = false

            PracticeUiEvent.PageAdvanced -> advanceToPage.value = null

            is PracticeUiEvent.Foreground -> {
                isForeground = event.inForeground
                updateTimer()
                if (!event.inForeground) {
                    elapsedSeconds.value?.let { saveElapsed(it) }
                }
            }

            PracticeUiEvent.TimeLimitToastShown -> showTimeLimitToast.value = false

            PracticeUiEvent.AnswerAllRandomly -> answerAll { random.nextInt(it.answers.size) }

            PracticeUiEvent.AnswerAllCorrectly -> answerAll { it.correct }
        }
    }

    /**
     * Writes an answer given in a situations drill as the answer to that situation.
     *
     * A drill asks the very questions the situations hold, in one run, so an answer given there is
     * the answer to that situation. It is written as it is picked rather than when the drill is
     * over, so leaving a drill halfway leaves the situations answered up to where it got to, and
     * the list can mark off the ones that are done with.
     */
    private suspend fun recordSituationAnswer(session: ExamSession, question: Question, chosen: Int) {
        if (kind != AttemptKind.Situations) return
        practiceAnswers.answer(session.bank.id, session.bank.version, question.id, chosen)
    }

    /**
     * Fills in the whole test at once, for the overflow menu's debug entries: every question the user
     * hasn't answered yet gets the option [answerOf] picks. Answers already given are left as they
     * are, so the shortcut can't wipe out work - or a mistake - that was there first.
     */
    private fun answerAll(answerOf: (Question) -> Int) {
        val session = session.value ?: return
        val questions = questions ?: return
        val unanswered = questions.withIndex().filterNot { (index, _) -> index in answered }
        viewModelScope.launch {
            unanswered.forEach { (_, practiceQuestion) ->
                session.answer(attemptId, practiceQuestion.question, answerOf(practiceQuestion.question))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        elapsedSeconds.value?.let { saveElapsed(it) }
    }

    /**
     * Whether answering question [index] moves the test on to the next one. The question order and
     * whether the attempt reveals answers are fixed when it loads, so only the end of the test and
     * the user's setting can change while it's open.
     */
    private fun nextPageAfter(index: Int): Int? {
        val advances = advancesAfterAnswer(
            advanceAfterAnswer = preferences.preferences.value.advanceAfterAnswer,
            checkImmediately = checksImmediately,
            finished = isFinished,
            questionIndex = index,
            count = questions?.size ?: 0,
        )
        return if (advances) index + 1 else null
    }

    private fun ready(
        session: ExamSession,
        attempt: TestAttempt,
        openResults: Boolean,
        close: Boolean,
        advanceToPage: Int?,
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
            close = close,
            advanceToPage = advanceToPage,
            elapsedSeconds = elapsedSeconds,
            allottedSeconds = timeLimitSeconds,
            showTimeLimitToast = showTimeLimitToast,
        )
    }
}
