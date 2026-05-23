package uno.lux.autoskola.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SelectedAnswerBg = Color(0xFFEEF5DA)
private val SelectedAnswerAccent = Color(0xFF7CB342)
private val UnselectedLetter = Color(0xFFBDBDBD)
private val CardBorder = Color(0xFFE0E0E0)
private val Subdued = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    questions: List<Question> = sampleQuestions,
    testName: String = "Test 1",
    group: String = "Group A,B",
    onBack: () -> Unit = {},
) {
    var currentIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    val current = questions[currentIndex]
    val selectedIndex = selectedAnswers[currentIndex]

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            testName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Text(
                            group,
                            color = Subdued,
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(IconArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White) {
                IconButton(onClick = { /* TODO: open question list */ }) {
                    Icon(IconMenu, contentDescription = "Question list")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Question ${currentIndex + 1} out of ${questions.size}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Text(
                        "${current.points} points",
                        color = Subdued,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = { /* TODO: overflow menu */ }) {
                    Icon(IconMoreVert, contentDescription = "More options")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                current.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(16.dp))
            AnswerCard(
                options = current.options,
                selectedIndex = selectedIndex,
                onSelect = { selectedAnswers[currentIndex] = it },
            )
        }
    }
}

@Composable
private fun AnswerCard(
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
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
                isSelected = selectedIndex == index,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun AnswerRow(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) SelectedAnswerBg else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            letter,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) SelectedAnswerAccent else UnselectedLetter,
            modifier = Modifier.width(40.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (isSelected) {
                Text(
                    "YOUR ANSWER",
                    color = Subdued,
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
