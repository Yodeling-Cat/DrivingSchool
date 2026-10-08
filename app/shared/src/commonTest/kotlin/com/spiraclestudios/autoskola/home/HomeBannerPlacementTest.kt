package com.spiraclestudios.autoskola.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Where the wide layout's banner goes: under the mistakes card where it fits on screen whole, above it
 * where it would be cut off - a 7-inch tablet on its side. The answer has to be the same whichever of
 * the two places the banner is in while it is measured, or it would jump back and forth.
 */
class HomeBannerPlacementTest {

    private val mistakes = 160
    private val gap = 30

    @Test
    fun aBannerThatEndsOnScreenStaysUnderTheMistakesCard() {
        assertTrue(bannerFitsUnderMistakes(900, aboveMistakes = false, mistakes, gap, viewportPx = 1000))
        assertTrue(bannerFitsUnderMistakes(1000, aboveMistakes = false, mistakes, gap, viewportPx = 1000))
    }

    @Test
    fun aBannerThatWouldEndBelowTheScreenGoesAboveIt() {
        assertFalse(bannerFitsUnderMistakes(1001, aboveMistakes = false, mistakes, gap, viewportPx = 1000))
    }

    @Test
    fun measuredAboveTheCardItIsJudgedWhereItWouldBeUnderIt() {
        // Above the card it ends at 850, which is on screen - but under it, it would end at 1040, which
        // is not, so it stays above rather than moving down and straight back up again.
        assertFalse(bannerFitsUnderMistakes(850, aboveMistakes = true, mistakes, gap, viewportPx = 1000))
        // And on a taller window the same position says it fits under the card.
        assertTrue(bannerFitsUnderMistakes(850, aboveMistakes = true, mistakes, gap, viewportPx = 1100))
    }
}
