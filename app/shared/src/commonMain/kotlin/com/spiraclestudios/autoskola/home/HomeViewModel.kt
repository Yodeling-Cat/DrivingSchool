package com.spiraclestudios.autoskola.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamProgress
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.stats.AttemptKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** An unfinished test the user can pick up again. */
data class ResumableTest(
    val attemptId: String,
    val kind: AttemptKind,
    val testNumber: Int?,
    val answered: Int,
    val total: Int,
)

/**
 * A test the user picked that already has an attempt under way, so the screen asks whether to
 * continue it or start over instead of choosing for them.
 */
data class ResumePrompt(
    val attemptId: String,
    val testNumber: Int,
    val answered: Int,
    val total: Int,
)

/** Everything the home screen shows about the user's progress. */
data class HomeProgress(
    val results: Map<Int, TestResult>,
    /** Test numbers with an unfinished attempt. */
    val inProgress: Set<Int>,
    val resumable: ResumableTest?,
    val mistakeCount: Int,
    val questionsAnswered: Int,
    val questionPoolSize: Int,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val exam: ExamLabel,
        val tests: List<PracticeTest>,
        val progress: HomeProgress,
        /** Whether any test has not been passed yet, which is what the "not passed" pill acts on. */
        val hasNotPassedTests: Boolean,
        /** An attempt to open; the screen navigates to it and then sends [HomeUiEvent.AttemptOpened]. */
        val openAttemptId: String? = null,
        /** Set when the picked test is already under way, so the screen asks what to do. */
        val resumePrompt: ResumePrompt? = null,
    ) : HomeUiState
}

sealed interface HomeUiEvent {
    data class StartTest(val testNumber: Int) : HomeUiEvent

    /** Starts a random test, or with [notPassed] a random one that hasn't been passed yet. */
    data class StartRandomTest(val notPassed: Boolean) : HomeUiEvent
    data object Resume : HomeUiEvent

    /** Opens the attempt the [HomeUiState.Ready.resumePrompt] is about. */
    data object ContinueTest : HomeUiEvent

    /** Throws the unfinished attempt away and starts the same test from the beginning. */
    data object StartOver : HomeUiEvent

    /** The user closed the prompt, leaving the test as it was. */
    data object DismissResumePrompt : HomeUiEvent
    data object PracticeMistakes : HomeUiEvent
    data object AttemptOpened : HomeUiEvent
}

class HomeViewModel(exams: ExamRepository, private val random: Random = Random.Default) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)
    private val resumePrompt = MutableStateFlow<ResumePrompt?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(HomeUiState.Loading)
            combine(session.progress, openAttemptId, resumePrompt) { progress, open, prompt ->
                HomeUiState.Ready(
                    exam = ExamLabel(session.category, session.bank.language),
                    tests = session.tests,
                    progress = progress.toHomeProgress(),
                    hasNotPassedTests = notPassedTests(session.tests, progress.results).isNotEmpty(),
                    openAttemptId = open,
                    resumePrompt = prompt,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onEvent(event: HomeUiEvent) {
        val session = session.value ?: return
        val shown = uiState.value as? HomeUiState.Ready
        when (event) {
            is HomeUiEvent.StartTest -> viewModelScope.launch {
                val test = session.tests.firstOrNull { it.number == event.testNumber } ?: return@launch
                // A test that's already under way could be picked up or started again; only the
                // user knows which, so ask instead of choosing for them.
                val underWay = session.openAttemptFor(test)
                if (underWay == null) {
                    openAttemptId.value = session.startOrResume(test).id
                } else {
                    resumePrompt.value = ResumePrompt(
                        attemptId = underWay.id,
                        testNumber = test.number,
                        answered = underWay.answers.size,
                        total = underWay.questionIds.size,
                    )
                }
            }

            is HomeUiEvent.StartRandomTest -> {
                val progress = shown?.progress ?: return
                val candidates = if (event.notPassed) notPassedTests(session.tests, progress.results) else session.tests
                if (candidates.isNotEmpty()) onEvent(HomeUiEvent.StartTest(candidates.random(random).number))
            }

            HomeUiEvent.Resume -> openAttemptId.value = shown?.progress?.resumable?.attemptId

            HomeUiEvent.ContinueTest -> {
                openAttemptId.value = resumePrompt.value?.attemptId
                resumePrompt.value = null
            }

            HomeUiEvent.StartOver -> resumePrompt.value?.let { prompt ->
                val test = session.tests.firstOrNull { it.number == prompt.testNumber } ?: return
                resumePrompt.value = null
                viewModelScope.launch { openAttemptId.value = session.restart(test).id }
            }

            HomeUiEvent.DismissResumePrompt -> resumePrompt.value = null

            HomeUiEvent.PracticeMistakes -> viewModelScope.launch {
                session.startMistakes()?.let { openAttemptId.value = it.id }
            }

            HomeUiEvent.AttemptOpened -> {
                openAttemptId.value = null
                resumePrompt.value = null
            }
        }
    }

    private fun ExamProgress.toHomeProgress() = HomeProgress(
        results = results,
        inProgress = inProgress,
        resumable = resumable?.let {
            ResumableTest(it.id, it.kind, it.testNumber, answered = it.answers.size, total = it.questionIds.size)
        },
        mistakeCount = mistakeCount,
        questionsAnswered = questionsAnswered,
        questionPoolSize = poolSize,
    )
}
