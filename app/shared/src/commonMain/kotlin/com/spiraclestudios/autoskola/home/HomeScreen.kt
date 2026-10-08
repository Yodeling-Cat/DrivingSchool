package com.spiraclestudios.autoskola.home

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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.TestResult
import com.spiraclestudios.autoskola.bank.PracticeTest
import com.spiraclestudios.autoskola.data.ads.AdSlot
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.onboarding.ExamLabel
import com.spiraclestudios.autoskola.practice.IconClose
import com.spiraclestudios.autoskola.ui.AdBanner
import com.spiraclestudios.autoskola.ui.AdBannerState
import com.spiraclestudios.autoskola.ui.AdGateDialog
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.BarIconButton
import com.spiraclestudios.autoskola.ui.CardRadius
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.DashedCircleIcon
import com.spiraclestudios.autoskola.ui.EyebrowLabel
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.LicenseRadius
import com.spiraclestudios.autoskola.ui.LoadingScreen
import com.spiraclestudios.autoskola.ui.LocalHeightClass
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.LocalWidthClass
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.OutlinePill
import com.spiraclestudios.autoskola.ui.PagePadding
import com.spiraclestudios.autoskola.ui.PickerPill
import com.spiraclestudios.autoskola.ui.PillHeight
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PrimaryPill
import com.spiraclestudios.autoskola.ui.RedoTestDialog
import com.spiraclestudios.autoskola.ui.ResumeDialog
import com.spiraclestudios.autoskola.ui.ScreenHeader
import com.spiraclestudios.autoskola.ui.TestStatus
import com.spiraclestudios.autoskola.ui.TestTile
import com.spiraclestudios.autoskola.ui.TileRadius
import com.spiraclestudios.autoskola.ui.WidthClass
import com.spiraclestudios.autoskola.ui.drawLicenseGlow
import com.spiraclestudios.autoskola.ui.drawLicenseWaves
import com.spiraclestudios.autoskola.ui.fullWidthItem
import com.spiraclestudios.autoskola.ui.previewExam
import com.spiraclestudios.autoskola.ui.previewExamLong
import com.spiraclestudios.autoskola.ui.previewPassedPrompt
import com.spiraclestudios.autoskola.ui.previewProgress
import com.spiraclestudios.autoskola.ui.previewRateCard
import com.spiraclestudios.autoskola.ui.previewResults
import com.spiraclestudios.autoskola.ui.previewResumable
import com.spiraclestudios.autoskola.ui.previewResumePrompt
import com.spiraclestudios.autoskola.ui.previewTests
import com.spiraclestudios.autoskola.ui.rememberAdBannerState
import com.spiraclestudios.autoskola.ui.theme.GRAIN_CARD
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.grain
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import com.spiraclestudios.autoskola.ui.theme.rememberGrainShader
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

// ── Measurements the design draws but the component kit doesn't name ──

private val LicenseCardHeight = 222.dp

/** Where the white glow sits over the card's left edge; see [drawLicenseGlow]. */
private val LicenseGlowCenter = DpOffset(90.dp, 40.dp)

private val ResumeCardRadius = 24.dp
private val ResumeWideRadius = 28.dp
private const val RESUME_GRAIN = 0.4f

/** The orange the resume card floats on: rgba(255, 150, 0, .6). */
private val ResumeShadow = Color(0x99FF9600)

private val MistakesCardHeight = 68.dp

/** The icon tile the rating card opens with, the size of the mistakes card's badge. */
private val RateTileSize = 44.dp
private val HomeBandWidth = 410.dp
private val GridGap = 12.dp
private val SectionGap = 16.dp
private val GridTileMinWidth = 180.dp
private val HeaderTopInset = 52.dp

/**
 * What a test's tile is in the grid, so a tile scrolled off screen is reused for the next tile rather
 * than for one of the cards above them.
 */
private const val TEST_TILE = "test"

/** The home banner's key in the grid, so the sticky header is the same item from one frame to the next. */
private const val HOME_BANNER_KEY = "home-banner"

