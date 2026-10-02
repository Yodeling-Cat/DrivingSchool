package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings

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
    ConfirmDialog(
        title = title,
        message = summary,
        confirmLabel = strings.continueTest,
        onConfirm = onContinue,
        dismissLabel = strings.startOver,
        onDismiss = onStartOver,
        onDismissRequest = onDismiss,
    )
}

/**
 * Asks whether a test that has already been passed should be taken again. It is the question
 * [ResumeDialog] asks with its answers the other way round: taking the test again is what the
 * highlighted button does here, and keeping the score it already has is what the quiet one does.
 */
@Composable
fun RedoTestDialog(
    title: String,
    message: String,
    onRedo: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmLabel = strings.startOver,
        onConfirm = onRedo,
        dismissLabel = strings.keepScore,
        onDismiss = onDismiss,
        onDismissRequest = onDismiss,
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
                    summary = strings.resumeSummary(12, 40),
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
                    summary = strings.resumeSummary(21, 88),
                    onContinue = {},
                    onStartOver = {},
                    onDismiss = {},
                )
            }
        }
    }
}

/** The other question, about a test that is already passed: redo it, or keep the score it has. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun RedoTestDialogPreview() {
    AppPreview {
        PreviewBoard {
            Column {
                PreviewLabel("a test that is already passed")
                RedoTestDialog(
                    title = strings.passedPromptTitle,
                    message = strings.passedPromptMessage(38, 40),
                    onRedo = {},
                    onDismiss = {},
                )
            }
        }
    }
}
