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
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.data.preferences.LegalDocument
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.onboarding.LicenceIcon
import com.spiraclestudios.autoskola.openUrl
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
fun SettingsRoute(onChangeExam: () -> Unit, onOpenAbout: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onChangeExam = onChangeExam,
        onOpenAbout = onOpenAbout,
        onOpenDocument = { openUrl(it.url) },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onChangeExam: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
) {
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
                SettingsNavRow(
                    title = strings.settingsChangeExam,
                    subtitle = state.exam?.shortTitle.orEmpty(),
                    onClick = onChangeExam,
                    // The same icon and colour the licence step offers the category in.
                    leading = { state.exam?.let { LicenceIcon(it.category) } },
                )

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

                SectionLabel(strings.settingsPrivacy, Modifier.padding(start = 6.dp))
                NajCard {
                    // The documents stay readable after onboarding, which is the other half of
                    // agreeing to them. They open in a browser; the answers below are what the
                    // user can actually change.
                    SettingsNavRow(
                        title = strings.legalPrivacyPolicy,
                        onClick = { onOpenDocument(LegalDocument.PrivacyPolicy) },
                        leading = { SettingsRowIcon(Icons.Outlined.Policy, colors.primaryDeep, colors.mintGlow) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsNavRow(
                        title = strings.legalTermsOfUse,
                        onClick = { onOpenDocument(LegalDocument.TermsOfUse) },
                        leading = { SettingsRowIcon(Icons.Outlined.Description, colors.primaryDeep, colors.mintGlow) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsSwitchRow(
                        title = strings.consentCrashReportingTitle,
                        description = strings.consentCrashReportingDescription,
                        checked = state.crashReportingEnabled,
                        icon = Icons.Outlined.BugReport,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetCrashReporting(it)) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsSwitchRow(
                        title = strings.consentAnalyticsTitle,
                        description = strings.consentAnalyticsDescription,
                        checked = state.analyticsEnabled,
                        icon = Icons.Outlined.Analytics,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetAnalytics(it)) },
                    )
                }

                SectionLabel(strings.settingsApp, Modifier.padding(start = 6.dp))
                SettingsNavRow(
                    title = strings.aboutApp,
                    onClick = onOpenAbout,
                    leading = { SettingsRowIcon(Icons.Outlined.Info, colors.primaryDeep, colors.mintGlow) },
                )
            }
        }
    }
}

/**
 * A setting that opens another screen: a mark, what it is, and the chevron that says it goes
 * somewhere. The whole row is the target.
 */
@Composable
private fun SettingsNavRow(
    title: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val colors = Naj.colors
    NajCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading()
            Column(Modifier.weight(1f)) {
                Text(title, style = Naj.type.rowTitle, color = colors.ink)
                subtitle?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(it, style = Naj.type.rowSubtitle, color = colors.muted)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.primaryDeep,
            )
        }
    }
}

/** The square mark a setting leads with, on the colour the setting is about. */
@Composable
private fun SettingsRowIcon(icon: ImageVector, tint: Color, background: Color) {
    IconTile(size = 40.dp, background = Brush.linearGradient(listOf(background, background))) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
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
    icon: ImageVector,
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
        SettingsRowIcon(icon, iconTint, iconBackground)
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
                crashReportingEnabled = true,
                analyticsEnabled = false,
                exam = previewExam,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenDocument = {},
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
                crashReportingEnabled = false,
                analyticsEnabled = true,
                exam = previewExam,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenDocument = {},
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
                crashReportingEnabled = true,
                analyticsEnabled = true,
                exam = null,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenDocument = {},
        )
    }
}