/** The strip under the stuck banner that the tests fade out beneath. */
private val StickyBannerGap = 12.dp

/** The gap between the actions, the mistakes card and the banner in the wide layout's band. */
private val BandGap = 12.dp

/** The room the pane beside the overview band keeps on either side of its content. */
private val PaneLeading = 16.dp
private val PaneTrailing = 32.dp

/** The gap between the license card's three figures, which sit together at its left edge. */
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
    state.passedPrompt?.let { PassedPromptDialog(it, onEvent) }
    state.pendingTestNumber?.let { AdGatePromptDialog(it, onEvent) }
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
    // Out here, not in the grid's item: the grid drops the item when it is scrolled off screen, and
    // the banner would go with it - a new request, an empty gap and a jump every time it came back.
    val banner = rememberAdBannerState(AdSlot.HomeBanner)
    val grid = rememberLazyGridState()
    // Stuck once the grid has scrolled past the banner's own place, so the tests are passing under it.
    val bannerStuck by remember {
        derivedStateOf {
            val item = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == HOME_BANNER_KEY }
                ?: return@derivedStateOf false
            grid.firstVisibleItemIndex > item.index ||
                (grid.firstVisibleItemIndex == item.index && grid.firstVisibleItemScrollOffset > 0)
        }
    }

    LazyVerticalGrid(
        columns = columns,
        state = grid,
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
            LicenseCard(totalTests = tests.size, progress = progress)
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
        // Under the mistakes card, in space the screen was already leaving empty: the place the plan
        // argues for, because nothing above or beside it is where a thumb is already going. A sticky
        // header, so it scrolls up with the cards and then stays at the top of the screen while the
        // tests scroll beneath it.
        if (state.showAds) {
            stickyHeader(key = HOME_BANNER_KEY) { StickyBanner(banner, stuck = bannerStuck) }
        }
        state.rateCard?.let { card ->
            fullWidthItem {
                RateCard(
                    card = card,
                    onRate = { onEvent(HomeUiEvent.RateApp) },
                    onDismiss = { onEvent(HomeUiEvent.DismissRateCard) },
                )
            }
        }
        fullWidthItem {
            Text(
                text = strings.allTests,
                style = Naj.type.sectionHeading,
                color = Naj.colors.ink,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        items(tests, key = { it.number }, contentType = { TEST_TILE }) { test ->
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
    // Remembered here rather than where the banner is drawn: it moves between two places in the band,
    // and a banner remembered at one of them would be a new ad request every time it moved.
    val banner = rememberAdBannerState(AdSlot.HomeBanner)
    // What decides between the two places: how tall the band's window is, where the banner ends in the
    // band's content, and how much room the mistakes card takes. See [bannerFitsUnderMistakes].
    var viewportPx by remember { mutableIntStateOf(0) }
    var bandContent by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bannerBottomPx by remember { mutableIntStateOf(0) }
    var mistakesHeightPx by remember { mutableIntStateOf(0) }
    var bannerAboveMistakes by remember { mutableStateOf(false) }
    val bandGapPx = with(LocalDensity.current) { BandGap.roundToPx() }
    LaunchedEffect(viewportPx, bannerBottomPx, mistakesHeightPx) {
        if (viewportPx > 0 && bannerBottomPx > 0) {
            bannerAboveMistakes = !bannerFitsUnderMistakes(
                bannerBottomPx = bannerBottomPx,
                aboveMistakes = bannerAboveMistakes,
                mistakesHeightPx = mistakesHeightPx,
                gapPx = bandGapPx,
                viewportPx = viewportPx,
            )
        }
    }
    val bannerSlot = @Composable {
        if (state.showAds) {
            AdBanner(
                banner,
                Modifier.onGloballyPositioned { coordinates ->
                    val content = bandContent ?: return@onGloballyPositioned
                    if (!content.isAttached) return@onGloballyPositioned
                    val top = content.localPositionOf(coordinates, Offset.Zero).y
                    bannerBottomPx = (top + coordinates.size.height).roundToInt()
                },
            )
        }
    }

    Row(Modifier.widthIn(max = ContentWidth.Wide).fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(HomeBandWidth)
                .fillMaxHeight()
                .onSizeChanged { viewportPx = it.height }
                .verticalScroll(rememberScrollState())
                // The scrolled content, so a position measured against it does not move with scrolling.
                .onPlaced { bandContent = it }
                .padding(horizontal = 24.dp, vertical = 36.dp),
        ) {
            ScreenHeader(trailing = { ExamPill(state.exam, onChangeExam) })
            Spacer(Modifier.height(20.dp))
            LicenseCard(totalTests = tests.size, progress = progress)
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(BandGap)) {
                ActionRow(
                    tests = tests,
                    notPassedEnabled = state.hasNotPassedTests,
                    onEvent = onEvent,
                    stacked = true,
                )
                // This layout gets the banner too, under the same card - or above it where under it the
                // banner would be cut off at the bottom of the window, as on a 7-inch tablet on its side.
                // The two are the same screen drawn for different windows, so a placement in one is a
                // placement in both.
                if (bannerAboveMistakes) bannerSlot()
                MistakesCard(
                    count = progress.mistakeCount,
                    onClick = { onEvent(HomeUiEvent.PracticeMistakes) },
                    modifier = Modifier.onSizeChanged { mistakesHeightPx = it.height },
                )
                if (!bannerAboveMistakes) bannerSlot()
                state.rateCard?.let { card ->
                    RateCard(
                        card = card,
                        onRate = { onEvent(HomeUiEvent.RateApp) },
                        onDismiss = { onEvent(HomeUiEvent.DismissRateCard) },
                    )
                }
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
                    items(tests, key = { it.number }, contentType = { TEST_TILE }) { test ->
                        TestTileItem(test = test, progress = progress, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

/**
 * The home banner as the grid's sticky header, with a strip under it that the tests fade out beneath.
 *
 * The strip is there for the taps as much as for the look: stuck to the top, the banner would otherwise
 * sit flush against the row of tiles scrolling under it, and a tile edge right against an ad is where
 * accidental clicks come from. The strip takes a press itself, so a tap just under the ad opens neither
 * the ad nor a test.
 *
 * It is drawn only while the banner is [stuck] with an ad in it: an empty slot still loading has nothing
 * framing it, and in its own place under the mistakes card there is nothing passing under it to fade -
 * a flat strip over the grainy page would only show as a band. It hangs below the banner rather than
 * taking room of its own, so the gap to the tests' heading is the grid's own and appearing moves nothing.
 */
@Composable
private fun StickyBanner(banner: AdBannerState, stuck: Boolean) {
    val paper = Naj.colors.paper
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            AdBanner(banner)
            if (stuck && banner.showsAd) {
                Box(
                    Modifier
                        .background(Brush.verticalGradient(listOf(paper, paper.copy(alpha = 0f))))
                        .pointerInput(Unit) {},
                )
            }
        },
    ) { measurables, constraints ->
        val ad = measurables[0].measure(constraints)
        val strip = measurables.getOrNull(1)?.measure(
            Constraints.fixed(ad.width, StickyBannerGap.roundToPx()),
        )
        layout(ad.width, ad.height) {
            ad.place(0, 0)
            strip?.place(0, ad.height)
        }
    }
}

/**
 * Whether the wide layout's banner fits on screen whole under the mistakes card, from where it ends now:
 * [bannerBottomPx] in the band's content, measured [aboveMistakes] the card or under it. Measured above
 * it, it is judged where it would end under it - lower by the card and the gap - so the answer is the
 * same in both places and the banner does not jump back and forth between them.
 */
internal fun bannerFitsUnderMistakes(
    bannerBottomPx: Int,
    aboveMistakes: Boolean,
    mistakesHeightPx: Int,
    gapPx: Int,
    viewportPx: Int,
): Boolean {
    val bottomUnderMistakes = if (aboveMistakes) bannerBottomPx + mistakesHeightPx + gapPx else bannerBottomPx
    return bottomUnderMistakes <= viewportPx
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

/** The license gradient card: its waves, the finished-tests hero, and the four stats under it. */
@Composable
private fun LicenseCard(totalTests: Int, progress: HomeProgress, modifier: Modifier = Modifier) {
    val colors = Naj.colors
    val shader = rememberGrainShader()
    val shape = RoundedCornerShape(LicenseRadius)
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
            .height(LicenseCardHeight)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = colors.ink.copy(alpha = 0.55f),
                spotColor = colors.ink.copy(alpha = 0.55f),
            )
            .clip(shape)
            .background(Brush.linearGradient(colors.licenseGradient)),
    ) {
        // The waves, the glow over the left edge and the grain all sit behind the card's content,
        // the way the design stacks them.
        Box(Modifier.fillMaxSize().grain(GRAIN_CARD, shader)) {
            Canvas(Modifier.fillMaxSize()) {
                drawLicenseWaves()
                drawLicenseGlow(LicenseGlowCenter)
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
    val summary = strings.resumeSummary(test.answered, test.total)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = ResumeShadow, spotColor = ResumeShadow)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.resumeTop, colors.resumeBottom)))
            .clickable(onClick = onResume),
    ) {
        // matchParentSize, not fillMaxSize: the grain and the hairline cover whatever the card turns
        // out to be without asking for a height of their own, so the row below is what sizes it. A
        // fillMaxSize child would fill the bounded height of the pane it sits in and drag the card
        // down the whole window with it.
        Box(Modifier.matchParentSize().grain(RESUME_GRAIN, shader)) { TopHighlight() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                EyebrowLabel(
                    text = strings.unfinishedTest,
                    style = Naj.type.letterSpacedSmall,
                    color = colors.resumeOn,
                )
                // The bar below is only as wide as the summary itself, so the two read as one block
                // rather than the bar running past the words it belongs to. The summary is measured
                // as it is drawn, so a wrapped line keeps the bar with the text rather than the
                // column it wraps in.
                var summaryWidth by remember { mutableStateOf(0) }
                Text(
                    text = summary,
                    style = if (wide) Naj.type.cardHero else Naj.type.cardHeroSmall,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 2.dp),
                    onTextLayout = { summaryWidth = it.size.width },
                )
                if (!wide) {
                    ProgressBar(
                        fraction = if (test.total == 0) 0f else test.answered.toFloat() / test.total,
                        height = 5.dp,
                        trackColor = colors.inkAt(0.15f),
                        fillColor = colors.ink,
                        modifier = Modifier
                            .width(with(LocalDensity.current) { summaryWidth.toDp() })
                            .padding(top = 8.dp),
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

/** The mistakes the user can practice: inert and grayed out when there are none. */
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

/**
 * The card asking the user to rate the app in the store it names, which sits under the mistakes
 * card: the button opens the app's page there, and the cross at its top right puts it away without
 * leaving the app. Neither answer is the user's only chance - the view model holds the ladder the
 * asking climbs - so the card carries no explanation of what closing it means.
 */
@Composable
private fun RateCard(
    card: RateCard,
    onRate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors

    NajCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The star and its yellow: the same warm tile the resume card is cut from, because a
            // rating is what the driver is being asked for rather than what they owe.
            IconTile(
                size = RateTileSize,
                background = Brush.linearGradient(listOf(colors.resumeTop, colors.resumeBottom)),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = colors.resumeOn,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(text = strings.rateCardTitle, style = Naj.type.cardTitle, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                Text(text = strings.rateCardMessage(card.storeName), style = Naj.type.rowSubtitle, color = colors.muted)
            }
            BarIconButton(
                IconClose,
                contentDescription = strings.rateCardDismiss,
                onClick = onDismiss,
                tint = colors.muted,
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryPill(
            text = strings.rateCardAction,
            onClick = onRate,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
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
    // A finished test summarizes itself; the other two say why they carry no score.
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
        summary = strings.resumeSummary(prompt.answered, prompt.total),
        onContinue = { onEvent(HomeUiEvent.ContinueTest) },
        onStartOver = { onEvent(HomeUiEvent.StartOver) },
        onDismiss = { onEvent(HomeUiEvent.DismissResumePrompt) },
    )
}

/** Asks whether to take a test that's already passed again, giving up the score it has. */
@Composable
private fun PassedPromptDialog(prompt: PassedPrompt, onEvent: (HomeUiEvent) -> Unit) {
    RedoTestDialog(
        title = strings.passedPromptTitle,
        message = strings.passedPromptMessage(prompt.score, prompt.maxScore),
        onRedo = { onEvent(HomeUiEvent.RedoTest) },
        onDismiss = { onEvent(HomeUiEvent.DismissPassedPrompt) },
    )
}

/**
 * The every-tenth-test gate, holding the test [testNumber] names. It is drawn by the screen rather
 * than navigated to, because it is a question about the screen the user is already on.
 */
@Composable
private fun AdGatePromptDialog(testNumber: Int, onEvent: (HomeUiEvent) -> Unit) {
    AdGateDialog(
        testNumber = testNumber,
        onWatchAd = { onEvent(HomeUiEvent.WatchAd) },
        onDismiss = { onEvent(HomeUiEvent.DismissAdGate) },
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

/** The hairline of white along the top of a colored card, which the design draws as an inset. */
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
    rateCard: RateCard? = null,
    resumePrompt: ResumePrompt? = null,
    passedPrompt: PassedPrompt? = null,
    exam: ExamLabel = previewExam,
    showAds: Boolean = false,
): HomeUiState.Ready {
    val tests = previewTests()
    return HomeUiState.Ready(
        exam = exam,
        tests = tests,
        progress = previewProgress(results, inProgress, resumable, mistakeCount, questionsAnswered),
        hasNotPassedTests = notPassedTests(tests, results).isNotEmpty(),
        rateCard = rateCard,
        resumePrompt = resumePrompt,
        passedPrompt = passedPrompt,
        showAds = showAds,
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

/**
 * The same layout with the banner drawn, which is what a free user's home screen looks like once
 * they have agreed to ads. Nothing else about the screen changes: the card sits in space that was
 * already blank.
 */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenBannerPreview() {
    AppPreview {
        HomeScreen(homeState(showAds = true), onEvent = {}, onChangeExam = {})
    }
}

/**
 * The same phone layout in an exam whose name the pill can't hold at this width: the wordmark keeps
 * its one line and the pill gives way in the middle, so the category it starts with and the
 * language it ends with both stay readable.
 */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenLongExamNamePreview() {
    AppPreview {
        HomeScreen(homeState(exam = previewExamLong), onEvent = {}, onChangeExam = {})
    }
}

/** The first launch: nothing done, nothing under way, and no mistakes to practice. */
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

/** What it asks when the picked test has already been passed: redo it, or keep that score. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenPassedPromptPreview() {
    AppPreview {
        HomeScreen(homeState(passedPrompt = previewPassedPrompt()), onEvent = {}, onChangeExam = {})
    }
}

/** The phone layout with the card asking for a store rating, under the mistakes card. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenRateCardPreview() {
    AppPreview {
        HomeScreen(homeState(rateCard = previewRateCard()), onEvent = {}, onChangeExam = {})
    }
}

/** The desktop layout, where the card sits in the band under the mistakes card as well. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun HomeScreenRateCardExpandedPreview() {
    AppPreview {
        HomeScreen(homeState(rateCard = previewRateCard()), onEvent = {}, onChangeExam = {})
    }
}

/** The card itself, in each of the two stores it can name. */
@Preview(widthDp = 400, heightDp = 330)
@Composable
private fun RateCardPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("PLAY STORE")
            RateCard(previewRateCard(), onRate = {}, onDismiss = {})
            PreviewLabel("APP STORE")
            RateCard(previewRateCard("App Store"), onRate = {}, onDismiss = {})
        }
    }
}
