package com.spiraclestudios.autoskola.practice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The question sheet's numbers in the room it is given: how tall the question list it holds is, and
 * where its top edge comes to rest shut and open.
 */
internal data class SheetGeometry(val listHeight: Dp, val restOffset: Dp, val openOffset: Dp)

/**
 * Measures the question sheet for a window [available] tall, with a [barSlotHeight] bar floating in
 * it and [navBarInset] of navigation bar at its end, keeping [openTopRoom] above itself while it's
 * open. That room is what holds the top app bar whole above the open sheet; the list gives up the
 * height for it.
 */
internal fun sheetGeometry(
    available: Dp,
    barSlotHeight: Dp,
    navBarInset: Dp,
    openTopRoom: Dp,
): SheetGeometry {
    val listHeight = (available - openTopRoom - barSlotHeight).coerceAtLeast(0.dp)
    return SheetGeometry(
        listHeight = listHeight,
        // Shut, the sheet rests with its top on the line the bar's peek leaves it at.
        restOffset = available - barSlotHeight - navBarInset,
        // Open, the bar's slot sits over the list and the sheet's bottom is the window's, so its top
        // edge comes to rest on the room it kept.
        openOffset = available - barSlotHeight - listHeight,
    )
}

/**
 * The part of the drag by which the question sheet has taken on its open look. Everything the drag
 * changes — the sheet's shadow, its own surface, the bar's toggle and its arrows — is done by the
 * time this much of the way open is behind it, so the sheet reads as open well before it has
 * settled there.
 */
internal const val SHEET_COVERED_AT = 0.3f

/**
 * How far the question sheet has been dragged open: 0 while it rests at [SheetGeometry.restOffset],
 * 1 once it's fully expanded at [SheetGeometry.openOffset], and a drag past either end stays at that
 * end. The offsets are the sheet's top edge in pixels. A sheet with no room between them can't be
 * open at all.
 */
internal fun sheetOpenProgress(offset: Float, restOffset: Float, openOffset: Float): Float {
    val travel = restOffset - openOffset
    if (travel <= 0f) return 0f
    return ((restOffset - offset) / travel).coerceIn(0f, 1f)
}

/**
 * How much of that open look the sheet has taken on: 0 at rest, 1 from [SHEET_COVERED_AT] of the
 * drag onwards.
 */
internal fun sheetCover(openProgress: Float): Float = (openProgress / SHEET_COVERED_AT).coerceIn(0f, 1f)

/** The sheet's shadow: none while it rests over the page, all of [full] once it's covered. */
internal fun sheetShadowElevation(cover: Float, full: Dp): Dp = full * cover

/**
 * The height of the sheet's own surface: the question list's height with the bar's slot growing
 * into it, so that it wraps the list at rest and the whole sheet once covered.
 */
internal fun sheetSurfaceHeight(cover: Float, listHeight: Dp, barSlotHeight: Dp): Dp =
    listHeight + barSlotHeight * cover
