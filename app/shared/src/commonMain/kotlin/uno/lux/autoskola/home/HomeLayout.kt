package uno.lux.autoskola.home

import uno.lux.autoskola.ui.HeightClass

/** How many tests one row of the phone layout holds. */
internal const val PHONE_COLUMNS = 2

/** How many a row holds in a window with the height for the two-pane layout's own measurements. */
private const val DESKTOP_COLUMNS = 3

/**
 * Whether the two-pane home layout has to draw what the phone layout draws. The layout is picked by
 * width, so a phone held sideways gets it too - and it is not tall enough for the desktop's
 * measurements: a third tile comes out narrower than the number and the status line under it, and
 * the resume banner's labelled Continue pill leaves the summary less room than the longer
 * translations need, so it wraps out of the card. Which columns the grid holds and which resume card
 * it draws both follow from this one rule, so a short window cannot get half of the phone's shape.
 */
private fun keepsThePhoneShape(height: HeightClass): Boolean = height == HeightClass.Compact

/** How many tests one row of the two-pane home layout holds; see [keepsThePhoneShape]. */
fun twoPaneColumns(height: HeightClass): Int = if (keepsThePhoneShape(height)) PHONE_COLUMNS else DESKTOP_COLUMNS

/**
 * Whether the two-pane layout's resume card is the tall banner with the labelled Continue pill, or
 * the phone layout's compact card, with the round play button and its own progress bar. See
 * [keepsThePhoneShape].
 */
fun twoPaneResumeWide(height: HeightClass): Boolean = !keepsThePhoneShape(height)
