package com.spiraclestudios.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.IconArrowBack
import com.spiraclestudios.autoskola.practice.IconMoreVert
import com.spiraclestudios.autoskola.ui.AdBanner
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.CardDivider
import com.spiraclestudios.autoskola.ui.ConfirmDialog
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.EnlargeableImage
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.OutlinePill
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.ResumeDialog
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.previewSituations
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import org.koin.compose.viewmodel.koinViewModel

/** How big a situation's drawing is drawn in the list: wide, and small enough to scan the index. */
private val SituationThumbWidth = 190.dp
private val SituationThumbHeight = 88.dp

/** Connects [IntersectionsScreen] to its [IntersectionsViewModel] and to navigation. */
@Composable
fun IntersectionsRoute(
    onBack: () -> Unit,
    onOpenAttempt: (attemptId: String) -> Unit,
    viewModel: IntersectionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openAttemptId = (state as? IntersectionsUiState.Ready)?.openAttemptId
    // Drilling happens in the practice screen, over the attempt the ViewModel started.
    LaunchedEffect(openAttemptId) {
        openAttemptId?.let {
            onOpenAttempt(it)
            viewModel.onEvent(IntersectionsUiEvent.AttemptOpened)
        }
    }
    IntersectionsScreen(state, viewModel::onEvent, onBack)
}

/**
 * The situations the exam asks about at intersections: the drawing of each and how many questions
 * come with it. The action above the list drills them all.
 */
@Composable
fun IntersectionsScreen(state: IntersectionsUiState, onEvent: (IntersectionsUiEvent) -> Unit, onBack: () -> Unit) {
    // Measured here, outside the list, so an enlarged drawing is never drawn bigger than the screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        Box(Modifier.fillMaxSize().paperBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = ContentWidth.Reading)
                        .fillMaxWidth()
                        .padding(bottom = LocalNavBarInset.current),
                ) {
                    SituationsHeader(
                        state = state,
                        onBack = onBack,
                        onResetAnswers = { onEvent(IntersectionsUiEvent.ResetAnswers) },
                    )
                    if (state is IntersectionsUiState.Ready && state.situations.isNotEmpty()) {
                        OutlinePill(
                            text = strings.situationsDrillAll,
                            onClick = { onEvent(IntersectionsUiEvent.Drill) },
                            icon = Icons.Filled.PlayArrow,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    when (state) {
                        IntersectionsUiState.Loading -> SituationsLoading(Modifier.weight(1f))

                        is IntersectionsUiState.Ready ->
                            if (state.situations.isEmpty()) {
                                SituationsEmpty(Modifier.weight(1f))
                            } else {
                                SituationList(
                                    situations = state.situations,
                                    page = page,
                                    modifier = Modifier.weight(1f),
                                )
                                // Pinned under the list rather than at the end of it: the list
                                // takes the weight above, so this is on screen however far down the
                                // situations the driver has scrolled.
                                if (state.showAds) {
                                    AdBanner(
                                        slot = AdSlot.IntersectionsBanner,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                    )
                                }
                            }
                    }
                }
            }
        }
        // A drill that was walked away from is asked about, not silently begun again.
        (state as? IntersectionsUiState.Ready)?.drillPrompt?.let { prompt ->
            ResumeDialog(
                title = strings.drillPromptTitle,
                summary = strings.resumeSummary(prompt.answered, prompt.total),
                onContinue = { onEvent(IntersectionsUiEvent.ContinueDrill) },
                onStartOver = { onEvent(IntersectionsUiEvent.StartOverDrill) },
                onDismiss = { onEvent(IntersectionsUiEvent.DismissDrillPrompt) },
            )
        }
        // Answers are what the list is read against, so clearing them is asked about as well.
        if ((state as? IntersectionsUiState.Ready)?.resetPrompt == true) {
            ConfirmDialog(
                title = strings.situationsResetTitle,
                message = strings.situationsResetMessage,
                confirmLabel = strings.situationsResetConfirm,
                onConfirm = { onEvent(IntersectionsUiEvent.ConfirmReset) },
                destructive = true,
                dismissLabel = strings.situationsResetKeep,
                onDismiss = { onEvent(IntersectionsUiEvent.DismissReset) },
                onDismissRequest = { onEvent(IntersectionsUiEvent.DismissReset) },
            )
        }
    }
}

/** Back, what the material is, how much of it there is, and what can be done with the answers. */
@Composable
private fun SituationsHeader(state: IntersectionsUiState, onBack: () -> Unit, onResetAnswers: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(strings.learnIntersections, style = Naj.type.screenTitle, color = colors.ink)
            (state as? IntersectionsUiState.Ready)?.takeIf { it.situations.isNotEmpty() }?.let { ready ->
                Text(
                    text = strings.situationsCount(ready.situations.size) +
                        " · " + strings.situationQuestions(ready.questionCount),
                    style = Naj.type.secondary,
                    color = colors.muted,
                )
            }
        }
        // An exam whose bank asks about no situations has no answers to clear, so it has no menu.
        if (state is IntersectionsUiState.Ready && state.situations.isNotEmpty()) {
            SituationsOverflowMenu(onResetAnswers)
        }
    }
}

