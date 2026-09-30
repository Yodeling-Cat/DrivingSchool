package uno.lux.autoskola.practice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uno.lux.autoskola.bank.Question
import uno.lux.autoskola.bank.Section
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.BarIconButton
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.HeightClass
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalHeightClass
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.NajCard
import uno.lux.autoskola.ui.NajSnackbar
import uno.lux.autoskola.ui.NavyPill
import uno.lux.autoskola.ui.QuestionChip
import uno.lux.autoskola.ui.RoundButton
import uno.lux.autoskola.ui.WhitePill
import uno.lux.autoskola.ui.WidthClass
import uno.lux.autoskola.ui.contentGutter
import uno.lux.autoskola.ui.displayText
import uno.lux.autoskola.ui.theme.AccentedText
import uno.lux.autoskola.ui.theme.Naj
import uno.lux.autoskola.ui.theme.accentNumber
import uno.lux.autoskola.ui.theme.paperBackground
import uno.lux.autoskola.ui.theme.paperGlows

/** Over the page behind an enlarged picture, which is drawn on top of it. */
private val EnlargedImageScrim = Color(0xB3000000)

private val QuestionPanelWidth = 360.dp

/** How far an enlarged picture stays away from the edges of the screen it's shown on. */
private val EnlargedImageMargin = 24.dp

/** The width an arrow beside the question needs, so it sits clear of the text. */
private val QuestionArrowSize = 56.dp

/** The bar pill and the room the sheet keeps around it. */
private val BarPillHeight = 68.dp
private val BarPillGap = 8.dp
private val BarPillHeightCompact = 56.dp
private val BarPillGapCompact = 4.dp

/** The width of the question column on a wide screen, with the arrows beside it. */
private val QuestionColumnWidth = 660.dp

/**
 * The shadow the bar throws on the question behind it. The sheet and the page are both light, so
 * the shadow is the only thing separating them — the Material default is barely visible.
 */
private val QuestionSheetShadow = 24.dp

/** Radius of the top corners of the question list's surface. */
private val SheetRadius = 32.dp

/**
 * Connects [PracticeScreen] to its [PracticeViewModel]. Calls [onClose] once the attempt is
 * closed, and [onOpenResults] when the test is finished or the user asks for its results.
 */
@Composable
fun PracticeRoute(
    attemptId: String,
    onClose: () -> Unit,
    onOpenResults: () -> Unit,
    viewModel: PracticeViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The clock runs while the test is in front of the user and stops when it isn't, so the time a
    // test shows is the time the user spent on it.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(PracticeUiEvent.Foreground(true))
        onPauseOrDispose { viewModel.onEvent(PracticeUiEvent.Foreground(false)) }
    }
    when (val current = state) {
        PracticeUiState.Loading -> LoadingScreen()

        PracticeUiState.Closed -> LaunchedEffect(Unit) { onClose() }

        is PracticeUiState.Ready -> {
            LaunchedEffect(current.openResults) {
                if (current.openResults) {
                    onOpenResults()
                    viewModel.onEvent(PracticeUiEvent.ResultsOpened)
                }
            }
            PracticeScreen(current, viewModel::onEvent, onBack = onClose, onOpenResults = onOpenResults)
        }
    }
}

