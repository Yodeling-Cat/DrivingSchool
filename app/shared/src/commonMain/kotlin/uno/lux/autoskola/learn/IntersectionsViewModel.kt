package uno.lux.autoskola.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.exam.CurrentExam
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.exam.ExamSession
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
    data class Ready(val situations: List<SituationItem>) : IntersectionsUiState {
        /** How many questions the situations hold altogether, folded into one run of practice. */
        val questionCount: Int get() = situations.sumOf { it.questions.size }
    }
}

/** The situations at intersections: the drawings the exam asks about, and what it asks about them. */
class IntersectionsViewModel(exams: ExamRepository, private val banks: QuestionBankRepository) : ViewModel() {

    val uiState: StateFlow<IntersectionsUiState> = exams.current
        .map { (it as? CurrentExam.Ready)?.session }
        .map { session ->
            if (session == null) {
                IntersectionsUiState.Loading
            } else {
                IntersectionsUiState.Ready(situationItems(session, banks))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IntersectionsUiState.Loading)
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
