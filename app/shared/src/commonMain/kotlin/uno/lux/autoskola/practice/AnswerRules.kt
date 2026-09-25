package uno.lux.autoskola.practice

/** How one answer option is shown. */
internal enum class AnswerState { Neutral, Selected, Correct, Wrong }

/** How a question number in the question list is shown. */
internal enum class ChipState { Correct, Incorrect, Answered, Unanswered }

/** In check-immediately mode the first pick is final. */
internal fun isAnswerLocked(checkImmediately: Boolean, selected: Int?): Boolean =
    checkImmediately && selected != null

/**
 * State of option [index]. Once [revealed], the correct option and a wrong pick are marked;
 * before that only the user's pick is.
 */
internal fun answerState(index: Int, selected: Int?, correct: Int, revealed: Boolean): AnswerState = when {
    !revealed -> if (selected == index) AnswerState.Selected else AnswerState.Neutral
    index == correct -> AnswerState.Correct
    index == selected -> AnswerState.Wrong
    else -> AnswerState.Neutral
}

/** The question list only gives away correctness when answers are checked immediately. */
internal fun chipState(selected: Int?, correct: Int, revealCorrectness: Boolean): ChipState = when {
    selected == null -> ChipState.Unanswered
    !revealCorrectness -> ChipState.Answered
    selected == correct -> ChipState.Correct
    else -> ChipState.Incorrect
}