/**
 * Runs one practice test. With [checkImmediately], picking an answer reveals whether it's
 * right (and which one is) and locks it; otherwise answers stay editable and are only
 * scored when the test is finished.
 *
 * Answers come from [state]; every pick is sent as [PracticeUiEvent.Answer] so it's saved
 * straight away, and the test moves on to the next question when the user asked for that (see
 * [PracticeUiState.Ready.advanceToPage]). Once every question is answered, a snackbar offers to
 * finish the test; once it's finished, every answer is revealed and the snackbar leads back to the
 * results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    state: PracticeUiState.Ready,
    onEvent: (PracticeUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenResults: () -> Unit,
) {
    val colors = Naj.colors
    val questions = remember(state.questions) { state.questions.map { it.question } }
    val sections = state.sections
    val testName = attemptName(state.kind, state.testNumber)
    val elapsed = elapsedClock(state.elapsedSeconds)
    val overTime = isOverTime(state.elapsedSeconds, state.allottedSeconds)
    val checkImmediately = state.checkImmediately
    val finished = state.finished
    val revealCorrectness = revealsCorrectness(checkImmediately, finished)
    val selectedAnswers = state.answers
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { questions.size })
    val currentIndex = pagerState.currentPage
    val sheetState = rememberStandardBottomSheetState(skipHiddenState = true)
    // The same bar on a tall window, where the list has a panel of its own: nothing expands or
    // drags this sheet, it's the bar with a shadow under it.
    val pinnedSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
        confirmValueChange = { it == SheetValue.PartiallyExpanded },
    )
    val scope = rememberCoroutineScope()
    val current = questions[currentIndex]
    // On wide screens the question list is always visible next to the question.
    // Phones in landscape: one slim bar instead of two, and no side panel, so the question keeps
    // most of the little height there is.
    val compactHeight = LocalHeightClass.current == HeightClass.Compact
    val showPanel = LocalWidthClass.current == WidthClass.Expanded && !compactHeight
    // The question list is pinned to the bottom and covers the questions once it's open. Where the
    // list has its own panel it never covers anything, so it can't be open or closed either.
    val listExpanded = !showPanel && sheetState.targetValue == SheetValue.Expanded

    /** Set while we're asking whether the user really wants to leave an unfinished test. */
    var confirmingLeave by remember { mutableStateOf(false) }

    /** Back closes the question list first, and asks before dropping an unfinished test. */
    fun handleBack() {
        when (backAction(listExpanded, finished)) {
            BackAction.CloseQuestionList -> scope.launch { sheetState.partialExpand() }
            BackAction.Leave -> onBack()
            BackAction.ConfirmLeave -> confirmingLeave = true
        }
    }

    // A finished test with its list closed is left by the navigation library's own handler, so
    // that path keeps its back animation and, on Android, its predictive gesture.
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = backAction(listExpanded, finished) != BackAction.Leave,
        onBackCompleted = ::handleBack,
    )
    // Resizing the window (desktop, split screen, rotation) mid-scroll can leave the pager
    // between pages, so re-align it to the page it was heading to whenever its width changes.
    var pagerWidth by remember { mutableStateOf(0) }
    LaunchedEffect(pagerWidth) {
        if (pagerWidth > 0) pagerState.scrollToPage(pagerState.targetPage)
    }

    // The arrows go beside the question whenever the page leaves gutters wide enough for them. On a
    // phone there is no room, so the bar keeps them. The arrows are drawn on top of the page, so
    // showing them doesn't change its width and the decision can't flip back and forth.
    val columnCap = if (compactHeight) ContentWidth.Wide else ContentWidth.Reading
    val gutter = contentGutter(available = with(LocalDensity.current) { pagerWidth.toDp() }, column = columnCap)
    val arrowsBeside = gutter >= QuestionArrowSize

    fun goTo(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index.coerceIn(0, questions.lastIndex)) }
    }

    // Auto-advance: the ViewModel decides when answering moves the test on and asks for the page
    // here. Reporting the move back is what lets the same question ask for it again later.
    LaunchedEffect(state.advanceToPage) {
        val target = state.advanceToPage ?: return@LaunchedEffect
        goTo(target)
        onEvent(PracticeUiEvent.PageAdvanced)
    }

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pillHeight = if (compactHeight) BarPillHeightCompact else BarPillHeight
    val pillGap = if (compactHeight) BarPillGapCompact else BarPillGap

    /** The bar's place in the sheet: the pill plus the room it floats in. */
    val barSlotHeight = pillHeight + pillGap * 2

    /** How far a question's content has to clear the pinned bar to be readable to its last line. */
    val questionBottomRoom = barSlotHeight + navBarInset + 16.dp
    // The open sheet covers the top app bar, so it only has to stay below the status bar. Phones in
    // landscape are inset below the status bar by the box the sheet lives in instead.
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topReserved = if (compactHeight) 0.dp else statusBarInset
    val sideInsets =
        WindowInsetsSides.Horizontal + if (compactHeight) WindowInsetsSides.Top else WindowInsetsSides.Horizontal

    // Stays up until acted on: first to finish the test, then to get back to its results.
    val snackbarHostState = remember { SnackbarHostState() }
    OverTimeNoticeEffect(
        show = state.showTimeLimitToast,
        message = strings.overTime((state.allottedSeconds / 60).toInt()),
        snackbarHostState = snackbarHostState,
        onShown = { onEvent(PracticeUiEvent.TimeLimitToastShown) },
    )
    FinishPromptEffect(
        prompt = finishPrompt(state.allAnswered, finished),
        snackbarHostState = snackbarHostState,
        onFinish = { onEvent(PracticeUiEvent.Finish) },
        onShowResults = onOpenResults,
    )

    // Phones in landscape hide the top bar: the bottom bar already names the test there.
    val topBar: @Composable () -> Unit = {
        if (!compactHeight) {
            PracticeTopBar(
                testName = testName,
                elapsed = elapsed,
                overTime = overTime,
                allAnswered = state.allAnswered,
                checkImmediately = checkImmediately,
                finished = finished,
                questionNumber = currentIndex + 1,
                questionCount = questions.size,
                onBack = ::handleBack,
            )
        }
    }

    /** [arrowAlpha] fades the arrows out as the sheet covers the questions. */
    val barContent: @Composable RowScope.(
        listExpanded: Boolean,
        onToggleList: () -> Unit,
        arrowAlpha: () -> Float,
    ) -> Unit = { listExpanded, onToggleList, arrowAlpha ->
        PracticeBarContent(
            compactHeight = compactHeight,
            testName = testName,
            elapsed = elapsed,
            currentIndex = currentIndex,
            questionCount = questions.size,
            points = current.points,
            overTime = overTime,
            showListToggle = !showPanel,
            listExpanded = listExpanded,
            canFinish = !finished,
            showArrows = !arrowsBeside,
            onBack = ::handleBack,
            onToggleList = onToggleList,
            onPrevious = { goTo(currentIndex - 1) },
            onNext = { goTo(currentIndex + 1) },
            arrowAlpha = arrowAlpha,
            onFinish = { onEvent(PracticeUiEvent.Finish) },
        )
    }

    /** The arrows for a page wide enough to hold them beside the question. */
    val besideArrows: @Composable BoxScope.() -> Unit = {
        QuestionArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = strings.previousQuestion,
            enabled = currentIndex > 0,
            onClick = { goTo(currentIndex - 1) },
            modifier = Modifier.align(Alignment.CenterStart).width(gutter).fillMaxHeight(),
        )
        QuestionArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = strings.nextQuestion,
            enabled = currentIndex < questions.size - 1,
            onClick = { goTo(currentIndex + 1) },
            modifier = Modifier.align(Alignment.CenterEnd).width(gutter).fillMaxHeight(),
        )
    }

    /** The questions themselves: the pager, with the arrows beside it where there's room. */
    val questionsPane: @Composable (PaddingValues) -> Unit = { padding ->
        // Only the top bar's room is taken out of the page: the questions run on behind the pinned
        // bar, and each page keeps its last answer clear of it with [questionBottomRoom].
        Row(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            // Compose's scrollable ignores drags made with a mouse (its own canDrag says so), so a
            // page is dragged by hand here. A touch drag never reaches this: the pager consumes it
            // first, being the closer of the two.
            val dragged = remember { mutableFloatStateOf(0f) }
            val dragState = rememberDraggableState { delta ->
                dragged.floatValue += delta
                scope.launch { pagerState.dispatchRawDelta(-delta) }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { dragged.floatValue = 0f },
                        onDragStopped = { velocity ->
                            val target = swipedPage(
                                currentPage = pagerState.currentPage,
                                dragged = dragged.floatValue,
                                pageSize = pagerWidth.toFloat(),
                                velocity = velocity,
                                pageCount = questions.size,
                            )
                            pagerState.animateScrollToPage(target)
                        },
                    ),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { pagerWidth = it.width },
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    QuestionPage(
                        question = questions[page],
                        imageUri = state.questions[page].imageUri,
                        selectedIndex = selectedAnswers[page],
                        locked = isAnswerLocked(checkImmediately, finished, selectedAnswers[page]),
                        skipped = questionSkipped(selectedAnswers[page], finished),
                        bottomRoom = questionBottomRoom,
                        onSelect = { onEvent(PracticeUiEvent.Answer(page, it)) },
                    )
                }
                if (arrowsBeside) besideArrows()
            }
        }
    }

    /** The question list, which has a panel of its own beside the question on a wide screen. */
    val questionPanel: @Composable () -> Unit = {
        Column(
            Modifier
                .width(QuestionPanelWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.sheet)
                .verticalScroll(rememberScrollState()),
        ) {
            QuestionGrid(
                questions = questions,
                sections = sections,
                currentIndex = currentIndex,
                selectedAnswers = selectedAnswers,
                revealCorrectness = revealCorrectness,
                finished = finished,
                onSelectQuestion = ::goTo,
            )
            // The list runs to the bottom of the window; the navigation bar's room is the last row.
            Spacer(Modifier.height(navBarInset))
        }
    }

    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors, mintY = 0.7f))) {
        if (showPanel) {
            // The bar is the bottom sheet a phone shows, over the questions alone: the list has a
            // panel of its own here, and the sheet's width is the question column's, so it lines up
            // with the question and the answers instead of running on under the list. It is pinned
            // — nothing drags or expands it — and its shadow is what separates it from the page.
            Column(
                Modifier
                    .fillMaxSize()
                    // The sheet's peek already makes room for the navigation bar, as on a phone.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                // The app bar stays over both panes, so it sits outside the sheet's scaffold.
                topBar()
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BottomSheetScaffold(
                        modifier = Modifier.weight(1f),
                        scaffoldState = rememberBottomSheetScaffoldState(pinnedSheetState, snackbarHostState),
                        containerColor = Color.Transparent,
                        sheetContainerColor = Color.Transparent,
                        sheetPeekHeight = barSlotHeight + navBarInset,
                        // The reading column's width: what holds the question and its answers.
                        sheetMaxWidth = QuestionColumnWidth,
                        sheetShadowElevation = 0.dp,
                        sheetDragHandle = null,
                        sheetSwipeEnabled = false,
                        sheetContent = {
                            // The list is always visible next to the question here, so the toggle is
                            // inert and the arrows never fade.
                            BarPill(pillHeight, pillGap) { barContent(false, {}, { 1f }) }
                        },
                        content = questionsPane,
                    )
                    Box(Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp)) { questionPanel() }
                }
            }
        } else {
            // The question list is a sheet pinned to the bottom: collapsed it shows just the bar. Its
            // content has a fixed height — a bar over a list tall enough to reach the status bar — so
            // the sheet's anchors don't move under the user's finger. Sizing or fading content from the
            // live drag offset makes the gesture fight its own layout, which is how taps and list
            // scrolls got eaten after a drag.
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    // The scaffold doesn't inset its content; the sheet stays below the status bar by
                    // its height instead.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(sideInsets)),
            ) {
                // The bar is as tall as the collapsed peek, and the list runs up to the status bar. The
                // navigation bar's room is at the end of the list, so it takes no height away from it.
                val gridHeight = (maxHeight - topReserved - barSlotHeight - navBarInset).coerceAtLeast(0.dp)
                val arrowsHidden by animateFloatAsState(if (listExpanded) 1f else 0f)

                fun toggleList() {
                    scope.launch { if (listExpanded) sheetState.partialExpand() else sheetState.expand() }
                }

                BottomSheetScaffold(
                    scaffoldState = rememberBottomSheetScaffoldState(sheetState, snackbarHostState),
                    containerColor = Color.Transparent,
                    // Closed, the pill floats straight over the page; open, it sits on the warm
                    // sheet the question list is drawn on.
                    sheetContainerColor = if (listExpanded) colors.sheet else Color.Transparent,
                    sheetPeekHeight = barSlotHeight + navBarInset,
                    sheetShadowElevation = QuestionSheetShadow,
                    sheetDragHandle = null,
                    topBar = topBar,
                    sheetContent = {
                        Column(Modifier.fillMaxWidth()) {
                            BarPill(pillHeight, pillGap) {
                                barContent(listExpanded, ::toggleList, { 1f - arrowsHidden })
                            }
                            Column(
                                Modifier
                                    .height(gridHeight)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                                    .background(colors.sheet)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                QuestionGrid(
                                    questions = questions,
                                    sections = sections,
                                    currentIndex = currentIndex,
                                    selectedAnswers = selectedAnswers,
                                    revealCorrectness = revealCorrectness,
                                    finished = finished,
                                    onSelectQuestion = {
                                        goTo(it)
                                        scope.launch { sheetState.partialExpand() }
                                    },
                                )
                                Spacer(Modifier.height(navBarInset))
                            }
                        }
                    },
                    content = questionsPane,
                )
            }
        }
        // The message bar sits above the pill rather than on top of it, the way the design stacks
        // them. It is not the scaffold's own host for that reason.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = barSlotHeight + navBarInset + 12.dp),
        ) { data -> NajSnackbar(data) }
    }

    if (confirmingLeave) {
        LeaveTestDialog(
            onDismiss = { confirmingLeave = false },
            onConfirm = {
                confirmingLeave = false
                onBack()
            },
        )
    }
}

