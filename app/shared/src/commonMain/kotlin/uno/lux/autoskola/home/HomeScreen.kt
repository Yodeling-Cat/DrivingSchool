package uno.lux.autoskola.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.PracticeTest
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.practice.attemptName
import uno.lux.autoskola.ui.AppPreview
import uno.lux.autoskola.ui.CardRadius
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.DashedCircleIcon
import uno.lux.autoskola.ui.EyebrowLabel
import uno.lux.autoskola.ui.HeightClass
import uno.lux.autoskola.ui.IconTile
import uno.lux.autoskola.ui.LicenceRadius
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalHeightClass
import uno.lux.autoskola.ui.LocalNavBarInset
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.OutlinePill
import uno.lux.autoskola.ui.PagePadding
import uno.lux.autoskola.ui.PickerPill
import uno.lux.autoskola.ui.PillHeight
import uno.lux.autoskola.ui.PrimaryPill
import uno.lux.autoskola.ui.ScreenHeader
import uno.lux.autoskola.ui.TestStatus
import uno.lux.autoskola.ui.TestTile
import uno.lux.autoskola.ui.TileRadius
import uno.lux.autoskola.ui.WidthClass
import uno.lux.autoskola.ui.fullWidthItem
import uno.lux.autoskola.ui.previewExam
import uno.lux.autoskola.ui.previewProgress
import uno.lux.autoskola.ui.previewResults
import uno.lux.autoskola.ui.previewResumable
import uno.lux.autoskola.ui.previewResumePrompt
import uno.lux.autoskola.ui.previewTests
import uno.lux.autoskola.ui.theme.GRAIN_CARD
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.grain
import uno.lux.autoskola.ui.theme.paperBackground
import uno.lux.autoskola.ui.theme.paperGlows
import uno.lux.autoskola.ui.theme.rememberGrainShader

// ── Measurements the design draws but the component kit doesn't name ──

private val LicenceCardHeight = 222.dp

/** The licence card's waves: ten of them, one every 16dp from y=40, 30dp from crest to trough. */
private const val LICENCE_WAVE_COUNT = 10
private val LicenceWaveStartY = 40.dp
private val LicenceWaveStepY = 16.dp
private val LicenceWaveAmplitude = 30.dp
private val LicenceWaveStroke = 1.2.dp
private val LicenceWaveOffset = 45.dp

/** The white glow over the card's left edge: a 260x200 ellipse centred on (90, 40). */
private val LicenceGlowCenterX = 90.dp
private val LicenceGlowCenterY = 40.dp
private val LicenceGlowWidth = 260.dp
private val LicenceGlowHeight = 200.dp

/** The yellow "unfinished test" card: 84dp, or the wide layout's taller banner. */
private val ResumeCardHeight = 84.dp
private val ResumeWideHeight = 96.dp
private val ResumeCardRadius = 24.dp
private val ResumeWideRadius = 28.dp
private val ResumeBarWidth = 150.dp
private const val RESUME_GRAIN = 0.4f

/** The orange the resume card floats on: rgba(255, 150, 0, .6). */
private val ResumeShadow = Color(0x99FF9600)

private val MistakesCardHeight = 68.dp
private val HomeBandWidth = 410.dp
private val GridGap = 12.dp
private val WideGridGap = 14.dp
private val SectionGap = 16.dp
private val GridTileMinWidth = 180.dp
private val HeaderTopInset = 52.dp

/** The 13sp bold caption the design sets the exam pill and the hero's label in. */
private val CaptionStyle: TextStyle
    @Composable get() = Naj.type.tileStatus.copy(fontSize = 13.sp)

/** Connects [HomeScreen] to its [HomeViewModel] and to navigation. */
@Composable
fun HomeRoute(
    onOpenAttempt: (attemptId: String) -> Unit,
    onChangeExam: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        HomeUiState.Loading -> LoadingScreen()

        is HomeUiState.Ready -> {
            LaunchedEffect(current.openAttemptId) {
                current.openAttemptId?.let {
                    onOpenAttempt(it)
                    viewModel.onEvent(HomeUiEvent.AttemptOpened)
                }
            }
            HomeScreen(current, viewModel::onEvent, onChangeExam)
        }
    }
}

