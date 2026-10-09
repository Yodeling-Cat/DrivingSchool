package com.spiraclestudios.autoskola.practice

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * How far past its own edges a shadow drawn inside the question sheet can reach: the bar's, and the
 * one the open sheet casts over the page.
 */
internal val SheetShadowReach = 64.dp

/**
 * The shape the scaffold gives the question sheet. The scaffold clips everything the sheet holds to
 * its shape, and the sheet is transparent — what is seen of it is drawn inside — so a shape the size
 * of the sheet only cuts off the shadows drawn in it. Where the sheet is narrower than the window
 * that cut draws the sheet's outline on the page as a shadow the sheet does not have. This one
 * reaches [SheetShadowReach] past the sheet on every side, so nothing in it is cut off.
 *
 * It is no shape to cast a shadow with: the scaffold's own shadow stays at none, and the sheet's
 * surface casts its own.
 */
internal object UnclippedSheetShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val reach = with(density) { SheetShadowReach.toPx() }
        return Outline.Rectangle(Rect(-reach, -reach, size.width + reach, size.height + reach))
    }
}
