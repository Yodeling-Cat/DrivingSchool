package uno.lux.autoskola.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val SelectedAnswerBg = Color(0xFFEDE7F6)
private val SelectedAnswerAccent = Color(0xFF5E35B1)
private val CorrectAnswerBg = Color(0xFFE8F5E9)
private val CorrectAnswerAccent = Color(0xFF388E3C)
private val WrongAnswerBg = Color(0xFFFFEBEE)
private val WrongAnswerAccent = Color(0xFFD32F2F)
private val UnselectedLetter = Color(0xFFBDBDBD)
private val CardBorder = Color(0xFFE0E0E0)
private val Subdued = Color(0xFF757575)

private val QuestionChipBg = Color(0xFFEEEEEE)
private val QuestionChipAnsweredBg = Color(0xFFD1C4E9)
private val QuestionChipCorrectBg = Color(0xFFC8E6C9)
private val QuestionChipIncorrectBg = Color(0xFFFFCDD2)
private val QuestionChipCurrentBorder = Color(0xFF5E35B1)

private val QuestionPanelWidth = 320.dp

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
 * straight away. Once every question is answered, a snackbar offers to finish the test; once
 * it's finished, every answer is revealed and the snackbar leads back to the results.
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
    val group = state.exam.name
    val checkImmediately = state.checkImmediately
    val finished = state.finished
    val revealCorrectness = revealsCorrectness(checkImmediately, finished)
    val selectedAnswers = state.answers
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { questions.size })
    val currentIndex = pagerState.currentPage
    val sheetState = rememberStandardBottomSheetState(skipHiddenState = true)
    val scope = rememberCoroutineScope()
    val current = questions[currentIndex]
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

    fun goTo(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index.coerceIn(0, questions.lastIndex)) }
    }

    val listExpanded = !showPanel && sheetState.targetValue == SheetValue.Expanded

    fun toggleList() {
        scope.launch { if (listExpanded) sheetState.partialExpand() else sheetState.expand() }
    }

    // Stays up until acted on: first to finish the test, then to get back to its results.
    val snackbarHostState = remember { SnackbarHostState() }
    val prompt = finishPrompt(state.allAnswered, finished)
    val promptMessage = when (prompt) {
        FinishPrompt.Finish -> strings.allQuestionsAnswered
        FinishPrompt.ShowResults -> strings.testFinished
        null -> null
    }
    val promptAction = when (prompt) {
        FinishPrompt.Finish -> strings.finish
        FinishPrompt.ShowResults -> strings.showResults
        null -> null
    }
    LaunchedEffect(prompt, promptMessage) {
        snackbarHostState.currentSnackbarData?.dismiss()
        if (prompt == null || promptMessage == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(promptMessage, promptAction, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) {
            when (prompt) {
                FinishPrompt.Finish -> onEvent(PracticeUiEvent.Finish)
                FinishPrompt.ShowResults -> onOpenResults()
            }
        }
    }

    val bottomBarPadding = if (compactHeight) PaddingValues(horizontal = 4.dp) else BottomAppBarDefaults.ContentPadding
    val barHeight = if (compactHeight) 56.dp else 80.dp

    val topBar: @Composable () -> Unit = {
        if (!compactHeight) {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(testName, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(group, color = Subdued, fontSize = 12.sp)
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
    }

    val barContent: @Composable RowScope.() -> Unit = {
        PracticeBarContent(
            compactHeight = compactHeight,
            testName = testName,
            currentIndex = currentIndex,
            questionCount = questions.size,
            points = current.points,
            showListToggle = !showPanel,
            listExpanded = listExpanded,
            canFinish = !finished,
            onBack = onBack,
            onToggleList = ::toggleList,
            onPrevious = { goTo(currentIndex - 1) },
            onNext = { goTo(currentIndex + 1) },
            onFinish = { onEvent(PracticeUiEvent.Finish) },
        )
    }

    val pagerContent: @Composable (PaddingValues) -> Unit = { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { pagerWidth = it.width },
                verticalAlignment = Alignment.Top,
            ) { page ->
                QuestionPage(
                    question = questions[page],
                    imageUri = state.questions[page].imageUri,
                    selectedIndex = selectedAnswers[page],
                    locked = isAnswerLocked(checkImmediately, finished, selectedAnswers[page]),
                    onSelect = { onEvent(PracticeUiEvent.Answer(page, it)) },
                )
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
                BottomAppBar(containerColor = Color.White, contentPadding = bottomBarPadding, content = barContent)
            },
            content = pagerContent,
        )
    } else {
        // The question list is a sheet pinned to the bottom: collapsed it shows just the bar.
        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val expandProgress by animateFloatAsState(if (listExpanded) 1f else 0f)
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(sheetState, snackbarHostState),
            containerColor = Color.White,
            sheetContainerColor = Color.White,
            sheetPeekHeight = barHeight + navBarInset,
            sheetDragHandle = null,
            topBar = topBar,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            sheetContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .padding(horizontal = if (compactHeight) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = barContent,
                )
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    QuestionGrid(
                        questions = questions,
                        sections = sections,
                        currentIndex = currentIndex,
                        selectedAnswers = selectedAnswers,
                        revealCorrectness = revealCorrectness,
                        onSelectQuestion = {
                            goTo(it)
                            scope.launch { sheetState.partialExpand() }
                        },
                    )
                    // Room for the navigation bar under the grid; collapsed, the peek height covers it.
                    Spacer(Modifier.height(navBarInset * expandProgress))
                }
            },
            content = pagerContent,
        )
    }
}

@Composable
private fun RowScope.PracticeBarContent(
    compactHeight: Boolean,
    testName: String,
    currentIndex: Int,
    questionCount: Int,
    points: Int,
    showListToggle: Boolean,
    listExpanded: Boolean,
    canFinish: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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
        Text(strings.points(points), color = Subdued, fontSize = 12.sp)
    }
    // Swiping doesn't work with a mouse, so always offer buttons too. They make no sense while
    // the question list covers the questions.
    AnimatedVisibility(!listExpanded) {
        Row {
            IconButton(onClick = onPrevious, enabled = currentIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = strings.previousQuestion)
            }
            IconButton(onClick = onNext, enabled = currentIndex < questionCount - 1) {
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
                    answers()
                    Spacer(Modifier.height(16.dp))
                }
                Box(Modifier.weight(2f)) {
                    QuestionImage(uri = imageUri!!, maxHeight = 220.dp)
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
                    QuestionImage(uri = uri)
                }
                Spacer(Modifier.height(16.dp))
                answers()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuestionImage(uri: String, maxHeight: Dp = 260.dp) {
    val painter = rememberAsyncImagePainter(uri)
    val imageState by painter.state.collectAsState()
    if (imageState is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
        )
    } else {
        // Keep the space, so the answers don't jump when the image arrives.
        Spacer(Modifier.height(maxHeight))
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
