package com.spiraclestudios.autoskola.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import najautoskola.app.shared.generated.resources.Res
import najautoskola.app.shared.generated.resources.bricolage_bold
import najautoskola.app.shared.generated.resources.bricolage_extrabold
import najautoskola.app.shared.generated.resources.dm_sans_bold
import najautoskola.app.shared.generated.resources.dm_sans_extrabold
import najautoskola.app.shared.generated.resources.dm_sans_medium
import najautoskola.app.shared.generated.resources.dm_sans_regular
import najautoskola.app.shared.generated.resources.fraunces_extrabold
import najautoskola.app.shared.generated.resources.fraunces_extrabold_italic
import org.jetbrains.compose.resources.Font

/**
 * The three bundled typefaces, one family per weight so a style never depends on the platform
 * resolving a `FontWeight` for us.
 *
 * Fraunces draws titles, Bricolage Grotesque numbers and headings, DM Sans everything else.
 */
class NajFonts(
    val fraunces: FontFamily,
    val frauncesItalic: FontFamily,
    val bricolage: FontFamily,
    val bricolageBold: FontFamily,
    val dmSans: FontFamily,
    val dmSansMedium: FontFamily,
    val dmSansBold: FontFamily,
    val dmSansExtraBold: FontFamily,
)

/** The design system's type scale: titles, numbers, body copy and the small caps labels. */
class NajType(fonts: NajFonts) {
    // Titles: Fraunces 800, 22-32, one word set in coral italic by [withAccent].
    val brand = TextStyle(fontFamily = fonts.fraunces, fontSize = 24.sp, letterSpacing = (-0.5).sp)
    val railMark = TextStyle(fontFamily = fonts.frauncesItalic, fontSize = 26.sp)
    val stepTitle = TextStyle(
        fontFamily = fonts.fraunces,
        fontSize = 32.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.8).sp,
    )
    val screenTitle = TextStyle(fontFamily = fonts.fraunces, fontSize = 30.sp, letterSpacing = (-0.7).sp)
    val barTitle = TextStyle(fontFamily = fonts.fraunces, fontSize = 22.sp, letterSpacing = (-0.4).sp)

    // The headings over a list or a group are titles too, as the design sets them: "All tests" and
    // "By section" are drawn in the display serif, a size apart from each other.
    val sectionHeading = TextStyle(fontFamily = fonts.fraunces, fontSize = 22.sp, letterSpacing = (-0.4).sp)
    val sectionHeadingLarge = TextStyle(
        fontFamily = fonts.fraunces,
        fontSize = 24.sp,
        letterSpacing = (-0.4).sp,
    )

    // Headings and numbers: Bricolage Grotesque 800.
    val heroNumber = TextStyle(
        fontFamily = fonts.bricolage,
        fontSize = 76.sp,
        lineHeight = 76.sp,
        letterSpacing = (-3).sp,
    )
    val heroSuffix = TextStyle(fontFamily = fonts.bricolageBold, fontSize = 30.sp)
    val statNumber = TextStyle(fontFamily = fonts.bricolage, fontSize = 22.sp)
    val tileNumber = TextStyle(
        fontFamily = fonts.bricolage,
        fontSize = 28.sp,
        lineHeight = 28.sp,
        letterSpacing = (-1).sp,
    )
    val counterNumber = TextStyle(fontFamily = fonts.bricolage, fontSize = 28.sp, lineHeight = 31.sp)
    val cardHero = TextStyle(fontFamily = fonts.bricolage, fontSize = 24.sp)
    val cardHeroSmall = TextStyle(fontFamily = fonts.bricolage, fontSize = 19.sp)
    val barNumber = TextStyle(fontFamily = fonts.bricolage, fontSize = 15.sp)
    val chipLetter = TextStyle(fontFamily = fonts.bricolage, fontSize = 15.sp)
    val answerLetter = TextStyle(fontFamily = fonts.bricolage, fontSize = 17.sp)
    val badgeNumber = TextStyle(fontFamily = fonts.bricolage, fontSize = 18.sp)
    val stamp = TextStyle(fontFamily = fonts.bricolage, fontSize = 18.sp, letterSpacing = 1.5.sp)
    val sectionScore = TextStyle(fontFamily = fonts.bricolage, fontSize = 14.sp)

    // Body copy: DM Sans.
    val question = TextStyle(fontFamily = fonts.dmSansMedium, fontSize = 18.sp, lineHeight = 26.sp)
    val questionWide = TextStyle(fontFamily = fonts.dmSansMedium, fontSize = 19.sp, lineHeight = 27.sp)
    val body = TextStyle(fontFamily = fonts.dmSans, fontSize = 16.sp, lineHeight = 24.sp)
    val cardTitle = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 16.sp)
    val option = TextStyle(fontFamily = fonts.dmSans, fontSize = 15.sp, lineHeight = 21.sp)
    val button = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 15.sp)
    val primaryButton = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 16.sp)
    val rowTitle = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 16.sp)
    val countryName = TextStyle(fontFamily = fonts.dmSansExtraBold, fontSize = 17.sp)
    val rowSubtitle = TextStyle(fontFamily = fonts.dmSans, fontSize = 13.sp, lineHeight = 18.sp)
    val secondary = TextStyle(fontFamily = fonts.dmSansMedium, fontSize = 12.5.sp)
    val tileStatus = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 12.sp)

    /** The words beside a hero number, as the license card sets "tests finished". */
    val heroCaption = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 13.sp)
    val small = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 12.sp)
    val eyebrow = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 10.5.sp, letterSpacing = 1.6.sp)
    val eyebrowTight = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 10.5.sp, letterSpacing = 1.sp)
    val capsLabel = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 11.sp, letterSpacing = 1.sp)
    val letterSpacedSmall = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 11.sp, letterSpacing = 0.8.sp)
    val tiny = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 11.5.sp)
    val navLabel = TextStyle(fontFamily = fonts.dmSansMedium, fontSize = 12.sp)
    val navLabelActive = TextStyle(fontFamily = fonts.dmSansBold, fontSize = 12.sp)
    val railLabel = TextStyle(fontFamily = fonts.dmSansMedium, fontSize = 11.sp)
}

