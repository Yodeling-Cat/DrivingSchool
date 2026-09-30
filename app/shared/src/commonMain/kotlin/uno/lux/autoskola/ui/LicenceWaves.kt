package uno.lux.autoskola.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * The waves the licence ramp carries, which both the home screen's licence card and the results
 * poster draw: ten of them, one every 16dp from y = 40, 30dp from crest to trough.
 */
private const val LICENCE_WAVE_COUNT = 10
private val LicenceWaveStartY = 40.dp
private val LicenceWaveStepY = 16.dp
private val LicenceWaveAmplitude = 30.dp
private val LicenceWaveStroke = 1.2.dp
private val LicenceWaveOffset = 45.dp
private const val LICENCE_WAVE_ALPHA = 0.35f

/** One quarter of the wave: a crest, from the baseline and back down to it. */
val LicenceWaveQuarter = 90.dp

/**
 * How many quarters of the wave it takes to cross a card [width] wide. The design draws four, which
 * is exactly the width of the card in a phone window - but a card is as wide as its window, and a
 * wider one leaves four quarters cut off part-way across. So the pattern carries on until the last
 * crest is past the card's right edge.
 */
fun licenceWaveQuarters(width: Dp): Int = ceil(width / LicenceWaveQuarter).toInt()

/**
 * The ten white wave lines over whatever this draws on - a licence card, or the results poster -
 * each a crest then a mirrored trough, carried across its whole width.
 */
fun DrawScope.drawLicenceWaves() {
    val quarter = LicenceWaveQuarter.toPx()
    val offset = LicenceWaveOffset.toPx()
    val amplitude = LicenceWaveAmplitude.toPx()
    val step = LicenceWaveStepY.toPx()
    val quarters = licenceWaveQuarters(size.width.toDp())
    repeat(LICENCE_WAVE_COUNT) { index ->
        val y = LicenceWaveStartY.toPx() + index * step
        val wave = Path().apply {
            moveTo(0f, y)
            repeat(quarters) { span ->
                // A crest on the even quarters and a trough on the odd ones, so the pattern keeps
                // the phase the design sets, whatever the card measures.
                val peak = if (span % 2 == 0) y - amplitude else y + amplitude
                quadraticTo(offset + quarter * span, peak, quarter * (span + 1), y)
            }
        }
        drawPath(wave, color = Color.White.copy(alpha = LICENCE_WAVE_ALPHA), style = Stroke(LicenceWaveStroke.toPx()))
    }
}
