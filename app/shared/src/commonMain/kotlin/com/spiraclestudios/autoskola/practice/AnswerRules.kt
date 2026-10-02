package com.spiraclestudios.autoskola.practice

import kotlin.time.Duration.Companion.milliseconds

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

/** How many of the questions at [indices] carry an answer. */
internal fun answeredAmong(answers: Map<Int, Int>, indices: Iterable<Int>): Int =
    indices.count { answers.containsKey(it) }

/**
 * How much of a test is behind the user: the questions that carry an answer, and the ones left open -
 * which is what a question never answered reads as once the test is finished.
 */
internal fun answeredAndSkipped(answers: Map<Int, Int>, questionCount: Int): Pair<Int, Int> {
    val answered = answeredAmong(answers, 0 until questionCount)
    return answered to (questionCount - answered).coerceAtLeast(0)
}

/**
 * How long the answered question stays on screen before the test moves on. Long enough for the
 * pick to be seen registering, short enough not to feel like a wait.
 */
internal val AUTO_ADVANCE_DELAY_MILLIS = 500.milliseconds

/**
 * Whether answering question [questionIndex] of a test with [count] questions moves straight on to
 * the next one. Only when the user asked for it, and never in a test that reveals answers as they're
 * picked: a checked answer has to stay on screen. A finished test is being reviewed, so it stays put.
 */
internal fun advancesAfterAnswer(
    advanceAfterAnswer: Boolean,
    checkImmediately: Boolean,
    finished: Boolean,
    questionIndex: Int,
    count: Int,
): Boolean = advanceAfterAnswer && !checkImmediately && !finished && questionIndex < count - 1

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
