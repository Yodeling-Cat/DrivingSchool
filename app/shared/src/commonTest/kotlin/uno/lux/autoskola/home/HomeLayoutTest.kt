package uno.lux.autoskola.home

import androidx.compose.ui.unit.dp
import uno.lux.autoskola.ui.HeightClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeLayoutTest {

    /** A Pixel 9 Pro held sideways: 952 x 426dp, Expanded by width and Compact by height. */
    private val sidewaysPhone = HeightClass.of(426.dp)

    @Test
    fun aPhoneHeldSidewaysKeepsThePhonesTwoColumns() {
        assertEquals(2, twoPaneColumns(sidewaysPhone))
    }

    @Test
    fun aWindowWithMoreHeightShowsThree() {
        // 480dp is where Material stops calling a window short; a tablet in either orientation is above it.
        assertEquals(3, twoPaneColumns(HeightClass.of(480.dp)))
        assertEquals(3, twoPaneColumns(HeightClass.of(1200.dp)))
    }

    @Test
    fun aPhoneHeldSidewaysDrawsTheCompactResumeCard() {
        assertFalse(twoPaneResumeWide(sidewaysPhone))
    }

    @Test
    fun aTallerWindowDrawsTheResumeBanner() {
        assertTrue(twoPaneResumeWide(HeightClass.of(480.dp)))
        assertTrue(twoPaneResumeWide(HeightClass.of(1200.dp)))
    }

    /** The design's four quarters of the wave are exactly the width of a phone's licence card. */
    @Test
    fun aPhonesCardTakesTheDesignsFourQuarters() {
        assertEquals(4, licenceWaveQuarters(360.dp))
    }

    /** Whatever the card measures, the last crest of the wave is past its right edge, not short of it. */
    @Test
    fun theWavesReachTheCardsWholeWidth() {
        // 368dp is the card in a phone window, 568dp the one in a medium window, 362dp the desktop band's.
        listOf(360.dp, 368.dp, 362.dp, 568.dp, 1200.dp).forEach { width ->
            val drawn = LicenceWaveQuarter * licenceWaveQuarters(width)
            assertTrue(
                drawn >= width,
                "the waves stop at $drawn dp, short of a ${width}dp card's right edge",
            )
        }
    }
}
