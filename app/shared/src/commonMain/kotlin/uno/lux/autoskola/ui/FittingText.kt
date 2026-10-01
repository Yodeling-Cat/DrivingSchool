package uno.lux.autoskola.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.theme.Naj

/**
 * [text] shortened to fit [maxWidth], keeping both of its ends and putting an ellipsis where the
 * middle was: a label whose number sits at the end, or at the start, still shows it when there is
 * only room for part of what it says.
 *
 * [widthOf] measures a candidate, which is what lets the rule be tested without a font. The strings
 * it shortens are short and it shaves a character at a time, so the measuring costs nothing.
 */
internal fun middleEllipsized(
    text: String,
    maxWidth: Float,
    widthOf: (String) -> Float,
    ellipsis: String = "…",
): String {
    if (widthOf(text) <= maxWidth) return text
    // Start from the two ends of equal length and shave whichever is longer, so what is left stays
    // as even as the text allows, and the last character - the number, in a label like "Question 2" -
    // is the last thing to go.
    var head = text.length / 2
    var tail = text.length - head
    while (head > 0 || tail > 0) {
        val candidate = text.take(head) + ellipsis + text.takeLast(tail)
        if (widthOf(candidate) <= maxWidth) return candidate
        if (head >= tail) head-- else tail--
    }
    return ellipsis
}

/**
 * [text] in [style], shortened in the middle when the room it is given can't hold all of it. Draw it
 * with `Modifier.weight(1f, fill = false)` where it shares a row, so it takes no more room than the
 * text it ends up with and the row stays centred on what is drawn.
 */
@Composable
fun FittingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val limit = with(LocalDensity.current) { maxWidth.toPx() }
        val shortened = remember(text, limit, style, measurer) {
            middleEllipsized(
                text = text,
                maxWidth = limit,
                widthOf = { candidate -> measurer.measure(candidate, style).size.width.toFloat() },
            )
        }
        Text(shortened, style = style, color = color, maxLines = 1)
    }
}

// ── Previews ──

/** A label that has room, and the same label with too little: both ends stay, the middle goes. */
@Preview(widthDp = 320, heightDp = 160)
@Composable
private fun FittingTextPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("room enough")
            FittingText(
                text = strings.questionNumber(2),
                style = Naj.type.tiny,
                color = Naj.colors.ink,
                modifier = Modifier.widthIn(max = 240.dp),
            )
            PreviewLabel("a number at the end, kept")
            FittingText(
                text = strings.questionNumber(2),
                style = Naj.type.tiny,
                color = Naj.colors.ink,
                modifier = Modifier.widthIn(max = 40.dp),
            )
        }
    }
}