/**
 * Tells the user, once, that the test has had the time a test is given. The notice waits its turn
 * on the snackbar host, so it never talks over the prompt that offers to finish the test.
 */
@Composable
private fun OverTimeNoticeEffect(
    show: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    LaunchedEffect(show, message) {
        if (!show) return@LaunchedEffect
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
        onShown()
    }
}

/**
 * Keeps the prompt that offers to finish the test - and later to show its results - up until the
 * user acts on it.
 */
@Composable
private fun FinishPromptEffect(
    prompt: FinishPrompt?,
    snackbarHostState: SnackbarHostState,
    onFinish: () -> Unit,
    onShowResults: () -> Unit,
) {
    val message = when (prompt) {
        FinishPrompt.Finish -> strings.allQuestionsAnswered
        FinishPrompt.ShowResults -> strings.testFinished
        null -> null
    }
    val action = when (prompt) {
        FinishPrompt.Finish -> strings.finish
        FinishPrompt.ShowResults -> strings.showResults
        null -> null
    }
    LaunchedEffect(prompt, message) {
        snackbarHostState.currentSnackbarData?.dismiss()
        if (prompt == null || message == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, action, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) {
            when (prompt) {
                FinishPrompt.Finish -> onFinish()
                FinishPrompt.ShowResults -> onShowResults()
            }
        }
    }
}

