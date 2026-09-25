package uno.lux.autoskola.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
