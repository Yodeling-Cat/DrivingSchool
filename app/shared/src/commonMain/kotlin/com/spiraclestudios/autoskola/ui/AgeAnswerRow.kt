package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * The question the three consent switches depend on: which side of 16 the user is on.
 *
 * It exists because the answers are not everybody's to give. In Slovakia the age at which a person can
 * agree to crash reports, usage statistics and ads alone is 16, and the app's questions serve
 * candidates from 15 — so below 16 the screen has to ask, and only a parent or guardian can agree on
 * the user's behalf (`plans/age-gate.md`).
 *
 * Two pills rather than a switch, because "not answered yet" is a state the screen has to be able to
 * draw: a switch that is off would say "under 16" for every adult who has not reached the row, and
 * that is not something the app should decide for them. The consent screen asks it and Settings lets
 * the user correct it, which is why it lives here rather than in either screen.
 *
 * Nothing here decides anything: [ConsentAge][com.spiraclestudios.autoskola.data.preferences.ConsentAge]
 * says who may answer, and this draws the answer.
 *
 * @param answer `null` until the user says, which is what the screen draws first.
 * @param missing a button was pressed without an answer, so the question is marked and says so.
 */
@Composable
fun AgeAnswerRow(
    answer: Boolean?,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            strings.consentAgeQuestion,
            style = Naj.type.body,
            color = if (missing) colors.wrongOn else colors.ink,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The chosen one is the filled pill, so the answer reads at a glance; the other stays
            // pressable, because changing an answer has to be as easy as giving one.
            AgePill(text = strings.consentAgeOver16, chosen = answer == true) { onAnswer(true) }
            AgePill(text = strings.consentAgeUnder16, chosen = answer == false) { onAnswer(false) }
        }
        if (missing) {
            Text(
                strings.consentAgeRequired,
                style = Naj.type.rowSubtitle,
                color = colors.wrongOn,
            )
        }
    }
}

/** One of the two answers. Filled when chosen, outlined when not, and never disabled. */
@Composable
private fun AgePill(text: String, chosen: Boolean, onClick: () -> Unit) {
    if (chosen) {
        PrimaryPill(text = text, onClick = onClick, height = AnswerPillHeight)
    } else {
        OutlinePill(text = text, onClick = onClick, height = AnswerPillHeight)
    }
}

private val AnswerPillHeight = 40.dp

// ── Previews ──

@Preview(widthDp = 400)
@Composable
private fun AgeAnswerRowUnansweredPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("Unanswered")
            AgeAnswerRow(answer = null, onAnswer = {})
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun AgeAnswerRowAnsweredPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("16 or older")
            AgeAnswerRow(answer = true, onAnswer = {})
            PreviewLabel("Under 16")
            AgeAnswerRow(answer = false, onAnswer = {})
            PreviewLabel("Pressed without an answer")
            AgeAnswerRow(answer = null, onAnswer = {}, missing = true)
        }
    }
}

/** Hungarian and Czech are the longer of the four; the pills sit side by side either way. */
@Preview(widthDp = 360)
@Composable
private fun AgeAnswerRowCzechPreview() {
    AppPreview(language = AppLanguage.Czech) {
        AgeAnswerRow(answer = null, onAnswer = {})
    }
}
