package uno.lux.autoskola.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.PracticeTest
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamProgress
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.stats.AttemptKind
import kotlin.random.Random

/** An unfinished test the user can pick up again. */
data class ResumableTest(
    val attemptId: String,
    val kind: AttemptKind,
    val testNumber: Int?,
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
        /** An attempt to open; the screen navigates to it and then sends [HomeUiEvent.AttemptOpened]. */
        val openAttemptId: String? = null,
    ) : HomeUiState
}

sealed interface HomeUiEvent {
    data class StartTest(val testNumber: Int) : HomeUiEvent

    /** Starts a random test, or with [notDoneYet] a random one that hasn't been started or finished. */
    data class StartRandomTest(val notDoneYet: Boolean) : HomeUiEvent
    data object Resume : HomeUiEvent
    data object DiscardResumable : HomeUiEvent
    data object PracticeMistakes : HomeUiEvent
    data object AttemptOpened : HomeUiEvent
}

class HomeViewModel(
    exams: ExamRepository,
    private val preferences: PreferencesRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(HomeUiState.Loading)
            combine(session.progress, openAttemptId) { progress, open ->
                HomeUiState.Ready(
                    exam = ExamLabel(session.category, session.bank.language),
                    tests = session.tests,
                    progress = progress.toHomeProgress(),
                    openAttemptId = open,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onEvent(event: HomeUiEvent) {
        val session = session.value ?: return
        val shown = uiState.value as? HomeUiState.Ready
        val checkImmediately = preferences.preferences.value.checkAnswersImmediately
        when (event) {
            is HomeUiEvent.StartTest -> viewModelScope.launch {
                val test = session.tests.firstOrNull { it.number == event.testNumber } ?: return@launch
                openAttemptId.value = session.startOrResume(test, checkImmediately).id
            }

            is HomeUiEvent.StartRandomTest -> {
                val progress = shown?.progress ?: return
                val candidates = session.tests.filter {
                    !event.notDoneYet || (it.number !in progress.results && it.number !in progress.inProgress)
                }
                if (candidates.isNotEmpty()) onEvent(HomeUiEvent.StartTest(candidates.random(random).number))
            }

            HomeUiEvent.Resume -> openAttemptId.value = shown?.progress?.resumable?.attemptId

            HomeUiEvent.DiscardResumable -> shown?.progress?.resumable?.let {
                viewModelScope.launch { session.discard(it.attemptId) }
            }

            HomeUiEvent.PracticeMistakes -> viewModelScope.launch {
                session.startMistakes(checkImmediately)?.let { openAttemptId.value = it.id }
            }

            HomeUiEvent.AttemptOpened -> openAttemptId.value = null
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
