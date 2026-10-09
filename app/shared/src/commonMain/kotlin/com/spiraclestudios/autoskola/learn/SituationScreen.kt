package com.spiraclestudios.autoskola.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.practice.ChipState
import com.spiraclestudios.autoskola.practice.SwipePager
import com.spiraclestudios.autoskola.practice.arrowKeyPaging
import com.spiraclestudios.autoskola.practice.arrowKeyTarget
import com.spiraclestudios.autoskola.ui.AnswerCard
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.BackHeader
import com.spiraclestudios.autoskola.ui.ChipRadius
import com.spiraclestudios.autoskola.ui.ContentLoading
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.EnlargeableImage
import com.spiraclestudios.autoskola.ui.FittingText
import com.spiraclestudios.autoskola.ui.IconArrowBack
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.PageArrow
import com.spiraclestudios.autoskola.ui.PageArrowSize
import com.spiraclestudios.autoskola.ui.PagerSideArrows
import com.spiraclestudios.autoskola.ui.PinnedAdBanner
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.contentGutter
import com.spiraclestudios.autoskola.ui.previewSituations
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** How tall a question's tab is, which is the height of the bar the tabs are drawn as. */
private val SituationTabHeight = 40.dp

/** How big the mark on a tab is, which has to fit inside it beside the question's number. */
private val SituationTabMarkSize = 14.dp

/** The white disc the mark of the tab being read sits on, so a tick or a cross reads against teal. */
private val SituationTabDiscSize = 22.dp

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
 * The situations the exam asks about, one to a page: a drawing, and the questions about it one to a
 * tab.
 *
 * The driver moves from one situation to the next without going back to the list - by swiping, by
 * the arrows beside the page where the window is wide enough to hold them, or by the arrow keys on a
 * keyboard. That is the practice screen's own way of moving between questions, and its code: the
 * pager, the arrow and the key rule are shared rather than written again here.
 *
 * They answer while they read, and an answer is marked as soon as it is picked - right or wrong,
 * with the right one shown beside it. Nothing is scored and nothing is locked: this is practice, so
 * an answer can be changed as often as they like and the marks follow the answer they have now. What
 * was answered is kept with the bank version being read, so coming back to a situation shows what
 * they made of it.
 */
@Composable
fun SituationScreen(state: SituationUiState, onEvent: (SituationUiEvent) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().paperBackground()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (state) {
                // Nothing read yet, and a screen on its way out, both keep the header and its room.
                SituationUiState.Loading, SituationUiState.Closed -> Column(Modifier.fillMaxSize()) {
                    SituationHeader(strings.learnIntersections, null, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (state is SituationUiState.Loading) ContentLoading()
                    }
                }

                is SituationUiState.Ready -> Situations(state, onEvent, onClose)
            }
        }
    }
}

/**
 * Every situation as a page, with the ways through them: swipe, the arrows beside, the arrow keys.
 *
 * The pages take the whole window - only what they draw is capped, as the practice screen's pages
 * are - so the arrows beside them have the window's own gutters to sit in, and a swipe brings the
 * next page in from the edge of the screen rather than clipping it inside a narrow column.
 */
@Composable
private fun Situations(state: SituationUiState.Ready, onEvent: (SituationUiEvent) -> Unit, onClose: () -> Unit) {
    val situations = state.situations
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { situations.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage

    // The width the pages are drawn at, which is what says whether arrows beside them have room.
    var pageWidth by remember { mutableIntStateOf(0) }
    val gutter = contentGutter(
        available = with(LocalDensity.current) { pageWidth.toDp() },
        column = ContentWidth.Reading,
    )
    val arrowsBeside = gutter >= PageArrowSize

    fun goTo(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index.coerceIn(0, situations.lastIndex)) }
    }

    // Arrow keys step through the situations, the way the arrows beside the page do.
    val pageFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { pageFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxWidth()
            .arrowKeyPaging(pageFocus, current, situations.size, ::goTo),
    ) {
        SituationHeader(
            title = strings.situationName(current + 1),
            questionCount = situations[current].questions.size,
            onClose = onClose,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SwipePager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                onPageSize = { pageWidth = it },
            ) { page ->
                SituationPage(
                    situation = situations[page],
                    answers = state.answers,
                    onEvent = onEvent,
                )
            }
            if (arrowsBeside) {
                PagerSideArrows(
                    gutter = gutter,
                    currentPage = current,
                    pageCount = situations.size,
                    previousDescription = strings.previousSituation,
                    nextDescription = strings.nextSituation,
                    goTo = ::goTo,
                )
            }
        }
        // Pinned under the pager rather than on each page: one banner for the whole screen, so moving
        // to the next situation is the same ad, not a new request on every swipe.
        if (state.showAds) PinnedAdBanner(AdSlot.SituationBanner)
    }
}

