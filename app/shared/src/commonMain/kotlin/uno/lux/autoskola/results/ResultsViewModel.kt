package uno.lux.autoskola.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.AttemptSummary
import uno.lux.autoskola.stats.SectionSummary
import uno.lux.autoskola.stats.TestAttempt
import uno.lux.autoskola.stats.summary

data class ResultSection(val title: String, val summary: SectionSummary)

sealed interface ResultsUiState {
    data object Loading : ResultsUiState

    /** The attempt isn't finished (or is gone), so there are no results to show. */
    data object Closed : ResultsUiState

    data class Ready(
        val exam: ExamLabel,
        val kind: AttemptKind,
        val testNumber: Int?,
        val summary: AttemptSummary,
        val sections: List<ResultSection>,
        /** How long the test ran. Zero on attempts made before the app timed tests. */
        val elapsedSeconds: Long,
        /**
         * Whether the card says how the attempt went, as passing or failing: a test is measured
         * against the pass mark, while a situations drill was never being passed or failed, so its
         * card has no stamp, no pass mark and no time to show.
         */
        val hasVerdict: Boolean,
    ) : ResultsUiState
}

/**
 * Whether an attempt of [kind] is given a verdict. A test is passed or failed against the pass mark;
 * a situations drill is a run through the reading material, which is only reported on.
 */
internal fun hasVerdict(kind: AttemptKind): Boolean = kind != AttemptKind.Situations

/** Shows how a finished attempt went. Only reads, so it has no events. */
class ResultsViewModel(private val attemptId: String, exams: ExamRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ResultsUiState> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(ResultsUiState.Loading)
            session.attempt(attemptId).map { attempt ->
                if (attempt?.isFinished == true) ready(session, attempt) else ResultsUiState.Closed
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultsUiState.Loading)

    private fun ready(session: ExamSession, attempt: TestAttempt): ResultsUiState.Ready {
        val summary = attempt.summary(session.questionsOf(attempt))
        return ResultsUiState.Ready(
            exam = ExamLabel(session.category, session.bank.language),
            kind = attempt.kind,
            testNumber = attempt.testNumber,
            summary = summary,
            sections = summary.sections.map {
                ResultSection(session.bank.section(it.section)?.title.orEmpty(), it)
            },
            elapsedSeconds = attempt.elapsedSeconds,
            hasVerdict = hasVerdict(attempt.kind),
        )
    }
}
