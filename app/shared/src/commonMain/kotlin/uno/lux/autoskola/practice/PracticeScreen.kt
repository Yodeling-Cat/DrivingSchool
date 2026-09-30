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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.HeightClass
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalHeightClass
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.WidthClass
import uno.lux.autoskola.ui.contentGutter

private val SelectedAnswerBg = Color(0xFFEDE7F6)
private val SelectedAnswerAccent = Color(0xFF5E35B1)
private val CorrectAnswerBg = Color(0xFFE8F5E9)
private val CorrectAnswerAccent = Color(0xFF388E3C)
private val WrongAnswerBg = Color(0xFFFFEBEE)
private val WrongAnswerAccent = Color(0xFFD32F2F)
private val SkippedAnswerBg = Color(0xFFFFF8E1)
private val SkippedAnswerAccent = Color(0xFFC2410C)
private val UnselectedLetter = Color(0xFFBDBDBD)
private val CardBorder = Color(0xFFE0E0E0)
private val Subdued = Color(0xFF757575)

/** Over the page behind an enlarged picture, which is drawn on top of it. */
private val EnlargedImageScrim = Color(0xB3000000)

private val QuestionChipBg = Color(0xFFEEEEEE)
private val QuestionChipAnsweredBg = Color(0xFFD1C4E9)
private val QuestionChipCorrectBg = Color(0xFFC8E6C9)
private val QuestionChipIncorrectBg = Color(0xFFFFCDD2)
private val QuestionChipSkippedBg = Color(0xFFFFECB3)
private val QuestionChipCurrentBorder = Color(0xFF5E35B1)

private val QuestionPanelWidth = 320.dp

/** How far an enlarged picture stays away from the edges of the screen it's shown on. */
private val EnlargedImageMargin = 24.dp

