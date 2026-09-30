package uno.lux.autoskola.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.ui.HeightClass
import kotlin.math.ceil

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

/** One quarter of the licence card's wave: a crest, from the baseline and back down to it. */
val LicenceWaveQuarter = 90.dp

/**
 * How many quarters of the wave it takes to cross the licence card. The design draws four, which is
 * exactly the width of the card in a phone window - but the card is as wide as its window, and the
 * wider one a medium window or the desktop band draws leaves the waves cut off a third of the way
 * across. So the pattern carries on until the last crest is past the card's right edge.
 */
fun licenceWaveQuarters(width: Dp): Int = ceil(width / LicenceWaveQuarter).toInt()
