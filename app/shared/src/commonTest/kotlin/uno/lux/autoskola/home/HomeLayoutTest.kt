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
}
