package com.spiraclestudios.autoskola.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.elapsedClock
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * The takes of one test, opened by a long press on its tile. A row opens its take: the one under way
 * is continued, and a finished one opens to be reviewed with its answers locked. Which is which is
 * the ViewModel's decision; the sheet only says so on the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TestHistorySheet(history: TestHistory, onOpen: (attemptId: String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Naj.colors.sheet,
    ) {
        TestHistoryContent(history, onOpen)
    }
}

/** What the sheet holds: the title, then a row per take or a line saying there is none yet. */
@Composable
private fun TestHistoryContent(history: TestHistory, onOpen: (attemptId: String) -> Unit) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        Text(
            text = strings.testHistoryTitle(history.testNumber),
            style = Naj.type.sectionHeading,
            color = colors.ink,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (history.entries.isEmpty()) {
            Text(strings.testHistoryEmpty, style = Naj.type.body, color = colors.muted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history.entries, key = { it.attemptId }) { entry ->
                    HistoryRow(entry, onClick = { onOpen(entry.attemptId) })
                }
            }
        }
    }
}

/** One take: which it was, how it went and how long it ran, and what opening it does. */
@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val colors = Naj.colors
    val result = entry.result
    val dot = when {
        result == null -> colors.resumeBottom
        result.passed -> colors.passedDot
        else -> colors.wrongDot
    }
    val time = strings.testHistoryTime(elapsedClock(entry.elapsedSeconds))
    // Status, then points or answers, then time, each on a line of its own, so none of them wraps
    // in the longer translations.
    val lines = if (result == null) {
        listOf(strings.inProgress, strings.resumeSummary(entry.answered, entry.total), time)
    } else {
        listOf(strings.testHistoryVerdict(result.passed), "${result.score} / ${result.maxScore}", time)
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            // An outline, so each take stands apart from the sheet behind it.
            .border(1.dp, colors.emptyOutline, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).background(dot, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(strings.testHistoryTake(entry.take), style = Naj.type.rowTitle, color = colors.ink)
            lines.forEach { Text(it, style = Naj.type.rowSubtitle, color = colors.muted) }
        }
        if (entry.resumable) {
            OpenLabel(strings.continueTest, Icons.Filled.PlayArrow, colors.ink, colors.roadYellow)
        } else {
            OpenLabel(strings.reviewTest, Icons.AutoMirrored.Filled.KeyboardArrowRight, Color.Transparent, colors.ink)
        }
    }
}

/** What opening a row does, set as a pill: ink for continuing, quiet for reviewing. */
@Composable
private fun OpenLabel(
    text: String,
    icon: ImageVector,
    background: Color,
    content: Color,
) {
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = Naj.type.button, color = content)
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
    }
}

// ── Previews ──

private fun previewHistory(vararg entries: HistoryEntry) = TestHistory(testNumber = 3, entries = entries.toList())

private fun previewEntry(take: Int, result: TestResult?, answered: Int = 40) = HistoryEntry(
    attemptId = "preview-$take",
    take = take,
    result = result,
    answered = answered,
    total = 40,
    elapsedSeconds = 754L + take * 61,
)

/** A test taken three times: once under way now, once passed and once failed before that. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun TestHistoryPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("UNDER WAY, PASSED, FAILED")
            TestHistoryContent(
                previewHistory(
                    previewEntry(3, result = null, answered = 12),
                    previewEntry(2, TestResult(37, 40)),
                    previewEntry(1, TestResult(31, 40)),
                ),
                onOpen = {},
            )
        }
    }
}

/** Only finished takes, so every row reviews; in Slovak, the longest of the labels. */
@Preview(widthDp = 400, heightDp = 500)
@Composable
private fun TestHistoryFinishedPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        PreviewBoard {
            PreviewLabel("FINISHED ONLY")
            TestHistoryContent(
                previewHistory(previewEntry(2, TestResult(40, 40)), previewEntry(1, TestResult(33, 40))),
                onOpen = {},
            )
        }
    }
}

/** A test nobody has taken yet. */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun TestHistoryEmptyPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("NO TAKES")
            TestHistoryContent(previewHistory(), onOpen = {})
        }
    }
}
