package uno.lux.autoskola.practice

/** How one answer option is shown. */
internal enum class AnswerState { Neutral, Selected, Correct, Wrong }

/** How a question number in the question list is shown. */
internal enum class ChipState { Correct, Incorrect, Skipped, Answered, Unanswered }

/** In check-immediately mode the first pick is final; once the test is finished, every answer is. */
internal fun isAnswerLocked(checkImmediately: Boolean, finished: Boolean, selected: Int?): Boolean =
    finished || (checkImmediately && selected != null)

/** Whether answered questions may show if they're right. */
internal fun revealsCorrectness(checkImmediately: Boolean, finished: Boolean): Boolean = checkImmediately || finished

/** What the snackbar at the bottom of a test offers. */
internal enum class FinishPrompt { Finish, ShowResults }

/** Offer to finish once everything is answered, and to go back to the results once finished. */
internal fun finishPrompt(allAnswered: Boolean, finished: Boolean): FinishPrompt? = when {
    finished -> FinishPrompt.ShowResults
    allAnswered -> FinishPrompt.Finish
    else -> null
}

/**
 * State of option [index]. Once [revealed], the correct option and a wrong pick are marked;
 * before that only the user's pick is.
 */
internal fun answerState(
    index: Int,
    selected: Int?,
    correct: Int,
    revealed: Boolean,
): AnswerState = when {
    !revealed -> if (selected == index) AnswerState.Selected else AnswerState.Neutral
    index == correct -> AnswerState.Correct
    index == selected -> AnswerState.Wrong
    else -> AnswerState.Neutral
}

/**
 * Whether the user left this question open. A question without an answer is only *skipped* once
 * the test is finished: in a running test (check-immediately included) it's simply not reached yet.
 */
internal fun questionSkipped(selected: Int?, finished: Boolean): Boolean = finished && selected == null

/** The question list only gives away correctness when answers are checked immediately. */
internal fun chipState(
    selected: Int?,
    correct: Int,
    revealCorrectness: Boolean,
    finished: Boolean,
): ChipState = when {
    questionSkipped(selected, finished) -> ChipState.Skipped
    selected == null -> ChipState.Unanswered
    !revealCorrectness -> ChipState.Answered
    selected == correct -> ChipState.Correct
    else -> ChipState.Incorrect
}
