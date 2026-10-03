package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * The dialog every question the app asks about work that is already there is put in: [title] and
 * [message] put the question, the highlighted button answers it and the quiet one backs out of it.
 * Tapping outside the dialog, or the system's back gesture, is the same back-out, which is why it
 * is a callback of its own rather than one of the two labeled answers.
 *
 * What the two answers are called is the caller's to say, because they differ by what is at stake:
 * carrying on or starting a test over, or giving up the answers to a situation. [destructive] is
 * the same decision in color, for the answer that gives something up.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    /** Whether that answer gives something up - the answers, a score - and is set in red for it. */
    destructive: Boolean = false,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = Naj.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(LicenseRadius),
        containerColor = colors.sheet,
        titleContentColor = colors.ink,
        textContentColor = colors.muted,
        title = { Text(text = title, style = Naj.type.sectionHeading) },
        text = { Text(text = message, style = Naj.type.body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (destructive) colors.wrongOn else colors.primaryDeep,
                ),
            ) {
                Text(text = confirmLabel, style = Naj.type.primaryButton)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.muted),
            ) {
                Text(text = dismissLabel, style = Naj.type.button)
            }
        },
    )
}

// ── Previews ──

/**
 * The two forms the app's question takes, as a user reads them: the ordinary one, whose highlighted
 * answer carries on, and the one whose answer gives something up and is set in red for it. The
 * second shows the longest wording the app asks with, since the layout has to hold whatever a
 * translation brings.
 */
@Preview(widthDp = 400, heightDp = 520)
@Composable
private fun ConfirmDialogPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("an answer that carries on")
            ConfirmDialog(
                title = strings.resumePromptTitle,
                message = strings.resumeSummary(12, 40),
                confirmLabel = strings.continueTest,
                onConfirm = {},
                dismissLabel = strings.startOver,
                onDismiss = {},
                onDismissRequest = {},
            )
            PreviewLabel("and one that gives the answers up")
            ConfirmDialog(
                title = strings.situationsResetTitle,
                message = strings.situationsResetMessage,
                confirmLabel = strings.situationsResetConfirm,
                onConfirm = {},
                destructive = true,
                dismissLabel = strings.situationsResetKeep,
                onDismiss = {},
                onDismissRequest = {},
            )
        }
    }
}
