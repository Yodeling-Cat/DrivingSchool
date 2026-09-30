package uno.lux.autoskola.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.ui.theme.Naj

/**
 * Material window size classes by width: phones are [Compact], small tablets and phones in
 * landscape [Medium], large tablets and desktop browsers [Expanded].
 */
enum class WidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        fun of(width: Dp): WidthClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.Compact }

/**
 * Material height classes. [Compact] (< 480dp) is mostly phones in landscape: little vertical
 * room, so put things side by side instead of stacking them.
 */
enum class HeightClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        fun of(height: Dp): HeightClass = when {
            height < 480.dp -> Compact
            height < 900.dp -> Medium
            else -> Expanded
        }
    }
}

val LocalHeightClass = staticCompositionLocalOf { HeightClass.Medium }

/** Maximum widths that keep content readable on large screens instead of stretching it. */
object ContentWidth {
    /** Forms, settings and single-column reading content. */
    val Narrow = 600.dp

    /** Question text and answers. */
    val Reading = 720.dp

    /** Multi-pane layouts. */
    val Wide = 1200.dp
}

/**
 * The free width on each side of a content column capped at [column] in a page [available] dp wide.
 * A page no wider than the column has no gutter, so anything meant to sit beside the content has to
 * find room elsewhere.
 */
fun contentGutter(available: Dp, column: Dp): Dp = (available - column).coerceAtLeast(0.dp) / 2

/** Measures the window and provides [LocalWidthClass] and [LocalHeightClass] to [content]. */
@Composable
fun ProvideWindowSizeClasses(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(
            LocalWidthClass provides WidthClass.of(maxWidth),
            LocalHeightClass provides HeightClass.of(maxHeight),
        ) {
            content()
        }
    }
}

// ── Previews ──

/**
 * What [ProvideWindowSizeClasses] hands the window it measures: each box below is one of the
 * screen sizes we support, with the classes it comes out as. The width is what a preview's own
 * `widthDp` sets, so this is also the quickest way to see which layout a preview will pick.
 */
@Preview(widthDp = 900, heightDp = 900)
@Composable
private fun WindowSizeClassesPreview() {
    AppPreview {
        PreviewBoard {
            listOf(
                Triple(360.dp, 640.dp, "Phone"),
                Triple(700.dp, 400.dp, "Phone in landscape"),
                Triple(700.dp, 900.dp, "Small tablet"),
                Triple(1200.dp, 800.dp, "Desktop"),
            ).forEach { (width, height, name) ->
                PreviewLabel("$name · $width x $height")
                Box(Modifier.width(width).height(height)) {
                    ProvideWindowSizeClasses {
                        Text(
                            "${LocalWidthClass.current} width · ${LocalHeightClass.current} height",
                            style = Naj.type.rowTitle,
                            color = Naj.colors.ink,
                        )
                    }
                }
            }
        }
    }
}
