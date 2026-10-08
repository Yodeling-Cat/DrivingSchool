package com.spiraclestudios.autoskola.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PreviewRow

/** Reads the theme the way screens read `MaterialTheme`: `Naj.colors.ink`, `Naj.type.body`. */
object Naj {
    val colors: NajColors
        @Composable @ReadOnlyComposable
        get() = LocalNajColors.current

    val type: NajType
        @Composable @ReadOnlyComposable
        get() = LocalNajType.current
}

/**
 * The app's theme: one place that owns the colors and the type scale.
 *
 * Every screen sits on [NajColors.paper] with its glows and grain (see `PaperBackground`), and
 * draws white cards on soft shadows instead of borders.
 */
@Composable
fun NajAutoskolaTheme(content: @Composable () -> Unit) {
    val colors = NajLightColors
    // Remembered, so the theme recomposing - as the fonts load, or the system's settings change -
    // hands the screens the same type scale. [LocalNajType] is static: a new one, equal or not,
    // would compose every screen under it again.
    val type = najType()
    val colorScheme = remember(colors) { najColorScheme(colors) }
    val typography = remember(type) { najTypography(type) }

    // The design has no dark variant yet, so the light palette is used on every platform.
    @Suppress("UNUSED_VARIABLE")
    val dark = isSystemInDarkTheme()

    CompositionLocalProvider(
        LocalNajColors provides colors,
        LocalNajType provides type,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

private fun najColorScheme(c: NajColors) = lightColorScheme(
    primary = c.primaryDeep,
    onPrimary = Color.White,
    primaryContainer = c.selectedTint,
    onPrimaryContainer = c.ink,
    secondary = c.primary,
    onSecondary = Color.White,
    secondaryContainer = c.notStartedTint,
    onSecondaryContainer = c.ink,
    tertiary = c.coral,
    onTertiary = Color.White,
    background = c.paper,
    onBackground = c.ink,
    surface = c.card,
    onSurface = c.ink,
    surfaceVariant = c.neutralTint,
    onSurfaceVariant = c.muted,
    surfaceContainer = c.paper,
    surfaceContainerHigh = c.card,
    outline = c.emptyOutline,
    outlineVariant = c.divider,
    error = c.wrongOn,
    onError = Color.White,
    errorContainer = c.wrongBottom,
    onErrorContainer = c.wrongOn,
    scrim = c.ink,
)

/** Material components (switches, dialogs, chips) inherit the bundled fonts from here. */
private fun najTypography(t: NajType) = Typography(
    displaySmall = t.sectionHeadingLarge,
    headlineMedium = t.sectionHeading,
    headlineSmall = t.barTitle,
    titleLarge = t.cardHeroSmall,
    titleMedium = t.cardTitle,
    titleSmall = t.button,
    bodyLarge = t.body,
    bodyMedium = t.option,
    bodySmall = t.rowSubtitle,
    labelLarge = t.button,
    labelMedium = t.tileStatus,
    labelSmall = t.capsLabel,
)

/** Pushes a style to another weight without restating its size. */
fun TextStyle.bold(): TextStyle = copy(fontWeight = FontWeight.Bold, fontSize = fontSize, lineHeight = lineHeight)

// ── Previews ──

/**
 * The two tables a screen reads: a line of sample text in each type token, and a swatch per color
 * token. A new token is checked here before it goes into a screen.
 */
@Preview(widthDp = 420, heightDp = 900)
@Composable
private fun NajAutoskolaThemePreview() {
    AppPreview {
        val colors = Naj.colors
        PreviewBoard {
            PreviewLabel("TYPE")
            listOf(
                "stepTitle" to Naj.type.stepTitle,
                "screenTitle" to Naj.type.screenTitle,
                "barTitle" to Naj.type.barTitle,
                "sectionHeading" to Naj.type.sectionHeading,
                "heroNumber" to Naj.type.heroNumber,
                "question" to Naj.type.question,
                "body" to Naj.type.body,
                "rowTitle" to Naj.type.rowTitle,
                "rowSubtitle" to Naj.type.rowSubtitle,
                "eyebrow" to Naj.type.eyebrow,
            ).forEach { (name, style) ->
                Text(name, style = Naj.type.capsLabel, color = colors.muted)
                Text("Road signs and rules", style = style, color = colors.ink)
            }
            PreviewLabel("COLORS")
            listOf(
                "paper" to colors.paper,
                "card" to colors.card,
                "sheet" to colors.sheet,
                "divider" to colors.divider,
                "ink" to colors.ink,
                "muted" to colors.muted,
                "coral" to colors.coral,
                "roadYellow" to colors.roadYellow,
                "primary" to colors.primary,
                "primaryDeep" to colors.primaryDeep,
                "navy" to colors.navy,
                "selectedTint" to colors.selectedTint,
                "passedOn" to colors.passedOn,
                "passedBottom" to colors.passedBottom,
                "wrongOn" to colors.wrongOn,
                "wrongBottom" to colors.wrongBottom,
                "skippedOn" to colors.skippedOn,
                "skippedTint" to colors.skippedTint,
                "notStartedOn" to colors.notStartedOn,
                "notStartedTint" to colors.notStartedTint,
            ).chunked(4).forEach { swatches ->
                PreviewRow {
                    swatches.forEach { (name, color) ->
                        Column(Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color),
                            )
                            Text(name, style = Naj.type.tiny, color = colors.muted, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
