package uno.lux.autoskola.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.ui.theme.Naj

/**
 * The design's dashed ring, in the proportions of the 24-unit box its SVG is drawn in: a circle of
 * radius 9, a stroke of 2.2, and dashes of 3.5 with a gap of 3.6.
 */
data class DashedCircle(
    val radius: Float,
    val strokeWidth: Float,
    val dash: Float,
    val gap: Float,
)

/** The ring's measurements at [diameter], scaled from the design's 24-unit box. */
fun dashedCircle(diameter: Float): DashedCircle = DashedCircle(
    radius = diameter * 9f / 24f,
    strokeWidth = diameter * 2.2f / 24f,
    dash = diameter * 3.5f / 24f,
    gap = diameter * 3.6f / 24f,
)

/**
 * The dashed ring itself, as the design marks a test that is not finished yet: `circle r="9"
 * stroke-width="2.2" stroke-dasharray="3.5 3.6"` with round caps. Material's outline icons draw a
 * solid ring, and a vector can't carry a dash pattern, so the ring is drawn here.
 */
@Composable
fun DashedCircleIcon(color: Color, modifier: Modifier = Modifier, diameter: Dp = 20.dp) {
    Canvas(modifier.size(diameter)) {
        val circle = dashedCircle(size.minDimension)
        drawCircle(
            color = color,
            radius = circle.radius,
            style = Stroke(
                width = circle.strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(circle.dash, circle.gap)),
            ),
        )
    }
}

// ── Previews ──

@Preview(widthDp = 280, heightDp = 160)
@Composable
private fun DashedCircleIconPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("AT 20DP, AT 32DP, AND IN INK")
            PreviewRow {
                DashedCircleIcon(Color.White)
                DashedCircleIcon(Color.White, diameter = 32.dp)
                DashedCircleIcon(Naj.colors.ink)
            }
        }
    }
}