/** The width an arrow beside the question needs, so it sits clear of the text. */
private val QuestionArrowSize = 56.dp

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
    val questions = remember(state.questions) { state.questions.map { it.question } }
    val sections = state.sections
    val testName = attemptName(state.kind, state.testNumber)
    val elapsed = elapsedClock(state.elapsedSeconds)
    val checkImmediately = state.checkImmediately
    val finished = state.finished
    val revealCorrectness = revealsCorrectness(checkImmediately, finished)
    val selectedAnswers = state.answers
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { questions.size })
    val currentIndex = pagerState.currentPage
    val sheetState = rememberStandardBottomSheetState(skipHiddenState = true)
    val scope = rememberCoroutineScope()
    val current = questions[currentIndex]
    // The question list is pinned to the bottom and covers the questions once it's open.
    val listExpanded = sheetState.targetValue == SheetValue.Expanded

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
    // On wide screens the question list is always visible next to the question.
    // Phones in landscape: one slim bar instead of two, and no side panel, so the question keeps
    // most of the little height there is.
    val compactHeight = LocalHeightClass.current == HeightClass.Compact
    val showPanel = LocalWidthClass.current == WidthClass.Expanded && !compactHeight
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
    val barHeight = if (compactHeight) 56.dp else 80.dp
    // The open sheet covers the top app bar, so it only has to stay below the status bar. Phones in
    // landscape are inset below the status bar by the box the sheet lives in instead.
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topReserved = if (compactHeight) 0.dp else statusBarInset
    val sideInsets =
        WindowInsetsSides.Horizontal + if (compactHeight) WindowInsetsSides.Top else WindowInsetsSides.Horizontal

    // Stays up until acted on: first to finish the test, then to get back to its results.
    val snackbarHostState = remember { SnackbarHostState() }
    FinishPromptEffect(
        prompt = finishPrompt(state.allAnswered, finished),
        snackbarHostState = snackbarHostState,
        onFinish = { onEvent(PracticeUiEvent.Finish) },
        onShowResults = onOpenResults,
    )

    val bottomBarPadding = if (compactHeight) PaddingValues(horizontal = 4.dp) else BottomAppBarDefaults.ContentPadding

    // Phones in landscape hide the top bar: the bottom bar already names the test there.
    val topBar: @Composable () -> Unit = {
        if (!compactHeight) PracticeTopBar(testName, elapsed, onBack = ::handleBack)
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

    val pagerContent: @Composable (PaddingValues) -> Unit = { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
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
                        onSelect = { onEvent(PracticeUiEvent.Answer(page, it)) },
                    )
                }
                if (arrowsBeside) besideArrows()
            }
            if (showPanel) {
                VerticalDivider(color = CardBorder)
                Column(
                    Modifier
                        .width(QuestionPanelWidth)
                        .fillMaxHeight()
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
                }
            }
        }
    }

    if (showPanel) {
        Scaffold(
            containerColor = Color.White,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            bottomBar = {
                // The list is always visible next to the question here, so the toggle is inert and
                // the arrows never fade.
                BottomAppBar(
                    containerColor = Color.White,
                    contentPadding = bottomBarPadding,
                    content = { barContent(false, {}, { 1f }) },
                )
            },
            content = pagerContent,
        )
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
            val gridHeight = (maxHeight - topReserved - barHeight - navBarInset).coerceAtLeast(0.dp)
            val arrowsHidden by animateFloatAsState(if (listExpanded) 1f else 0f)

            fun toggleList() {
                scope.launch { if (listExpanded) sheetState.partialExpand() else sheetState.expand() }
            }

            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(sheetState, snackbarHostState),
                containerColor = Color.White,
                sheetContainerColor = Color.White,
                sheetPeekHeight = barHeight + navBarInset,
                // The sheet and the screen behind it are both white, so the shadow is what separates
                // them: the Material default is barely visible.
                sheetShadowElevation = 24.dp,
                sheetDragHandle = null,
                topBar = topBar,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                sheetContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight + navBarInset)
                            .padding(horizontal = if (compactHeight) 4.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = { barContent(listExpanded, ::toggleList, { 1f - arrowsHidden }) },
                    )
                    Column(Modifier.height(gridHeight).verticalScroll(rememberScrollState())) {
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
                },
                content = pagerContent,
            )
        }
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

/** The title of a test: its name above how long it has been running. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeTopBar(testName: String, elapsed: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(testName, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(elapsed, color = Subdued, fontSize = 12.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(IconArrowBack, contentDescription = strings.back)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
    )
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
    Box(modifier, contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, CardBorder, CircleShape),
        ) {
            Icon(icon, contentDescription = description)
        }
    }
}

/** Asks whether to leave an unfinished test, whose answers would be lost. */
@Composable
private fun LeaveTestDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.leaveTestTitle) },
        text = { Text(strings.leaveTestMessage) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.leaveTest) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.keepTesting) } },
    )
}

@Composable
private fun RowScope.PracticeBarContent(
    compactHeight: Boolean,
    testName: String,
    elapsed: String,
    currentIndex: Int,
    questionCount: Int,
    points: Int,
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
    var showOverflowMenu by remember { mutableStateOf(false) }
    if (compactHeight) {
        IconButton(onClick = onBack) {
            Icon(IconArrowBack, contentDescription = strings.back)
        }
    }
    if (showListToggle) {
        QuestionListToggle(expanded = listExpanded, onClick = onToggleList)
    } else {
        Spacer(Modifier.width(16.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
        Text(
            if (compactHeight) {
                "$testName · ${strings.questionOutOf(currentIndex + 1, questionCount)}"
            } else {
                strings.questionOutOf(currentIndex + 1, questionCount)
            },
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
        )
        // Phones in landscape hide the top bar, so the time runs in the bar's second line there.
        Text(
            if (compactHeight) "${strings.points(points)} · $elapsed" else strings.points(points),
            color = Subdued,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
    // The arrows are beside the question wherever the page has room for them. They make no sense
    // while the question list covers the questions, which is what [arrowAlpha] is for.
    if (showArrows) {
        Row(Modifier.graphicsLayer { alpha = arrowAlpha() }) {
            val usable = arrowAlpha() > 0.5f
            IconButton(onClick = onPrevious, enabled = usable && currentIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = strings.previousQuestion)
            }
            IconButton(onClick = onNext, enabled = usable && currentIndex < questionCount - 1) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = strings.nextQuestion)
            }
        }
    }
    // Finishing is the only option, so there's no menu once the test is finished.
    if (canFinish) {
        Box {
            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(IconMoreVert, contentDescription = strings.moreOptions)
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(strings.finishTest) },
                    onClick = {
                        showOverflowMenu = false
                        onFinish()
                    },
                )
            }
        }
    }
}

