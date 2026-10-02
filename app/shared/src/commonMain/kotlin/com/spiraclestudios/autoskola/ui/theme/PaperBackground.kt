package com.spiraclestudios.autoskola.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import com.spiraclestudios.autoskola.ui.AppPreview
import najautoskola.app.shared.generated.resources.Res
import najautoskola.app.shared.generated.resources.grain
import org.jetbrains.compose.resources.imageResource

/** Grain over the page, as the design system draws it. */
const val GRAIN_PAGE = 0.28f

/** Grain over a coloured card — the licence card, the resume card, the result card. */
const val GRAIN_CARD = 0.35f

/**
 * One of the page's soft glows: a squashed radial gradient, sized and placed in fractions of the
 * page, fading out at [fade] of its radius (CSS `radial-gradient(... #FFE9D2 0%, transparent 70%)`).
 */
data class PaperGlow(
    val color: Color,
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float,
    val fade: Float = 0.7f,
)

/** Warm peach into the top-left corner, mint into the bottom-right: the phone and wizard pages. */
fun paperGlows(colors: NajColors, mintY: Float = 0.9f, mintRadius: Float = 0.4f): List<PaperGlow> = listOf(
    PaperGlow(colors.peachGlow, 0f, 0f, 0.9f, 0.4f),
    PaperGlow(colors.mintGlow, 1f, mintY, 0.8f, mintRadius),
)

/**
 * The page behind every screen: the warm paper, its corner glows, and the tiling grain over the
 * top of both. Content is drawn above the grain, as it is in the design.
 *
 * The glows are drawn inside [clipRect] because a `DrawScope` does not clip to the node it belongs
 * to: a glow centred on this page's corner would otherwise paint the same distance to the left of
 * it, over whatever is next to the page - the navigation rail, which came out washed pale instead
 * of navy.
 */
@Composable
fun Modifier.paperBackground(glows: List<PaperGlow>, grain: Float = GRAIN_PAGE): Modifier {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    return this
        .drawBehind {
            clipRect {
                drawRect(colors.paper)
                glows.forEach { drawGlow(it) }
            }
        }
        .grain(grain, shader)
}

/** The page without the glows, for screens that sit on plain paper (the question list sheet). */
@Composable
fun Modifier.paperBackground(grain: Float = GRAIN_PAGE): Modifier = paperBackground(paperGlows(Naj.colors), grain)

/** The tiling grain tile, kept for the lifetime of the composition. */
@Composable
fun rememberGrainShader(): Shader {
    val tile = imageResource(Res.drawable.grain)
    return remember(tile) { ImageShader(tile, TileMode.Repeated, TileMode.Repeated) }
}

/**
 * Draws the grain tile behind whatever [Modifier.grain] is applied to, so a card's own background
 * and its text stay crisp: the design lays the grain under the content, not over it.
 */
fun Modifier.grain(alpha: Float = GRAIN_PAGE, shader: Shader): Modifier = drawBehind {
    if (alpha > 0f) {
        drawRect(
            brush = ShaderBrush(shader),
            alpha = alpha,
            blendMode = BlendMode.Multiply,
        )
    }
}

private fun DrawScope.drawGlow(glow: PaperGlow) {
    val radiusX = size.width * glow.radiusX
    val radiusY = size.height * glow.radiusY
    if (radiusX <= 0f || radiusY <= 0f) return
    translate(size.width * glow.centerX, size.height * glow.centerY) {
        // Scaling the canvas turns the circular gradient into the CSS ellipse.
        scale(radiusX, radiusY, pivot = Offset.Zero) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to glow.color, glow.fade to glow.color.copy(alpha = 0f)),
                    center = Offset.Zero,
                    radius = 1f,
                ),
                topLeft = Offset(-1f, -1f),
                size = Size(2f, 2f),
            )
        }
    }
}

// ── Previews ──

/**
 * The page the screens sit on, with the default corner glows on the left and the mint pulled up the
 * page on the right - the variant settings and results ask for.
 */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun PaperBackgroundPreview() {
    AppPreview {
        val colors = Naj.colors
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().paperBackground(paperGlows(colors)))
            Box(Modifier.weight(1f).fillMaxHeight().paperBackground(paperGlows(colors, mintY = 0.6f)))
        }
    }
}
