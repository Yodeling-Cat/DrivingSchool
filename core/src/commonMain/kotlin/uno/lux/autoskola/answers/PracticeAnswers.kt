package uno.lux.autoskola.answers

import kotlinx.serialization.Serializable

/**
 * The answers a driver has given to the questions they read while practising - the situations at
 * intersections today - one answer per question and nothing else: no score, no timing, because
 * reading them is practice rather than a test.
 *
 * Question ids are unique within one version of a bank, so these go with that version and start
 * empty when a new one is imported.
 */
@Serializable
data class PracticeAnswers(
    val byQuestion: Map<Int, Int> = emptyMap(),
    /**
     * The questions whose answer has been checked, which is what shows whether it was right. Kept
     * apart from the answers themselves and remembered with them, so coming back to a question the
     * driver checked shows them the marks rather than asking them to check again.
     */
    val revealed: Set<Int> = emptySet(),
) {

    /** The option the driver picked for [questionId], or `null` while they haven't answered it. */
    fun chosenFor(questionId: Int): Int? = byQuestion[questionId]

    /** Whether the driver has checked the answer to [questionId]. */
    fun isRevealed(questionId: Int): Boolean = questionId in revealed

    /**
     * The same answers with [questionId] answered by [chosen]. Answering again replaces the answer
     * that was there, so a driver can change their mind as often as they like; what they checked
     * stays checked, because the marks are wanted for the answer they have now.
     */
    fun withAnswer(questionId: Int, chosen: Int): PracticeAnswers =
        copy(byQuestion = byQuestion + (questionId to chosen))

    /**
     * The same answers with [questionId]'s answer checked: a question the driver hasn't answered has
     * nothing to check, and is left as it was.
     */
    fun withReveal(questionId: Int): PracticeAnswers =
        if (questionId in byQuestion) copy(revealed = revealed + questionId) else this
}