/**
 * Progress overview and test picker. Phones and medium windows get one scrolling column of cards
 * and a test grid; an expanded window puts the overview in a band beside the resume banner and a
 * three-column grid.
 */
@Composable
fun HomeScreen(state: HomeUiState.Ready, onEvent: (HomeUiEvent) -> Unit, onChangeExam: () -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors))) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            if (LocalWidthClass.current == WidthClass.Expanded) {
                ExpandedHome(state, onEvent, onChangeExam)
            } else {
                ColumnHome(state, onEvent, onChangeExam)
            }
        }
    }

    state.resumePrompt?.let { ResumePromptDialog(it, onEvent) }
}

/** The phone and medium layout: the cards in one scrolling column, then the tests as a grid. */
@Composable
private fun ColumnHome(state: HomeUiState.Ready, onEvent: (HomeUiEvent) -> Unit, onChangeExam: () -> Unit) {
    val tests = state.tests
    val progress = state.progress
    // A phone shows two tests side by side; a medium window fits as many as the tiles allow.
    val columns = if (LocalWidthClass.current == WidthClass.Medium) {
        GridCells.Adaptive(GridTileMinWidth)
    } else {
        GridCells.Fixed(2)
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = Modifier.fillMaxHeight().widthIn(max = ContentWidth.Narrow).fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PagePadding,
            end = PagePadding,
            top = HeaderTopInset,
            // The floating navigation pill sits over the bottom of the page.
            bottom = 24.dp + LocalNavBarInset.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        fullWidthItem {
            ScreenHeader(
                trailing = { ExamPill(state.exam, onChangeExam) },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        fullWidthItem {
            LicenceCard(totalTests = tests.size, progress = progress)
        }
        progress.resumable?.let { unfinished ->
            fullWidthItem {
                ResumeCard(unfinished, onResume = { onEvent(HomeUiEvent.Resume) }, wide = false)
            }
        }
        fullWidthItem {
            ActionRow(
                tests = tests,
                notPassedEnabled = state.hasNotPassedTests,
                onEvent = onEvent,
            )
        }
        fullWidthItem {
            MistakesCard(count = progress.mistakeCount, onClick = { onEvent(HomeUiEvent.PracticeMistakes) })
        }
        fullWidthItem {
            Text(
                text = strings.allTests,
                style = Naj.type.sectionHeading,
                color = Naj.colors.ink,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        items(tests, key = { it.number }) { test ->
            TestTileItem(test = test, progress = progress, onEvent = onEvent)
        }
    }
}

/** The wide layout: the overview band on the left, the resume banner and the grid on the right. */
@Composable
private fun ExpandedHome(state: HomeUiState.Ready, onEvent: (HomeUiEvent) -> Unit, onChangeExam: () -> Unit) {
    val colors = Naj.colors
    val tests = state.tests
    val progress = state.progress
    // A phone held sideways gets this layout but not its desktop measurements; see [twoPaneColumns]
    // and [twoPaneResumeWide].
    val height = LocalHeightClass.current

    Row(Modifier.widthIn(max = ContentWidth.Wide).fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(HomeBandWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
        ) {
            ScreenHeader(trailing = { ExamPill(state.exam, onChangeExam) })
            Spacer(Modifier.height(20.dp))
            LicenceCard(totalTests = tests.size, progress = progress)
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionRow(
                    tests = tests,
                    notPassedEnabled = state.hasNotPassedTests,
                    onEvent = onEvent,
                    stacked = true,
                )
                MistakesCard(count = progress.mistakeCount, onClick = { onEvent(HomeUiEvent.PracticeMistakes) })
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                // No bottom inset here: the grid's viewport has to reach the window's bottom edge,
                // or a row being scrolled is cut off 24dp above it. The breathing room below the
                // last row is the grid's own bottom content padding instead.
                .padding(start = 16.dp, end = 32.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            progress.resumable?.let { unfinished ->
                ResumeCard(
                    unfinished,
                    onResume = { onEvent(HomeUiEvent.Resume) },
                    wide = twoPaneResumeWide(height),
                )
            }
            Text(text = strings.allTests, style = Naj.type.sectionHeadingLarge, color = colors.ink)
            LazyVerticalGrid(
                columns = GridCells.Fixed(twoPaneColumns(height)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(WideGridGap),
                verticalArrangement = Arrangement.spacedBy(WideGridGap),
            ) {
                items(tests, key = { it.number }) { test ->
                    TestTileItem(test = test, progress = progress, onEvent = onEvent)
                }
            }
        }
    }
}

/** The white pill naming the exam, which opens the exam picker. */
@Composable
private fun ExamPill(exam: ExamLabel, onClick: () -> Unit) {
    PickerPill(
        text = "${exam.name} \u00b7 ${exam.bankLanguage.uppercase()}",
        contentDescription = strings.change,
        onClick = onClick,
    )
}

/** The licence gradient card: its waves, the finished-tests hero, and the four stats under it. */
@Composable
private fun LicenceCard(totalTests: Int, progress: HomeProgress, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    val shape = RoundedCornerShape(LicenceRadius)
    val results = progress.results
    val finished = results.size
    val passed = results.values.count { it.passed }
    val average = if (results.isEmpty()) null else results.values.sumOf { it.percent } / results.size
    // Passed, the card names the stage they have reached rather than what they are preparing for.
    val stage = when (trainingStage(results, totalTests)) {
        TrainingStage.InTraining -> strings.progressEyebrow
        TrainingStage.Expert -> strings.progressEyebrowPassed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LicenceCardHeight)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = colors.ink.copy(alpha = 0.55f),
                spotColor = colors.ink.copy(alpha = 0.55f),
            )
            .clip(shape)
            .background(Brush.linearGradient(colors.licenceGradient)),
    ) {
        // The waves, the glow over the left edge and the grain all sit behind the card's content,
        // the way the design stacks them.
        Box(Modifier.fillMaxSize().grain(GRAIN_CARD, shader)) {
            Canvas(Modifier.fillMaxSize()) {
                drawLicenceWaves()
                drawLicenceGlow()
            }
            TopHighlight()
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            EyebrowLabel(
                text = stage.uppercase(),
                style = Naj.type.eyebrow,
                color = colors.inkAt(0.75f),
            )
            Spacer(Modifier.height(10.dp))
            HeroRow(finished = finished, total = totalTests)
            Spacer(Modifier.height(18.dp))
            ProgressBar(
                fraction = if (totalTests == 0) 0f else finished.toFloat() / totalTests,
                height = 8.dp,
                trackColor = colors.inkAt(0.16f),
                fillColor = colors.ink,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            StatsRow(finished = finished, passed = passed, average = average, progress = progress)
        }
    }
}

/** The hero number, the number of tests beside it, and the words of the caption beside both. */
@Composable
private fun HeroRow(finished: Int, total: Int) {
    val colors = Naj.colors
    val caption = strings.testsFinishedLabel

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$finished",
            style = Naj.type.heroNumber,
            color = colors.ink,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = "/ $total",
            style = Naj.type.heroSuffix,
            color = colors.ink.copy(alpha = 0.6f),
            modifier = Modifier.alignByBaseline(),
        )
        if (caption.isNotBlank()) {
            Text(
                text = twoLines(caption),
                style = CaptionStyle,
                color = colors.ink,
                modifier = Modifier.alignByBaseline().padding(start = 4.dp),
            )
        }
    }
}

/** Passed, failed, average, and the questions answered out of the pool. */
@Composable
private fun StatsRow(
    finished: Int,
    passed: Int,
    average: Int?,
    progress: HomeProgress,
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Stat(label = strings.passed, value = "$passed", modifier = Modifier.weight(1f))
        Stat(label = strings.failed, value = "${finished - passed}", modifier = Modifier.weight(1f))
        Stat(
            label = strings.averageShort,
            value = average?.let { "$it%" } ?: "\u2013",
            modifier = Modifier.weight(1f),
        )
    }
}

/** One figure of the stats row: the number over its label, right-aligned in the last column. */
@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val colors = Naj.colors
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(text = value, style = Naj.type.statNumber, color = colors.ink)
        Text(
            text = label.uppercase(),
            style = Naj.type.tiny,
            color = colors.inkAt(0.7f),
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
    }
}

/** The unfinished test: how far it got, and how to carry on. [wide] is the 96dp banner. */
@Composable
private fun ResumeCard(
    test: ResumableTest,
    onResume: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    val shape = RoundedCornerShape(if (wide) ResumeWideRadius else ResumeCardRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (wide) ResumeWideHeight else ResumeCardHeight)
            .shadow(14.dp, shape, ambientColor = ResumeShadow, spotColor = ResumeShadow)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.resumeTop, colors.resumeBottom)))
            .clickable(onClick = onResume),
    ) {
        Box(Modifier.fillMaxSize().grain(RESUME_GRAIN, shader)) { TopHighlight() }
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                EyebrowLabel(
                    text = strings.unfinishedTest,
                    style = Naj.type.letterSpacedSmall,
                    color = colors.resumeOn,
                )
                Text(
                    text = strings.resumeSummary(attemptName(test.kind, test.testNumber), test.answered, test.total),
                    style = if (wide) Naj.type.cardHero else Naj.type.cardHeroSmall,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (!wide) {
                    ProgressBar(
                        fraction = if (test.total == 0) 0f else test.answered.toFloat() / test.total,
                        height = 5.dp,
                        trackColor = colors.inkAt(0.15f),
                        fillColor = colors.ink,
                        modifier = Modifier.width(ResumeBarWidth).padding(top = 8.dp),
                    )
                }
            }
            if (wide) ContinuePill() else ResumeCircle()
        }
    }
}

