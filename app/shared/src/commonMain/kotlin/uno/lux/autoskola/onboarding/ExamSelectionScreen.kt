package uno.lux.autoskola.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.bank.LicenseCategory
import uno.lux.autoskola.bank.QuestionBankInfo
import uno.lux.autoskola.data.preferences.TestSelection
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.settings.LanguageMenu

private val Background = Color(0xFFF5F5F5)
private val Subdued = Color(0xFF616161)
private val Selected = Color(0xFFEDE7F6)

/**
 * Connects [ExamSelectionScreen] to its [ExamSelectionViewModel]. Calls [onDone] once an exam
 * is picked; [onBack] is `null` when there's nothing to go back to.
 */
@Composable
fun ExamSelectionRoute(
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
    viewModel: ExamSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    ExamSelectionScreen(state, viewModel::onEvent, onBack)
}

/**
 * Lets the user pick the exam they're preparing for: country + language (a question bank)
 * and license group. [onBack] is `null` during onboarding, where a choice is required.
 */
@Composable
fun ExamSelectionScreen(
    state: ExamSelectionUiState,
    onEvent: (ExamSelectionUiEvent) -> Unit,
    /** `null` when there's nothing to go back to, e.g. on first launch. */
    onBack: (() -> Unit)?,
) {
    val banks = state.banks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (banks == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(IconArrowBack, contentDescription = strings.back) }
                    }
                    Spacer(Modifier.weight(1f))
                    LanguageMenu(current = state.language, onSelect = {
                        onEvent(ExamSelectionUiEvent.SelectLanguage(it))
                    })
                }
                Text(
                    strings.examSelectionTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.examSelectionSubtitle,
                    color = Subdued,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            banks.groupBy { it.country }.forEach { (country, countryBanks) ->
                item(key = country) {
                    Text(
                        strings.countryName(country),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 24.dp),
                    )
                }
                items(countryBanks, key = { it.id }) { bank ->
                    BankCard(
                        bank = bank,
                        selectedCategory = state.current?.takeIf { it.bankId == bank.id }?.category,
                        onSelect = { onEvent(ExamSelectionUiEvent.Select(TestSelection(bank.id, it))) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BankCard(bank: QuestionBankInfo, selectedCategory: LicenseCategory?, onSelect: (LicenseCategory) -> Unit) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            bankTitle(bank.language),
            color = Subdued,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, shape)
                .clip(shape)
                .background(Color.White),
        ) {
            LicenseCategory.entries
                .filter { (bank.questionCounts[it] ?: 0) > 0 }
                .forEach { category ->
                    CategoryRow(
                        label = category.displayName(bank.language),
                        category = category,
                        questionCount = bank.questionCounts.getValue(category),
                        selected = category == selectedCategory,
                        onClick = { onSelect(category) },
                    )
                }
        }
    }
}

@Composable
private fun CategoryRow(
    label: String,
    category: LicenseCategory,
    questionCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Selected else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(category.icon, contentDescription = null, tint = category.tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text("$questionCount", color = Subdued, fontSize = 14.sp)
    }
}
