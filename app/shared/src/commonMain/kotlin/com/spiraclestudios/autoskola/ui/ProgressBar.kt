package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.theme.Naj

/** A rounded bar with [fraction] of it filled, both ends of the fill rounded as the track is. */
@Composable
fun ProgressBar(
    fraction: Float,
    height: Dp,
    track: Color,
    fill: Brush,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.height(height).clip(shape).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(fill),
        )
    }
}

/** A [ProgressBar] filled with one color. */
@Composable
fun ProgressBar(
    fraction: Float,
    height: Dp,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    ProgressBar(fraction, height, track, SolidColor(fill), modifier)
}

// ── Previews ──

@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun ProgressBarPreview() {
    AppPreview {
        val colors = Naj.colors
        PreviewBoard {
            PreviewLabel("EMPTY, PART, FULL")
            ProgressBar(0f, 6.dp, colors.divider, colors.passedDot, Modifier.fillMaxWidth())
            ProgressBar(0.4f, 6.dp, colors.divider, colors.wrongDot, Modifier.fillMaxWidth())
            ProgressBar(1f, 6.dp, colors.divider, colors.passedDot, Modifier.fillMaxWidth())
            PreviewLabel("ON A GRADIENT")
            ProgressBar(
                0.6f,
                6.dp,
                colors.inkAt(0.1f),
                Brush.horizontalGradient(colors.primaryGradient),
                Modifier.fillMaxWidth(),
            )
        }
    }
}
