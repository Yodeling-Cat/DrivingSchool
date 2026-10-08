package com.spiraclestudios.autoskola.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel

/**
 * Splits a label into text runs, marking the first number so a title can set it in the accent
 * color: `"Test 3"` becomes `("Test ", false), ("3", true)`.
 *
 * Languages order the number differently ("3. teszt"), so the split is found rather than assumed.
 */
fun accentNumber(label: String): List<Pair<String, Boolean>> {
    if (label.isEmpty()) return emptyList()
    val start = label.indexOfFirst { it.isDigit() }
    if (start < 0) return listOf(label to false)
    var end = start
    while (end < label.length && label[end].isDigit()) end++
    return listOf(
        label.substring(0, start) to false,
        label.substring(start, end) to true,
        label.substring(end) to false,
    ).filter { it.first.isNotEmpty() }
}

/**
 * A title whose *one* word is set in coral italic, the way the design system asks for titles: the
 * word is carried separately in the strings table rather than guessed from the sentence.
 *
 * The runs are separated by a single space, so a translation stores the words without having to
 * remember a trailing space.
 */
fun accentWord(before: String, word: String, after: String = ""): List<Pair<String, Boolean>> {
    val runs = listOf(before to false, word to true, after to false)
        .filter { it.first.isNotBlank() }
        .map { it.first.trim() to it.second }
    // A space joins the runs, so a translation stores its words without a trailing space.
    return runs.mapIndexed { index, run -> if (index == 0) run else " " + run.first to run.second }
}

/** Renders runs from [accentNumber] or [accentWord]: the marked runs are coral and Fraunces italic. */
@Composable
fun AccentedText(
    runs: List<Pair<String, Boolean>>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Naj.colors.ink,
    textAlign: TextAlign? = null,
) {
    val coral = Naj.colors.coral
    val accent = Naj.type.accent
    // The theme's own face rather than the fonts looked up again for every title, and the text built
    // once for the runs it is given.
    val text = remember(runs, color, coral, accent) { accentString(runs, color, coral, accent) }
    Text(
        text = text,
        style = style.copy(color = color),
        modifier = modifier,
        textAlign = textAlign,
    )
}

private fun accentString(
    runs: List<Pair<String, Boolean>>,
    color: Color,
    coral: Color,
    italic: FontFamily,
): AnnotatedString = buildAnnotatedString {
    runs.forEach { (text, accent) ->
        if (accent) {
            withStyle(SpanStyle(color = coral, fontFamily = italic)) { append(text) }
        } else {
            append(text)
        }
    }
}

/**
 * Splits the wordmark around its last word so it can take the coral italic the design gives it:
 * "Naj Autoškola" becomes ("Naj ", "Autoškola"). A one-word name keeps no accent at all.
 */
fun brandParts(appName: String): List<Pair<String, Boolean>> {
    val lastSpace = appName.lastIndexOf(' ')
    if (lastSpace <= 0 || lastSpace == appName.length - 1) return listOf(appName to false)
    return listOf(
        appName.substring(0, lastSpace + 1) to false,
        appName.substring(lastSpace + 1) to true,
    )
}

// ── Previews ──

/** The three ways a title carries its accent: a word, a number, and the wordmark. */
@Preview(widthDp = 400, heightDp = 280)
@Composable
private fun AccentedTextPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("ONE WORD IN CORAL ITALIC")
            AccentedText(accentWord("Which license", "category?"), style = Naj.type.stepTitle)
            PreviewLabel("THE FIRST NUMBER IN CORAL ITALIC")
            AccentedText(accentNumber("Test 7"), style = Naj.type.barTitle)
            PreviewLabel("THE WORDMARK")
            AccentedText(brandParts("Naj Autoškola"), style = Naj.type.brand)
        }
    }
}