/** Back, what the situation being read is called, and how much is asked about it. */
@Composable
private fun SituationHeader(title: String, questionCount: Int?, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        BackHeader(
            title = title,
            onBack = onClose,
            modifier = Modifier.widthIn(max = ContentWidth.Reading),
            subtitle = questionCount?.let { strings.situationQuestions(it) },
        )
    }
}

/**
 * One situation's page: the drawing, the tab of each question, and the question being read. Which
 * tab that is belongs to the page, so a situation swiped away from and come back to opens where it
 * was left.
 *
 * The page takes the whole window and caps only what it draws, as the practice screen's pages do:
 * the drawing is measured against the page it is on, so an enlarged copy never outgrows the screen.
 */
@Composable
private fun SituationPage(situation: SituationItem, answers: Map<Int, Int>, onEvent: (SituationUiEvent) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val question = situation.questions.getOrNull(tab) ?: situation.questions.firstOrNull() ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageSize = DpSize(maxWidth, maxHeight)
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Reading)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalNavBarInset.current),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EnlargeableImage(uri = situation.imageUri, page = pageSize)
                if (situation.questions.size > 1) {
                    SituationTabs(
                        selected = situation.questions.indexOf(question),
                        states = questionStates(situation.questions, answers),
                        onSelect = { tab = it },
                    )
                }
                SituationQuestionBlock(
                    question = question,
                    chosen = answers[question.questionId],
                    onSelect = { chosen -> onEvent(SituationUiEvent.Answer(question.questionId, chosen)) },
                )
            }
        }
    }
}

/**
 * One tab per question: the questions about a situation are asked about the same drawing, and a
 * driver reads one at a time rather than all of them at once.
 *
 * The tabs are drawn as one bar rather than a row of separate pills: a track the whole row sits on,
 * the question being read filled in the app's teal, and the questions beside it left on the track
 * with a hairline between them. That is what says how many questions there are and which one is
 * open at a glance, where a row of floating pills read as more answers under the drawing.
 *
 * A tab carries the mark the list gives a finished situation - the green tick for a question
 * answered right, a red cross for one answered wrongly - and nothing at all while it hasn't been
 * answered. On the tab being read the mark sits on a white disc, because the deeper green and red
 * are too dim to read against the teal that tab is filled with.
 */