/** The header's overflow: one action, and the answers it clears are asked about before they go. */
@Composable
private fun SituationsOverflowMenu(onResetAnswers: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RoundButton(
            icon = IconMoreVert,
            contentDescription = strings.moreOptions,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Naj.colors.card,
        ) {
            DropdownMenuItem(
                text = {
                    Text(strings.situationsResetAnswers, style = Naj.type.cardTitle, color = Naj.colors.ink)
                },
                onClick = {
                    expanded = false
                    onResetAnswers()
                },
            )
        }
    }
}

/** The situations, in the order the exam lists them. */
@Composable
private fun SituationList(situations: List<SituationItem>, page: DpSize, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(situations, key = { it.number }) { situation ->
            SituationRow(situation, page)
            CardDivider()
        }
    }
}

/** One situation: its drawing, what it is called, and how much is asked about it. */
@Composable
private fun SituationRow(situation: SituationItem, page: DpSize) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(SituationThumbWidth)) {
            EnlargeableImage(
                uri = situation.imageUri,
                page = page,
                maxHeight = SituationThumbHeight,
                enlargeable = false,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(strings.situationName(situation.number), style = Naj.type.rowTitle, color = colors.ink)
            Spacer(Modifier.height(3.dp))
            Text(
                strings.situationQuestions(situation.questions.size),
                style = Naj.type.rowSubtitle,
                color = colors.muted,
            )
        }
        // Every question about it answered, and answered right: nothing left to practice here.
        if (situation.solved) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = strings.answeredCorrectly,
                tint = colors.passedOn,
                modifier = Modifier.size(20.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.primaryDeep,
        )
    }
}

/** An exam whose bank holds no situations, which the bundled banks all do. */
@Composable
private fun SituationsEmpty(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(strings.situationsNothingFound, style = Naj.type.body, color = Naj.colors.muted)
    }
}

/** Nothing to list until the exam and its bank have been read. */
@Composable
private fun SituationsLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Naj.colors.primaryDeep)
    }
}

// ── Previews ──

/** The situations of an exam, on a phone. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun IntersectionsScreenPreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(previewSituations),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The banner pinned under the list, which is where the free tier's ad sits on this screen. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun IntersectionsScreenBannerPreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(previewSituations, showAds = true),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The same list while the exam is being read. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun IntersectionsScreenLoadingPreview() {
    AppPreview {
        IntersectionsScreen(IntersectionsUiState.Loading, onEvent = {}, onBack = {})
    }
}

/** A tablet, where the reading column is capped and centerd. */
@Preview(widthDp = 900, heightDp = 700)
@Composable
private fun IntersectionsScreenWidePreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(previewSituations),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The names in Slovak, whose words are longer than the English ones. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun IntersectionsScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(previewSituations),
            onEvent = {},
            onBack = {},
        )
    }
}

/** One row of the list on its own, beside what it draws. */
@Preview(widthDp = 400, heightDp = 220)
@Composable
private fun SituationRowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("a situation with questions of its own")
            SituationRow(previewSituations[0], DpSize(400.dp, 220.dp))
            SituationRow(previewSituations[1], DpSize(400.dp, 220.dp))
        }
    }
}

/** A drill left half-read: the screen asks whether to carry on with it before starting another. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun IntersectionsScreenDrillPromptPreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(
                situations = previewSituations,
                drillPrompt = DrillPrompt(attemptId = "attempt-1", answered = 21, total = 88),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The prompt standing between the menu's action and the answers it clears. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun IntersectionsScreenResetPromptPreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(situations = previewSituations, resetPrompt = true),
            onEvent = {},
            onBack = {},
        )
    }
}

/** The list's header: what the material holds, and the menu over the answers beside it. */
@Preview(widthDp = 400, heightDp = 160)
@Composable
private fun SituationsHeaderPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("what the list holds")
            SituationsHeader(
                state = IntersectionsUiState.Ready(previewSituations),
                onBack = {},
                onResetAnswers = {},
            )
        }
    }
}

/** The same header before the exam and its bank have been read: nothing to count, nothing to clear. */
@Preview(widthDp = 400, heightDp = 160)
@Composable
private fun SituationsHeaderLoadingPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("while the exam is being read")
            SituationsHeader(state = IntersectionsUiState.Loading, onBack = {}, onResetAnswers = {})
        }
    }
}

/** The menu button on its own. The menu itself opens on tap, so this is what there is to tap. */
@Preview(widthDp = 140, heightDp = 120)
@Composable
private fun SituationsOverflowMenuPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("the header's overflow")
            SituationsOverflowMenu(onResetAnswers = {})
        }
    }
}