/** The colour of the test's clock: over time it warns the way a wrong answer does. */
@Composable
private fun elapsedColor(overTime: Boolean): Color = if (overTime) Naj.colors.wrongOn else Naj.colors.muted

/**
 * The question page's header: the way back, the test's name with its number in the coral italic,
 * the clock, and how far through the test is.
 */
@Composable
private fun PracticeTopBar(
    testName: String,
    elapsed: String,
    overTime: Boolean,
    allAnswered: Boolean,
    checkImmediately: Boolean,
    finished: Boolean,
    questionNumber: Int,
    questionCount: Int,
    onBack: () -> Unit,
) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
            AccentedText(
                accentNumber(testName),
                style = Naj.type.barTitle,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            val pill = timerPill(
                allAnswered = allAnswered,
                checkImmediately = checkImmediately,
                finished = finished,
                overTime = overTime,
            )
            val pillBackground = when (pill) {
                TimerPill.Running -> colors.card
                TimerPill.InTime -> colors.passedBottom
                TimerPill.OverTime -> colors.wrongBottom
            }
            val pillContent = when (pill) {
                TimerPill.Running -> elapsedColor(overTime)
                TimerPill.InTime -> colors.passedOn
                TimerPill.OverTime -> colors.wrongOn
            }
            WhitePill(height = 36.dp, tint = pillBackground) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint = if (pill == TimerPill.Running) colors.primaryDeep else pillContent,
                    modifier = Modifier.size(16.dp),
                )
                Text(elapsed, style = Naj.type.tileStatus, color = pillContent)
            }
        }
        Spacer(Modifier.height(18.dp))
        QuestionProgress(questionNumber, questionCount)
    }
}

