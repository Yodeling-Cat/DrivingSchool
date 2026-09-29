package uno.lux.autoskola.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.bank.PracticeTest
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.onboarding.ExamLabel
import uno.lux.autoskola.practice.attemptName
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.WidthClass

private val CardBorder = Color(0xFFE0E0E0)
private val Subdued = Color(0xFF757575)
private val Accent = Color(0xFF5E35B1)
private val PassedColor = Color(0xFF388E3C)
private val FailedColor = Color(0xFFD32F2F)

private val InProgressColor = Color(0xFFF57C00)
private val ResumeBg = Color(0xFFFFF3E0)

private val SidePaneWidth = 360.dp
private val TestTileMinWidth = 240.dp

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
 * Progress overview and test picker. Phones get one column; wider screens show the tests as a
 * grid, and expanded screens move the overview into a side pane next to the grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState.Ready, onEvent: (HomeUiEvent) -> Unit, onChangeExam: () -> Unit) {
    val tests = state.tests
    val progress = state.progress
    val twoPane = LocalWidthClass.current == WidthClass.Expanded

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text(strings.appName, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val overview: @Composable () -> Unit = {
                Overview(
                    exam = state.exam,
                    tests = tests,
                    progress = progress,
                    onChangeExam = onChangeExam,
                    onEvent = onEvent,
                )
            }
            Row(Modifier.widthIn(max = ContentWidth.Wide).fillMaxSize()) {
                if (twoPane) {
                    Column(
                        Modifier
                            .width(SidePaneWidth)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) { overview() }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(TestTileMinWidth),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!twoPane) fullWidth { overview() }
                    fullWidth {
                        Text(
                            strings.allTests,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = if (twoPane) 0.dp else 12.dp),
                        )
                    }
                    items(tests, key = { it.number }) { test ->
                        TestTile(
                            name = strings.testName(test.number),
                            result = progress.results[test.number],
                            inProgress = test.number in progress.inProgress,
                            onClick = { onEvent(HomeUiEvent.StartTest(test.number)) },
                        )
                    }
                }
            }
        }
    }

    state.resumePrompt?.let { ResumePromptDialog(it, onEvent) }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Overview(
    exam: ExamLabel,
    tests: List<PracticeTest>,
    progress: HomeProgress,
    onChangeExam: () -> Unit,
    onEvent: (HomeUiEvent) -> Unit,
) {
    val unfinished = tests.filter { it.number !in progress.results && it.number !in progress.inProgress }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        progress.resumable?.let {
            ResumeCard(
                it,
                onResume = { onEvent(HomeUiEvent.Resume) },
                onDiscard = { onEvent(HomeUiEvent.DiscardResumable) },
            )
        }
        ProgressCard(
            examName = exam.name,
            examLanguage = exam.title,
            totalTests = tests.size,
            progress = progress,
            onChangeExam = onChangeExam,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onEvent(HomeUiEvent.StartRandomTest(notDoneYet = false)) },
                enabled = tests.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.randomTest)
            }
            OutlinedButton(
                onClick = { onEvent(HomeUiEvent.StartRandomTest(notDoneYet = true)) },
                enabled = unfinished.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.notDoneYet)
            }
        }
        MistakesCard(count = progress.mistakeCount, onPractice = { onEvent(HomeUiEvent.PracticeMistakes) })
    }
}

@Composable
private fun ResumeCard(test: ResumableTest, onResume: () -> Unit, onDiscard: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ResumeBg)
            .clickable(onClick = onResume)
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
    ) {
        Text(strings.unfinishedTest, color = InProgressColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            strings.resumeSummary(attemptName(test.kind, test.testNumber), test.answered, test.total),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDiscard) { Text(strings.discard, color = Subdued) }
            TextButton(onClick = onResume) { Text(strings.continueTest, color = InProgressColor) }
        }
    }
}

/** Asks what to do with a test that's already under way: continue it, or start over. */
@Composable
private fun ResumePromptDialog(prompt: ResumePrompt, onEvent: (HomeUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(HomeUiEvent.DismissResumePrompt) },
        title = { Text(strings.resumePromptTitle) },
        text = {
            Text(strings.resumeSummary(strings.testName(prompt.testNumber), prompt.answered, prompt.total))
        },
        confirmButton = {
            TextButton(onClick = { onEvent(HomeUiEvent.ContinueTest) }) {
                Text(strings.continueTest)
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(HomeUiEvent.StartOver) }) {
                Text(strings.startOver)
            }
        },
    )
}

@Composable
private fun MistakesCard(count: Int, onPractice: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val enabled = count > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, CardBorder, shape)
            .clickable(enabled = enabled, onClick = onPractice)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                strings.practiceMistakes,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color.Unspecified else Subdued,
            )
            Text(strings.mistakesToReview(count), color = if (enabled) FailedColor else Subdued, fontSize = 13.sp)
        }
        if (enabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Accent)
        }
    }
}

@Composable
private fun ProgressCard(
    examName: String,
    examLanguage: String,
    totalTests: Int,
    progress: HomeProgress,
    onChangeExam: () -> Unit,
) {
    val results = progress.results
    val completed = results.size
    val passed = results.values.count { it.passed }
    val averagePercent = if (results.isEmpty()) null else results.values.sumOf { it.percent } / results.size
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, CardBorder, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(examName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(examLanguage, color = Subdued, fontSize = 13.sp)
            }
            TextButton(onClick = onChangeExam) { Text(strings.change, color = Accent) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            strings.testsFinished(completed, totalTests),
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { if (totalTests == 0) 0f else completed.toFloat() / totalTests },
            color = Accent,
            trackColor = CardBorder,
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${strings.questionsAnswered}: ${progress.questionsAnswered} / ${progress.questionPoolSize}",
            color = Subdued,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Stat(label = strings.passed, value = "$passed", modifier = Modifier.weight(1f))
            Stat(label = strings.failed, value = "${completed - passed}", modifier = Modifier.weight(1f))
            Stat(
                label = strings.averageScore,
                value = averagePercent?.let { "$it %" } ?: "–",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(label, color = Subdued, fontSize = 12.sp)
    }
}

@Composable
private fun TestTile(
    name: String,
    result: TestResult?,
    inProgress: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (inProgress) {
            Text(strings.inProgress, color = InProgressColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        } else if (result == null) {
            Text(strings.notStarted, color = Subdued, fontSize = 13.sp)
        } else {
            Text(
                strings.resultSummary(result.score, result.maxScore, result.passed),
                color = if (result.passed) PassedColor else FailedColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
