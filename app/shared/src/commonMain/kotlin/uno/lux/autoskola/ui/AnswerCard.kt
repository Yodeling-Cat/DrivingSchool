package uno.lux.autoskola.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.AnswerState
import uno.lux.autoskola.practice.answerState
import uno.lux.autoskola.ui.theme.Naj

/**
 * The answers to one question, laid out to be picked.
 *
 * [onSelect] is null when the answer is locked, which is what a test that checks as it goes, or one
 * that has been finished, does. [revealed] says whether the marks are shown: without it the card
 * only shows what the driver picked, which is what reading a situation needs - the question is
 * there to be answered, not given away.
 *
 * The card is shared: the practice screen draws a question of a test with it, the intersections
 * material draws the questions about a situation, and both must mark an answer the same way.
 */
@Composable
internal fun AnswerCard(
    options: List<String>,
    selectedIndex: Int?,
    correct: Int,
    revealed: Boolean,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    NajCard(modifier, shadow = 0.3f) {
        Column(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                }
                AnswerRow(
                    letter = ('A' + index).toString(),
                    text = option,
                    state = answerState(index, selectedIndex, correct, revealed),
                    isUserAnswer = selectedIndex == index,
                    onClick = onSelect?.let { { it(index) } },
                )
            }
        }
    }
}

@Composable
private fun AnswerRow(
    letter: String,
    text: String,
    state: AnswerState,
    isUserAnswer: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = Naj.colors
    val (background, accent) = when (state) {
        AnswerState.Neutral -> null to colors.muted

        AnswerState.Selected -> Brush.horizontalGradient(listOf(colors.mintGlow, colors.selectedTint)) to
            colors.primaryDeep

        AnswerState.Correct -> Brush.horizontalGradient(listOf(colors.correctTint, colors.correctTint)) to
            colors.passedOn

        AnswerState.Wrong -> Brush.horizontalGradient(listOf(colors.wrongBottom, colors.wrongBottom)) to
            colors.wrongOn
    }
    val label = when {
        state == AnswerState.Correct -> strings.correctAnswer
        isUserAnswer -> strings.yourAnswer
        else -> null
    }
    val marked = state != AnswerState.Neutral
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (background != null) Modifier.background(background) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (marked) accent else colors.neutralTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                style = Naj.type.answerLetter,
                color = if (marked) Color.White else colors.neutralOn,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 7.dp)) {
            if (label != null) {
                Text(label, style = Naj.type.eyebrowTight, color = accent)
                Spacer(Modifier.height(2.dp))
            }
            Text(text, style = Naj.type.option, color = colors.ink)
        }
    }
}

// ── Previews ──

/** The states an answer can be in, on a question the driver has and hasn't answered. */
@Preview(widthDp = 400, heightDp = 720)
@Composable
private fun AnswerCardPreview() {
    AppPreview {
        PreviewBoard {
            val options = listOf("modré vozidlo.", "červené vozidlo odbočujúce vľavo.", "vaše vozidlo.")
            PreviewLabel("nothing answered yet")
            AnswerCard(options, selectedIndex = null, correct = 2, revealed = false, onSelect = {})
            PreviewLabel("answered, not checked")
            AnswerCard(options, selectedIndex = 1, correct = 2, revealed = false, onSelect = {})
            PreviewLabel("answered, checked, wrong")
            AnswerCard(options, selectedIndex = 1, correct = 2, revealed = true, onSelect = {})
            PreviewLabel("checked and locked, right")
            AnswerCard(options, selectedIndex = 2, correct = 2, revealed = true, onSelect = null)
        }
    }
}
