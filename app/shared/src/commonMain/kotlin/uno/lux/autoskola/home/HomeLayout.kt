package uno.lux.autoskola.home

import uno.lux.autoskola.ui.HeightClass

/**
 * How many tests one row of the two-pane home layout holds. The layout is picked by width, so a
 * phone held sideways gets it too - and its leftover pane fits only two tiles, while a third comes
 * out narrower than the number and the status line under it. So the short window keeps the two the
 * phone layout draws, and a window with height keeps the desktop's three.
 */
fun twoPaneColumns(height: HeightClass): Int = if (height == HeightClass.Compact) 2 else 3

/**
 * Whether the two-pane layout's resume card is the tall banner with the labelled Continue pill. A
 * phone held sideways is not tall enough for it: the pill leaves the summary less room than the
 * longer translations need and it wraps out of the card, so the short window draws what the phone
 * layout draws - the compact card, with the round play button and its own progress bar.
 */
fun twoPaneResumeWide(height: HeightClass): Boolean = height != HeightClass.Compact
