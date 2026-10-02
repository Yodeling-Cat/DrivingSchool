package com.spiraclestudios.autoskola.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The licence ramp's waves, which the home screen's card and the results poster both draw. */
class LicenceWavesTest {

    /** The design's four quarters of the wave are exactly the width of a phone's licence card. */
    @Test
    fun aPhonesCardTakesTheDesignsFourQuarters() {
        assertEquals(4, licenceWaveQuarters(360.dp))
    }

    /** Whatever the card measures, the last crest of the wave is past its right edge, not short of it. */
    @Test
    fun theWavesReachTheCardsWholeWidth() {
        // 368dp is a phone's card, 568dp a medium window's, 362dp the desktop home band's.
        listOf(360.dp, 368.dp, 362.dp, 568.dp, 1200.dp).forEach { width ->
            val drawn = LicenceWaveQuarter * licenceWaveQuarters(width)
            assertTrue(
                drawn >= width,
                "the waves stop at $drawn dp, short of a ${width}dp card's right edge",
            )
        }
    }
}
