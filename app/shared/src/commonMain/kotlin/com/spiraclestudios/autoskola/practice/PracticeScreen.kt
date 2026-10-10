package com.spiraclestudios.autoskola.practice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.Section
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.isDebugBuild
import com.spiraclestudios.autoskola.stats.AttemptKind
import com.spiraclestudios.autoskola.stats.TestAttempt
import com.spiraclestudios.autoskola.ui.AnswerCard
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.BarIconButton
import com.spiraclestudios.autoskola.ui.CardDivider
import com.spiraclestudios.autoskola.ui.ConfirmDialog
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.EnlargeableImage
import com.spiraclestudios.autoskola.ui.HeightClass
import com.spiraclestudios.autoskola.ui.IconArrowBack
import com.spiraclestudios.autoskola.ui.IconClose
import com.spiraclestudios.autoskola.ui.IconMenu
import com.spiraclestudios.autoskola.ui.IconMoreVert
import com.spiraclestudios.autoskola.ui.ImageSideBySideMaxHeight
import com.spiraclestudios.autoskola.ui.LoadingScreen
import com.spiraclestudios.autoskola.ui.LocalHeightClass
import com.spiraclestudios.autoskola.ui.LocalWidthClass
import com.spiraclestudios.autoskola.ui.NajDropdownMenu
import com.spiraclestudios.autoskola.ui.NajMenuItem
import com.spiraclestudios.autoskola.ui.NavyPill
import com.spiraclestudios.autoskola.ui.PageArrow
import com.spiraclestudios.autoskola.ui.PageArrowSize
import com.spiraclestudios.autoskola.ui.PagerSideArrows
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PreviewRow
import com.spiraclestudios.autoskola.ui.ProgressBar
import com.spiraclestudios.autoskola.ui.QuestionChip
import com.spiraclestudios.autoskola.ui.QuestionMedia
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.VideoItem
import com.spiraclestudios.autoskola.ui.WhitePill
import com.spiraclestudios.autoskola.ui.WidthClass
import com.spiraclestudios.autoskola.ui.contentGutter
import com.spiraclestudios.autoskola.ui.displayText
import com.spiraclestudios.autoskola.ui.previewExam
import com.spiraclestudios.autoskola.ui.previewLongSectionQuestions
import com.spiraclestudios.autoskola.ui.previewLongSections
import com.spiraclestudios.autoskola.ui.previewPracticeQuestions
import com.spiraclestudios.autoskola.ui.previewSections
import com.spiraclestudios.autoskola.ui.theme.AccentedText
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.accentNumber
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val QuestionPanelWidth = 360.dp

/** The bar pill and the room the sheet keeps around it. */
private val BarPillHeight = 68.dp
private val BarPillGap = 8.dp
private val BarPillHeightCompact = 56.dp
private val BarPillGapCompact = 4.dp

/** The count a question is drawn against: white at 0.55, the dimming the design's own bar shows. */
private val BarCountText = Color.White.copy(alpha = 0.55f)

/** The width of the question column on a wide screen, with the arrows beside it. */
private val QuestionColumnWidth = 660.dp

/**
 * The shadow the question sheet throws on the page beneath it while it's open. The sheet and the
 * page are both light, so the shadow is the only thing separating them — the Material default is
 * barely visible. It comes up over the stretch of the drag left after the sheet has its open look —
 * from [SHEET_COVERED_AT] of the way open to fully expanded — which is what [sheetShadowCover] measures.
 */
private val QuestionSheetShadow = 24.dp

/** Radius of the top corners of the question list's surface. */
private val SheetRadius = 32.dp