/** The round play button the phone's resume card ends with. */
@Composable
private fun ResumeCircle() {
    val colors = Naj.colors
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.ink),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = colors.roadYellow,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** The wide layout's continue pill: ink, with the road-yellow play mark. */
@Composable
private fun ContinuePill() {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .height(PillHeight)
            .clip(RoundedCornerShape(PillHeight / 2))
            .background(colors.ink)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = colors.roadYellow,
            modifier = Modifier.size(22.dp),
        )
        Text(text = strings.continueTest, style = Naj.type.button, color = colors.roadYellow)
    }
}

/**
 * The two ways to start a test: a random one, or one that has not been passed. They stack on a wide
 * window, where a row of two half-width buttons on a band this wide leaves both of them oddly long.
 * On a phone they sit side by side at their own widths, because the two labels differ by half a word
 * per language: the short one is only as wide as its label, and the one that spells out what it
 * starts takes the rest of the row. The random one wears its shorter label on a phone for the same
 * reason ([randomPillLabel]) and names the test from a tablet up. The "not passed" pill is live for
 * as long as a test is still unpassed: a failed one counts, because it can be taken again.
 */
@Composable
private fun ActionRow(
    tests: List<PracticeTest>,
    notPassedEnabled: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val randomLabel = randomPillLabel(LocalWidthClass.current, strings)

    val randomTest = @Composable { pillModifier: Modifier ->
        OutlinePill(
            text = randomLabel,
            onClick = { onEvent(HomeUiEvent.StartRandomTest(notPassed = false)) },
            modifier = pillModifier,
            icon = Icons.Filled.Shuffle,
            enabled = tests.isNotEmpty(),
        )
    }
    val notPassed = @Composable { pillModifier: Modifier ->
        PrimaryPill(
            text = strings.notPassed,
            onClick = { onEvent(HomeUiEvent.StartRandomTest(notPassed = true)) },
            modifier = pillModifier,
            iconContent = { DashedCircleIcon(Color.White) },
            enabled = notPassedEnabled,
        )
    }

    if (stacked) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            notPassed(Modifier.fillMaxWidth())
            randomTest(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            randomTest(Modifier)
            notPassed(Modifier.weight(1f))
        }
    }
}

