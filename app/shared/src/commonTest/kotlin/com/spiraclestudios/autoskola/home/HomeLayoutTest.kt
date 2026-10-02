package com.spiraclestudios.autoskola.home

import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.HeightClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeLayoutTest {

    /** A Pixel 9 Pro held sideways: 952 x 426dp, Expanded by width and Compact by height. */
    private val sidewaysPhone = HeightClass.of(426.dp)

    /** The pane that phone's window leaves beside the rail and the band: 952 - 88 - 410 - 48. */
    private val sidewaysPane = 406.dp

    /**
     * The window this rule was asked for: 912dp wide (a 1140px browser at 125% scaling) and 918dp
     * tall. The height is the desktop's; the pane beside the band is 912 - 88 - 410 - 48.
     */
    private val narrowPane = 366.dp

    /** A 1100dp desktop window's pane: 1100 - 88 - 410 - 48. */
    private val desktopPane = 554.dp

    @Test
    fun aPhoneHeldSidewaysKeepsThePhonesTwoColumns() {
        assertEquals(2, twoPaneColumns(sidewaysPhone, sidewaysPane))
    }

    /** A tall window is no help if the pane beside the band has no room for three tiles. */
    @Test
    fun aNarrowPaneKeepsThePhonesTwoColumns() {
        assertEquals(2, twoPaneColumns(HeightClass.of(918.dp), narrowPane))
    }

    @Test
    fun aPaneWithRoomForThreeTilesShowsThree() {
        // 480dp is where Material stops calling a window short; a tablet in either orientation is above it.
        assertEquals(3, twoPaneColumns(HeightClass.of(480.dp), desktopPane))
        assertEquals(3, twoPaneColumns(HeightClass.of(1200.dp), desktopPane))
    }

    /** The boundary is the three tiles and the gaps between them, and nothing else. */
    @Test
    fun threeTilesAndTheirGapsAreExactlyEnough() {
        val threeTiles = TileMinWidth * 3 + WideGridGap * 2
        assertEquals(3, twoPaneColumns(HeightClass.of(900.dp), threeTiles))
        assertEquals(2, twoPaneColumns(HeightClass.of(900.dp), threeTiles - 1.dp))
    }

    @Test
    fun aPhoneHeldSidewaysDrawsTheCompactResumeCard() {
        assertFalse(twoPaneResumeWide(sidewaysPhone, sidewaysPane))
    }

    @Test
    fun aNarrowPaneDrawsTheCompactResumeCard() {
        assertFalse(twoPaneResumeWide(HeightClass.of(918.dp), narrowPane))
    }

    @Test
    fun aTallWideWindowDrawsTheResumeBanner() {
        assertTrue(twoPaneResumeWide(HeightClass.of(480.dp), desktopPane))
        assertTrue(twoPaneResumeWide(HeightClass.of(1200.dp), desktopPane))
    }

    /** One rule decides both halves, so a cramped window can never get half of the desktop's shape. */
    @Test
    fun theTwoHalvesOfTheShapeAgreeAtEverySize() {
        HeightClass.entries.forEach { height ->
            listOf(0.dp, narrowPane, sidewaysPane, desktopPane).forEach { pane ->
                val threeColumns = twoPaneColumns(height, pane) == 3
                assertEquals(
                    threeColumns,
                    twoPaneResumeWide(height, pane),
                    "the two halves disagree at $height with a $pane pane",
                )
            }
        }
    }
}
