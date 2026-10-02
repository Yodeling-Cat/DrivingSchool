package com.spiraclestudios.autoskola.practice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SheetGeometryTest {

    /**
     * A phone's window: a bar 84dp tall floating in it, 24dp of navigation bar at its end, and the
     * app bar's 124dp kept above the sheet while it is open.
     */
    private val window = sheetGeometry(
        available = 800.dp,
        barSlotHeight = 84.dp,
        navBarInset = 24.dp,
        openTopRoom = 124.dp,
    )

    /**
     * How far open the sheet is with its top edge at [offset]. The drag reports offsets in pixels
     * and the geometry measures them in dp, but the two cancel out: what is read here is the share
     * of the travel that is behind the drag.
     */
    private fun openAt(offset: Dp): Float =
        sheetOpenProgress(offset.value, window.restOffset.value, window.openOffset.value)

    @Test
    fun theListTakesTheHeightLeftBelowTheRoomTheOpenSheetKeeps() {
        assertEquals(592.dp, window.listHeight)
    }

    @Test
    fun theSheetRestsOnThePeekLineAndComesToRestOnTheRoomItKeepsWhenOpen() {
        // Shut: the bar's slot plus the navigation bar's room are all that shows, so the sheet's top
        // edge is that far off the bottom of the window.
        assertEquals(692.dp, window.restOffset)
        // Open: the whole app bar's room is left above the sheet, which is what keeps the bar whole.
        assertEquals(124.dp, window.openOffset)
    }

    @Test
    fun aDragFromThePeekLineToFullyOpenRunsFromNothingToTheWholeOpenLook() {
        val shut = openAt(window.restOffset)
        val open = openAt(window.openOffset)

        assertEquals(0f, shut)
        assertEquals(1f, open)
        assertEquals(0.dp, sheetShadowElevation(sheetCover(shut), full = 24.dp))
        assertEquals(24.dp, sheetShadowElevation(sheetCover(open), full = 24.dp))
        assertEquals(592.dp, sheetSurfaceHeight(sheetCover(shut), window.listHeight, barSlotHeight = 84.dp))
        assertEquals(676.dp, sheetSurfaceHeight(sheetCover(open), window.listHeight, barSlotHeight = 84.dp))
    }

    @Test
    fun aWindowWithNoRoomLeftLeavesAnEmptyListAndNothingToOpen() {
        val squeezed = sheetGeometry(
            available = 100.dp,
            barSlotHeight = 84.dp,
            navBarInset = 24.dp,
            openTopRoom = 124.dp,
        )

        assertEquals(0.dp, squeezed.listHeight)
        assertEquals(
            0f,
            sheetOpenProgress(squeezed.restOffset.value, squeezed.restOffset.value, squeezed.openOffset.value),
        )
    }

    @Test
    fun theSheetLooksHalfOpenOnceHalfOfTheDragIsBehindIt() {
        // Half of the 30% of the drag the whole open look takes.
        val travel = window.restOffset - window.openOffset
        val half = openAt(window.restOffset - travel * 0.15f)

        assertEquals(0.15f, half, 1e-5f)
        assertEquals(0.5f, sheetCover(half), 1e-5f)
    }

    @Test
    fun halfTheOpenLookIsHalfTheShadowOverHalfTheBarSlot() {
        assertEquals(12.dp, sheetShadowElevation(cover = 0.5f, full = 24.dp))
        assertEquals(642.dp, sheetSurfaceHeight(cover = 0.5f, listHeight = 600.dp, barSlotHeight = 84.dp))
    }

    @Test
    fun theOpenLookIsWholeFromThirtyPercentOfTheDragOnwards() {
        assertEquals(1f, sheetCover(SHEET_COVERED_AT))
        assertEquals(1f, sheetCover(0.5f))
        assertEquals(1f, sheetCover(1f))
    }

    @Test
    fun aDragPastEitherEndStaysAtThatEnd() {
        // The drag can overshoot both ends of its travel.
        assertEquals(0f, openAt(window.restOffset + 50.dp))
        assertEquals(1f, openAt(window.openOffset - 50.dp))
        assertEquals(1f, openAt(0.dp))
    }

    @Test
    fun aSheetWithNoRoomToOpenHasNotOpened() {
        assertEquals(0f, sheetOpenProgress(offset = 800f, restOffset = 800f, openOffset = 800f))
    }
}
