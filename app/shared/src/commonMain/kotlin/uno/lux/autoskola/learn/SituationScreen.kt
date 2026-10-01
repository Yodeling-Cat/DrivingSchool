package uno.lux.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.ui.AnswerCard
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.EnlargeableImage
import uno.lux.autoskola.ui.LocalNavBarInset
import uno.lux.autoskola.ui.OutlinePill
import uno.lux.autoskola.ui.PreviewBoard
import uno.lux.autoskola.ui.PreviewLabel
import uno.lux.autoskola.ui.RoundButton
import uno.lux.autoskola.ui.previewSituations
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.paperBackground

/** How tall a question's tab is. Round, like every other pill in the app. */
private val SituationTabHeight = 36.dp

/** Connects [SituationScreen] to its [SituationViewModel] and to navigation. */
@Composable
fun SituationRoute(
    number: Int,
    onClose: () -> Unit,
    onOpenAttempt: (attemptId: String) -> Unit,
    viewModel: SituationViewModel = koinViewModel { parametersOf(number) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openAttemptId = (state as? SituationUiState.Ready)?.openAttemptId
    // Drilling happens in the practice screen, over the attempt the ViewModel started.
    LaunchedEffect(openAttemptId) {
        openAttemptId?.let {
            onOpenAttempt(it)
            viewModel.onEvent(SituationUiEvent.AttemptOpened)
        }
    }
    // A situation the material doesn't have any more closes the screen rather than showing another.
    LaunchedEffect(state) { if (state is SituationUiState.Closed) onClose() }
    SituationScreen(state, viewModel::onEvent, onClose)
}

/**
 * One situation: the drawing of it, and the questions the exam asks about it, one to a tab.
 *
 * The driver answers them while they read. A situation's questions are practice rather than a test,
 * so nothing is scored and nothing is locked: an answer can be changed at any time, and it is kept
 * with the bank version, so coming back to a situation shows what was answered. Asking to check an
 * answer is what shows whether it was the right one - the answer is not given away before that.
 */
@Composable
fun SituationScreen(state: SituationUiState, onEvent: (SituationUiEvent) -> Unit, onClose: () -> Unit) {
    // Measured here, outside the scroll, so an enlarged drawing is never drawn bigger than the screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        // Where the driver is looking, and which answers they asked to check. Both are about this
        // visit: what they answered is the ViewModel's, and is not forgotten.
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var checked by remember { mutableStateOf(emptySet<Int>()) }
        Box(Modifier.fillMaxSize().paperBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = ContentWidth.Reading).fillMaxWidth()) {
                    SituationHeader(state, onClose)
                    when (state) {
                        SituationUiState.Loading -> SituationLoading(Modifier.weight(1f))

                        // The screen is on its way out; leave the space it took.
                        SituationUiState.Closed -> Spacer(Modifier.weight(1f))

                        is SituationUiState.Ready -> {
                            OutlinePill(
                                text = strings.situationsDrillOne,
                                onClick = { onEvent(SituationUiEvent.Drill) },
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                            SituationReading(
                                state = state,
                                page = page,
                                tab = tab,
                                checked = checked,
                                onSelectTab = { tab = it },
                                onCheck = { questionId -> checked = checked + questionId },
                                onEvent = onEvent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Back, and what the situation is called. */
@Composable
private fun SituationHeader(state: SituationUiState, onClose: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onClose)
        Column(Modifier.weight(1f)) {
            Text(
                text = (state as? SituationUiState.Ready)?.let { strings.situationName(it.situation.number) }
                    ?: strings.learnIntersections,
                style = Naj.type.screenTitle,
                color = colors.ink,
            )
            (state as? SituationUiState.Ready)?.let { ready ->
                Text(
                    strings.situationQuestions(ready.situation.questions.size),
                    style = Naj.type.secondary,
                    color = colors.muted,
                )
            }
        }
    }
}

/** The drawing, the tab of each question, and the question being read. */
@Composable
private fun SituationReading(
    state: SituationUiState.Ready,
    page: DpSize,
    tab: Int,
    checked: Set<Int>,
    onSelectTab: (Int) -> Unit,
    onCheck: (questionId: Int) -> Unit,
    onEvent: (SituationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val questions = state.situation.questions
    val question = questions.getOrNull(tab) ?: questions.firstOrNull() ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EnlargeableImage(uri = state.situation.imageUri, page = page)
        if (questions.size > 1) {
            SituationTabs(count = questions.size, selected = tab, onSelect = onSelectTab)
        }
        SituationQuestionBlock(
            question = question,
            chosen = state.answers[question.questionId],
            checked = question.questionId in checked,
            onCheck = { onCheck(question.questionId) },
            onSelect = { chosen -> onEvent(SituationUiEvent.Answer(question.questionId, chosen)) },
        )
    }
}

/**
 * One tab per question: the questions about a situation are asked about the same drawing, and a
 * driver reads one at a time rather than all of them at once.
 */
@Composable
private fun SituationTabs(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(SituationTabHeight / 2)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val open = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SituationTabHeight)
                    .clip(shape)
                    .background(if (open) colors.ink else colors.card)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    strings.questionNumber(index + 1),
                    style = Naj.type.tiny,
                    color = if (open) Color.White else colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** One question of the situation: what it asks, the answers, and the way to check the answer. */
@Composable
private fun SituationQuestionBlock(
    question: SituationQuestion,
    chosen: Int?,
    checked: Boolean,
    onCheck: () -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(question.text, style = Naj.type.question, color = colors.ink)
        AnswerCard(
            options = question.answers,
            selectedIndex = chosen,
            correct = question.correct,
            revealed = checked,
            onSelect = onSelect,
        )
        // Nothing to check before an answer is picked, and checking again says nothing new.
        if (chosen != null && !checked) {
            OutlinePill(text = strings.checkAnswer, onClick = onCheck)
        }
    }
}

/** Nothing to show until the exam and its bank have been read. */
@Composable
private fun SituationLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Naj.colors.primaryDeep)
    }
}

// ── Previews ──

/** A situation on a phone: its drawing and its first question, unanswered. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0], answers = emptyMap()),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The same question answered, with nothing given away yet. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenAnsweredPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0], answers = mapOf(1813 to 2)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** A situation with one question has no tabs to show. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenOneQuestionPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[1], answers = mapOf(1815 to 1)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The same questions in Slovak, which the bank's own text is written in. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0], answers = mapOf(1813 to 2)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** A tablet, where the reading column is capped and centred. */
@Preview(widthDp = 900, heightDp = 800)
@Composable
private fun SituationScreenWidePreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0], answers = mapOf(1813 to 2)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The situation still being read. */
@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun SituationScreenLoadingPreview() {
    AppPreview { SituationScreen(state = SituationUiState.Loading, onEvent = {}, onClose = {}) }
}

/** One question on its own, answered and checked, which is the one thing the screen can't show. */
@Preview(widthDp = 400, heightDp = 420)
@Composable
private fun SituationQuestionCheckedPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("answered and checked")
            SituationQuestionBlock(
                question = previewSituations[0].questions[0],
                chosen = 1,
                checked = true,
                onCheck = {},
                onSelect = {},
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** The tabs on their own: the question being read, and the ones waiting. */
@Preview(widthDp = 400, heightDp = 120)
@Composable
private fun SituationTabsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("one tab per question")
            SituationTabs(count = 4, selected = 1, onSelect = {}, modifier = Modifier.padding(horizontal = 12.dp))
            SituationTabs(count = 2, selected = 0, onSelect = {}, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}
