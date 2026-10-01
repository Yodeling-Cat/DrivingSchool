package uno.lux.autoskola.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.practice.attemptName
import uno.lux.autoskola.stats.AttemptKind
import uno.lux.autoskola.stats.AttemptSummary
import uno.lux.autoskola.stats.elapsedMinutes
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.CardDivider
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.EyebrowLabel
import uno.lux.autoskola.ui.LicenceRadius
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.NajCard
import uno.lux.autoskola.ui.OutlinePill
import uno.lux.autoskola.ui.PrimaryPill
import uno.lux.autoskola.ui.RoundButton
import uno.lux.autoskola.ui.WidthClass
import uno.lux.autoskola.ui.drawLicenceGlow
import uno.lux.autoskola.ui.drawLicenceWaves
import uno.lux.autoskola.ui.previewExam
import uno.lux.autoskola.ui.previewSections
import uno.lux.autoskola.ui.previewSummary
import uno.lux.autoskola.ui.theme.AccentedText
import uno.lux.autoskola.ui.theme.GRAIN_CARD
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.accentNumber
import uno.lux.autoskola.ui.theme.grain
import uno.lux.autoskola.ui.theme.paperBackground
import uno.lux.autoskola.ui.theme.paperGlows
import uno.lux.autoskola.ui.theme.rememberGrainShader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The page's own room: the header sits below the status bar, the last card clears the bottom. */
private val PageTop = 48.dp
private val PageBottom = 24.dp

/** The result card, as the design draws it: one fixed-height poster. */
private val CardHeight = 236.dp
private val CardInset = 20.dp

/** The score bar, and the tick that marks the pass mark on it. */
private val TrackHeight = 8.dp
private val TrackTickHeight = 18.dp
private val TrackTickWidth = 2.dp

/** The three counting tiles. */
private val CountRadius = 20.dp
private val CountGap = 10.dp

/** Where the white glow sits, off the poster's top-left corner; see [drawLicenceGlow]. */
private val PosterGlowCentre = DpOffset(-40.dp, -60.dp)

/** How far the stamp is tilted, the way a rubber stamp lands. */
private const val STAMP_TILT = -8f

/**
 * Connects [ResultsScreen] to its [ResultsViewModel]. [onReview] goes back to the test with
 * its answers revealed; [onDone] goes back to the list of tests, and [onDoneToList] to the
 * situations a drill was begun from.
 */
@Composable
fun ResultsRoute(
    attemptId: String,
    onReview: () -> Unit,
    onDone: () -> Unit,
    onDoneToList: () -> Unit,
    viewModel: ResultsViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        ResultsUiState.Loading -> LoadingScreen()

        ResultsUiState.Closed -> LaunchedEffect(Unit) { onReview() }

        is ResultsUiState.Ready -> ResultsScreen(
            state = current,
            onReview = onReview,
            // A drill was begun from the situations, so that is where its way out leads.
            onDone = if (current.hasVerdict) onDone else onDoneToList,
        )
    }
}

/** How a finished test went: the score, right and wrong answers, and a breakdown by section. */
@Composable
fun ResultsScreen(state: ResultsUiState.Ready, onReview: () -> Unit, onDone: () -> Unit) {
    val colors = Naj.colors
    // Expanded screens show the breakdown next to the score instead of below it.
    val twoPane = LocalWidthClass.current == WidthClass.Expanded
    val eyebrow = "${attemptName(state.kind, state.testNumber)} · ${strings.resultsTitle}".uppercase()
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors, mintY = 0.6f))) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (twoPane) ContentWidth.Wide else ContentWidth.Narrow)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = PageTop, bottom = PageBottom),
            ) {
                Header(state, onReview)
                Spacer(Modifier.height(24.dp))
                if (twoPane) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            ResultCard(
                                result = state.summary.result,
                                eyebrow = eyebrow,
                                elapsedSeconds = state.elapsedSeconds,
                                hasVerdict = state.hasVerdict,
                            )
                            Counts(state.summary)
                            Actions(onReview, onDone, doneLabel(state))
                        }
                        // The breakdown's pane already starts at the page's own padding.
                        Box(Modifier.weight(1f)) { Sections(state.sections, 0.dp) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        ResultCard(
                            result = state.summary.result,
                            eyebrow = eyebrow,
                            elapsedSeconds = state.elapsedSeconds,
                            hasVerdict = state.hasVerdict,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Counts(state.summary, Modifier.padding(horizontal = 16.dp))
                        Actions(onReview, onDone, doneLabel(state), Modifier.padding(horizontal = 16.dp))
                        Sections(state.sections, 16.dp)
                    }
                }
            }
        }
    }
}