@Composable
private fun SituationTabs(
    selected: Int,
    /** How each question stands, by the position its tab is drawn at. */
    states: List<ChipState>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(ChipRadius)
    val count = states.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SituationTabHeight)
            .shadow(8.dp, shape, spotColor = colors.ink.copy(alpha = 0.25f))
            .clip(shape)
            .background(colors.selectedTint)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val open = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(tabShape(index, count))
                    // The tab being read is filled; the rest stay on the track, so the bar reads as
                    // one control with the open tab standing out of it.
                    .then(
                        if (open) {
                            Modifier.background(Brush.verticalGradient(colors.primaryGradient))
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = open, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                // A hairline where two tabs that are neither read nor answered meet, so where one
                // ends and the next begins doesn't disappear into the track.
                if (!open && index > 0 && index - 1 != selected) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .background(colors.primaryDeep.copy(alpha = 0.22f)),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SituationTabMark(state = states.getOrNull(index), open = open)
                    // The label gives way in the middle, so the question's number stays readable
                    // however long the language makes "Question 2" and however many tabs there are.
                    FittingText(
                        text = strings.questionNumber(index + 1),
                        style = Naj.type.tiny,
                        color = if (open) Color.White else colors.primaryDeep,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

/**
 * The corners a tab of [count] keeps at [index]: the bar's own at either end of the row, and square
 * edges against the tabs beside it, so the tabs join into one bar instead of drifting apart.
 */
internal fun tabShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(ChipRadius)
    index == 0 -> RoundedCornerShape(topStart = ChipRadius, bottomStart = ChipRadius)
    index == count - 1 -> RoundedCornerShape(topEnd = ChipRadius, bottomEnd = ChipRadius)
    else -> RectangleShape
}

/**
 * What a tab shows of its question beyond its number: nothing until it has been answered, and then
 * the tick or the cross the question list would mark it with. [open] is the tab being read, where
 * the mark is drawn on a white disc - the tick's green and the cross's red are too dim on the teal
 * that tab is filled with, and on white they read exactly as they do on the track.
 */
@Composable
private fun SituationTabMark(state: ChipState?, open: Boolean) {
    val colors = Naj.colors
    val (icon, description, tint) = when (state) {
        ChipState.Correct -> Triple(
            Icons.Filled.CheckCircle,
            strings.answeredCorrectly,
            colors.passedOn,
        )

        ChipState.Incorrect -> Triple(
            Icons.Filled.Cancel,
            strings.answeredWrongly,
            colors.wrongOn,
        )

        // Not answered yet, which is the only case with no mark at all.
        else -> return
    }
    if (!open) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(SituationTabMarkSize))
        return
    }
    Box(
        modifier = Modifier.size(SituationTabDiscSize).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(SituationTabMarkSize))
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

// ── Previews ──

/** The situations of an exam, opened on the first: its drawing and its first question. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 0),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The banner under the answers, which is where the free tier's ad sits on this screen. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenBannerPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 0, showAds = true),
            onEvent = {},
            onClose = {},
        )
    }
}

/** Opened on the second situation, which is what swiping to it shows. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenSecondPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 1),
            onEvent = {},
            onClose = {},
        )
    }
}

/** A situation answered right, with the mark its tab carries for it. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenAnsweredPreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 0, answers = mapOf(1813 to 2)),
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
            state = SituationUiState.Ready(previewSituations, initialPage = 0, answers = mapOf(1813 to 1)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The same situations in Slovak, which the bank's own text is written in. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SituationScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 1),
            onEvent = {},
            onClose = {},
        )
    }
}

/** A window wide enough to hold the arrows beside the page, which is where they are drawn. */
@Preview(widthDp = 1100, heightDp = 800)
@Composable
private fun SituationScreenWidePreview() {
    AppPreview {
        SituationScreen(
            state = SituationUiState.Ready(previewSituations, initialPage = 0, answers = mapOf(1813 to 2)),
            onEvent = {},
            onClose = {},
        )
    }
}

/** The situations still being read. */
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

/** The tabs on their own: the tab being read, and the marks a question gets once it is answered. */
@Preview(widthDp = 400, heightDp = 160)
@Composable
private fun SituationTabsPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("the first tab read, and every way a question can stand")
            SituationTabs(
                selected = 0,
                states = listOf(
                    ChipState.Correct,
                    ChipState.Incorrect,
                    ChipState.Unanswered,
                    ChipState.Correct,
                ),
                onSelect = {},
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            PreviewLabel("the last tab read, answered wrongly")
            SituationTabs(
                selected = 1,
                states = listOf(ChipState.Correct, ChipState.Incorrect),
                onSelect = {},
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** Four tabs on a narrow phone, where a label longer than its tab loses its middle. */
@Preview(widthDp = 300, heightDp = 120)
@Composable
private fun SituationTabsNarrowPreview() {
    AppPreview {
        SituationTabs(
            selected = 2,
            states = listOf(ChipState.Correct, ChipState.Unanswered, ChipState.Incorrect, ChipState.Unanswered),
            onSelect = {},
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