/** How far through the test the user is, in the teal gradient the design uses for progress. */
@Composable
private fun QuestionProgress(questionNumber: Int, questionCount: Int) {
    val colors = Naj.colors
    val fraction = if (questionCount == 0) 0f else questionNumber.toFloat() / questionCount
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.inkAt(0.1f))) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(colors.primaryGradient)),
        )
    }
}

/**
 * One of the two arrows beside the question, in the gutter the reading column leaves on a page
 * wide enough to have one. [modifier] is the whole gutter, so the button sits centred in it.
 */
@Composable
private fun QuestionArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.card)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = colors.ink.copy(alpha = if (enabled) 1f else 0.3f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** Asks whether to leave an unfinished test, whose answers would be lost. */
@Composable
private fun LeaveTestDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Naj.colors.card,
        shape = RoundedCornerShape(22.dp),
        title = { Text(strings.leaveTestTitle, style = Naj.type.cardTitle, color = Naj.colors.ink) },
        text = { Text(strings.leaveTestMessage, style = Naj.type.option, color = Naj.colors.muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.leaveTest, style = Naj.type.button, color = Naj.colors.wrongOn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.keepTesting, style = Naj.type.button, color = Naj.colors.primaryDeep)
            }
        },
    )
}

/** The navy pill the test's controls live in, with the room the page keeps around it. */
@Composable
private fun BarPill(height: Dp, gap: Dp, content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = gap)) {
        NavyPill(Modifier.fillMaxWidth(), height = height, arrangement = Arrangement.Start, content = content)
    }
}

