package uno.lux.autoskola.learn

import androidx.compose.foundation.background
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
import uno.lux.autoskola.practice.AnswerState
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.practice.answerState
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.EnlargeableImage
import uno.lux.autoskola.ui.LocalNavBarInset
import uno.lux.autoskola.ui.NajCard
import uno.lux.autoskola.ui.OutlinePill
import uno.lux.autoskola.ui.PreviewBoard
import uno.lux.autoskola.ui.PreviewLabel
import uno.lux.autoskola.ui.RoundButton
import uno.lux.autoskola.ui.previewSituations
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.paperBackground

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
 * One situation: the drawing of it, and every question the exam asks about it with the answer the
 * regulation wants. Reading it is the point - the questions are there to be studied, not answered -
 * and the action above the drawing is what runs them through the practice screen.
 */
@Composable
fun SituationScreen(state: SituationUiState, onEvent: (SituationUiEvent) -> Unit, onClose: () -> Unit) {
    // Measured here, outside the scroll, so an enlarged drawing is never drawn bigger than the screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        Box(Modifier.fillMaxSize().paperBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = ContentWidth.Reading).fillMaxWidth()) {
                    SituationHeader(state, onClose)
                    (state as? SituationUiState.Ready)?.let {
                        OutlinePill(
                            text = strings.situationsDrillOne,
                            onClick = { onEvent(SituationUiEvent.Drill) },
                            icon = Icons.Filled.PlayArrow,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    when (state) {
                        SituationUiState.Loading -> SituationLoading(Modifier.weight(1f))

                        // The screen is on its way out; leave the space it took.
                        SituationUiState.Closed -> Spacer(Modifier.weight(1f))

                        is SituationUiState.Ready ->
                            SituationBody(state.situation, page, Modifier.weight(1f))
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

/** The drawing, then the questions it is asked with. */
@Composable
private fun SituationBody(situation: SituationItem, page: DpSize, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        EnlargeableImage(uri = situation.imageUri, page = page)
        situation.questions.forEach { question ->
            SituationQuestionBlock(question)
        }
    }
}

/** One question about the situation, with its answers and the one that's right. */
@Composable
private fun SituationQuestionBlock(question: SituationQuestion, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(question.text, style = Naj.type.question, color = colors.ink)
        SituationAnswers(question.answers, question.correct)
    }
}

/** The answers to one question, with the right one marked: nothing here is a choice to make. */
@Composable
private fun SituationAnswers(answers: List<String>, correct: Int, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    NajCard(modifier.fillMaxWidth(), shadow = 0.3f) {
        Column(Modifier.fillMaxWidth()) {
            answers.forEachIndexed { index, answer ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                }
                SituationAnswerRow(
                    letter = ('A' + index).toString(),
                    text = answer,
                    right = answerState(index, selected = null, correct = correct, revealed = true) ==
                        AnswerState.Correct,
                )
            }
        }
    }
}

/** One answer of a situation, with the letter the practice screen gives it. */
@Composable
private fun SituationAnswerRow(letter: String, text: String, right: Boolean) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (right) Modifier.background(colors.correctTint) else Modifier)
            .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (right) colors.passedOn else colors.neutralTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                style = Naj.type.answerLetter,
                color = if (right) Color.White else colors.neutralOn,
            )
        }
        Column(Modifier.weight(1f).padding(top = 7.dp)) {
            if (right) {
                Text(strings.correctAnswer, style = Naj.type.eyebrowTight, color = colors.passedOn)
                Spacer(Modifier.height(2.dp))
            }
            Text(text, style = Naj.type.option, color = colors.ink)
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

/** One situation on a phone: its drawing and the questions asked about it. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[0]),
            onEvent = {},
            onClose = {},
        )
    }
}

/** A situation whose questions a taller screen fits beside the drawing. */
@Preview(widthDp = 900, heightDp = 700)
@Composable
private fun SituationScreenWidePreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations[1]),
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
            state = SituationUiState.Ready(previewSituations[0]),
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

/** One question's answers on their own, with the right one marked. */
@Preview(widthDp = 400, heightDp = 260)
@Composable
private fun SituationAnswersPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("one question's answers")
            SituationAnswers(
                answers = previewSituations[0].questions[0].answers,
                correct = previewSituations[0].questions[0].correct,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
