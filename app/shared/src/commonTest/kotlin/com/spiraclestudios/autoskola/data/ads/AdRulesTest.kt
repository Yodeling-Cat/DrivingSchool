package com.spiraclestudios.autoskola.data.ads

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * When the app offers the ad-or-buy choice, and when the closing ad gives way to it.
 *
 * The frequency is counted in practice tests *started*, so what is offered at nine is the tenth
 * test: the count on the way into test ten is nine. Both rules read that count and nothing else,
 * which is what keeps them testable without a clock, a store or an SDK.
 */
class AdRulesTest {

    @Test
    fun theOfferIsDueOnTheWayIntoEveryTenthTest() {
        // Nine tests have been started, so the next tap starts the tenth.
        assertTrue(AdRules.offerDue(9))
        assertTrue(AdRules.offerDue(19))
        assertTrue(AdRules.offerDue(29))
    }

    @Test
    fun nothingIsOfferedOnACountThatIsNotAboutToStartATenthTest() {
        assertFalse(AdRules.offerDue(0))
        assertFalse(AdRules.offerDue(8))
        // Straight after a gated test: the user has just been through the gate, so it re-arms and
        // the next nine tests are theirs without an interruption.
        assertFalse(AdRules.offerDue(10))
        assertFalse(AdRules.offerDue(18))
        assertFalse(AdRules.offerDue(20))
    }

    @Test
    fun theClosingAdYieldsToAGateThatIsAlreadyDue() {
        // The two rules deliberately read the same number rather than differing by one. The test
        // that has just finished has already counted, so the user's next start is evaluated at
        // exactly this count: after the ninth test the next tap is gated, and the closing ad has to
        // give way to it. Two fullscreen ads on one trip through the app is the thing being avoided,
        // and the gate is the one worth spending.
        assertTrue(AdRules.closingAdYields(9))
        assertTrue(AdRules.closingAdYields(19))

        // And after a test whose own start was gated, the closing ad is allowed: the gate has
        // already been paid for this trip, so the result screen gets its ad.
        assertFalse(AdRules.closingAdYields(10))
        assertFalse(AdRules.closingAdYields(20))
        assertFalse(AdRules.closingAdYields(0))
    }

    @Test
    fun theClosingAdYieldsInExactlyTheCasesTheOfferIsDue() {
        // Pinned as a property, not just at the handful of counts above: if either rule is ever
        // changed on its own, this is the test that says the pair no longer agrees.
        for (count in 0..100) {
            if (AdRules.offerDue(count)) {
                assertTrue(AdRules.closingAdYields(count), "closing ad did not yield at $count")
            } else {
                assertFalse(AdRules.closingAdYields(count), "closing ad yielded at $count")
            }
        }
    }
}
