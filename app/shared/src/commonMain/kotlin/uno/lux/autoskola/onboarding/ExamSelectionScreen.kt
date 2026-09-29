package uno.lux.autoskola.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.practice.IconArrowBack
import uno.lux.autoskola.settings.LanguageMenu

private val Background = Color(0xFFF5F5F5)
private val Subdued = Color(0xFF616161)
private val Disabled = Color(0xFFBDBDBD)

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
 * A three-step wizard for the exam to prepare for: country, then the language to take it in,
 * then the license category. [onBack] is `null` during onboarding, where a choice is required.
 */
@Composable
fun ExamSelectionScreen(
    state: ExamSelectionUiState,
    onEvent: (ExamSelectionUiEvent) -> Unit,
    /** `null` when there's nothing to go back to, e.g. on first launch. */
    onBack: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (state.banks == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }
        // Going back a step is handled here (it's pure navigation within the screen); only
        // leaving the wizard entirely goes through onBack.
        val onStepBack: (() -> Unit)? = when {
            state.step != ExamSelectionStep.ChooseCountry -> {
                { onEvent(ExamSelectionUiEvent.StepBack) }
            }

            else -> onBack
        }
        when (val step = state.step) {
            is ExamSelectionStep.ChooseCountry ->
                CountryStep(state.countryOptions, state.language, onStepBack, onEvent)

            is ExamSelectionStep.ChooseBankLanguage ->
                LanguageStep(step.banks, state.language, onStepBack, onEvent)

            is ExamSelectionStep.ChooseCategory ->
                CategoryStep(step.bank, state.language, onStepBack, onEvent)
        }
    }
}

@Composable
private fun CountryStep(
    options: List<CountryOption>,
    language: AppLanguage,
    onBack: (() -> Unit)?,
    onEvent: (ExamSelectionUiEvent) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth {
            StepHeader(
                onBack,
                language,
                onEvent,
                strings.examSelectionCountryTitle,
                strings.examSelectionCountrySubtitle,
            )
        }
        items(options, key = { it.country }) { option ->
            CountryCard(option, onClick = { onEvent(ExamSelectionUiEvent.ChooseCountry(option.country)) })
        }
    }
}

@Composable
private fun CountryCard(option: CountryOption, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .aspectRatio(1.2f)
            .shadow(if (option.available) 1.dp else 0.dp, shape)
            .clip(shape)
            .background(if (option.available) Color.White else Color(0xFFEEEEEE))
            .clickable(enabled = option.available, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(flagEmoji(option.country), fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            strings.countryName(option.country),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (option.available) Color.Black else Disabled,
            textAlign = TextAlign.Center,
        )
        if (!option.available) {
            Spacer(Modifier.height(4.dp))
            Text(strings.examSelectionCountryComingSoon, fontSize = 12.sp, color = Disabled)
        }
    }
}

@Composable
private fun LanguageStep(
    banks: List<QuestionBankInfo>,
    language: AppLanguage,
    onBack: (() -> Unit)?,
    onEvent: (ExamSelectionUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        item {
            StepHeader(
                onBack,
                language,
                onEvent,
                strings.examSelectionLanguageTitle,
                strings.examSelectionLanguageSubtitle,
            )
        }
        items(banks, key = { it.id }) { bank ->
            BigOptionRow(
                title = bankTitle(bank.language),
                onClick = { onEvent(ExamSelectionUiEvent.ChooseBank(bank)) },
            )
        }
    }
}

@Composable
private fun CategoryStep(
    bank: QuestionBankInfo,
    language: AppLanguage,
    onBack: (() -> Unit)?,
    onEvent: (ExamSelectionUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        item {
            StepHeader(onBack, language, onEvent, strings.examSelectionCategoryTitle, null)
        }
        item {
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
                            onClick = { onEvent(ExamSelectionUiEvent.ChooseCategory(category)) },
                        )
                    }
            }
        }
    }
}

/** Back button + app-language switcher, then a step's title and optional subtitle. */
@Composable
private fun StepHeader(
    onBack: (() -> Unit)?,
    language: AppLanguage,
    onEvent: (ExamSelectionUiEvent) -> Unit,
    title: String,
    subtitle: String?,
) {
    // A plain Column: a lazy grid's spanned item (unlike a lazy column's) doesn't stack bare
    // sibling composables along the main axis on its own.
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(IconArrowBack, contentDescription = strings.back) }
            }
            Spacer(Modifier.weight(1f))
            LanguageMenu(current = language, onSelect = { onEvent(ExamSelectionUiEvent.SelectLanguage(it)) })
        }
        Text(
            title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = Subdued,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BigOptionRow(title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .shadow(1.dp, shape)
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryRow(
    label: String,
    category: LicenseCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(category.icon, contentDescription = null, tint = category.tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/**
 * Regional-indicator flag emoji for an ISO 3166-1 alpha-2 country code, e.g. "SK" -> "🇸🇰".
 * Regional indicators sit above U+FFFF, so each one needs a UTF-16 surrogate pair - [Char]
 * alone can't hold a code point that high.
 */
private fun flagEmoji(countryCode: String): String = buildString {
    countryCode.uppercase().forEach { letter ->
        val codePoint = letter.code - 'A'.code + 0x1F1E6
        val offset = codePoint - 0x10000
        append(((offset shr 10) + 0xD800).toChar())
        append(((offset and 0x3FF) + 0xDC00).toChar())
    }
}
