package com.spiraclestudios.autoskola.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.onboarding.LicenceIcon
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.NajSwitch
import com.spiraclestudios.autoskola.ui.SectionLabel
import com.spiraclestudios.autoskola.ui.previewExam
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import org.koin.compose.viewmodel.koinViewModel

/** Connects [SettingsScreen] to its [SettingsViewModel] and to navigation. */
@Composable
fun SettingsRoute(onChangeExam: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onEvent, onChangeExam)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit, onChangeExam: () -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors, mintY = 0.7f))) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 52.dp, bottom = 24.dp + LocalNavBarInset.current),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.settingsTitle,
                        style = Naj.type.screenTitle,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    LanguageMenu(
                        current = state.language,
                        onSelect = { onEvent(SettingsUiEvent.SelectLanguage(it)) },
                    )
                }
                SectionLabel(strings.settingsExam, Modifier.padding(start = 6.dp))
                NajCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onChangeExam)
                            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // The same icon and colour the licence step offers the category in.
                        state.exam?.let { LicenceIcon(it.category) }
                        Column(Modifier.weight(1f)) {
                            Text(strings.settingsChangeExam, style = Naj.type.rowTitle, color = colors.ink)
                            Text(
                                state.exam?.shortTitle.orEmpty(),
                                style = Naj.type.rowSubtitle,
                                color = colors.muted,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.primaryDeep,
                        )
                    }
                }

                SectionLabel(strings.settingsAnswers, Modifier.padding(start = 6.dp))
                NajCard {
                    SettingsSwitchRow(
                        title = strings.settingsCheckImmediately,
                        description = strings.settingsCheckImmediatelyDescription,
                        checked = state.checkAnswersImmediately,
                        icon = Icons.Filled.Check,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetCheckAnswersImmediately(it)) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsSwitchRow(
                        title = strings.settingsAdvanceAfterAnswer,
                        description = strings.settingsAdvanceAfterAnswerDescription,
                        checked = state.advanceAfterAnswer,
                        enabled = state.canAdvanceAfterAnswer,
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        iconTint = colors.disabledOn,
                        iconBackground = colors.neutralTint,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetAdvanceAfterAnswer(it)) },
                    )
                }
            }
        }
    }
}

/**
 * One setting with its switch; the whole row toggles it. [enabled] is false for a setting that
 * another one rules out, which greys the row out and leaves the switch off.
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val colors = Naj.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(size = 40.dp, background = Brush.linearGradient(listOf(iconBackground, iconBackground))) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Naj.type.rowTitle,
                color = if (enabled) colors.ink else colors.disabledOn,
            )
            Spacer(Modifier.height(3.dp))
            Text(description, style = Naj.type.rowSubtitle, color = colors.muted)
        }
        Spacer(Modifier.width(4.dp))
        NajSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

// ── Previews ──

/** Answers scored at the end, so moving on after an answer is available. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun SettingsScreenPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.English,
                checkAnswersImmediately = false,
                advanceAfterAnswer = true,
                exam = previewExam,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}

/** Checking answers as they're picked, which greys out the setting it rules out. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun SettingsScreenCheckingPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.Slovak,
                checkAnswersImmediately = true,
                advanceAfterAnswer = false,
                exam = previewExam,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}

/** No exam yet: the row keeps its place without a category to name. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun SettingsScreenNoExamPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.English,
                checkAnswersImmediately = false,
                advanceAfterAnswer = false,
                exam = null,
            ),
            onEvent = {},
            onChangeExam = {},
        )
    }
}
