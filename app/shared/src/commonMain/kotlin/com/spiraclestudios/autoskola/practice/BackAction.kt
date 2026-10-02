package com.spiraclestudios.autoskola.practice

/** What a back press does while a test is open. */
internal sealed interface BackAction {
    /** The question list is covering the questions, so close it instead of leaving. */
    data object CloseQuestionList : BackAction

    /** Leaving would lose the answers the user is part way through, so ask first. */
    data object ConfirmLeave : BackAction

    /** Nothing to lose: leave the test. */
    data object Leave : BackAction
}

/**
 * Back closes the question list when it's open, and only then considers leaving the test: a
 * finished test can be left right away, an unfinished one needs confirmation.
 */
internal fun backAction(questionListExpanded: Boolean, finished: Boolean): BackAction = when {
    questionListExpanded -> BackAction.CloseQuestionList
    finished -> BackAction.Leave
    else -> BackAction.ConfirmLeave
}
