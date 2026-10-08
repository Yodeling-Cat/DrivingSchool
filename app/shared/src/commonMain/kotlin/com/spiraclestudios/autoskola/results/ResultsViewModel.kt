package com.spiraclestudios.autoskola.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamProgress
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.AttemptSummary
import com.spiraclestudios.autoskola.stats.SectionSummary
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.stats.summary
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
        /**
         * Practice tests started, which is not drawn: it is what the closing ad is measured in,
         * `plans/ads.md`'s rule being that the ad yields when the user\'s next tap would be gated.
         * It is here rather than recomputed on the way out so that leaving costs no work.
         */
        val practiceTestsStarted: Int = 0,
        /**
         * Whether the user is on their way out, which the route navigates on. Set by
         * [ResultsUiEvent.Done] once the closing ad - if there is one - is over, so the ad is
         * finished with before the next screen appears rather than over it.
         */
        val leaving: Boolean = false,
        /** Whether the banner pinned under the results is drawn: until the ads are bought away. */
        val showAds: Boolean = false,
    ) : ResultsUiState
}

sealed interface ResultsUiEvent {
    /**
     * The user is done with the result and going back to the list of tests. The one way out of this
     * screen that may have an ad in it: going back to read the answers is pure navigation and is not
     * asked about here, because a driver on their way to find out why an answer was wrong is the
     * last person to interrupt with an advertisement.
     */
    data object Done : ResultsUiEvent
}

/**
 * Whether an attempt of [kind] is given a verdict. A test is passed or failed against the pass mark;
 * a situations drill is a run through the reading material, which is only reported on.
 */
internal fun hasVerdict(kind: AttemptKind): Boolean = kind != AttemptKind.Situations

/** Shows how a finished attempt went. Only reads, apart from the one thing leaving does. */
class ResultsViewModel(private val attemptId: String, exams: ExamRepository, private val ads: AdRepository) :
    ViewModel() {

    private val leaving = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ResultsUiState> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(ResultsUiState.Loading)
            combine(
                session.attempt(attemptId),
                session.progress,
                leaving,
                ads.adsAllowed,
            ) { attempt, progress, leaving, showAds ->
                if (attempt?.isFinished == true) {
                    ready(session, attempt, progress, leaving, showAds)
                } else {
                    ResultsUiState.Closed
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultsUiState.Loading)

    fun onEvent(event: ResultsUiEvent) {
        when (event) {
            // The ad first, and the flag after it: the route navigates on `leaving`, so the ad is
            // over before anything else is drawn. A refusal - no consent, or Premium - and an ad
            // that yields to the next gate both fall through to the same flag without waiting.
            ResultsUiEvent.Done -> viewModelScope.launch {
                val ready = uiState.value as? ResultsUiState.Ready
                ready?.let { ads.showClosingAd(it.practiceTestsStarted) }
                leaving.value = true
            }
        }
    }

    private fun ready(
        session: ExamSession,
        attempt: TestAttempt,
        progress: ExamProgress,
        leaving: Boolean,
        showAds: Boolean,
    ): ResultsUiState.Ready {
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
            practiceTestsStarted = progress.practiceTestsStarted,
            leaving = leaving,
            showAds = showAds,
        )
    }
}
