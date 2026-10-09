package com.spiraclestudios.autoskola.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.settings.LanguageMenu
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.CardDivider
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconArrowBack
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.previewBankInfo
import com.spiraclestudios.autoskola.ui.previewBanks
import com.spiraclestudios.autoskola.ui.theme.AccentedText
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.accentWord
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import org.koin.compose.viewmodel.koinViewModel

private val StepBarWidth = 28.dp
private val StepBarHeight = 6.dp
private val CountryCardHeight = 150.dp
private val FlagHeight = 42.dp

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
 * Its back button and the system's walk the same steps, and both only leave the screen from the
 * first one.
 */
@Composable
fun ExamSelectionScreen(
    state: ExamSelectionUiState,
    onEvent: (ExamSelectionUiEvent) -> Unit,
    /** `null` when there's nothing to go back to, e.g. on first launch. */
    onBack: (() -> Unit)?,
) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors))) {
        if (state.banks == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.primaryDeep)
            return@Box
        }
        // Going back a step is handled here (it's pure navigation within the screen); only
        // leaving the wizard entirely goes through onBack.
        val onStepBack: (() -> Unit)? = when {
            state.hasPreviousStep -> {
                { onEvent(ExamSelectionUiEvent.StepBack) }
            }

            else -> onBack
        }
        // The system's back button - Android's gesture or key - walks the wizard exactly as the
        // header's button does. With no step behind this one it stands aside, so back then does
        // what it otherwise would: leave the screen (and, in onboarding, the app). Placed with the
        // header's decision so the two can't disagree about where a step starts.
        val systemBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = systemBackState,
            isBackEnabled = state.hasPreviousStep,
            onBackCompleted = { onEvent(ExamSelectionUiEvent.StepBack) },
        )
        // Only onboarding offers the app-language picker; when the user opened this from another
        // screen, the language is changed in settings.
        val pickerLanguage = state.language.takeIf { onBack == null }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            StepHeader(onStepBack, pickerLanguage, onEvent)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                when (val step = state.step) {
                    is ExamSelectionStep.ChooseCountry -> CountryStep(state.countryOptions, onEvent)
                    is ExamSelectionStep.ChooseBankLanguage -> LanguageStep(step.banks, onEvent)
                    is ExamSelectionStep.ChooseCategory -> CategoryStep(step.bank, onEvent)
                }
            }
        }
    }
}

/** Back button, the app-language switcher, then the step's title and subtitle. */
@Composable
private fun StepHeader(onBack: (() -> Unit)?, language: AppLanguage?, onEvent: (ExamSelectionUiEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RoundButton(
                icon = IconArrowBack,
                contentDescription = strings.back,
                onClick = onBack,
            )
        }
        Spacer(Modifier.weight(1f))
        if (language != null) {
            LanguageMenu(current = language, onSelect = { onEvent(ExamSelectionUiEvent.SelectLanguage(it)) })
        }
    }
}

