package com.spiraclestudios.autoskola.data.ads

/**
 * When an ad is due, counted in practice tests started.
 *
 * Deliberately arithmetic and nothing else — no clock, no store, no SDK — so the awkward cases (the
 * gate landing on the test a user was about to start, the closing ad landing beside it) are settled
 * here and pinned in `AdRulesTest` rather than discovered on a device.
 *
 * Whether the app may ask for an ad *at all* is a different question and lives in `AdRepository`:
 * consent and entitlement, rather than arithmetic.
 */
object AdRules {

    /**
     * Every tenth practice test is offered the choice between watching an ad and buying Premium.
     *
     * Only practice tests count. A repeat of the questions the user got wrong and a run through the
     * situations are short drills started on impulse, and interrupting one of those would spend the
     * interruption on the wrong thing.
     */
    const val OFFER_EVERY = 10

    /**
     * Whether the offer is due on the way into the next test, given how many practice tests have
     * been started. True at 9, 19 and 29, which are the counts on the way into the tenth, twentieth
     * and thirtieth test — the reason the remainder is one short of the multiple rather than equal
     * to it, and the reason the count has to be read *before* the new test is recorded.
     */
    fun offerDue(practiceTestsStarted: Int): Boolean = practiceTestsStarted % OFFER_EVERY == OFFER_EVERY - 1

    /**
     * Whether the closing ad, shown once the result screen is done with, has to give way to a gate
     * that is already due.
     *
     * This reads the same count as [offerDue] rather than one either side of it, and that is the
     * point rather than a coincidence: the test that has just been marked has already been counted,
     * so the user's next start is evaluated at exactly this count. After the ninth test the next tap
     * is gated, so the closing ad yields and that trip through the app costs one fullscreen ad
     * instead of two — and the gate is the one worth spending, because it is the one that can be
     * bought out of.
     */
    fun closingAdYields(practiceTestsStarted: Int): Boolean = offerDue(practiceTestsStarted)
}