/**
 * Connects [PracticeScreen] to its [PracticeViewModel]. Calls [onClose] when the test is left or the
 * attempt is gone, and [onOpenResults] when the test is finished or the user asks for its results.
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
            // The screen closes when the driver leaves, not when they press back: a test they
            // answered nothing in is dropped first.
            LaunchedEffect(current.close) { if (current.close) onClose() }
            PracticeScreen(current, viewModel::onEvent, onOpenResults = onOpenResults)
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
fun PracticeScreen(state: PracticeUiState.Ready, onEvent: (PracticeUiEvent) -> Unit, onOpenResults: () -> Unit) {
    val colors = Naj.colors
    // What the state holds is read out of it here, and only that is used below: the state is a new
    // one every second the clock ticks, and a slot that held on to it would be redrawn every time.
    val practiceQuestions = state.questions
    val questions = remember(practiceQuestions) { practiceQuestions.map { it.question } }
    val sections = state.sections
    val testName = attemptName(state.kind, state.testNumber)
    // The clock is the one thing here that changes every second, so it is handed down to be read
    // where it is drawn: the bars around it, the questions and the list then sit the tick out.
    val elapsedSeconds by rememberUpdatedState(state.elapsedSeconds)
    val elapsed = { elapsedClock(elapsedSeconds) }
    val overTime = isOverTime(state.elapsedSeconds, state.allottedSeconds)
    val allAnswered = state.allAnswered
    val checkImmediately = state.checkImmediately
    val finished = state.finished
    val revealCorrectness = revealsCorrectness(checkImmediately, finished)
    val selectedAnswers = state.answers

    // How much of the test is answered, and how much is left open: what the bar's own line counts
    // once the question list opens, and what the top bar's line of progress fills by.
    val (answered, skipped) = answeredAndSkipped(selectedAnswers, questions.size)
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { questions.size })
    val currentIndex = pagerState.currentPage
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
    )
    // The same bar on a tall window, where the list has a panel of its own: nothing expands or
    // drags this sheet, it's the bar with a shadow under it.
    val pinnedSheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
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

            // Leaving is the ViewModel's to do: a test with nothing answered in it goes with it.
            BackAction.Leave -> onEvent(PracticeUiEvent.Leave)

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
    val arrowsBeside = gutter >= PageArrowSize

    fun goTo(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index.coerceIn(0, questions.lastIndex)) }
    }

    // Arrow keys step through the questions on a desktop keyboard, the way the arrows beside the
    // question and in the bar do. The page takes the focus on the way in, so the keys work without
    // having to click the question first; a child that takes focus by being clicked still hands its
    // key events up to here, because they are asked for as preview events.
    val pageFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { pageFocus.requestFocus() }

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

    // The app bar's own height, as it is drawn: the open sheet keeps that much room at the top, so
    // the whole bar stays visible behind it. It stays zero on a phone in landscape, where the top
    // bar is gone and the bar at the bottom names the test instead.
    var appBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // The room the open sheet keeps above itself. A phone in landscape has no top bar — it is inset
    // below the status bar by the box the sheet lives in instead — so the navigation bar's room is
    // what is left over there.
    val openTopRoom = if (compactHeight) navBarInset else appBarHeight
    val sideInsets =
        if (compactHeight) WindowInsetsSides.Horizontal + WindowInsetsSides.Top else WindowInsetsSides.Horizontal

    // The sheet's top edge, less the room Material's host keeps under its Snackbar: the message then
    // sits just clear of the sheet's top instead of floating over the questions.
    val messageLift = barSlotHeight + navBarInset - SnackbarHostInset + SnackbarAboveSheet

    // Stays up until acted on: first to finish the test, then to get back to its results.
    val snackbarHostState = remember { SnackbarHostState() }

    // A message bar sits over the questions too, so while one is up the content under it needs the
    // height the host takes up on top of the bar's slot - otherwise the last answer can't be
    // scrolled out from under it.
    val snackbarUp = snackbarHostState.currentSnackbarData != null
    val messageRoom = if (snackbarUp) SnackbarHeight + SnackbarHostInset * 2 + SnackbarAboveSheet else 0.dp

    /** How far a question's content has to clear the pinned bar to be readable to its last line. */
    val questionBottomRoom = barSlotHeight + navBarInset + 16.dp + messageRoom
    OverTimeNoticeEffect(
        show = state.showTimeLimitToast,
        // Only a run with a time limit can run out of it, so only that one has a message to show.
        message = state.allottedSeconds?.let { strings.overTime((it / 60).toInt()) }.orEmpty(),
        snackbarHostState = snackbarHostState,
        onShown = { onEvent(PracticeUiEvent.TimeLimitToastShown) },
    )
    FinishPromptEffect(
        prompt = finishPrompt(allAnswered, finished),
        snackbarHostState = snackbarHostState,
        onFinish = { onEvent(PracticeUiEvent.Finish) },
        onShowResults = onOpenResults,
    )

    // Phones in landscape hide the top bar: the bottom bar already names the test there.
    val topBar: @Composable () -> Unit = {
        if (!compactHeight) {
            Box(Modifier.onSizeChanged { appBarHeight = with(density) { it.height.toDp() } }) {
                PracticeTopBar(
                    testName = testName,
                    elapsed = elapsed,
                    overTime = overTime,
                    allAnswered = allAnswered,
                    checkImmediately = checkImmediately,
                    finished = finished,
                    answered = answered,
                    questionCount = questions.size,
                    onBack = ::handleBack,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
    }

    /**
     * [cover] is how much of the sheet's open look the bar has taken on: it is what turns the
     * toggle into a cross and takes the arrows away as the sheet opens over the questions. It is
     * read as the bar is drawn rather than handed over as a number, because it changes on every
     * frame of a drag.
     */
    val barContent: @Composable RowScope.(
        listExpanded: Boolean,
        onToggleList: () -> Unit,
        cover: () -> Float,
    ) -> Unit = { listExpanded, onToggleList, cover ->
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
            cover = cover,
            answered = answered,
            skipped = skipped,
            showArrows = !arrowsBeside,
            onBack = ::handleBack,
            onToggleList = onToggleList,
            onPrevious = { goTo(currentIndex - 1) },
            onNext = { goTo(currentIndex + 1) },
            onFinish = { onEvent(PracticeUiEvent.Finish) },
            // The shortcuts are built away in a release build, where the menu has no place for them.
            onDebugAnswers = if (isDebugBuild()) onEvent else null,
        )
    }

    /** The arrows for a page wide enough to hold them beside the question. */
    val besideArrows: @Composable BoxScope.() -> Unit = {
        PagerSideArrows(
            gutter = gutter,
            currentPage = currentIndex,
            pageCount = questions.size,
            previousDescription = strings.previousQuestion,
            nextDescription = strings.nextQuestion,
            goTo = ::goTo,
        )
    }

    /** The questions themselves: the pager, with the arrows beside it where there's room. */
    val questionsPane: @Composable (PaddingValues) -> Unit = { padding ->
        // Only the top bar's room is taken out of the page: the questions run on behind the pinned
        // bar, and each page keeps its last answer clear of it with [questionBottomRoom].
        Row(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SwipePager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    onPageSize = { pagerWidth = it },
                ) { page ->
                    QuestionPage(
                        question = questions[page],
                        active = page == pagerState.settledPage,
                        imageUri = practiceQuestions[page].imageUri,
                        video = practiceQuestions[page].video,
                        answerImageUris = practiceQuestions[page].answerImageUris,
                        selectedIndex = selectedAnswers[page],
                        locked = isAnswerLocked(checkImmediately, finished, selectedAnswers[page]),
                        skipped = questionSkipped(selectedAnswers[page], finished),
                        bottomRoom = questionBottomRoom,
                        onSelect = { onEvent(PracticeUiEvent.Answer(page, it)) },
                        onImageError = { onEvent(PracticeUiEvent.ImageFailed) },
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

    Box(
        Modifier
            .fillMaxSize()
            .paperBackground(paperGlows(colors, mintY = 0.7f))
            .arrowKeyPaging(pageFocus, currentIndex, questions.size, ::goTo),
    ) {
        if (showPanel) {
            // The bar is the bottom sheet a phone shows, over the questions alone: the list has a
            // panel of its own here, and the sheet's width is the question column's, so it lines up
            // with the question and the answers instead of running on under the list. It is pinned
            // — nothing drags or expands it — and the bar's own shadow is what separates it from the
            // page, which the sheet's shape leaves whole.
            Column(
                Modifier
                    .fillMaxSize()
                    // The sheet's peek already makes room for the navigation bar, as on a phone.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                // The app bar stays over both panes, so it sits outside the sheet's scaffold.
                topBar()
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        BottomSheetScaffold(
                            modifier = Modifier.fillMaxSize(),
                            scaffoldState = rememberBottomSheetScaffoldState(pinnedSheetState, snackbarHostState),
                            // The screen places the message bar itself: the scaffold lifts its own host by
                            // the sheet's peek as well, which stacks with any room given here.
                            snackbarHost = {},
                            containerColor = Color.Transparent,
                            sheetContainerColor = Color.Transparent,
                            sheetPeekHeight = barSlotHeight + navBarInset,
                            // The reading column's width: what holds the question and its answers.
                            sheetMaxWidth = QuestionColumnWidth,
                            sheetShape = UnclippedSheetShape,
                            sheetShadowElevation = 0.dp,
                            sheetDragHandle = null,
                            sheetSwipeEnabled = false,
                            sheetContent = {
                                // The list is always visible next to the question here, so the toggle is
                                // inert and the arrows never leave the bar.
                                BarPill(pillHeight, pillGap) { barContent(false, {}, { 0f }) }
                            },
                            content = questionsPane,
                        )
                        // The message belongs to the questions: it stays over the reading column rather
                        // than running on under the list's panel, which sits outside this box.
                        ExamMessageBar(
                            host = snackbarHostState,
                            lift = messageLift,
                            modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = QuestionColumnWidth),
                        )
                    }
                    Box(Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp)) { questionPanel() }
                }
            }
        } else {
            // The question list is a sheet pinned to the bottom: collapsed it shows just the bar. Its
            // content has a fixed height — a bar over a list tall enough to reach the app bar — so the
            // sheet's anchors don't move under the user's finger. What the drag changes is what the
            // sheet looks like, not what it measures: its shadow, its own surface, the bar's toggle and
            // its arrows all follow the drag. Sizing or fading content from the live drag offset is what
            // makes the gesture fight its own layout, which is how taps and list scrolls got eaten after
            // a drag.
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    // The scaffold doesn't inset its content; the sheet stays below the status bar by
                    // its height instead.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(sideInsets)),
            ) {
                // The list's height, and the lines the sheet's top edge rests on: on the peek line shut,
                // and on the room it keeps above itself once it's open. The navigation bar's room is at
                // the end of the list, so it takes no height away from it.
                val geometry = sheetGeometry(
                    available = maxHeight,
                    barSlotHeight = barSlotHeight,
                    navBarInset = navBarInset,
                    openTopRoom = openTopRoom,
                )
                val restOffset = with(density) { geometry.restOffset.toPx() }
                val openOffset = with(density) { geometry.openOffset.toPx() }

                // How far the list has been dragged open — 0 where the peek leaves it, 1 fully expanded
                // — and how much of its open look it has taken on. The sheet's surface, the bar's
                // toggle and its arrows all follow the second one, and all of them are done by the time
                // [SHEET_COVERED_AT] of the drag is behind it. Both change on every frame of a drag, so
                // they are read where the sheet is laid out and drawn rather than here, which would
                // compose the sheet and its bar again on every one of those frames.
                val openProgress by remember(sheetState, restOffset, openOffset) {
                    derivedStateOf {
                        if (!sheetState.hasPartiallyExpandedState || !sheetState.hasExpandedState) {
                            0f
                        } else {
                            sheetOpenProgress(sheetState.requireOffset(), restOffset, openOffset)
                        }
                    }
                }
                val cover = { sheetCover(openProgress) }

                // The sheet rests with its top on the line the peek gives it; anything above that
                // line means the list is on its way open, if only by a hair, and the message leaves.
                val messageHidden by remember(sheetState, restOffset) {
                    derivedStateOf {
                        sheetState.hasPartiallyExpandedState &&
                            sheetState.requireOffset() < restOffset - SHEET_OPEN_TOLERANCE
                    }
                }

                fun toggleList() {
                    scope.launch { if (listExpanded) sheetState.partialExpand() else sheetState.expand() }
                }

                BottomSheetScaffold(
                    scaffoldState = rememberBottomSheetScaffoldState(sheetState, snackbarHostState),
                    snackbarHost = {},
                    containerColor = Color.Transparent,
                    // The sheet's own surface is drawn inside it instead, so that it can grow over the
                    // bar's slot as the list is dragged open.
                    sheetContainerColor = Color.Transparent,
                    sheetPeekHeight = barSlotHeight + navBarInset,
                    // The sheet's surface below casts its shadow, so nothing here cuts one off.
                    sheetShape = UnclippedSheetShape,
                    sheetShadowElevation = 0.dp,
                    sheetDragHandle = null,
                    topBar = topBar,
                    sheetContent = {
                        Box(Modifier.fillMaxWidth()) {
                            // The sheet's own surface, behind everything the sheet holds: it wraps the
                            // question list at rest and grows up over the bar's slot as the list is
                            // dragged open, covering the sheet by [SHEET_COVERED_AT] of the way. Its
                            // height is read as it is measured, so a drag only lays it out again.
                            // Its shadow ramps from that point to fully expanded instead: the sheet
                            // lies flat on the page while it is still coming up, and starts casting
                            // one once it is open. It is read as the surface is drawn, so a drag only
                            // draws it again.
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .layoutHeight { sheetSurfaceHeight(cover(), geometry.listHeight, barSlotHeight) }
                                    .graphicsLayer {
                                        shadowElevation = sheetShadowElevation(
                                            sheetShadowCover(openProgress),
                                            QuestionSheetShadow,
                                        ).toPx()
                                        shape = RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius)
                                        clip = true
                                    }
                                    .background(colors.sheet),
                            )
                            Column(Modifier.fillMaxWidth()) {
                                BarPill(pillHeight, pillGap) {
                                    barContent(listExpanded, ::toggleList, cover)
                                }
                                Column(
                                    Modifier
                                        .height(geometry.listHeight)
                                        .fillMaxWidth()
                                        // Clipped to the same rounded top its surface is drawn with, so
                                        // a scrolled chip can't square its corners off.
                                        .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
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
                        }
                    },
                    content = questionsPane,
                )
                // The message comes back when the list settles shut, since the prompt it carries
                // waits to be acted on rather than timing out while it is out of sight.
                if (!messageHidden) {
                    ExamMessageBar(
                        host = snackbarHostState,
                        lift = messageLift,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    if (confirmingLeave) {
        LeaveTestDialog(
            onDismiss = { confirmingLeave = false },
            onConfirm = {
                confirmingLeave = false
                onEvent(PracticeUiEvent.Leave)
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

/** The color of the test's clock: over time it warns the way a wrong answer does. */
@Composable
private fun elapsedColor(overTime: Boolean): Color = if (overTime) Naj.colors.wrongOn else Naj.colors.muted

/**
 * The clock, in a box as wide as the widest clock of the same shape: the time is drawn over an
 * invisible placeholder of eights, so the pill around it keeps its width as the digits change. The
 * placeholder is hidden from accessibility, which has no use for it. [elapsed] is read here, where
 * it is drawn, so the clock ticking redraws the clock and nothing around it.
 */
@Composable
private fun ElapsedLabel(elapsed: () -> String, style: TextStyle, color: Color) {
    val time = elapsed()
    Box {
        Text(
            elapsedClockPlaceholder(time),
            style = style,
            color = Color.Transparent,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(time, style = style, color = color, maxLines = 1)
    }
}

/**
 * The question page's header: the way back, the test's name with its number in the coral italic,
 * the clock, and how much of the test is answered.
 */
@Composable
private fun PracticeTopBar(
    testName: String,
    elapsed: () -> String,
    overTime: Boolean,
    allAnswered: Boolean,
    checkImmediately: Boolean,
    finished: Boolean,
    answered: Int,
    questionCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 48.dp)) {
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
                ElapsedLabel(elapsed, style = Naj.type.tileStatus, color = pillContent)
            }
        }
        Spacer(Modifier.height(18.dp))
        QuestionProgress(answered, questionCount)
    }
}

/** How far through the test the user is, in the teal gradient the design uses for progress. */
@Composable
private fun QuestionProgress(answered: Int, questionCount: Int) {
    val colors = Naj.colors
    val fraction = answeredFraction(answered, questionCount)
    ProgressBar(
        fraction = fraction,
        height = 6.dp,
        track = colors.inkAt(0.1f),
        fill = Brush.horizontalGradient(colors.primaryGradient),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Asks whether to leave an unfinished test, whose answers would be lost. */
@Composable
private fun LeaveTestDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ConfirmDialog(
        title = strings.leaveTestTitle,
        message = strings.leaveTestMessage,
        confirmLabel = strings.leaveTest,
        onConfirm = onConfirm,
        destructive = true,
        dismissLabel = strings.keepTesting,
        onDismiss = onDismiss,
        onDismissRequest = onDismiss,
    )
}

/** The navy pill the test's controls live in, with the room the page keeps around it. */
@Composable
private fun BarPill(height: Dp, gap: Dp, content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = gap)) {
        NavyPill(Modifier.fillMaxWidth(), height = height, arrangement = Arrangement.Start, content = content)
    }
}

/**
 * The bar's second line, which turns over as the question list opens: what the question is worth while
 * the list is shut, and how much of the test is behind the user once it's open. The two cross-fade with
 * the drag itself, so the count is the line in place by the time the sheet has taken its open look.
 * The line is as wide as the text that is showing — not the longer of the two, which would leave the
 * clock behind it in landscape stranded past the end of the points — and what of the other text runs
 * past that is cut off. The width is read from [cover] as the line is measured, so a drag only lays
 * it out again.
 */
@Composable
private fun BarTestLine(shut: String, open: String, cover: () -> Float) {
    val colors = Naj.colors
    Layout(
        modifier = Modifier.clipToBounds(),
        content = {
            Text(
                shut,
                style = Naj.type.tiny,
                color = colors.roadYellow,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = 1f - cover() },
            )
            Text(
                open,
                style = Naj.type.tiny,
                color = colors.roadYellow,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = cover() },
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0)
        val (shutText, openText) = measurables.map { it.measure(loose) }
        val width = constraints.constrainWidth(barTestLineWidth(shutText.width, openText.width, cover()))
        layout(width, maxOf(shutText.height, openText.height)) {
            shutText.place(0, 0)
            openText.place(0, 0)
        }
    }
}

@Composable
private fun RowScope.PracticeBarContent(
    compactHeight: Boolean,
    testName: String,
    elapsed: () -> String,
    currentIndex: Int,
    questionCount: Int,
    points: Int,
    overTime: Boolean,
    showListToggle: Boolean,
    listExpanded: Boolean,
    canFinish: Boolean,
    /** How much of the sheet's open look the bar has taken on: 0 while the list is shut, 1 once it's covered. */
    cover: () -> Float,
    /** How much of the test is behind the user: the questions answered, and the ones left open. */
    answered: Int,
    skipped: Int,
    /** The arrows sit beside the question instead, so the bar leaves them out. */
    showArrows: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    /** The debug answers, or null in a release build, where the menu leaves them out. */
    onDebugAnswers: ((PracticeUiEvent) -> Unit)?,
) {
    val colors = Naj.colors
    var showOverflowMenu by remember { mutableStateOf(false) }
    if (compactHeight) {
        BarIconButton(IconArrowBack, strings.back, onClick = onBack)
    }
    if (showListToggle) {
        QuestionListToggle(expanded = listExpanded, cover = cover, onClick = onToggleList)
    }
    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
        // The question the test is on, counted against how many there are: the count sits dimmed
        // behind the number, and a phone in landscape has the test's name before both.
        Row {
            if (compactHeight) {
                Text(
                    barQuestionPrefix(testName),
                    style = Naj.type.barNumber,
                    color = Color.White,
                    maxLines = 1,
                )
            }
            Text(
                strings.questionCounter(currentIndex + 1),
                style = Naj.type.barNumber,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                questionCountRun(strings.questionCount(questionCount)),
                style = Naj.type.barNumber,
                color = BarCountText,
                maxLines = 1,
            )
        }
        // The count the second line turns over to once the question list is open.
        val testLine = barAnsweredLine(strings.answeredCount(answered), strings.skippedCount(skipped))
        // Phones in landscape hide the top bar, so the time runs in the bar's second line there,
        // and only the time itself turns red when the test is over it.
        if (compactHeight) {
            Row {
                BarTestLine(
                    shut = barBeforeClock(strings.points(points)),
                    open = barBeforeClock(testLine),
                    cover = cover,
                )
                ElapsedLabel(
                    elapsed,
                    style = Naj.type.tiny,
                    color = if (overTime) colors.wrongBottom else colors.roadYellow,
                )
            }
        } else {
            BarTestLine(shut = strings.points(points), open = testLine, cover = cover)
        }
    }
    // The arrows are beside the question wherever the page has room for them. They make no sense
    // while the question list covers the questions, so they leave the bar as it opens — gone, not
    // just faded out, since there is nothing left for them to step through.
    if (showArrows) {
        // Composed again only as the arrows come and go and turn usable, not on every frame of the
        // drag that fades them.
        val arrowsShown by remember(cover) { derivedStateOf { cover() < 1f } }
        val usable by remember(cover) { derivedStateOf { cover() < 0.5f } }
        if (arrowsShown) {
            Row(Modifier.graphicsLayer { alpha = 1f - cover() }) {
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
    }
    // Finishing is the only option, so there's no menu once the test is finished.
    if (canFinish) {
        Box {
            BarIconButton(IconMoreVert, strings.moreOptions, onClick = { showOverflowMenu = true })
            NajDropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                NajMenuItem(
                    strings.finishTest,
                    onClick = {
                        showOverflowMenu = false
                        onFinish()
                    },
                )
                val debugAnswers = onDebugAnswers
                if (debugAnswers != null) {
                    NajMenuItem(
                        strings.answerAllRandomly,
                        onClick = {
                            showOverflowMenu = false
                            debugAnswers(PracticeUiEvent.AnswerAllRandomly)
                        },
                    )
                    NajMenuItem(
                        strings.answerAllCorrectly,
                        onClick = {
                            showOverflowMenu = false
                            debugAnswers(PracticeUiEvent.AnswerAllCorrectly)
                        },
                    )
                }
            }
        }
    }
}

/**
 * A height read as the layout is measured rather than as it is composed, the way `Modifier.height`
 * takes it otherwise: one that follows a drag frame by frame then lays the layout out again on each
 * of them, and composes nothing.
 */
private fun Modifier.layoutHeight(height: () -> Dp): Modifier = layout { measurable, constraints ->
    val heightPx = constraints.constrainHeight(height().roundToPx())
    val placeable = measurable.measure(constraints.copy(minHeight = heightPx, maxHeight = heightPx))
    layout(placeable.width, heightPx) { placeable.place(0, 0) }
}

/** The room Material's own SnackbarHost keeps under the Snackbar it shows. */
private val SnackbarHostInset = 12.dp

/** How tall Material's Snackbar is, whatever its text. */
private val SnackbarHeight = 48.dp

/**
 * Material's own message bar: as wide as the box it is placed in, and sitting [lift] above that
 * box's bottom - which is measured to the sheet's top edge, so each of the two layouts gets a bar
 * over its own questions and nothing else.
 */
@Composable
private fun ExamMessageBar(host: SnackbarHostState, lift: Dp, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = host,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = lift),
    )
}

/** How far above the sheet's top edge the message bar sits. */
private val SnackbarAboveSheet = 4.dp

/** How far the sheet has to move off its resting line before the message gets out of the way. */
private const val SHEET_OPEN_TOLERANCE = 1.5f

/**
 * A question-list icon that turns into an X (and back) as the question list opens. It follows the
 * drag the way the rest of the sheet does: [cover] is how far open the list has been dragged, and
 * the icon that has faded out is gone rather than just invisible. [expanded] is the state the list
 * is heading for, which is what a tap on it does.
 */
@Composable
private fun QuestionListToggle(expanded: Boolean, cover: () -> Float, onClick: () -> Unit) {
    // Each icon is composed again only as it comes and goes; the turn and the fade between them are
    // drawn from [cover] as it changes.
    val menuShown by remember(cover) { derivedStateOf { cover() < 1f } }
    val closeShown by remember(cover) { derivedStateOf { cover() > 0f } }
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (menuShown) {
            Icon(
                IconMenu,
                contentDescription = if (expanded) null else strings.questionList,
                tint = Color.White,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    val covered = cover()
                    alpha = 1f - covered
                    rotationZ = 90f * covered
                },
            )
        }
        if (closeShown) {
            Icon(
                IconClose,
                contentDescription = if (expanded) strings.closeQuestionList else null,
                tint = Color.White,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    val covered = cover()
                    alpha = covered
                    rotationZ = -90f * (1f - covered)
                },
            )
        }
    }
}

