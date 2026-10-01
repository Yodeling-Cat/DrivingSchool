package uno.lux.autoskola.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
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
import uno.lux.autoskola.ui.ResumeDialog
import uno.lux.autoskola.ui.ScreenHeader
import uno.lux.autoskola.ui.TestStatus
import uno.lux.autoskola.ui.TestTile
import uno.lux.autoskola.ui.TileRadius
import uno.lux.autoskola.ui.WidthClass
import uno.lux.autoskola.ui.drawLicenceGlow
import uno.lux.autoskola.ui.drawLicenceWaves
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

/** Where the white glow sits over the card's left edge; see [drawLicenceGlow]. */
private val LicenceGlowCentre = DpOffset(90.dp, 40.dp)

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
private val SectionGap = 16.dp
private val GridTileMinWidth = 180.dp
private val HeaderTopInset = 52.dp

/** The room the pane beside the overview band keeps on either side of its content. */
private val PaneLeading = 16.dp
private val PaneTrailing = 32.dp

/** The gap between the licence card's three figures, which sit together at its left edge. */
private val StatGap = 24.dp

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
 * and a test grid; an expanded window puts the overview in a band beside the resume card and a grid
 * of two or three columns, a window too short or too narrow for the desktop's measurements keeping
 * the phone layout's two.
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
        GridCells.Fixed(PHONE_COLUMNS)
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
    // A phone held sideways, or a window whose pane is too narrow for the desktop's measurements,
    // gets this layout without them; see [twoPaneColumns] and [twoPaneResumeWide].
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
        // What the grid and the resume banner have to draw in is the pane's own content width, and it
        // is what picks the shape of both: a window wide enough to be Expanded can still leave this
        // pane too narrow for three tiles; see [twoPaneColumns].
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val paneWidth = maxWidth - PaneLeading - PaneTrailing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    // No bottom inset here: the grid's viewport has to reach the window's bottom edge,
                    // or a row being scrolled is cut off 24dp above it. The breathing room below the
                    // last row is the grid's own bottom content padding instead.
                    .padding(start = PaneLeading, end = PaneTrailing, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                progress.resumable?.let { unfinished ->
                    ResumeCard(
                        unfinished,
                        onResume = { onEvent(HomeUiEvent.Resume) },
                        wide = twoPaneResumeWide(height, paneWidth),
                    )
                }
                Text(text = strings.allTests, style = Naj.type.sectionHeadingLarge, color = colors.ink)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(twoPaneColumns(height, paneWidth)),
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
                drawLicenceGlow(LicenceGlowCentre)
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
            StatsRow(finished = finished, passed = passed, average = average)
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
                style = Naj.type.heroCaption,
                color = colors.ink,
                // The caption's own last line sits on the numbers' baseline: aligned by its first
                // line, a two-line label hangs a whole line below them.
                modifier = Modifier.alignBy(LastBaseline).padding(start = 4.dp),
            )
        }
    }
}

/** Passed, failed, and the average score of the tests taken. */
@Composable
private fun StatsRow(finished: Int, passed: Int, average: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        // The three figures stay together at the card's left edge. Spread across it - a column each -
        // they read as three unrelated things, and the numbers they hold are one or two characters
        // wide, so most of the card was empty space between them.
        horizontalArrangement = Arrangement.spacedBy(StatGap),
    ) {
        Stat(label = strings.passed, value = "$passed")
        Stat(label = strings.failed, value = "${finished - passed}")
        Stat(label = strings.averageShort, value = average?.let { "$it%" } ?: "\u2013")
    }
}

/** One figure of the stats row: the number over its label. */
@Composable
private fun Stat(label: String, value: String) {
    val colors = Naj.colors
    Column {
        Text(text = value, style = Naj.type.statNumber, color = colors.ink)
        Text(text = label.uppercase(), style = Naj.type.tiny, color = colors.inkAt(0.7f))
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
    ResumeDialog(
        title = strings.resumePromptTitle,
        summary = strings.resumeSummary(strings.testName(prompt.testNumber), prompt.answered, prompt.total),
        onContinue = { onEvent(HomeUiEvent.ContinueTest) },
        onStartOver = { onEvent(HomeUiEvent.StartOver) },
        onDismiss = { onEvent(HomeUiEvent.DismissResumePrompt) },
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

/**
 * A 912 x 918dp window - a 1140px browser window at 125% scaling - which is Expanded by width and
 * tall enough for the desktop layout, yet leaves the pane beside the band too little room for three
 * tiles of [TileMinWidth]: there the status labels wrap onto a second line and are clipped, so it
 * keeps the phone's two columns and its compact resume card. The preview draws no rail, so its pane
 * is the 88dp that rail takes wider than the app's, and it still decides the same way.
 */
@Preview(widthDp = 912, heightDp = 918)
@Composable
private fun HomeScreenNarrowPreview() {
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
