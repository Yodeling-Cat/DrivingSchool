package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.situations.situations
import uno.lux.autoskola.ui.displayText

/** One question about a situation, as the screens read it. */
data class SituationQuestion(
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
    ) : IntersectionsUiState {
        /** How many questions the situations hold altogether, folded into one run of practice. */
        val questionCount: Int get() = situations.sumOf { it.questions.size }
    }
}

sealed interface IntersectionsUiEvent {
    /** Drills every situation, in the practice screen. */
    data object Drill : IntersectionsUiEvent

    /** The screen opened the attempt [IntersectionsUiState.Ready.openAttemptId] asked for. */
    data object AttemptOpened : IntersectionsUiEvent
}

/** The situations at intersections: the drawings the exam asks about, and what it asks about them. */
class IntersectionsViewModel(
    exams: ExamRepository,
    private val banks: QuestionBankRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val session: StateFlow<ExamSession?> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val openAttemptId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<IntersectionsUiState> = combine(session, openAttemptId) { session, open ->
        if (session == null) {
            IntersectionsUiState.Loading
        } else {
            IntersectionsUiState.Ready(situations = situationItems(session, banks), openAttemptId = open)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IntersectionsUiState.Loading)

    fun onEvent(event: IntersectionsUiEvent) {
        val session = session.value ?: return
        when (event) {
            IntersectionsUiEvent.Drill -> viewModelScope.launch {
                val questions = situations(session.bank, session.category).flatMap { it.questions }
                val checkImmediately = preferences.preferences.value.checkAnswersImmediately
                openAttemptId.value = session.startSituations(questions, checkImmediately)?.id
            }

            IntersectionsUiEvent.AttemptOpened -> openAttemptId.value = null
        }
    }
}

/**
 * The situations of [session] as the screens draw them, numbered from one and in the bank's order.
 * The numbering is the material's own, so the situation the list calls third is the one the detail
 * screen calls third.
 */
internal fun situationItems(session: ExamSession, banks: QuestionBankRepository): List<SituationItem> =
    situations(session.bank, session.category).mapIndexed { index, situation ->
        SituationItem(
            number = index + 1,
            imageUri = banks.imageUri(session.bank, situation.image),
            questions = situation.questions.map { it.toSituationQuestion() },
        )
    }

private fun Question.toSituationQuestion() = SituationQuestion(
    text = displayText(text),
    answers = answers.map(::displayText),
    correct = correct,
)