@Composable
private fun QuestionPage(
    question: Question,
    /** Whether this is the page on screen, rather than one the pager has composed beside it. */
    active: Boolean,
    imageUri: String?,
    /** Drawn in place of the picture, which is then its still. */
    video: VideoItem?,
    answerImageUris: List<String>?,
    selectedIndex: Int?,
    /** A locked answer is final, so it's shown as right or wrong. */
    locked: Boolean,
    /** The user left this question open in a finished test. */
    skipped: Boolean,
    /** The room the pinned bar takes at the bottom, which the page's content has to clear. */
    bottomRoom: Dp,
    onSelect: (Int) -> Unit,
    /** Called when this question's picture could not be loaded. */
    onImageError: () -> Unit,
) {
    val revealed = locked
    val wide = LocalWidthClass.current != WidthClass.Compact
    // Worked out once per question: the page is composed again whenever its answer changes.
    val options = remember(question) { question.answers.map(::displayText) }
    val text = remember(question) { displayText(question.text) }
    val answers: @Composable () -> Unit = {
        AnswerCard(
            options = options,
            selectedIndex = selectedIndex,
            correct = question.correct,
            revealed = revealed,
            // Once checked, the answer is final.
            onSelect = if (revealed) null else onSelect,
            images = answerImageUris,
        )
    }
    val questionText: @Composable () -> Unit = {
        // Selectable, so a question can be copied out — to look up the rule it's about, say. The
        // answers aren't: a tap on one of those picks an answer.
        SelectionContainer {
            Text(
                text,
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
                        QuestionMedia(
                            video = video,
                            imageUri = imageUri,
                            page = page,
                            active = active,
                            maxHeight = ImageSideBySideMaxHeight,
                            onImageError = onImageError,
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
                    QuestionMedia(video, imageUri, page, active, onImageError = onImageError)
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
 * Question numbers grouped by section. Answered questions are colored by correctness only
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
    // The test's questions don't change while it runs, so they are sorted into sections once rather
    // than every time an answer or the question being read does.
    val grouped: List<Pair<Section, List<Int>>> = remember(questions, sections) {
        sections.mapNotNull { section ->
            val indices = questions.withIndex()
                .filter { it.value.section == section.id }
                .map { it.index }
            if (indices.isEmpty()) null else section to indices
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        grouped.forEachIndexed { sectionIndex, (section, indices) ->
            if (sectionIndex > 0) {
                CardDivider()
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                // The section, and how much of it the user has answered. The count is measured first and
                // keeps its own width, while the title takes what is left of the row: a long one wraps
                // onto another line rather than squeezing the count into a column of its own characters.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        section.title.uppercase(),
                        style = Naj.type.capsLabel,
                        color = colors.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        questionCounterLabel(
                            strings.questionCounter(answeredAmong(selectedAnswers, indices)),
                            strings.questionCount(indices.size),
                        ),
                        style = Naj.type.capsLabel,
                        color = colors.muted,
                    )
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

// ── Previews ──

/**
 * One test as the screen receives it: the sample questions, the answers given so far (question index
 * → answer index), and which page the pager opens on. The four questions are enough to fill the
 * question list with two sections and to leave one open for the finished test.
 */
private fun practiceState(
    questions: List<PracticeQuestion> = previewPracticeQuestions(),
    checkImmediately: Boolean = false,
    answers: Map<Int, Int> = emptyMap(),
    initialPage: Int = 0,
    allAnswered: Boolean = false,
    finished: Boolean = false,
    elapsedSeconds: Long = 412L,
    showTimeLimitToast: Boolean = false,
) = PracticeUiState.Ready(
    exam = previewExam,
    kind = AttemptKind.PracticeTest,
    testNumber = 7,
    sections = previewSections,
    questions = questions,
    checkImmediately = checkImmediately,
    answers = answers,
    initialPage = initialPage,
    allAnswered = allAnswered,
    finished = finished,
    elapsedSeconds = elapsedSeconds,
    allottedSeconds = TestAttempt.OFFICIAL_DURATION_SECONDS,
    showTimeLimitToast = showTimeLimitToast,
)

/** A test just opened: nothing answered, nothing revealed, the clock running. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PracticeScreenPreview() {
    AppPreview {
        PracticeScreen(practiceState(), onEvent = {}, onOpenResults = {})
    }
}

/** An answer picked, marked as the user's own but not yet as right or wrong. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PracticeScreenAnsweredPreview() {
    AppPreview {
        PracticeScreen(
            practiceState(answers = mapOf(0 to 1)),
            onEvent = {},
            onOpenResults = {},
        )
    }
}

/** Check-immediately mode: the picked option is final, so the wrong one is marked and the right
 * one is shown beside it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PracticeScreenCheckedPreview() {
    AppPreview {
        PracticeScreen(
            practiceState(checkImmediately = true, answers = mapOf(0 to 0, 1 to 2), initialPage = 1),
            onEvent = {},
            onOpenResults = {},
        )
    }
}

/** A finished test being reviewed, opened on the question the user left open. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PracticeScreenFinishedPreview() {
    AppPreview {
        PracticeScreen(
            practiceState(finished = true, answers = mapOf(0 to 0, 1 to 2), initialPage = 2),
            onEvent = {},
            onOpenResults = {},
        )
    }
}

/** Past the time a test is given: the clock warns, and the notice says why. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PracticeScreenOverTimePreview() {
    AppPreview {
        PracticeScreen(
            practiceState(elapsedSeconds = 1_900L, showTimeLimitToast = true),
            onEvent = {},
            onOpenResults = {},
        )
    }
}

/** A desktop window: the question list has a panel of its own beside the question. */
@Preview(widthDp = 1200, heightDp = 860)
@Composable
private fun PracticeScreenExpandedPreview() {
    AppPreview {
        PracticeScreen(
            practiceState(answers = mapOf(0 to 1), initialPage = 1),
            onEvent = {},
            onOpenResults = {},
        )
    }
}

/** A phone in landscape: the top bar goes and the test runs on one slim bar. */
@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun PracticeScreenLandscapePreview() {
    AppPreview {
        PracticeScreen(practiceState(), onEvent = {}, onOpenResults = {})
    }
}

/** The bar's list toggle as the sheet is dragged open: the menu turns into a cross as it's covered. */
@Preview(widthDp = 400, heightDp = 180)
@Composable
private fun QuestionListTogglePreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("LIST TOGGLE: SHUT, HALF COVERED, COVERED")
            PreviewRow {
                listOf(0f, 0.15f, 0.3f, 0.6f, 1f).forEach { cover ->
                    NavyPill(Modifier.width(64.dp), height = 56.dp) {
                        QuestionListToggle(expanded = cover >= 0.5f, cover = { cover }, onClick = {})
                    }
                }
            }
        }
    }
}

/** The question list as the sheet draws it: a section beside how much of it is answered. */
@Preview(widthDp = 400, heightDp = 620)
@Composable
private fun QuestionGridPreview() {
    AppPreview {
        Box(Modifier.fillMaxSize().background(Naj.colors.sheet)) {
            QuestionGrid(
                questions = previewPracticeQuestions().map { it.question },
                sections = previewSections,
                currentIndex = 2,
                selectedAnswers = mapOf(0 to 1, 1 to 0),
                revealCorrectness = false,
                finished = false,
                onSelectQuestion = {},
            )
        }
    }
}

/** A section whose title runs long: the title wraps, and the count beside it keeps its own room. */
@Preview(widthDp = 400, heightDp = 220)
@Composable
private fun QuestionGridLongTitlePreview() {
    AppPreview(language = AppLanguage.Slovak) {
        Box(Modifier.fillMaxSize().background(Naj.colors.sheet)) {
            QuestionGrid(
                questions = previewLongSectionQuestions,
                sections = previewLongSections,
                currentIndex = 0,
                selectedAnswers = mapOf(0 to 1),
                revealCorrectness = false,
                finished = false,
                onSelectQuestion = {},
            )
        }
    }
}

/** The top bar's line of progress: nothing answered, part of the test, and all of it. */
@Preview(widthDp = 400, heightDp = 180)
@Composable
private fun QuestionProgressPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("PROGRESS: NOTHING ANSWERED, HALF, ALL")
            QuestionProgress(answered = 0, questionCount = 4)
            QuestionProgress(answered = 2, questionCount = 4)
            QuestionProgress(answered = 4, questionCount = 4)
        }
    }
}