@Composable
fun najFonts(): NajFonts {
    // `Font` is a composable that loads (and caches) the face, so it has to be called here and the
    // families remembered from its result.
    val fraunces = Font(Res.font.fraunces_extrabold)
    val frauncesItalic = Font(Res.font.fraunces_extrabold_italic)
    val bricolage = Font(Res.font.bricolage_extrabold)
    val bricolageBold = Font(Res.font.bricolage_bold)
    val dmSans = Font(Res.font.dm_sans_regular)
    val dmSansMedium = Font(Res.font.dm_sans_medium)
    val dmSansBold = Font(Res.font.dm_sans_bold)
    val dmSansExtraBold = Font(Res.font.dm_sans_extrabold)
    return remember(fraunces, frauncesItalic, bricolage, bricolageBold, dmSans, dmSansMedium, dmSansBold) {
        NajFonts(
            fraunces = FontFamily(fraunces),
            frauncesItalic = FontFamily(frauncesItalic),
            bricolage = FontFamily(bricolage),
            bricolageBold = FontFamily(bricolageBold),
            dmSans = FontFamily(dmSans),
            dmSansMedium = FontFamily(dmSansMedium),
            dmSansBold = FontFamily(dmSansBold),
            dmSansExtraBold = FontFamily(dmSansExtraBold),
        )
    }
}

@Composable
fun najType(): NajType {
    val fonts = najFonts()
    return remember(fonts) { NajType(fonts) }
}

val LocalNajType = staticCompositionLocalOf<NajType> { error("No NajType provided") }
