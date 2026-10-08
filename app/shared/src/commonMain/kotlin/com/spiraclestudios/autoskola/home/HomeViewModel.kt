package com.spiraclestudios.autoskola.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.StoreListing
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.testStartedProperties
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamProgress
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.data.exam.TestPick
import com.spiraclestudios.autoskola.data.review.ReviewRepository
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.storeListing
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

/**
 * A test the user picked that they have already passed, so the screen asks before it is taken
 * again: redoing it gives up the score it has, which is a pass.
 */
data class PassedPrompt(val testNumber: Int, val score: Int, val maxScore: Int)

/** Everything the home screen shows about the user's progress. */
data class HomeProgress(
    val results: Map<Int, TestResult>,
    /** Test numbers with an unfinished attempt. */
    val inProgress: Set<Int>,
    val resumable: ResumableTest?,
    val mistakeCount: Int,
    val questionsAnswered: Int,
    val questionPoolSize: Int,
    /** Finished practice tests, counting every take; what the rating card's milestones count up. */
    val finishedTests: Int,
    /**
     * Practice tests started, counting every take and every abandoned one: what the ads gate counts
     * its frequency in. Started rather than finished, because the gate is the door into a test and a
     * driver who walks away from one went through it just the same.
     */
    val practiceTestsStarted: Int,
)

/**
 * The card asking the user to rate the app in the store it names. Which store that is, and whether
 * there is one at all, is the platform's answer (see [storeListing]); the screen only draws it.
 */
data class RateCard(val storeName: String)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val exam: ExamLabel,
        val tests: List<PracticeTest>,
        val progress: HomeProgress,
        /** Whether any test has not been passed yet, which is what the "not passed" pill acts on. */
        val hasNotPassedTests: Boolean,
        /** The card asking for a store rating, or `null` where it isn't shown. */
        val rateCard: RateCard? = null,
        /** An attempt to open; the screen navigates to it and then sends [HomeUiEvent.AttemptOpened]. */
        val openAttemptId: String? = null,
        /** Set when the picked test is already under way, so the screen asks what to do. */
        val resumePrompt: ResumePrompt? = null,
        /** Set when the picked test has already been passed, so the screen asks before redoing it. */
        val passedPrompt: PassedPrompt? = null,
        /**
         * Whether the banner under the mistakes card is drawn. False unless the state says
         * otherwise, because the failure of forgetting it should be no ad rather than an ad the
         * user did not agree to.
         */
        val showAds: Boolean = false,
        /**
         * The test the user asked for that the every-tenth-test gate is in front of, or `null` when
         * nothing is waiting. The test is not under way: the screen asks about it, and dismissing the
         * dialog leaves it unstarted rather than starting it for free.
         */
        val pendingTestNumber: Int? = null,
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

    /** Takes the test the [HomeUiState.Ready.passedPrompt] is about again, giving up its score. */
    data object RedoTest : HomeUiEvent

    /** The user closed the prompt, leaving the test as it was. */
    data object DismissResumePrompt : HomeUiEvent

    /** The user closed the passed prompt, leaving the score the test has. */
    data object DismissPassedPrompt : HomeUiEvent
    data object PracticeMistakes : HomeUiEvent
    data object AttemptOpened : HomeUiEvent

    /** The card's button: the store is asked to let the user rate the app, which ends the asking. */
    data object RateApp : HomeUiEvent

    /** The card's cross: not now, which the app asks again about later in the ladder. */
    data object DismissRateCard : HomeUiEvent

    /** The gate's one action: the user agreed to the ad, so the test it is holding may start. */
    data object WatchAd : HomeUiEvent

    /** The gate's way out: the user stays on the home screen with the held test unstarted. */
    data object DismissAdGate : HomeUiEvent
}

/**
 * The three things the home screen can be part-way through asking. Combined into one source so that
 * [HomeViewModel.uiState] can follow the ads answer as well: there is no `combine` that takes six.
 */