/** The round back button and the two-line title, at the top of the page. */
@Composable
private fun Header(state: ResultsUiState.Ready, onBack: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Column {
            AccentedText(
                accentNumber("${attemptName(state.kind, state.testNumber)} ${strings.resultsTitle}"),
                style = Naj.type.barTitle,
                color = colors.ink,
            )
            Text(
                state.exam.shortTitle,
                style = Naj.type.secondary,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** What the way out of the results is called: a drill goes back to the situations. */
@Composable
private fun doneLabel(state: ResultsUiState.Ready): String =
    if (state.hasVerdict) strings.backToTests else strings.backToList

/**
 * The result: the licence ramp with its waves, glow and grain, the pass stamp, the score over the
 * pass-mark bar — and, under all of it, the percentage, the mark it had to beat and the time the
 * test took.
 *
 * An attempt that isn't given a verdict - a situations drill - is shown without any of that: the
 * score alone, with no stamp, no pass mark and no time, because there was nothing to pass.
 */
@Composable
private fun ResultCard(
    result: TestResult,
    eyebrow: String,
    elapsedSeconds: Long,
    hasVerdict: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    val shape = RoundedCornerShape(LicenceRadius)
    val passPercent = TestResult.PASS_PERCENT
    val stampColor = if (result.passed) colors.passedOn else colors.wrongOn
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = colors.ink.copy(alpha = 0.55f),
                spotColor = colors.ink.copy(alpha = 0.55f),
            )
            .clip(shape),
    ) {
        // The poster: ramp, waves and highlight, with the grain over them but under the text.
        Box(
            Modifier
                .matchParentSize()
                .cssGradient(colors.licenceGradient, 115f)
                .drawBehind {
                    drawLicenceWaves()
                    drawLicenceGlow(PosterGlowCentre)
                }
                .grain(GRAIN_CARD, shader),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = CardInset, end = 18.dp, top = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            EyebrowLabel(
                text = eyebrow,
                modifier = Modifier.weight(1f),
                style = Naj.type.eyebrow,
                color = colors.inkAt(0.75f),
            )
            if (hasVerdict) {
                Stamp(passed = result.passed, color = stampColor, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(start = CardInset, top = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                result.score.toString(),
                style = Naj.type.heroNumber,
                color = colors.ink,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "/ ${result.maxScore}",
                style = Naj.type.heroSuffix,
                color = colors.ink.copy(alpha = 0.6f),
                modifier = Modifier.alignByBaseline(),
            )
        }
        // The bar is inset like the footer under it, so its right end stays on the card: the score,
        // and with it the tick, is a share of the card's own width, not of its edge.
        ScoreBar(
            percent = result.percent,
            // No pass mark on a drill's card: there is nothing it was being measured against.
            passPercent = passPercent.takeIf { hasVerdict },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = CardInset, end = CardInset, top = 146.dp),
        )
        // Under the bar: the percentage and the pass mark on the left, the time on the right. An
        // attempt from before the app timed tests has no time to show, so the corner stays empty -
        // and a drill's card keeps the whole footer empty, since it was never marked against anything.
        if (hasVerdict) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = CardInset, end = CardInset, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${result.percent} % · ${strings.passMarkShort(passPercent)}",
                    style = Naj.type.small,
                    color = colors.inkAt(0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (elapsedSeconds > 0) {
                    Text(
                        strings.minutesShort(elapsedMinutes(elapsedSeconds)),
                        style = Naj.type.small,
                        color = colors.inkAt(0.75f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The tilted PASSED / FAILED stamp the design puts on the card's top-right corner. */
@Composable
private fun Stamp(passed: Boolean, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .rotate(STAMP_TILT)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.35f))
            .border(3.dp, color, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            (if (passed) strings.passedStamp else strings.failedStamp).uppercase(),
            style = Naj.type.stamp,
            color = color,
            maxLines = 1,
        )
    }
}

/** The score bar: the track, the score filled in, and - where there is one - the pass-mark tick. */
@Composable
private fun ScoreBar(percent: Int, passPercent: Int?, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    val filled = (percent / 100f).coerceIn(0f, 1f)
    val pass = passPercent?.let { (it / 100f).coerceIn(0f, 1f) }
    Canvas(modifier.fillMaxWidth().height(TrackTickHeight)) {
        val barTop = (size.height - TrackHeight.toPx()) / 2f
        val radius = CornerRadius(TrackHeight.toPx() / 2f)
        drawRoundRect(
            color = colors.inkAt(0.16f),
            topLeft = Offset(0f, barTop),
            size = Size(size.width, TrackHeight.toPx()),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = colors.ink,
            topLeft = Offset(0f, barTop),
            size = Size(size.width * filled, TrackHeight.toPx()),
            cornerRadius = radius,
        )
        if (pass != null) {
            drawRect(
                color = colors.ink,
                topLeft = Offset(size.width * pass, 0f),
                size = Size(TrackTickWidth.toPx(), size.height),
            )
        }
    }
}

/** How many answers were right, wrong and left blank, as three equal tiles. */
@Composable
private fun Counts(summary: AttemptSummary, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CountGap)) {
        CountTile(
            label = strings.correctCount,
            count = summary.correct,
            tint = listOf(colors.passedTop, colors.passedBottom),
            text = colors.passedOn,
            modifier = Modifier.weight(1f),
        )
        CountTile(
            label = strings.wrongCount,
            count = summary.wrong,
            tint = listOf(colors.wrongTop, colors.wrongBottom),
            text = colors.wrongOn,
            modifier = Modifier.weight(1f),
        )
        CountTile(
            label = strings.unansweredCount,
            count = summary.unanswered,
            tint = listOf(colors.card, colors.neutralTint),
            text = colors.muted,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One counting tile: a big number with its label, on the tint that goes with it. */
@Composable
private fun CountTile(
    label: String,
    count: Int,
    tint: List<Color>,
    text: Color,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(CountRadius)
    Column(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = colors.ink.copy(alpha = 0.35f),
                spotColor = colors.ink.copy(alpha = 0.35f),
            )
            .clip(shape)
            .cssGradient(tint, 150f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(count.toString(), style = Naj.type.counterNumber, color = text)
        Text(
            label,
            style = Naj.type.small,
            color = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The two ways out: review the answers, or go back to where the attempt was begun. */
@Composable
private fun Actions(
    onReview: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinePill(text = strings.reviewAnswers, onClick = onReview, modifier = Modifier.weight(1f))
        PrimaryPill(text = doneLabel, onClick = onDone, modifier = Modifier.weight(1f))
    }
}

/** The heading and the card the per-section breakdown is drawn on. */
@Composable
private fun Sections(sections: List<ResultSection>, sidePadding: Dp, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth()) {
        Text(
            strings.bySection,
            style = Naj.type.sectionHeading,
            color = colors.ink,
            modifier = Modifier.padding(start = sidePadding + 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        NajCard(Modifier.fillMaxWidth().padding(horizontal = sidePadding), shadow = 0.3f) {
            sections.forEachIndexed { index, section ->
                if (index > 0) CardDivider()
                SectionRow(section)
            }
        }
    }
}

/** One section: its name, how it scored, and the same score as a bar. */
@Composable
private fun SectionRow(section: ResultSection) {
    val colors = Naj.colors
    val summary = section.summary
    val allRight = summary.total > 0 && summary.correct == summary.total
    val on = if (allRight) colors.passedOn else colors.wrongOn
    val fill = if (allRight) colors.passedDot else colors.wrongDot
    val progress = if (summary.total == 0) 0f else summary.correct.toFloat() / summary.total
    // The scale's medium weight has no 14sp step of its own; the design's row name is that size.
    val nameStyle = Naj.type.question.copy(fontSize = 14.sp, lineHeight = 20.sp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row {
            Text(
                section.title,
                style = nameStyle,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text("${summary.correct} / ${summary.total}", style = Naj.type.sectionScore, color = on)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.divider)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(fill))
        }
    }
}

/** A linear gradient at a CSS angle: [degrees] clockwise from "to top", over the box's own size. */
private fun Modifier.cssGradient(colors: List<Color>, degrees: Float): Modifier = drawBehind {
    drawRect(cssBrush(colors, degrees, size))
}

/** The gradient line runs through the box's centre, as CSS lays one out. */
private fun cssBrush(colors: List<Color>, degrees: Float, size: Size): Brush {
    val radians = degrees * PI / 180.0
    val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
    val length = abs(size.width * direction.x) + abs(size.height * direction.y)
    val centre = Offset(size.width / 2f, size.height / 2f)
    val half = direction * (length / 2f)
    return Brush.linearGradient(colors, centre - half, centre + half)
}

// ── Previews ──

/** A finished attempt over the sample sections, with the pass mark where the summary puts it. */
private fun resultsState(
    summary: AttemptSummary = previewSummary(),
    elapsedSeconds: Long = 23 * 60,
    hasVerdict: Boolean = true,
): ResultsUiState.Ready = ResultsUiState.Ready(
    exam = previewExam,
    kind = if (hasVerdict) AttemptKind.PracticeTest else AttemptKind.Situations,
    testNumber = if (hasVerdict) 7 else null,
    summary = summary,
    sections = summary.sections.mapIndexed { index, section ->
        ResultSection(previewSections[index].title, section)
    },
    elapsedSeconds = elapsedSeconds,
    hasVerdict = hasVerdict,
)

/** A passed test on a phone: the poster, the three counts, the actions and the breakdown. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun ResultsScreenPreview() {
    AppPreview {
        ResultsScreen(resultsState(), onReview = {}, onDone = {})
    }
}

/** A failed test: 75 %, under the 90 % the stamp turns on, done in twelve minutes. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun ResultsScreenFailedPreview() {
    AppPreview {
        ResultsScreen(
            resultsState(previewSummary(correct = 30, wrong = 9), elapsedSeconds = 12 * 60),
            onReview = {},
            onDone = {},
        )
    }
}

/** A test finished before the app timed tests: the bottom-right corner stays empty. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun ResultsScreenUntimedPreview() {
    AppPreview {
        ResultsScreen(
            resultsState(previewSummary(correct = 30, wrong = 9), elapsedSeconds = 0L),
            onReview = {},
            onDone = {},
        )
    }
}

/** A desktop window, where the breakdown sits beside the score instead of below it. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun ResultsScreenExpandedPreview() {
    AppPreview {
        ResultsScreen(resultsState(), onReview = {}, onDone = {})
    }
}

/** A situations drill: nothing to pass, so no stamp, no pass mark, no time, and back to the list. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun ResultsScreenDrillPreview() {
    AppPreview {
        ResultsScreen(
            resultsState(elapsedSeconds = 31 * 60, hasVerdict = false),
            onReview = {},
            onDone = {},
        )
    }
}
