package com.spiraclestudios.autoskola.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's color tokens, one per row of the design system's color table.
 *
 * Screens read these instead of repeating hex values, so a shade is changed in one place. The
 * pairs (`passedOn`, `wrongOn`, ...) are the foreground that belongs on that background.
 */
class NajColors(
    // Surfaces
    val paper: Color,
    val peachGlow: Color,
    val mintGlow: Color,
    val card: Color,
    val sheet: Color,
    val divider: Color,
    val emptyOutline: Color,
    // Content
    val ink: Color,
    val muted: Color,
    val coral: Color,
    val roadYellow: Color,
    // Primary
    val primary: Color,
    val primaryDeep: Color,
    val selectedTint: Color,
    // Navy bars (navigation pill, question bar)
    val navy: Color,
    val navyLight: Color,
    // Navigation rail (tablets and desktop; the phone's pill stays navy)
    val rail: Color,
    val railDivider: Color,
    val railContent: Color,
    // Status
    val passedOn: Color,
    val passedTop: Color,
    val passedBottom: Color,
    val passedDot: Color,
    val wrongOn: Color,
    val wrongTop: Color,
    val wrongBottom: Color,
    val wrongDot: Color,
    val skippedOn: Color,
    val skippedOnTint: Color,
    val skippedTint: Color,
    val skippedDot: Color,
    val notStartedOn: Color,
    val notStartedTint: Color,
    val notStartedDot: Color,
    val correctOn: Color,
    val correctTint: Color,
    // Progress cards
    val resumeTop: Color,
    val resumeBottom: Color,
    val resumeOn: Color,
    val mistakesTop: Color,
    val mistakesBottom: Color,
    // Controls
    val neutralTint: Color,
    val neutralOn: Color,
    val disabledOn: Color,
    val trackOff: Color,
) {
    /** Primary buttons and switches run from [primary] into [primaryDeep]. */
    val primaryGradient: List<Color> get() = listOf(primary, primaryDeep)

    /** The license card, the progress card and the result card share this diagonal ramp. */
    val licenseGradient: List<Color> get() = listOf(licenseStart, licenseSecond, licenseThird, licenseEnd)

    val licenseStart: Color get() = Color(0xFF12B5A6)
    val licenseSecond: Color get() = Color(0xFF7BD88F)
    val licenseThird: Color get() = Color(0xFFFFD37A)
    val licenseEnd: Color get() = Color(0xFFFF9E7A)

    /** Ink as it is used for hairlines and washes: the same color, at an opacity. */
    fun inkAt(alpha: Float): Color = ink.copy(alpha = alpha)

    val whiteVeil: Color get() = Color.White.copy(alpha = 0.7f)
}

val NajLightColors = NajColors(
    paper = Color(0xFFFFF8EE),
    peachGlow = Color(0xFFFFE9D2),
    mintGlow = Color(0xFFDFF3E6),
    card = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFCF7),
    divider = Color(0xFFE0D2BC),
    emptyOutline = Color(0xFFE4D9C6),
    ink = Color(0xFF0F2A3A),
    muted = Color(0xFF5B7080),
    coral = Color(0xFFFF5A3C),
    roadYellow = Color(0xFFFFC629),
    primary = Color(0xFF14A396),
    primaryDeep = Color(0xFF0F766E),
    selectedTint = Color(0xFFCFEDE7),
    navy = Color(0xFF0F2A3A),
    navyLight = Color(0xFF163A4F),
    rail = Color(0xFFCFEDE7),
    railDivider = Color(0xFFB5DDD5),
    railContent = Color(0xFF3D5566),
    passedOn = Color(0xFF12703C),
    passedTop = Color(0xFFE4F7EB),
    passedBottom = Color(0xFFC6EBD4),
    passedDot = Color(0xFF2E9E5B),
    wrongOn = Color(0xFFB3262B),
    wrongTop = Color(0xFFFFE9E7),
    wrongBottom = Color(0xFFFFCFCB),
    wrongDot = Color(0xFFE5484D),
    skippedOn = Color(0xFF8A5200),
    skippedOnTint = Color(0xFF7A4A00),
    skippedTint = Color(0xFFFFE08A),
    skippedDot = Color(0xFFFFB800),
    notStartedOn = Color(0xFF5B7080),
    notStartedTint = Color(0xFFEAF5F2),
    notStartedDot = Color(0xFFB8CCC8),
    correctOn = Color(0xFF12703C),
    correctTint = Color(0xFFC6EBD4),
    resumeTop = Color(0xFFFFD84D),
    resumeBottom = Color(0xFFFFB800),
    resumeOn = Color(0xFF7A4A00),
    mistakesTop = Color(0xFFFF6B6B),
    mistakesBottom = Color(0xFFE5484D),
    neutralTint = Color(0xFFF3EDE2),
    neutralOn = Color(0xFF5B7080),
    disabledOn = Color(0xFF8C8279),
    trackOff = Color(0xFFEDE6D8),
)

val LocalNajColors = staticCompositionLocalOf { NajLightColors }