/** The mistakes the user can practise: inert and greyed out when there are none. */
@Composable
private fun MistakesCard(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    val enabled = count > 0
    val shape = RoundedCornerShape(CardRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MistakesCardHeight)
            .shadow(6.dp, shape, ambientColor = colors.inkAt(0.3f), spotColor = colors.inkAt(0.3f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.card, colors.wrongTop)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            size = 44.dp,
            background = Brush.linearGradient(listOf(colors.mistakesTop, colors.mistakesBottom)),
        ) {
            Text(text = "$count", style = Naj.type.badgeNumber, color = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.practiceMistakes,
                style = Naj.type.cardTitle,
                color = if (enabled) colors.ink else colors.disabledOn,
            )
            Text(text = strings.mistakesToReview(count), style = Naj.type.secondary, color = colors.muted)
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.primaryDeep,
            )
        }
    }
}

/** One test in the grid: its number, and how it stands. */
@Composable
private fun TestTileItem(
    test: PracticeTest,
    progress: HomeProgress,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    val result = progress.results[test.number]
    val status = when {
        test.number in progress.inProgress -> TestStatus.InProgress
        result == null -> TestStatus.NotStarted
        result.passed -> TestStatus.Passed
        else -> TestStatus.Failed
    }
    // A finished test summarises itself; the other two say why they carry no score.
    val label = result?.let { strings.resultSummary(it.score, it.maxScore, it.passed) }
        ?: if (status == TestStatus.InProgress) strings.inProgress else strings.notStarted

    TestTile(
        number = "${test.number}",
        status = status,
        statusLabel = label,
        onClick = { onEvent(HomeUiEvent.StartTest(test.number)) },
        modifier = modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(TileRadius),
            ambientColor = colors.ink.copy(alpha = 0.25f),
            spotColor = colors.ink.copy(alpha = 0.25f),
        ),
    )
}