/** The step's title, the coral italic word included, and the wizard's progress bars above it. */
@Composable
private fun StepTitle(
    filled: Int,
    before: String,
    accent: String,
    subtitle: String?,
) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .size(StepBarWidth, StepBarHeight)
                        .clip(RoundedCornerShape(StepBarHeight / 2))
                        .background(if (index < filled) colors.primaryDeep else colors.emptyOutline),
                )
            }
        }
        AccentedText(
            accentWord(before, accent),
            style = Naj.type.stepTitle,
            modifier = Modifier.padding(top = 18.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = Naj.type.button,
                color = colors.muted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun CountryStep(options: List<CountryOption>, onEvent: (ExamSelectionUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = ContentWidth.Narrow)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        StepTitle(
            filled = 1,
            before = strings.examSelectionCountryTitleBefore,
            accent = strings.examSelectionCountryTitleAccent,
            subtitle = strings.examSelectionCountrySubtitle,
        )
        Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { option ->
                        CountryCard(
                            option,
                            modifier = Modifier.weight(1f),
                            onClick = { onEvent(ExamSelectionUiEvent.ChooseCountry(option.country)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryCard(option: CountryOption, modifier: Modifier, onClick: () -> Unit) {
    val colors = Naj.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .height(CountryCardHeight)
            .clip(shape)
            .background(if (option.available) colors.card else colors.card.copy(alpha = 0.45f))
            .then(
                if (option.available) Modifier else Modifier.border(1.5.dp, colors.emptyOutline, shape),
            )
            .clickable(enabled = option.available, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val flag = countryFlag(option.country)
        if (flag != null) {
            Image(
                painter = rememberVectorPainter(flag),
                contentDescription = null,
                colorFilter = if (option.available) {
                    null
                } else {
                    // The design washes a country without a bank out to gray.
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })
                },
                // A flag is drawn as it is: square to its own edges, with no rounded corner and no
                // shadow under it. Flags differ in proportion - Hungary's is 2:1, Poland's 8:5 - so
                // the vector's own shape decides the width; every flag comes out [FlagHeight] tall.
                modifier = Modifier
                    .height(FlagHeight)
                    .width(FlagHeight * (flag.defaultWidth / flag.defaultHeight))
                    .graphicsLayer { alpha = if (option.available) 1f else 0.55f },
            )
        } else {
            Text(option.country, style = Naj.type.cardHero, color = colors.muted)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            strings.countryName(option.country),
            style = Naj.type.countryName,
            color = if (option.available) colors.ink else colors.disabledOn,
        )
        if (!option.available) {
            Spacer(Modifier.height(2.dp))
            Text(strings.examSelectionCountryComingSoon, style = Naj.type.secondary, color = colors.disabledOn)
        }
    }
}

@Composable
private fun LanguageStep(banks: List<QuestionBankInfo>, onEvent: (ExamSelectionUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = ContentWidth.Narrow)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        StepTitle(
            filled = 2,
            before = strings.examSelectionLanguageTitleBefore,
            accent = strings.examSelectionLanguageTitleAccent,
            subtitle = strings.examSelectionLanguageSubtitle,
        )
        Spacer(Modifier.height(32.dp))
        NajCard {
            banks.forEachIndexed { index, bank ->
                if (index > 0) CardDivider()
                GroupedRow(
                    title = bankTitle(bank.language),
                    onClick = { onEvent(ExamSelectionUiEvent.ChooseBank(bank)) },
                )
            }
        }
    }
}

@Composable
private fun CategoryStep(bank: QuestionBankInfo, onEvent: (ExamSelectionUiEvent) -> Unit) {
    val bankLanguage = AppLanguage.fromCode(bank.language)?.nativeName ?: bank.language
    Column(
        modifier = Modifier
            .widthIn(max = ContentWidth.Narrow)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        StepTitle(
            filled = 3,
            before = strings.examSelectionCategoryTitleBefore,
            accent = strings.examSelectionCategoryTitleAccent,
            subtitle = "${strings.countryName(bank.country)} \u00b7 $bankLanguage",
        )
        Spacer(Modifier.height(32.dp))
        NajCard {
            LicenseCategory.entries
                .filter { (bank.questionCounts[it] ?: 0) > 0 }
                .forEachIndexed { index, category ->
                    if (index > 0) CardDivider()
                    CategoryRow(
                        label = category.displayName(bank.language),
                        category = category,
                        onClick = { onEvent(ExamSelectionUiEvent.ChooseCategory(category)) },
                    )
                }
        }
    }
}

/** A row in a grouped list: 64pt tall, its leading tile, a 16pt bold label and a chevron. */
@Composable
private fun GroupedRow(title: String, onClick: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = Naj.type.rowTitle, color = colors.ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.primaryDeep)
    }
}

/** The license categories, with the icons and names the app already uses for them. */
@Composable
private fun CategoryRow(label: String, category: LicenseCategory, onClick: () -> Unit) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LicenseIcon(category)
        Text(label, style = Naj.type.rowTitle, color = colors.ink, modifier = Modifier.weight(1f))
    }
}

// ── Previews ──

/** A step of the wizard with the sample banks behind it. */
private fun selectionState(step: ExamSelectionStep) = ExamSelectionUiState(
    banks = previewBanks(),
    step = step,
    language = AppLanguage.English,
)

/** The wizard's three steps, plus the flash of the spinner while the banks are read. */

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ExamSelectionCountryStepPreview() {
    AppPreview {
        ExamSelectionScreen(
            state = selectionState(ExamSelectionStep.ChooseCountry),
            onEvent = {},
            // Onboarding: there is nothing behind this screen, and the language is picked here.
            onBack = null,
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ExamSelectionLanguageStepPreview() {
    AppPreview {
        ExamSelectionScreen(
            state = selectionState(ExamSelectionStep.ChooseBankLanguage("SK", previewBanks())),
            onEvent = {},
            onBack = null,
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ExamSelectionCategoryStepPreview() {
    AppPreview {
        ExamSelectionScreen(
            state = selectionState(ExamSelectionStep.ChooseCategory(previewBankInfo("sk-sk"))),
            onEvent = {},
            onBack = null,
        )
    }
}

/** Opened from settings: the back button is there, and the language moves out of the header. */
@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ExamSelectionChangeExamPreview() {
    AppPreview {
        ExamSelectionScreen(
            state = selectionState(ExamSelectionStep.ChooseCountry),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ExamSelectionLoadingPreview() {
    AppPreview {
        ExamSelectionScreen(
            state = ExamSelectionUiState(
                banks = null,
                step = ExamSelectionStep.ChooseCountry,
                language = AppLanguage.English,
            ),
            onEvent = {},
            onBack = null,
        )
    }
}
