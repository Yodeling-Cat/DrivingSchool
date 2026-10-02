package com.spiraclestudios.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.answers.PracticeAnswers
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.exam.CurrentExam
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.exam.ExamSession
import com.spiraclestudios.autoskola.situations.isSolvedBy
import com.spiraclestudios.autoskola.situations.situations
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
)

/** One situation as the screens draw it. */
data class SituationItem(
    /** Where it sits in the material, counted from 1, which is what the screens call it. */
    val number: Int,
    val imageUri: String,
    val questions: List<SituationQuestion>,
    /** Every question answered, and answered right, so there is nothing left to practise here. */
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

sealed interface IntersectionsUiEvent {
    /** Drills every situation, asking what to do first when one is already under way. */
    data object Drill : IntersectionsUiEvent

    /** Opens the drill [IntersectionsUiState.Ready.drillPrompt] is about, where it was left. */
    data object ContinueDrill : IntersectionsUiEvent

    /** Throws the drill that's under way away and begins every situation from the start. */
    data object StartOverDrill : IntersectionsUiEvent

    /** The user closed the prompt, leaving the drill as it was. */
    data object DismissDrillPrompt : IntersectionsUiEvent

    /** The screen opened the attempt [IntersectionsUiState.Ready.openAttemptId] asked for. */
    data object AttemptOpened : IntersectionsUiEvent
}

/** The situations at intersections: the drawings the exam asks about, and what it asks about them. */
class IntersectionsViewModel(
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val answers: PracticeAnswerRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)
    private val drillPrompt = MutableStateFlow<DrillPrompt?>(null)

    /** What the driver has answered while reading, which marks the situations they are done with. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val given: Flow<PracticeAnswers> = session.filterNotNull()
        .flatMapLatest { answers.answers(it.bank.id, it.bank.version) }

    val uiState: StateFlow<IntersectionsUiState> =
        combine(session, given, openAttemptId, drillPrompt) { session, given, open, prompt ->
            if (session == null) {
                IntersectionsUiState.Loading
            } else {
                IntersectionsUiState.Ready(
                    situations = situationItems(session, banks, given),
                    openAttemptId = open,
                    drillPrompt = prompt,
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
                } else {
                    drillPrompt.value = DrillPrompt(
                        attemptId = underWay.id,
                        answered = underWay.answers.size,
                        total = underWay.questionIds.size,
                    )
                }
            }

            IntersectionsUiEvent.ContinueDrill -> {
                openAttemptId.value = drillPrompt.value?.attemptId
                drillPrompt.value = null
            }

            IntersectionsUiEvent.StartOverDrill -> {
                drillPrompt.value = null
                viewModelScope.launch { openAttemptId.value = drill(session, startOver = true) }
            }

            IntersectionsUiEvent.DismissDrillPrompt -> drillPrompt.value = null

            IntersectionsUiEvent.AttemptOpened -> {
                openAttemptId.value = null
                drillPrompt.value = null
            }
        }
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
 * The situations of [session] as the screens draw them, numbered from one and in the bank's order.
 * The numbering is the material's own, so the situation the list calls third is the one the detail
 * screen calls third. [answers] are what the driver has answered while reading, which is what says
 * whether a situation has been worked out in full.
 */
internal fun situationItems(
    session: ExamSession,
    banks: QuestionBankRepository,
    answers: PracticeAnswers = PracticeAnswers(),
): List<SituationItem> = situations(session.bank, session.category).mapIndexed { index, situation ->
    SituationItem(
        number = index + 1,
        imageUri = banks.imageUri(session.bank, situation.image),
        questions = situation.questions.map { it.toSituationQuestion() },
        solved = situation.isSolvedBy(answers),
    )
}

private fun Question.toSituationQuestion() = SituationQuestion(
    questionId = id,
    text = displayText(text),
    answers = answers.map(::displayText),
    correct = correct,
)