/** Asks what to do with a test that's already under way: continue it, or start over. */
@Composable
private fun ResumePromptDialog(prompt: ResumePrompt, onEvent: (HomeUiEvent) -> Unit) {
    val colors = Naj.colors
    AlertDialog(
        onDismissRequest = { onEvent(HomeUiEvent.DismissResumePrompt) },
        shape = RoundedCornerShape(LicenceRadius),
        containerColor = colors.sheet,
        titleContentColor = colors.ink,
        textContentColor = colors.muted,
        title = { Text(text = strings.resumePromptTitle, style = Naj.type.sectionHeading) },
        text = {
            Text(
                text = strings.resumeSummary(strings.testName(prompt.testNumber), prompt.answered, prompt.total),
                style = Naj.type.body,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(HomeUiEvent.ContinueTest) },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.primaryDeep),
            ) {
                Text(text = strings.continueTest, style = Naj.type.primaryButton)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(HomeUiEvent.StartOver) },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.muted),
            ) {
                Text(text = strings.startOver, style = Naj.type.button)
            }
        },
    )
}

/** A rounded bar with [fraction] of it filled. */
@Composable
private fun ProgressBar(
    fraction: Float,
    height: Dp,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.height(height).clip(shape).background(trackColor)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(fillColor),
        )
    }
}

/** The hairline of white along the top of a coloured card, which the design draws as an inset. */
@Composable
private fun TopHighlight(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) { drawRect(Color.White.copy(alpha = 0.7f)) }
}

/**
 * The ten white wave lines: each is a crest then a mirrored trough, carried across the card's whole
 * width. How many quarters of the pattern that takes is [licenceWaveQuarters]' business; the last
 * one ends past the right edge, or a card wider than the design's 360dp cut the waves off mid-card.
 */
private fun DrawScope.drawLicenceWaves() {
    val quarter = LicenceWaveQuarter.toPx()
    val offset = LicenceWaveOffset.toPx()
    val amplitude = LicenceWaveAmplitude.toPx()
    val step = LicenceWaveStepY.toPx()
    val quarters = licenceWaveQuarters(size.width.toDp())
    repeat(LICENCE_WAVE_COUNT) { index ->
        val y = LicenceWaveStartY.toPx() + index * step
        val wave = Path().apply {
            moveTo(0f, y)
            repeat(quarters) { span ->
                // A crest on the even quarters and a trough on the odd ones, so the pattern keeps
                // the phase the design sets, whatever the card measures.
                val peak = if (span % 2 == 0) y - amplitude else y + amplitude
                quadraticTo(offset + quarter * span, peak, quarter * (span + 1), y)
            }
        }
        drawPath(wave, color = Color.White.copy(alpha = 0.35f), style = Stroke(LicenceWaveStroke.toPx()))
    }
}

/** The white glow over the licence card's left edge, as a squashed radial gradient. */
private fun DrawScope.drawLicenceGlow() {
    val radiusX = LicenceGlowWidth.toPx() / 2f
    val radiusY = LicenceGlowHeight.toPx() / 2f
    translate(LicenceGlowCenterX.toPx(), LicenceGlowCenterY.toPx()) {
        // Scaling the canvas turns the circular gradient into the design's ellipse.
        scale(radiusX, radiusY, pivot = Offset.Zero) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color.White.copy(alpha = 0.55f), 1f to Color.Transparent),
                    center = Offset.Zero,
                    radius = 1f,
                ),
                topLeft = Offset(-1f, -1f),
                size = Size(2f, 2f),
            )
        }
    }
}

