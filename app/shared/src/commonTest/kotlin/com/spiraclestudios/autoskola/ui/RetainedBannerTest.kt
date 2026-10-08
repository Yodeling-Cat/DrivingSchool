package com.spiraclestudios.autoskola.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * A banner in a lazy list leaves the composition every time it is scrolled off screen. What is kept
 * here is what decides whether coming back is the ad the user already had or a fresh request.
 */
class RetainedBannerTest {

    private class FakeBanner(val widthDp: Int) : LoadedBanner {
        var destroyed = false
        override fun destroy() {
            destroyed = true
        }
    }

    private val loads = mutableListOf<FakeBanner>()
    private val load: (Int) -> LoadedBanner = { width -> FakeBanner(width).also { loads += it } }

    @Test
    fun aBannerScrolledBackOnScreenIsTheOneAlreadyLoaded() {
        val retained = RetainedBanner()

        val first = retained.at(360, load)
        val again = retained.at(360, load)

        // One request, however often the user scrolls past it: the SDK refreshes it on its own.
        assertSame(first, again)
        assertEquals(1, loads.size)
    }

    @Test
    fun theWidthIsRememberedSoTheSlotKeepsItsSizeWhenItComesBack() {
        val retained = RetainedBanner()
        assertEquals(0, retained.widthDp)

        retained.at(360, load)

        assertEquals(360, retained.widthDp)
    }

    @Test
    fun aNewWidthAsksForANewBannerAndDestroysTheOldOne() {
        val retained = RetainedBanner()
        val narrow = retained.at(360, load)

        // A rotation or a resized window: an ad rendered for the old width is the wrong shape.
        val wide = retained.at(700, load)

        assertNotSame(narrow, wide)
        assertEquals(listOf(360, 700), loads.map { it.widthDp })
        assertEquals(listOf(true, false), loads.map { it.destroyed })
    }

    @Test
    fun releasingDestroysTheBannerAndTheNextUseLoadsAgain() {
        val retained = RetainedBanner()
        retained.at(360, load)

        // The screen is gone.
        retained.release()

        assertEquals(true, loads.single().destroyed)
        retained.at(360, load)
        assertEquals(2, loads.size)
    }

    @Test
    fun releasingWhatWasNeverLoadedIsNothing() {
        RetainedBanner().release()

        assertEquals(0, loads.size)
    }
}
