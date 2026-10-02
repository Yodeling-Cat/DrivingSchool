package com.spiraclestudios.autoskola.review

/** How many finished tests go by before the app first asks the user to rate it. */
const val TESTS_BEFORE_FIRST_ASK = 10

/** How many more finished tests go by before it asks again, after the first card was dismissed. */
const val TESTS_BEFORE_SECOND_ASK = 20

/**
 * How far the app has got in asking the user to rate it in the store.
 *
 * Asking is a ladder of three rungs: once after the driver has worked through a batch of tests,
 * again a batch later if the first card was closed, and once more when every test of the exam has
 * been finished and passed - the moment the app has done what the driver came for. Nothing is
 * asked after the last rung, and a driver who was taken to the store has rated, so the asking
 * ends there too.
 */
enum class ReviewAsk {
    /** The first card, due after [TESTS_BEFORE_FIRST_ASK] finished tests. */
    FirstAsk,

    /** The second, due [TESTS_BEFORE_SECOND_ASK] finished tests after the first was dismissed. */
    SecondAsk,

    /** The last, due once every test has been finished and passed. */
    FinalAsk,

    /** Asked as much as the app will: nothing more is shown, and nothing more is asked. */
    AskedEnough,
}

/**
 * What the app remembers about asking the user to rate it: where the ladder stands, and how many
 * tests had been finished when the card was dismissed last.
 */
data class ReviewRequest(
    val ask: ReviewAsk = ReviewAsk.FirstAsk,
    /** Finished tests at the last dismissal; `null` while the card has never been dismissed. */
    val dismissedAfter: Int? = null,
) {
    /**
     * Whether the card asking for a rating is due.
     *
     * [finishedTests] counts every finished practice test - a test taken again is another test
     * done, and it does not matter whether it was passed - and [everyTestPassed] says the driver
     * has finished and passed every test of the exam.
     */
    fun isDue(finishedTests: Int, everyTestPassed: Boolean): Boolean = when (ask) {
        ReviewAsk.FirstAsk -> finishedTests >= TESTS_BEFORE_FIRST_ASK
        ReviewAsk.SecondAsk -> finishedTests >= (dismissedAfter ?: 0) + TESTS_BEFORE_SECOND_ASK
        ReviewAsk.FinalAsk -> everyTestPassed
        ReviewAsk.AskedEnough -> false
    }

    /**
     * The request as it stands after the user closed the card without going to the store, having
     * finished [finishedTests] tests.
     *
     * A dismissal on the last rung - or one while every test is already passed, which leaves the
     * last rung nothing to wait for - ends the ladder: there is nothing further to ask, and the
     * card that would come straight back is not one the user made room for by closing it.
     */
    fun dismissed(finishedTests: Int, everyTestPassed: Boolean): ReviewRequest = copy(
        ask = when (ask) {
            ReviewAsk.FirstAsk -> ReviewAsk.SecondAsk
            ReviewAsk.SecondAsk -> if (everyTestPassed) ReviewAsk.AskedEnough else ReviewAsk.FinalAsk
            ReviewAsk.FinalAsk -> ReviewAsk.AskedEnough
            ReviewAsk.AskedEnough -> ReviewAsk.AskedEnough
        },
        dismissedAfter = finishedTests,
    )

    /** The request as it stands after the user was taken to the store: they have rated, we are done. */
    fun rated(): ReviewRequest = copy(ask = ReviewAsk.AskedEnough)
}