@Composable
private fun RowScope.PracticeBarContent(
    compactHeight: Boolean,
    testName: String,
    elapsed: String,
    currentIndex: Int,
    questionCount: Int,
    points: Int,
    overTime: Boolean,
    showListToggle: Boolean,
    listExpanded: Boolean,
    canFinish: Boolean,
    /** The arrows sit beside the question instead, so the bar leaves them out. */
    showArrows: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    arrowAlpha: () -> Float,
    onFinish: () -> Unit,
) {
    val colors = Naj.colors
    var showOverflowMenu by remember { mutableStateOf(false) }
    if (compactHeight) {
        BarIconButton(IconArrowBack, strings.back, onClick = onBack)
    }
    if (showListToggle) {
        QuestionListToggle(expanded = listExpanded, onClick = onToggleList)
    }
    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
        Text(
            if (compactHeight) {
                "$testName \\u00b7 ${strings.questionOutOf(currentIndex + 1, questionCount)}"
            } else {
                strings.questionOutOf(currentIndex + 1, questionCount)
            },
            style = Naj.type.barNumber,
            color = Color.White,
            maxLines = 1,
        )
        // Phones in landscape hide the top bar, so the time runs in the bar's second line there,
        // and only the time itself turns red when the test is over it.
        if (compactHeight) {
            Row {
                Text(
                    "${strings.points(points)} \\u00b7 ",
                    style = Naj.type.tiny,
                    color = colors.roadYellow,
                    maxLines = 1,
                )
                Text(
                    elapsed,
                    style = Naj.type.tiny,
                    color = if (overTime) colors.wrongBottom else colors.roadYellow,
                    maxLines = 1,
                )
            }
        } else {
            Text(strings.points(points), style = Naj.type.tiny, color = colors.roadYellow, maxLines = 1)
        }
    }
    // The arrows are beside the question wherever the page has room for them. They make no sense
    // while the question list covers the questions, which is what [arrowAlpha] is for.
    if (showArrows) {
        Row(Modifier.graphicsLayer { alpha = arrowAlpha() }) {
            val usable = arrowAlpha() > 0.5f
            BarIconButton(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                strings.previousQuestion,
                onClick = onPrevious,
                enabled = usable && currentIndex > 0,
            )
            BarIconButton(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                strings.nextQuestion,
                onClick = onNext,
                enabled = usable && currentIndex < questionCount - 1,
            )
        }
    }
    // Finishing is the only option, so there's no menu once the test is finished.
    if (canFinish) {
        Box {
            BarIconButton(IconMoreVert, strings.moreOptions, onClick = { showOverflowMenu = true })
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = colors.card,
            ) {
                DropdownMenuItem(
                    text = { Text(strings.finishTest, style = Naj.type.cardTitle, color = colors.ink) },
                    onClick = {
                        showOverflowMenu = false
                        onFinish()
                    },
                )
            }
        }
    }
}

/** A question-list icon that turns into an X (and back) as the question list opens. */
@Composable
private fun QuestionListToggle(expanded: Boolean, onClick: () -> Unit) {
    val progress by animateFloatAsState(if (expanded) 1f else 0f)
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            IconMenu,
            contentDescription = if (expanded) null else strings.questionList,
            tint = Color.White,
            modifier = Modifier.size(24.dp).graphicsLayer {
                alpha = 1f - progress
                rotationZ = 90f * progress
            },
        )
        Icon(
            IconClose,
            contentDescription = if (expanded) strings.closeQuestionList else null,
            tint = Color.White,
            modifier = Modifier.size(24.dp).graphicsLayer {
                alpha = progress
                rotationZ = -90f * (1f - progress)
            },
        )
    }
}

