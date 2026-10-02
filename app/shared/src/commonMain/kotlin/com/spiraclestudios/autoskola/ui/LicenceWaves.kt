package com.spiraclestudios.autoskola.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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

/** The design's glow: a 260x200 ellipse, so 130x100 from its centre out to its edge. */
private val LicenceGlowRadiusX = 130.dp
private val LicenceGlowRadiusY = 100.dp
private const val LICENCE_GLOW_ALPHA = 0.55f

/**
 * The white glow that bleeds into a licence ramp - the home screen's card, the results poster - from
 * one of its edges, centred on [centre]. Scaling the canvas is what turns the gradient's circle into
 * the design's ellipse, and [centre] sits off the edge it bleeds in from.
 */
fun DrawScope.drawLicenceGlow(centre: DpOffset) {
    translate(centre.x.toPx(), centre.y.toPx()) {
        // Scaling the canvas turns the circular gradient into the design's ellipse.
        scale(LicenceGlowRadiusX.toPx(), LicenceGlowRadiusY.toPx(), pivot = Offset.Zero) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color.White.copy(alpha = LICENCE_GLOW_ALPHA), 1f to Color.Transparent),
                    center = Offset.Zero,
                    radius = 1f,
                ),
                topLeft = Offset(-1f, -1f),
                size = Size(2f, 2f),
            )
        }
    }
}