private data class Prompts(val openAttemptId: String?, val resume: ResumePrompt?, val passed: PassedPrompt?)

/**
 * A start the gate is standing in front of: what to begin once the user is through, the test it is
 * holding, and the count it was measured at so that both halves of the gate agree on which cycle
 * this is.
 */
private class GatedStart(val practiceTestsStarted: Int, val testNumber: Int, val start: suspend () -> Unit)

class HomeViewModel(
    exams: ExamRepository,
    private val analytics: AnalyticsRepository,
    private val review: ReviewRepository,
    private val ads: AdRepository,
    /** The store this build can be rated in, or `null` where it has none (the web, today's iOS). */
    private val store: StoreListing? = storeListing,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)
    private val resumePrompt = MutableStateFlow<ResumePrompt?>(null)
    private val passedPrompt = MutableStateFlow<PassedPrompt?>(null)

    /** What the gate is holding, if anything. Cleared only by the two answers to the dialog. */
    private val gated = MutableStateFlow<GatedStart?>(null)

    private val prompts = combine(openAttemptId, resumePrompt, passedPrompt, ::Prompts)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = session
        .flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(HomeUiState.Loading)
            combine(
                session.progress,
                prompts,
                review.request,
                ads.adsAllowed,
                gated,
            ) { progress, prompts, request, showAds, gated ->
                // The card is due at the milestones the asking is measured in, and only where the
                // platform has a store page to send the driver to.
                val allPassed = everyTestPassed(session.tests, progress.results)
                HomeUiState.Ready(
                    exam = ExamLabel(session.category, session.bank.language),
                    tests = session.tests,
                    progress = progress.toHomeProgress(),
                    hasNotPassedTests = notPassedTests(session.tests, progress.results).isNotEmpty(),
                    rateCard = store
                        ?.takeIf { request.isDue(progress.finishedTests, allPassed) }
                        ?.let { RateCard(storeName = it.name) },
                    openAttemptId = prompts.openAttemptId,
                    resumePrompt = prompts.resume,
                    passedPrompt = prompts.passed,
                    showAds = showAds,
                    pendingTestNumber = gated?.testNumber,
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
                // What the test already has decides what picking it means, and two of those -
                // an attempt under way, a score that is already a pass - are the user's to choose
                // between rather than ours.
                when (val pick = session.pick(test)) {
                    is TestPick.UnderWay -> resumePrompt.value = ResumePrompt(
                        attemptId = pick.attempt.id,
                        testNumber = test.number,
                        answered = pick.attempt.answers.size,
                        total = pick.attempt.questionIds.size,
                    )

                    is TestPick.Passed -> passedPrompt.value = PassedPrompt(
                        testNumber = test.number,
                        score = pick.result.score,
                        maxScore = pick.result.maxScore,
                    )

                    TestPick.Fresh -> gate(shown?.progress, test.number) {
                        openAttemptId.value = session.startOrResume(test).id
                        started(AttemptKind.PracticeTest, resumed = false)
                    }
                }
            }

            is HomeUiEvent.StartRandomTest -> {
                val progress = shown?.progress ?: return
                val candidates = if (event.notPassed) notPassedTests(session.tests, progress.results) else session.tests
                if (candidates.isNotEmpty()) onEvent(HomeUiEvent.StartTest(candidates.random(random).number))
            }

            HomeUiEvent.Resume -> shown?.progress?.resumable?.let { resumable ->
                openAttemptId.value = resumable.attemptId
                started(resumable.kind, resumed = true)
            }

            HomeUiEvent.ContinueTest -> {
                resumePrompt.value?.let {
                    openAttemptId.value = it.attemptId
                    started(AttemptKind.PracticeTest, resumed = true)
                }
                resumePrompt.value = null
            }

            HomeUiEvent.StartOver -> resumePrompt.value?.let { prompt ->
                val test = session.tests.firstOrNull { it.number == prompt.testNumber } ?: return
                resumePrompt.value = null
                gate(shown?.progress, test.number) {
                    openAttemptId.value = session.restart(test).id
                    started(AttemptKind.PracticeTest, resumed = false)
                }
            }

            HomeUiEvent.DismissResumePrompt -> resumePrompt.value = null

            // Redoing a passed test is starting it over, the way the prompt about an unfinished one
            // does it: a fresh attempt from the beginning, with nothing of the old one in it. It is
            // that attempt's score that takes the place of the pass the test already has.
            HomeUiEvent.RedoTest -> passedPrompt.value?.let { prompt ->
                val test = session.tests.firstOrNull { it.number == prompt.testNumber } ?: return
                passedPrompt.value = null
                gate(shown?.progress, test.number) {
                    openAttemptId.value = session.restart(test).id
                    started(AttemptKind.PracticeTest, resumed = false)
                }
            }

            HomeUiEvent.DismissPassedPrompt -> passedPrompt.value = null

            HomeUiEvent.PracticeMistakes -> viewModelScope.launch {
                session.startMistakes()?.let {
                    openAttemptId.value = it.id
                    started(AttemptKind.Mistakes, resumed = false)
                }
            }

            HomeUiEvent.AttemptOpened -> {
                openAttemptId.value = null
                resumePrompt.value = null
                passedPrompt.value = null
            }

            // The card's two answers. The cross only closes the app's ask, so the ladder knows how
            // many tests had been done when it was closed and when to put it again; the press on the
            // button asks the store for its rating (Play's sheet over the app, or the store's page),
            // and the asking is over whether or not they went on to write the rating.
            HomeUiEvent.DismissRateCard -> viewModelScope.launch {
                val ready = shown ?: return@launch
                review.dismissed(
                    finishedTests = ready.progress.finishedTests,
                    everyTestPassed = everyTestPassed(ready.tests, ready.progress.results),
                )
            }

            // The gate's two answers. Watching runs the start that was held; dismissing drops it, so
            // the user is left where they were. Nothing is written by the dismissal, which is what
            // makes tapping the test again ask again.
            HomeUiEvent.WatchAd -> viewModelScope.launch {
                val waiting = gated.value ?: return@launch
                ads.showTestOfferAd(waiting.practiceTestsStarted)
                gated.value = null
                waiting.start()
            }

            HomeUiEvent.DismissAdGate -> gated.value = null

            HomeUiEvent.RateApp -> {
                val listing = store?.takeIf { shown?.rateCard != null } ?: return
                viewModelScope.launch { review.rate(listing) }
            }
        }
    }

    /**
     * Runs [start] now, or holds it if the every-tenth-test gate is in front of it and asks the screen
     * instead.
     *
     * Called from the three events that begin a new attempt, rather than from one choke point, which
     * is what keeps the drills out of it: a repeat of the questions the user got wrong and a run
     * through the situations are short drills started on impulse, and neither is what the gate is for.
     * Continuing an attempt that is already under way is not gated either - the gate let it in.
     */
    private fun gate(progress: HomeProgress?, testNumber: Int, start: suspend () -> Unit) {
        if (progress != null && ads.shouldOfferTest(progress.practiceTestsStarted)) {
            gated.value = GatedStart(progress.practiceTestsStarted, testNumber, start)
        } else {
            viewModelScope.launch { start() }
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
        finishedTests = finishedTests,
        practiceTestsStarted = practiceTestsStarted,
    )

    /**
     * Records an attempt being opened, and whether it is a saved one being continued rather than a
     * new one being created. Only an attempt the user is actually taken into is recorded: asking
     * what to do about a test already under way is not yet starting or continuing it.
     */
    private fun started(kind: AttemptKind, resumed: Boolean) {
        analytics.capture(AnalyticsEvent.TestStarted, testStartedProperties(kind, resumed))
    }
}
