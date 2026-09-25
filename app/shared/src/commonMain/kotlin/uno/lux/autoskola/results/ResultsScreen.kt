package uno.lux.autoskola.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import org.koin.core.parameter.parametersOf
import uno.lux.autoskola.TestResult
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.practice.attemptName
import uno.lux.autoskola.ui.ContentWidth
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.LocalWidthClass
import uno.lux.autoskola.ui.WidthClass

private val Accent = Color(0xFF5E35B1)
private val PassedColor = Color(0xFF388E3C)
private val PassedBg = Color(0xFFE8F5E9)
private val FailedColor = Color(0xFFD32F2F)
private val FailedBg = Color(0xFFFFEBEE)
private val CardBorder = Color(0xFFE0E0E0)
private val Subdued = Color(0xFF757575)
private val TrackColor = Color(0xFFEEEEEE)

/**
 * Connects [ResultsScreen] to its [ResultsViewModel]. [onReview] goes back to the test with
 * its answers revealed, [onDone] back to the list of tests.
 */
@Composable
fun ResultsRoute(
    attemptId: String,
    onReview: () -> Unit,
    onDone: () -> Unit,
    viewModel: ResultsViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        ResultsUiState.Loading -> LoadingScreen()
        ResultsUiState.Closed -> LaunchedEffect(Unit) { onReview() }
        is ResultsUiState.Ready -> ResultsScreen(current, onReview = onReview, onDone = onDone)
    }
}

/** How a finished test went: the score, right and wrong answers, and a breakdown by section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(state: ResultsUiState.Ready, onReview: () -> Unit, onDone: () -> Unit) {
    // Expanded screens show the breakdown next to the score instead of below it.
    val twoPane = LocalWidthClass.current == WidthClass.Expanded
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.resultsTitle, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            "${attemptName(state.kind, state.testNumber)} · ${state.exam.name}",
                            color = Subdued,
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onReview) {
                        Icon(IconArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Box(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            val overview: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScoreCard(state.summary.result)
                    Counts(state)
                    Actions(onReview = onReview, onDone = onDone)
                }
            }
            if (twoPane) {
                Row(
                    Modifier.widthIn(max = ContentWidth.Wide).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(Modifier.weight(1f)) { overview() }
                    Box(Modifier.weight(1f)) { Sections(state.sections) }
                }
            } else {
                Column(
                    Modifier.widthIn(max = ContentWidth.Narrow).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    overview()
                    Sections(state.sections)
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(result: TestResult) {
    val color = if (result.passed) PassedColor else FailedColor
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (result.passed) PassedBg else FailedBg)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (result.passed) strings.youPassed else strings.youFailed,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text("${result.score} / ${result.maxScore}", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("${result.percent} %", color = Subdued, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(strings.passMark(TestResult.PASS_PERCENT), color = Subdued, fontSize = 13.sp)
    }
}

@Composable
private fun Counts(state: ResultsUiState.Ready) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CountTile(strings.correctCount, state.summary.correct, PassedColor, Modifier.weight(1f))
        CountTile(strings.wrongCount, state.summary.wrong, FailedColor, Modifier.weight(1f))
        CountTile(strings.unansweredCount, state.summary.unanswered, Subdued, Modifier.weight(1f))
    }
}

@Composable
private fun CountTile(label: String, count: Int, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .border(1.dp, CardBorder, shape)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Subdued, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun Actions(onReview: () -> Unit, onDone: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f)) {
            Text(strings.reviewAnswers, maxLines = 1)
        }
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            modifier = Modifier.weight(1f),
        ) {
            Text(strings.backToTests, maxLines = 1)
        }
    }
}

@Composable
private fun Sections(sections: List<ResultSection>) {
    val shape = RoundedCornerShape(12.dp)
    Column {
        Text(strings.bySection, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, CardBorder, shape)) {
            sections.forEachIndexed { index, section ->
                if (index > 0) HorizontalDivider(color = CardBorder)
                SectionRow(section)
            }
        }
    }
}

@Composable
private fun SectionRow(section: ResultSection) {
    val summary = section.summary
    val allRight = summary.correct == summary.total
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                "${summary.correct} / ${summary.total}",
                color = if (allRight) PassedColor else FailedColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (summary.total == 0) 0f else summary.correct.toFloat() / summary.total },
            modifier = Modifier.fillMaxWidth(),
            color = if (allRight) PassedColor else Accent,
            trackColor = TrackColor,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
