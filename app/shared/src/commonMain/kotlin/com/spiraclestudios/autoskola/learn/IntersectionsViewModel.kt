package com.spiraclestudios.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.answers.PracticeAnswers
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.situationOpenedProperties
import com.spiraclestudios.autoskola.data.analytics.testStartedProperties
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.situations.Situation
import com.spiraclestudios.autoskola.situations.isSolvedBy
import com.spiraclestudios.autoskola.situations.situations
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.ui.VideoItem
import com.spiraclestudios.autoskola.ui.displayText
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

/** One question about a situation, as the screens read it. */
data class SituationQuestion(
    /** The bank's id of the question, which is what an answer is kept against. */
    val questionId: Int,
    val text: String,
    val answers: List<String>,
    /** Index into [answers]. */
    val correct: Int,
    /** The pictures the answers are, one per answer, when they are pictures rather than words. */
    val answerImageUris: List<String>? = null,
)

/** One situation as the screens draw it. */
data class SituationItem(
    /** Where it sits in the material, counted from 1, which is what the screens call it. */
    val number: Int,
    /** Its drawing, or for a situation to watch, the still that stands for its [video]. */
    val imageUri: String,
    val questions: List<SituationQuestion>,
    /** The video the situation is, for the ones that are filmed rather than drawn. */
    val video: VideoItem? = null,
    /** Every question answered, and answered right, so there is nothing left to practice here. */
    val solved: Boolean = false,
)

sealed interface IntersectionsUiState {
    data object Loading : IntersectionsUiState

    /** The situations of the exam being taken, in the order the bank lists them. */
    data class Ready(
        val situations: List<SituationItem>,
        /**
         * An attempt to open, once the driver has asked to drill them; the screen navigates to it
         * and then sends [IntersectionsUiEvent.AttemptOpened].
         */
        val openAttemptId: String? = null,
        /** Set when a drill is already under way, so the screen asks what to do about it. */
        val drillPrompt: DrillPrompt? = null,
        /** Set while the screen asks whether to clear every answer to the situations. */
        val resetPrompt: Boolean = false,
        /** Whether the banner pinned under the list is drawn. False unless the state says so. */
        val showAds: Boolean = false,
    ) : IntersectionsUiState {
        /** How many questions the situations hold altogether, folded into one run of practice. */
        val questionCount: Int get() = situations.sumOf { it.questions.size }
    }
}

/**
 * A drill the driver walked away from, so the screen asks whether to carry on with it or begin
 * again instead of choosing for them.
 */
data class DrillPrompt(val attemptId: String, val answered: Int, val total: Int)

/**
 * The three things this screen can be part-way through asking, in one source: [uiState] has to
 * follow the ads answer too, and there is no `combine` that takes six.
 */
private data class Prompts(val openAttemptId: String?, val drill: DrillPrompt?, val reset: Boolean)

sealed interface IntersectionsUiEvent {
    /** Drills every situation, asking what to do first when one is already under way. */
    data object Drill : IntersectionsUiEvent

    /** Opens the drill [IntersectionsUiState.Ready.drillPrompt] is about, where it was left. */
    data object ContinueDrill : IntersectionsUiEvent

    /** Throws the drill that's under way away and begins every situation from the start. */
    data object StartOverDrill : IntersectionsUiEvent

    /** The user closed the prompt, leaving the drill as it was. */
    data object DismissDrillPrompt : IntersectionsUiEvent

    /** The driver asked to clear every answer to the situations, which the screen asks about first. */
    data object ResetAnswers : IntersectionsUiEvent

    /** The driver confirmed it: what was answered while reading goes, and so does any drill. */
    data object ConfirmReset : IntersectionsUiEvent

    /** The user closed the prompt, keeping their answers. */
    data object DismissReset : IntersectionsUiEvent

    /** The screen opened the attempt [IntersectionsUiState.Ready.openAttemptId] asked for. */
    data object AttemptOpened : IntersectionsUiEvent

    /** The driver opened one situation of the list, which the screen view alone can't say. */
    data class OpenSituation(val number: Int) : IntersectionsUiEvent
}

