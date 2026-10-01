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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    viewModel: SituationViewModel = koinViewModel { parametersOf(number) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A situation the material doesn't have any more closes the screen rather than showing another.
    LaunchedEffect(state) { if (state is SituationUiState.Closed) onClose() }
    SituationScreen(state, viewModel::onEvent, onClose)
}

/**
 * One situation: the drawing of it, and the questions the exam asks about it, one to a tab.
 *
 * The driver answers them while they read, and an answer is marked as soon as it is picked - right
 * or wrong, with the right one shown beside it. Nothing is scored and nothing is locked: this is
 * practice, so an answer can be changed as often as they like and the marks follow the answer they
 * have now. What was answered is kept with the bank version being read, so coming back to a
 * situation shows what they made of it.
 */
@Composable
fun SituationScreen(state: SituationUiState, onEvent: (SituationUiEvent) -> Unit, onClose: () -> Unit) {
    // Measured here, outside the scroll, so an enlarged drawing is never drawn bigger than the screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        // Which question the driver is looking at is about this visit; what they answered is not.
        var tab by rememberSaveable { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().paperBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = ContentWidth.Reading).fillMaxWidth()) {
                    SituationHeader(state, onClose)
                    when (state) {
                        SituationUiState.Loading -> SituationLoading(Modifier.weight(1f))

                        // The screen is on its way out; leave the space it took.
                        SituationUiState.Closed -> Spacer(Modifier.weight(1f))

                        is SituationUiState.Ready -> SituationReading(
                            state = state,
                            page = page,
                            tab = tab,
                            onSelectTab = { tab = it },
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f),
                        )
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
    onSelectTab: (Int) -> Unit,
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

/**
 * One question of the situation: what it asks, and the answers, marked as soon as one is picked.
 * There is no separate step to asking whether the answer was right: picking it is the answer.
 */
@Composable
private fun SituationQuestionBlock(
    question: SituationQuestion,
    chosen: Int?,
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
            // Answered is marked, unanswered is not: there is nothing to check separately.
            revealed = chosen != null,
            onSelect = onSelect,
        )
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

/** The same question answered wrongly: picking an answer is what marks it, at once. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenAnsweredWrongPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0], answers = mapOf(1813 to 1)),
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

/** The answers on their own: nothing picked, and the marks an answer gets the moment it is. */
@Preview(widthDp = 400, heightDp = 520)
@Composable
private fun SituationQuestionBlockPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("nothing answered")
            SituationQuestionBlock(
                question = previewSituations[0].questions[0],
                chosen = null,
                onSelect = {},
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            PreviewLabel("answered wrongly")
            SituationQuestionBlock(
                question = previewSituations[0].questions[0],
                chosen = 1,
                onSelect = {},
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            PreviewLabel("answered rightly")
            SituationQuestionBlock(
                question = previewSituations[0].questions[0],
                chosen = 2,
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