/** Breaks a label at its middle space, the way the design sets the hero's two-line caption. */
private fun twoLines(label: String): String {
    val words = label.split(' ').filter { it.isNotBlank() }
    if (words.size < 2) return label
    val fold = (words.size + 1) / 2
    return words.take(fold).joinToString(" ") + "\n" + words.drop(fold).joinToString(" ")
}

// ── Previews ──

/**
 * The home screen's state over the sample profile: six of eight tests done, the seventh under way,
 * and the yellow resume card offering the one that was left half finished. Whether the tests can be
 * started is the same rule the app uses, so a preview can't drift from it.
 */
private fun homeState(
    results: Map<Int, TestResult> = previewResults(
        1 to TestResult(36, 40),
        2 to TestResult(31, 40),
        3 to TestResult(40, 40),
        4 to TestResult(38, 40),
        5 to TestResult(33, 40),
        6 to TestResult(37, 40),
    ),
    inProgress: Set<Int> = setOf(7),
    resumable: ResumableTest? = previewResumable(),
    mistakeCount: Int = 12,
    questionsAnswered: Int = 236,
    resumePrompt: ResumePrompt? = null,
): HomeUiState.Ready {
    val tests = previewTests()
    return HomeUiState.Ready(
        exam = previewExam,
        tests = tests,
        progress = previewProgress(results, inProgress, resumable, mistakeCount, questionsAnswered),
        hasNotPassedTests = notPassedTests(tests, results).isNotEmpty(),
        resumePrompt = resumePrompt,
    )
}

/** The phone layout, with the progress card, the resume card and the test grid. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenPreview() {
    AppPreview {
        HomeScreen(homeState(), onEvent = {}, onChangeExam = {})
    }
}

/** The first launch: nothing done, nothing under way, and no mistakes to practise. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenFreshPreview() {
    AppPreview {
        HomeScreen(
            homeState(
                results = emptyMap(),
                inProgress = emptySet(),
                resumable = null,
                mistakeCount = 0,
                questionsAnswered = 0,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}

/** Every test passed, so the card names the stage the driver reached instead. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenExpertPreview() {
    AppPreview {
        HomeScreen(
            homeState(
                results = (1..8).associateWith { TestResult(38, 40) },
                inProgress = emptySet(),
                resumable = null,
                mistakeCount = 3,
                questionsAnswered = 861,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}

/** Every test taken but one failed, so the "not passed" pill still has a test to offer. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenTestsLeftPreview() {
    AppPreview {
        HomeScreen(
            homeState(
                results = (1..8).associateWith { TestResult(if (it == 4) 30 else 38, 40) },
                inProgress = emptySet(),
                resumable = null,
                mistakeCount = 5,
                questionsAnswered = 861,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}

/** A medium window, where the grid takes as many tiles per row as they allow. */
@Preview(widthDp = 700, heightDp = 900)
@Composable
private fun HomeScreenMediumPreview() {
    AppPreview {
        HomeScreen(homeState(), onEvent = {}, onChangeExam = {})
    }
}

/**
 * A phone held sideways - 952x426dp is a Pixel 9 Pro in landscape. The width still picks the
 * two-pane layout, and the short window is what gives it the phone's compact resume card and two
 * tiles per row instead of the desktop's banner and three.
 */
@Preview(widthDp = 952, heightDp = 426)
@Composable
private fun HomeScreenLandscapePreview() {
    AppPreview {
        HomeScreen(homeState(), onEvent = {}, onChangeExam = {})
    }
}

/** A desktop window: the overview in a band, the grid and the resume banner beside it. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun HomeScreenExpandedPreview() {
    AppPreview {
        HomeScreen(homeState(), onEvent = {}, onChangeExam = {})
    }
}

/** What the screen asks when the picked test is already under way. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenResumePromptPreview() {
    AppPreview {
        HomeScreen(homeState(resumePrompt = previewResumePrompt()), onEvent = {}, onChangeExam = {})
    }
}
