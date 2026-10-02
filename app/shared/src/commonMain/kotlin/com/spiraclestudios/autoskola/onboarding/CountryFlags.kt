package com.spiraclestudios.autoskola.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PreviewRow

/**
 * The flag of an ISO 3166-1 alpha-2 [countryCode], or `null` when we haven't drawn one.
 *
 * Flags are drawn rather than left to the platform's emoji font: the regional-indicator glyphs such
 * a font uses for a flag are missing from the fonts most desktop browsers fall back to - Windows
 * ships no flag emoji at all - so an emoji flag renders on Android and comes out as two empty boxes
 * on the web. A vector is the same flag everywhere.
 */
internal fun countryFlag(countryCode: String): ImageVector? = FLAGS[countryCode.uppercase()]

/** The codes [countryFlag] answers for; a card for any other country falls back to its own code. */
internal val countriesWithFlags: Set<String> get() = FLAGS.keys

// Transcribed from the Wikimedia Commons SVGs linked above each flag, kept in each SVG's own
// coordinate space so the numbers still match the source. National flags are official symbols,
// which makes the sources public domain. Every flag is drawn 40dp tall, as wide as its own
// proportions make it.

// https://commons.wikimedia.org/wiki/File:Flag_of_Slovakia.svg
private val FlagSlovakia: ImageVector = ImageVector.Builder(
    name = "FlagSlovakia",
    defaultWidth = 60.dp,
    defaultHeight = 40.dp,
    viewportWidth = 900f,
    viewportHeight = 600f,
).apply {
    addPath(
        addPathNodes(
            "M0 400h900V200H0Z",
        ),
        fill = SolidColor(Color(0xFF254AA5)),
    )
    addPath(
        addPathNodes(
            "M0 200h900V0H0Z",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addPath(
        addPathNodes(
            "M0 600h900V400H0Z",
        ),
        fill = SolidColor(Color(0xFFED1C24)),
    )
    addPath(
        addPathNodes(
            "m269.993 459.98-3.906-1.867c-25.267-12.173-56.294-30.4-81.294-58.133-25-27.733-43.8" +
                "-65.307-43.8-114.24 0-93.6 4.52-136.68 4.52-136.68l.84-8.067h247.28l.84 8.067s4.534 " +
                "43.093 4.534 136.68c0 48.933-18.8 86.507-43.814 114.24-25 27.733-56.026 45.96-81.293 58.133Z",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addPath(
        addPathNodes(
            "M270 450c-49.38-23.76-120-70.94-120-164.25S154.46 150 154.46 150h231.07S390 192.44 390 " +
                "285.75 319.37 426.24 270 450",
        ),
        fill = SolidColor(Color(0xFFED1C24)),
    )
    addPath(
        addPathNodes(
            "M280.56 261.28c13.36.22 39.45.74 62.67-7.03 0 0-.61 8.31-.61 17.99 0 9.67.61 17.98.61 " +
                "17.98-21.3-7.12-47.61-7.27-62.67-7.08v51.54h-21.12v-51.54c-15.07-.2-41.37-.04-62.68 " +
                "7.08 0 0 .62-8.3.62-17.98s-.62-17.99-.62-17.99c23.23 7.77 49.31 7.25 62.68 7.03v-32.37c" +
                "-12.19-.1-29.74.48-49.6 7.12 0 0 .62-8.3.62-17.98s-.62-17.98-.62-17.98c19.83 6.62 37.36 " +
                "7.22 49.54 7.11-.62-20.5-6.6-46.33-6.6-46.33s12.3.96 17.22.96c4.92 0 17.21-.96 17.21" +
                "-.96s-5.97 25.83-6.6 46.33c12.18.1 29.72-.49 49.55-7.11 0 0-.62 8.3-.62 17.98 0 9.67.62 " +
                "17.98.62 17.98-19.86-6.64-37.42-7.22-49.6-7.12v32.37",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addPath(
        addPathNodes(
            "M270 329.1c-24.87 0-38.19 34.46-38.19 34.46s-7.4-16.34-27.68-16.34c-13.73 0-23.82 12.2" +
                "-30.25 23.5 24.97 39.7 64.8 64.2 96.11 79.28 31.32-15.07 71.16-39.58 96.13-79.28-6.43" +
                "-11.3-16.52-23.5-30.25-23.5a30.52 30.52 0 0 0-27.69 16.34s-13.32-34.46-38.19-34.46Z",
        ),
        fill = SolidColor(Color(0xFF254AA5)),
    )
    addOutline(path = "M7.5 7.5h885v585H7.5Z", strokeWidth = 15f)
}.build()

// https://commons.wikimedia.org/wiki/File:Flag_of_the_Czech_Republic.svg
private val FlagCzechia: ImageVector = ImageVector.Builder(
    name = "FlagCzechia",
    defaultWidth = 60.dp,
    defaultHeight = 40.dp,
    viewportWidth = 900f,
    viewportHeight = 600f,
).apply {
    addPath(
        addPathNodes(
            "M0 0h900v600H0Z",
        ),
        fill = SolidColor(Color(0xFFD7141A)),
    )
    addPath(
        addPathNodes(
            "M0 0h900v300H0Z",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addPath(
        addPathNodes(
            "M 450,300 0,0 V 600 z",
        ),
        fill = SolidColor(Color(0xFF11457E)),
    )
    addOutline(path = "M7.5 7.5h885v585H7.5Z", strokeWidth = 15f)
}.build()

// https://commons.wikimedia.org/wiki/File:Flag_of_Hungary.svg
private val FlagHungary: ImageVector = ImageVector.Builder(
    name = "FlagHungary",
    defaultWidth = 80.dp,
    defaultHeight = 40.dp,
    viewportWidth = 1200f,
    viewportHeight = 600f,
).apply {
    addPath(
        addPathNodes(
            "M0 0h1200v600H0",
        ),
        fill = SolidColor(Color(0xFF477050)),
    )
    addPath(
        addPathNodes(
            "M0 0h1200v400H0",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addPath(
        addPathNodes(
            "M0 0h1200v200H0",
        ),
        fill = SolidColor(Color(0xFFCE2939)),
    )
    addOutline(path = "M7.5 7.5h1185v585H7.5Z", strokeWidth = 15f)
}.build()

// https://commons.wikimedia.org/wiki/File:Flag_of_Poland.svg
private val FlagPoland: ImageVector = ImageVector.Builder(
    name = "FlagPoland",
    defaultWidth = 64.dp,
    defaultHeight = 40.dp,
    viewportWidth = 8f,
    viewportHeight = 5f,
).apply {
    addPath(
        addPathNodes(
            "M0 0h8v5H0Z",
        ),
        fill = SolidColor(Color(0xFFDC143C)),
    )
    addPath(
        addPathNodes(
            "M0 0h8v2.5H0Z",
        ),
        fill = SolidColor(Color(0xFFFFFFFF)),
    )
    addOutline(path = "M0.0625 0.0625h7.875v4.875H0.0625Z", strokeWidth = 0.125f)
}.build()

// The flags above, by ISO 3166-1 alpha-2 code. Top-level properties initialize in file order, so
// this table has to come after the vectors it names.
private val FLAGS: Map<String, ImageVector> = mapOf(
    "SK" to FlagSlovakia,
    "CZ" to FlagCzechia,
    "HU" to FlagHungary,
    "PL" to FlagPoland,
)

/**
 * A hairline around the whole flag: without it a flag's white band vanishes on a white card. The
 * stroke is one dp in this flag's own units, and the path runs half a stroke inside the viewport so
 * the line stays visible.
 */
private fun ImageVector.Builder.addOutline(path: String, strokeWidth: Float) = addPath(
    addPathNodes(path),
    stroke = SolidColor(Color(0x40000000)),
    strokeLineWidth = strokeWidth,
)

// ── Previews ──

/** Every flag we have drawn, at the height the country cards give them. */
@Preview(widthDp = 400, heightDp = 200)
@Composable
private fun CountryFlagsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("40DP TALL, EACH AS WIDE AS ITS OWN PROPORTIONS")
            PreviewRow {
                countriesWithFlags.sorted().forEach { code ->
                    val flag = countryFlag(code) ?: return@forEach
                    Image(
                        painter = rememberVectorPainter(flag),
                        contentDescription = code,
                        modifier = Modifier
                            .height(40.dp)
                            .width(40.dp * (flag.defaultWidth / flag.defaultHeight)),
                    )
                }
            }
        }
    }
}
