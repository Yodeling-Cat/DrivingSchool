package com.spiraclestudios.autoskola.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.HeightClass

/** How many tests one row of the phone layout holds. */
internal const val PHONE_COLUMNS = 2

/** How many a row holds in a window with the room for the two-pane layout's own measurements. */
private const val DESKTOP_COLUMNS = 3

/** The gap between two tiles of the two-pane grid. */
internal val WideGridGap = 14.dp

/**
 * The narrowest tile the wide grid draws. Its own frame is 46dp - the 16dp padding on each side, the
 * 8dp status dot and the 6dp gap before the label - and the widest label the four languages write
 * there is the score of a failed test, "40 / 40 · Neúspešný" in Slovak and Czech at 117.5dp. That
 * comes to 164dp, which this rounds up with a little air. A narrower tile wraps the label onto a
 * second line, and the tile's 76dp height then clips it: at the 114dp tiles a 912dp window drew,
 * English came out as "Not" and "29 / 100 ·".
 */
internal val TileMinWidth = 170.dp

/** The pane three of those tiles need, with the gaps between them. */
private val ThreeTilePane = TileMinWidth * 3 + WideGridGap * 2

/**
 * Whether the two-pane home layout has to draw what the phone layout draws. The layout is picked by
 * width, so a phone held sideways gets it too - and it is neither tall enough nor, in a narrow
 * window, wide enough for the desktop's measurements: the pane beside the overview band has to leave
 * room for three tiles of [TileMinWidth], or every status label wraps and clips, and the resume
 * banner's labelled Continue pill leaves the summary less room than the longer translations need, so
 * it wraps out of the card. Which columns the grid holds and which resume card it draws both follow
 * from this one rule, so a cramped window cannot get half of the desktop's shape.
 */
private fun keepsThePhoneShape(height: HeightClass, paneWidth: Dp): Boolean =
    height == HeightClass.Compact || paneWidth < ThreeTilePane

/** How many tests one row of the two-pane home layout holds; see [keepsThePhoneShape]. */
fun twoPaneColumns(height: HeightClass, paneWidth: Dp): Int =
    if (keepsThePhoneShape(height, paneWidth)) PHONE_COLUMNS else DESKTOP_COLUMNS

/**
 * Whether the two-pane layout's resume card is the tall banner with the labelled Continue pill, or
 * the phone layout's compact card, with the round play button and its own progress bar. See
 * [keepsThePhoneShape].
 */
fun twoPaneResumeWide(height: HeightClass, paneWidth: Dp): Boolean = !keepsThePhoneShape(height, paneWidth)