/** The situations at intersections: the drawings the exam asks about, and what it asks about them. */
class IntersectionsViewModel(
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val answers: PracticeAnswerRepository,
    private val analytics: AnalyticsRepository,
    private val ads: AdRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)
    private val drillPrompt = MutableStateFlow<DrillPrompt?>(null)
    private val resetPrompt = MutableStateFlow(false)

    /** What the driver has answered while reading, which marks the situations they are done with. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val given: Flow<PracticeAnswers> = session.filterNotNull()
        .flatMapLatest { answers.answers(it.bank.id, it.bank.version) }

    private val prompts = combine(openAttemptId, drillPrompt, resetPrompt, ::Prompts)

    private val items = SituationItems(banks)

    val uiState: StateFlow<IntersectionsUiState> =
        combine(session, given, prompts, ads.adsAllowed) { session, given, prompts, showAds ->
            if (session == null) {
                IntersectionsUiState.Loading
            } else {
                IntersectionsUiState.Ready(
                    situations = items.of(session, given),
                    openAttemptId = prompts.openAttemptId,
                    drillPrompt = prompts.drill,
                    resetPrompt = prompts.reset,
                    showAds = showAds,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IntersectionsUiState.Loading)

    fun onEvent(event: IntersectionsUiEvent) {
        val session = session.value ?: return
        when (event) {
            IntersectionsUiEvent.Drill -> viewModelScope.launch {
                // A drill that's already under way could be picked up or begun again, and only the
                // driver knows which: ask instead of choosing for them.
                val underWay = session.openSituationsAttempt()
                if (underWay == null) {
                    openAttemptId.value = drill(session, startOver = false)
                    if (openAttemptId.value != null) started(resumed = false)
                } else {
                    drillPrompt.value = DrillPrompt(
                        attemptId = underWay.id,
                        answered = underWay.answers.size,
                        total = underWay.questionIds.size,
                    )
                }
            }

            IntersectionsUiEvent.ContinueDrill -> {
                drillPrompt.value?.let {
                    openAttemptId.value = it.attemptId
                    started(resumed = true)
                }
                drillPrompt.value = null
            }

            IntersectionsUiEvent.StartOverDrill -> {
                drillPrompt.value = null
                viewModelScope.launch {
                    openAttemptId.value = drill(session, startOver = true)
                    if (openAttemptId.value != null) started(resumed = false)
                }
            }

            IntersectionsUiEvent.DismissDrillPrompt -> drillPrompt.value = null

            IntersectionsUiEvent.ResetAnswers -> resetPrompt.value = true

            IntersectionsUiEvent.ConfirmReset -> {
                resetPrompt.value = false
                drillPrompt.value = null
                viewModelScope.launch {
                    // A drill writes each answer to the situations as well, so the run through them
                    // and the answers given while reading are the same answers and go together.
                    session.discardSituationsDrill()
                    answers.reset(session.bank.id, session.bank.version)
                }
            }

            IntersectionsUiEvent.DismissReset -> resetPrompt.value = false

            IntersectionsUiEvent.AttemptOpened -> {
                openAttemptId.value = null
                drillPrompt.value = null
            }

            is IntersectionsUiEvent.OpenSituation -> analytics.capture(
                AnalyticsEvent.SituationOpened,
                situationOpenedProperties(event.number),
            )
        }
    }

    /** Records a drill being opened, and whether it is a saved one being continued. */
    private fun started(resumed: Boolean) {
        analytics.capture(
            AnalyticsEvent.TestStarted,
            testStartedProperties(AttemptKind.Situations, resumed),
        )
    }

    /**
     * Starts a run through every situation - or, with [startOver], a fresh one in place of the drill
     * that's under way. Answers with the attempt's id, or `null` when there is nothing to drill.
     */
    private suspend fun drill(session: ExamSession, startOver: Boolean): String? {
        val questions = situations(session.bank, session.category).flatMap { it.questions }
        val attempt = if (startOver) {
            session.restartSituations(questions)
        } else {
            session.startSituations(questions)
        }
        return attempt?.id
    }
}

/**
 * The situations of an exam as the screens draw them, numbered from one and in the bank's order.
 * The numbering is the material's own, so the situation the list calls third is the one the detail
 * screen calls third. The answers given while reading are what says whether a situation has been
 * worked out in full.
 *
 * Built once per session: only whether a situation is solved follows the answers, and the list and
 * every item that did not change are handed back as the same instances, so a screen redraws only the
 * situation an answer changed (see AGENTS.md, "Compose performance").
 */
internal class SituationItems(private val banks: QuestionBankRepository) {
    private var session: ExamSession? = null
    private var situations: List<Situation> = emptyList()
    private var items: List<SituationItem> = emptyList()

    fun of(session: ExamSession, answers: PracticeAnswers = PracticeAnswers()): List<SituationItem> {
        if (session !== this.session) {
            this.session = session
            situations = situations(session.bank, session.category)
            items = situations.mapIndexed { index, situation ->
                SituationItem(
                    number = index + 1,
                    imageUri = banks.imageUri(session.bank, situation.image),
                    questions = situation.questions.map {
                        it.toSituationQuestion(banks.answerImageUris(session.bank, it))
                    },
                    video = situation.questions.firstNotNullOfOrNull { question ->
                        question.video?.let { VideoItem(banks.videoUri(session.bank, question)!!, it.loops) }
                    },
                    solved = situation.isSolvedBy(answers),
                )
            }
            return items
        }
        var changed = false
        val updated = items.mapIndexed { index, item ->
            val solved = situations[index].isSolvedBy(answers)
            if (solved == item.solved) {
                item
            } else {
                changed = true
                item.copy(solved = solved)
            }
        }
        if (changed) items = updated
        return items
    }
}

private fun Question.toSituationQuestion(answerImageUris: List<String>?) = SituationQuestion(
    questionId = id,
    text = displayText(text),
    answers = answers.map(::displayText),
    correct = correct,
    answerImageUris = answerImageUris,
)
