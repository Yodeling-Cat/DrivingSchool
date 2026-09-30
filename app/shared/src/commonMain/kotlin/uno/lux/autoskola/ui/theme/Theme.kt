package uno.lux.autoskola.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
 * The app's theme: one place that owns the colours and the type scale.
 *
 * Every screen sits on [NajColors.paper] with its glows and grain (see `PaperBackground`), and
 * draws white cards on soft shadows instead of borders.
 */
@Composable
fun NajAutoskolaTheme(content: @Composable () -> Unit) {
    val colors = NajLightColors
    val fonts = najFonts()
    val type = NajType(fonts)

    // The design has no dark variant yet, so the light palette is used on every platform.
    @Suppress("UNUSED_VARIABLE")
    val dark = isSystemInDarkTheme()

    CompositionLocalProvider(
        LocalNajColors provides colors,
        LocalNajType provides type,
    ) {
        MaterialTheme(
            colorScheme = najColorScheme(colors),
            typography = najTypography(type),
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
