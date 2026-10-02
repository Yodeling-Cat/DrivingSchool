package com.spiraclestudios.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.IconArrowBack
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.CardDivider
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
    onOpenSituation: (Int) -> Unit,
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
    IntersectionsScreen(state, viewModel::onEvent, onBack, onOpenSituation)
}

/**
 * The situations the exam asks about at intersections: the drawing of each and how many questions
 * come with it. The drawing opens bigger when it's tapped; the row opens the situation itself, and
 * the action above the list drills them all.
 */
@Composable
fun IntersectionsScreen(
    state: IntersectionsUiState,
    onEvent: (IntersectionsUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenSituation: (Int) -> Unit,
) {
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
                    SituationsHeader(state, onBack)
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
                                    // Reported before navigating, so which situation was opened is
                                    // known: the screen view that follows only says "situation".
                                    onOpenSituation = { number ->
                                        onEvent(IntersectionsUiEvent.OpenSituation(number))
                                        onOpenSituation(number)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                    }
                }
            }
        }
        // A drill that was walked away from is asked about, not silently begun again.
        (state as? IntersectionsUiState.Ready)?.drillPrompt?.let { prompt ->
            ResumeDialog(
                title = strings.drillPromptTitle,
                summary = strings.resumeSummary(strings.situationsDrillName, prompt.answered, prompt.total),
                onContinue = { onEvent(IntersectionsUiEvent.ContinueDrill) },
                onStartOver = { onEvent(IntersectionsUiEvent.StartOverDrill) },
                onDismiss = { onEvent(IntersectionsUiEvent.DismissDrillPrompt) },
            )
        }
    }
}

/** Back, what the material is, and how much of it there is. */
@Composable
private fun SituationsHeader(state: IntersectionsUiState, onBack: () -> Unit) {
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
    }
}

/** The situations, in the order the exam lists them. */
@Composable
private fun SituationList(
    situations: List<SituationItem>,
    page: DpSize,
    onOpenSituation: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(situations, key = { it.number }) { situation ->
            SituationRow(situation, page, onClick = { onOpenSituation(situation.number) })
            CardDivider()
        }
    }
}

/** One situation: its drawing, what it is called, and how much is asked about it. */
@Composable
private fun SituationRow(situation: SituationItem, page: DpSize, onClick: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(SituationThumbWidth)) {
            EnlargeableImage(uri = situation.imageUri, page = page, maxHeight = SituationThumbHeight)
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
        // Every question about it answered, and answered right: nothing left to practise here.
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
            onOpenSituation = {},
        )
    }
}

/** The same list while the exam is being read. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun IntersectionsScreenLoadingPreview() {
    AppPreview {
        IntersectionsScreen(IntersectionsUiState.Loading, onEvent = {}, onBack = {}, onOpenSituation = {})
    }
}

/** A tablet, where the reading column is capped and centred. */
@Preview(widthDp = 900, heightDp = 700)
@Composable
private fun IntersectionsScreenWidePreview() {
    AppPreview {
        IntersectionsScreen(
            state = IntersectionsUiState.Ready(previewSituations),
            onEvent = {},
            onBack = {},
            onOpenSituation = {},
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
            onOpenSituation = {},
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
            SituationRow(previewSituations[0], DpSize(400.dp, 220.dp), onClick = {})
            SituationRow(previewSituations[1], DpSize(400.dp, 220.dp), onClick = {})
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
            onOpenSituation = {},
        )
    }
}
