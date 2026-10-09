package com.spiraclestudios.autoskola.practice

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertTrue

class SheetShapeTest {

    /** A sheet narrower than its window, the way it is in landscape: 640dp wide, 84dp of it showing. */
    private val sheet = Size(640f, 84f)

    private val outline = UnclippedSheetShape
        .createOutline(sheet, LayoutDirection.Ltr, Density(1f))
        .bounds

    @Test
    fun theSheetDoesNotCutOffAShadowAtItsSides() {
        // The bar's own shadow spreads past the sheet's sides when the sheet is narrower than the
        // window; cut off there, it draws the sheet's outline on the page as a shadow of its own.
        assertTrue(outline.left <= -SheetShadowReach.value, "left edge at ${outline.left}")
        assertTrue(outline.right >= sheet.width + SheetShadowReach.value, "right edge at ${outline.right}")
    }

    @Test
    fun theSheetDoesNotCutOffAShadowAboveIt() {
        // The bar sits a few dp below the sheet's top edge, and the open sheet casts its shadow up
        // over the page from that edge.
        assertTrue(outline.top <= -SheetShadowReach.value, "top edge at ${outline.top}")
    }
}