@Composable
private fun QuestionPage(
    question: Question,
    imageUri: String?,
    selectedIndex: Int?,
    /** A locked answer is final, so it's shown as right or wrong. */
    locked: Boolean,
    /** The user left this question open in a finished test. */
    skipped: Boolean,
    /** The room the pinned bar takes at the bottom, which the page's content has to clear. */
    bottomRoom: Dp,
    onSelect: (Int) -> Unit,
) {
    val revealed = locked
    val wide = LocalWidthClass.current != WidthClass.Compact
    val answers: @Composable () -> Unit = {
        AnswerCard(
            options = question.answers.map(::displayText),
            selectedIndex = selectedIndex,
            correct = question.correct,
            revealed = revealed,
            // Once checked, the answer is final.
            onSelect = if (revealed) null else onSelect,
        )
    }
    val questionText: @Composable () -> Unit = {
        // Selectable, so a question can be copied out — to look up the rule it's about, say. The
        // answers aren't: a tap on one of those picks an answer.
        SelectionContainer {
            Text(
                displayText(question.text),
                style = if (wide) Naj.type.questionWide else Naj.type.question,
                color = Naj.colors.ink,
            )
        }
    }
    // With little height (phones in landscape) a stacked image would push the answers off
    // screen, so show it next to the question and answers instead.
    val sideBySide = imageUri != null && LocalHeightClass.current == HeightClass.Compact

    // Measured here, outside the scroll, so a picture can be kept within the page it's on: a sign
    // is never drawn bigger than the screen showing it.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = DpSize(maxWidth, maxHeight)
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (sideBySide) {
                Row(
                    modifier = Modifier
                        .widthIn(max = ContentWidth.Wide)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomRoom),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(3f)) {
                        questionText()
                        Spacer(Modifier.height(12.dp))
                        if (skipped) {
                            SkippedNotice()
                            Spacer(Modifier.height(8.dp))
                        }
                        answers()
                        Spacer(Modifier.height(16.dp))
                    }
                    Box(Modifier.weight(2f)) {
                        QuestionImage(
                            uri = imageUri,
                            maxHeight = QuestionImageSideBySideMaxHeight,
                            page = page,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = ContentWidth.Reading)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomRoom),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    imageUri?.let { uri -> QuestionImage(uri = uri, page = page) }
                    questionText()
                    if (skipped) SkippedNotice()
                    answers()
                }
            }
        }
    }
}

/**
 * States that the question was left open, above the answers that are revealed anyway. The correct
 * answer still shows as correct: this only says that none of them was the user's.
 */
@Composable
private fun SkippedNotice() {
    val colors = Naj.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.skippedTint)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(strings.skippedQuestion, style = Naj.type.eyebrowTight, color = colors.skippedOn)
    }
}

/**
 * The picture of a question: drawn [QUESTION_IMAGE_SCALE] times its own size so a sign is readable,
 * and tapping it opens a copy that's twice as big. [page] is the room the page has and [maxHeight]
 * how tall the picture may get in it, so no picture is ever drawn bigger than the screen.
 */
@Composable
private fun QuestionImage(uri: String, page: DpSize, maxHeight: Dp = QuestionImageMaxHeight) {
    // The pictures are drawn bigger than they are, so the sharper filter is what keeps their edges
    // from turning into steps: bicubic on Skia, the platform's best on Android.
    val painter = rememberAsyncImagePainter(uri, filterQuality = FilterQuality.High)
    val imageState by painter.state.collectAsState()
    val density = LocalDensity.current

    /** Whether the enlarged copy is open. */
    var enlarged by remember(uri) { mutableStateOf(false) }

    val loaded = imageState
    if (loaded is AsyncImagePainter.State.Success) {
        val intrinsic = with(density) { painter.intrinsicSize.toDpSize() }
        // A traffic-situation drawing is a scene that runs to its own edges: rounding it would cut
        // the road off. Everything else - signs, pictograms - sits on a plain background and is
        // rounded like the rest of the app.
        val shape = if (isSituationDrawing(uri)) RectangleShape else RoundedCornerShape(12.dp)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val height = minOf(maxHeight, page.height)
            val size = questionImageSize(intrinsic, maxWidth = maxWidth, maxHeight = height)
            Image(
                painter = painter,
                contentDescription = strings.enlargeImage,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .clickable { enlarged = true },
            )
            if (enlarged) {
                // The picture has been decoded already, so the copy draws the bitmap the inline
                // one does, at the size the screen has room for.
                EnlargedQuestionImage(
                    painter = loaded.painter,
                    drawn = size,
                    onDismiss = { enlarged = false },
                )
            }
        }
    } else {
        // Keep the space, so the answers don't jump when the image arrives.
        Spacer(Modifier.height(minOf(maxHeight, page.height)))
    }
}

/**
 * One picture, as large as the screen allows, over the test it belongs to. It's closed by tapping
 * anywhere - including the picture itself - or with the button in the corner.
 */
