package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * The question the consent switches depend on: which age band the user is in.
 *
 * It exists because the answers are not everybody's to give. In Slovakia the age at which a person can
 * agree to crash reports and usage statistics alone is 16, and the app's questions serve candidates
 * from 15 — so below 16 the screen has to ask, and only a parent or guardian can agree on the user's
 * behalf. Personalized ads have their own line at 18, which is why there are three bands rather than
 * two (`plans/age-gate.md`).
 *
 * Pills rather than a switch, because "not answered yet" is a state the screen has to be able to draw:
 * a switch that is off would say "under 16" for every adult who has not reached the row, and that is
 * not something the app should decide for them. The consent screen asks it and Settings lets the user
 * correct it, which is why it lives here rather than in either screen.
 *
 * Nothing here decides anything: [ConsentAge][com.spiraclestudios.autoskola.data.preferences.ConsentAge]
 * says who may answer, and this draws the answer.
 *
 * @param answer `null` until the user says, which is what the screen draws first — and while it is
 *   `null` the consent card is closed and its buttons are dead, so the question is the whole screen.
 * @param options the bands offered, lowest first. All three on the consent screen; Settings passes only
 *   the user's own band and the ones above it, because nobody gets younger.
 */
@Composable
fun AgeAnswerRow(
    answer: AgeBand?,
    onAnswer: (AgeBand) -> Unit,
    modifier: Modifier = Modifier,
    options: List<AgeBand> = AgeBand.entries,
) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            strings.consentAgeQuestion,
            style = Naj.type.body,
            color = colors.ink,
            fontWeight = FontWeight.Medium,
        )
        // A flow rather than a row: three pills side by side do not always fit a phone in the longest
        // of the four languages, and one that wraps is better than one that is cut off.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The chosen one is the filled pill, so the answer reads at a glance; the others stay
            // pressable, because changing an answer has to be as easy as giving one.
            options.forEach { band ->
                AgePill(text = band.label(), chosen = answer == band) { onAnswer(band) }
            }
        }
    }
}

/** The words on the band's pill. */
@Composable
private fun AgeBand.label(): String = when (this) {
    AgeBand.Under16 -> strings.consentAgeUnder16
    AgeBand.From16To17 -> strings.consentAgeTeen
    AgeBand.Adult -> strings.consentAgeAdult
}

/** One of the answers. Filled when chosen, outlined when not, and never disabled. */
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
            PreviewLabel("Under 16")
            AgeAnswerRow(answer = AgeBand.Under16, onAnswer = {})
            PreviewLabel("16 to 17")
            AgeAnswerRow(answer = AgeBand.From16To17, onAnswer = {})
            PreviewLabel("18 or older")
            AgeAnswerRow(answer = AgeBand.Adult, onAnswer = {})
        }
    }
}

/** What a 16 or 17-year-old is offered: their own band and the one above it, and no way back down. */
@Preview(widthDp = 400)
@Composable
private fun AgeAnswerRowTeenOptionsPreview() {
    AppPreview {
        AgeAnswerRow(
            answer = AgeBand.From16To17,
            onAnswer = {},
            options = listOf(AgeBand.From16To17, AgeBand.Adult),
        )
    }
}

/** Slovak has the longest label of the four; on a narrow phone the pills wrap rather than overflow. */
@Preview(widthDp = 320)
@Composable
private fun AgeAnswerRowNarrowSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        AgeAnswerRow(answer = null, onAnswer = {})
    }
}

/** Hungarian and Czech are the longer of the four; the pills sit side by side or wrap either way. */
@Preview(widthDp = 360)
@Composable
private fun AgeAnswerRowCzechPreview() {
    AppPreview(language = AppLanguage.Czech) {
        AgeAnswerRow(answer = null, onAnswer = {})
    }
}