/** A hamburger that turns into an X (and back) as the question list opens. */
@Composable
private fun QuestionListToggle(expanded: Boolean, onClick: () -> Unit) {
    val progress by animateFloatAsState(if (expanded) 1f else 0f)
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                IconMenu,
                contentDescription = if (expanded) null else strings.questionList,
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - progress
                    rotationZ = 90f * progress
                },
            )
            Icon(
                IconClose,
                contentDescription = if (expanded) strings.closeQuestionList else null,
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    rotationZ = -90f * (1f - progress)
                },
            )
        }
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
    onSelect: (Int) -> Unit,
) {
    val revealed = locked
    val answers: @Composable () -> Unit = {
        AnswerCard(
            options = question.answers,
            selectedIndex = selectedIndex,
            correct = question.correct,
            revealed = revealed,
            // Once checked, the answer is final.
            onSelect = if (revealed) null else onSelect,
        )
    }
    val questionText: @Composable () -> Unit = {
        Text(question.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    questionText()
                    imageUri?.let { uri ->
                        Spacer(Modifier.height(12.dp))
                        QuestionImage(uri = uri, page = page)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (skipped) {
                        SkippedNotice()
                        Spacer(Modifier.height(8.dp))
                    }
                    answers()
                    Spacer(Modifier.height(16.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SkippedAnswerBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            strings.skippedQuestion,
            color = SkippedAnswerAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )
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
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val height = minOf(maxHeight, page.height)
            val size = questionImageSize(intrinsic, maxWidth = maxWidth, maxHeight = height)
            Image(
                painter = painter,
                contentDescription = strings.enlargeImage,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
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
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(IconClose, contentDescription = strings.closeImage, tint = Color.White)
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
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

@Composable
private fun AnswerRow(
    letter: String,
    text: String,
    state: AnswerState,
    isUserAnswer: Boolean,
    onClick: (() -> Unit)?,
) {
    val (background, accent) = when (state) {
        AnswerState.Neutral -> Color.White to UnselectedLetter
        AnswerState.Selected -> SelectedAnswerBg to SelectedAnswerAccent
        AnswerState.Correct -> CorrectAnswerBg to CorrectAnswerAccent
        AnswerState.Wrong -> WrongAnswerBg to WrongAnswerAccent
    }
    val label = when {
        state == AnswerState.Correct -> strings.correctAnswer
        isUserAnswer -> strings.yourAnswer
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            letter,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.width(40.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (label != null) {
                Text(
                    label,
                    color = if (state == AnswerState.Selected) Subdued else accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(section.title, color = Subdued, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    indices.forEach { idx ->
                        QuestionChip(
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
private fun QuestionChip(
    number: Int,
    state: ChipState,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val bg = when (state) {
        ChipState.Correct -> QuestionChipCorrectBg
        ChipState.Incorrect -> QuestionChipIncorrectBg
        ChipState.Skipped -> QuestionChipSkippedBg
        ChipState.Answered -> QuestionChipAnsweredBg
        ChipState.Unanswered -> QuestionChipBg
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(bg)
            .then(if (isCurrent) Modifier.border(2.dp, QuestionChipCurrentBorder, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
