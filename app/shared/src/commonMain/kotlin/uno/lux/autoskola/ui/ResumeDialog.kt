package uno.lux.autoskola.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.theme.Naj

/**
 * Asks what to do with work that is already under way: carry on with it where it was left, or start
 * it over. [title] and [summary] say what the work is, since a test and a situations drill both ask
 * the same question about themselves, and the buttons say the same thing for both.
 */
@Composable
fun ResumeDialog(
    title: String,
    summary: String,
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Naj.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(LicenceRadius),
        containerColor = colors.sheet,
        titleContentColor = colors.ink,
        textContentColor = colors.muted,
        title = { Text(text = title, style = Naj.type.sectionHeading) },
        text = { Text(text = summary, style = Naj.type.body) },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.primaryDeep),
            ) {
                Text(text = strings.continueTest, style = Naj.type.primaryButton)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onStartOver,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.muted),
            ) {
                Text(text = strings.startOver, style = Naj.type.button)
            }
        },
    )
}

// ── Previews ──

/** The question as a user reads it: what is under way, and the two answers to it. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun ResumeDialogPreview() {
    AppPreview {
        PreviewBoard {
            Column {
                PreviewLabel("a test that is already started")
                ResumeDialog(
                    title = strings.resumePromptTitle,
                    summary = strings.resumeSummary(strings.testName(7), 12, 40),
                    onContinue = {},
                    onStartOver = {},
                    onDismiss = {},
                )
            }
        }
    }
}

/** The same question about a drill of the situations. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun ResumeDialogDrillPreview() {
    AppPreview {
        PreviewBoard {
            Column {
                PreviewLabel("a drill of the situations")
                ResumeDialog(
                    title = strings.drillPromptTitle,
                    summary = strings.resumeSummary(strings.situationsDrillName, 21, 88),
                    onContinue = {},
                    onStartOver = {},
                    onDismiss = {},
                )
            }
        }
    }
}