@Composable
private fun EnlargedQuestionImage(painter: Painter, drawn: DpSize, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EnlargedImageScrim)
                .clickable(onClick = onDismiss)
                .padding(EnlargedImageMargin),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(enlargedImageSize(drawn, DpSize(maxWidth, maxHeight))),
                )
            }
            RoundButton(
                icon = IconClose,
                contentDescription = strings.closeImage,
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/** [onSelect] is null when the answer is locked. */
@Composable
private fun AnswerCard(
    options: List<String>,
    selectedIndex: Int?,
    correct: Int,
    revealed: Boolean,
    onSelect: ((Int) -> Unit)?,
) {
    val colors = Naj.colors
    NajCard(shadow = 0.3f) {
        Column(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                }
                AnswerRow(
                    letter = ('A' + index).toString(),
                    text = option,
                    state = answerState(index, selectedIndex, correct, revealed),
                    isUserAnswer = selectedIndex == index,
                    onClick = onSelect?.let { { it(index) } },
                )
            }
        }
    }
}

@Composable
private fun AnswerRow(
    letter: String,
    text: String,
    state: AnswerState,
    isUserAnswer: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = Naj.colors
    val (background, accent) = when (state) {
        AnswerState.Neutral -> null to colors.muted

        AnswerState.Selected -> Brush.horizontalGradient(listOf(colors.mintGlow, colors.selectedTint)) to
            colors.primaryDeep

        AnswerState.Correct -> Brush.horizontalGradient(listOf(colors.correctTint, colors.correctTint)) to
            colors.passedOn

        AnswerState.Wrong -> Brush.horizontalGradient(listOf(colors.wrongBottom, colors.wrongBottom)) to
            colors.wrongOn
    }
    val label = when {
        state == AnswerState.Correct -> strings.correctAnswer
        isUserAnswer -> strings.yourAnswer
        else -> null
    }
    val marked = state != AnswerState.Neutral
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (background != null) Modifier.background(background) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (marked) accent else colors.neutralTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                style = Naj.type.answerLetter,
                color = if (marked) Color.White else colors.neutralOn,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 7.dp)) {
            if (label != null) {
                Text(label, style = Naj.type.eyebrowTight, color = accent)
                Spacer(Modifier.height(2.dp))
            }
            Text(text, style = Naj.type.option, color = colors.ink)
        }
    }
}

/**
 * Question numbers grouped by section. Answered questions are coloured by correctness only
 * when [revealCorrectness] is set, so the list doesn't give answers away otherwise.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionGrid(
    questions: List<Question>,
    sections: List<Section>,
    currentIndex: Int,
    selectedAnswers: Map<Int, Int>,
    revealCorrectness: Boolean,
    finished: Boolean,
    onSelectQuestion: (Int) -> Unit,
) {
    val colors = Naj.colors
    val grouped: List<Pair<Section, List<Int>>> =
        sections.mapNotNull { section ->
            val indices = questions.withIndex()
                .filter { it.value.section == section.id }
                .map { it.index }
            if (indices.isEmpty()) null else section to indices
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        grouped.forEachIndexed { sectionIndex, (section, indices) ->
            if (sectionIndex > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row {
                    Text(section.title.uppercase(), style = Naj.type.capsLabel, color = colors.muted)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    indices.forEach { idx ->
                        QuestionChipRow(
                            number = idx + 1,
                            state = chipState(
                                selected = selectedAnswers[idx],
                                correct = questions[idx].correct,
                                revealCorrectness = revealCorrectness,
                                finished = finished,
                            ),
                            isCurrent = idx == currentIndex,
                            onClick = { onSelectQuestion(idx) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QuestionChipRow(
    number: Int,
    state: ChipState,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colors = Naj.colors
    if (isCurrent) {
        QuestionChip(
            number = number.toString(),
            background = colors.ink,
            textColor = colors.roadYellow,
            borderColor = colors.ink,
            shadowed = true,
            onClick = onClick,
        )
        return
    }
    val (background, text, border) = when (state) {
        ChipState.Correct -> Triple(colors.correctTint, colors.correctOn, colors.correctTint)
        ChipState.Incorrect -> Triple(colors.wrongBottom, colors.wrongOn, colors.wrongBottom)
        ChipState.Skipped -> Triple(colors.skippedTint, colors.skippedOn, colors.skippedTint)
        ChipState.Answered -> Triple(colors.selectedTint, colors.ink, colors.selectedTint)
        ChipState.Unanswered -> Triple(colors.card, colors.muted, colors.emptyOutline)
    }
    QuestionChip(
        number = number.toString(),
        background = background,
        textColor = text,
        borderColor = border,
        outlined = background == colors.card,
        onClick = onClick,
    )
}
