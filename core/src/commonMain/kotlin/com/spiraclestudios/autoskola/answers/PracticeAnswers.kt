package com.spiraclestudios.autoskola.answers

import kotlinx.serialization.Serializable

/**
 * The answers a driver has given to the questions they read while practising - the situations at
 * intersections today - one answer per question and nothing else: no score, no timing, because
 * reading them is practice rather than a test.
 *
 * An answer is what says whether it was right: the screen marks the option that was picked as soon
 * as it is picked, so there is nothing to keep about checking. Question ids are unique within one
 * version of a bank, so these go with that version and start empty when a new one is imported.
 */
@Serializable
data class PracticeAnswers(val byQuestion: Map<Int, Int> = emptyMap()) {

    /** The option the driver picked for [questionId], or `null` while they haven't answered it. */
    fun chosenFor(questionId: Int): Int? = byQuestion[questionId]

    /**
     * The same answers with [questionId] answered by [chosen]. Answering again replaces the answer
     * that was there, so a driver can change their mind as often as they like and see what the
     * answer they have now comes to.
     */
    fun withAnswer(questionId: Int, chosen: Int): PracticeAnswers =
        copy(byQuestion = byQuestion + (questionId to chosen))
}
