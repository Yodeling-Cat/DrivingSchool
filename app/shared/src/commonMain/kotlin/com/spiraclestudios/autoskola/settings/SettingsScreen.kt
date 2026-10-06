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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.copyToClipboard
import com.spiraclestudios.autoskola.data.preferences.LegalDocument
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.onboarding.LicenseIcon
import com.spiraclestudios.autoskola.openUrl
import com.spiraclestudios.autoskola.ui.AgeAnswerRow
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.CardRadius
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconTile
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.NajSwitch
import com.spiraclestudios.autoskola.ui.OutlinePill
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.SectionLabel
import com.spiraclestudios.autoskola.ui.previewExam
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import org.koin.compose.viewmodel.koinViewModel

/** Connects [SettingsScreen] to its [SettingsViewModel] and to navigation. */
@Composable
fun SettingsRoute(
    onChangeExam: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeedback: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onChangeExam = onChangeExam,
        onOpenAbout = onOpenAbout,
        onOpenFeedback = onOpenFeedback,
        onOpenDocument = { openUrl(it.url) },
        onCopyIdentifier = { identifier ->
            copyToClipboard(identifier)
            viewModel.onEvent(SettingsUiEvent.DeviceIdentifierCopied)
        },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onChangeExam: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
    onCopyIdentifier: (String) -> Unit,
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
                    // The same icon and color the license step offers the category in.
                    leading = { state.exam?.let { LicenseIcon(it.category) } },
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
                        // Square, so it reads as the first row of this card rather than a button
                        // laid on top of it. The card clips its top corners to its own curve.
                        shape = RectangleShape,
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsNavRow(
                        title = strings.legalTermsOfUse,
                        onClick = { onOpenDocument(LegalDocument.TermsOfUse) },
                        leading = { SettingsRowIcon(Icons.Outlined.Description, colors.primaryDeep, colors.mintGlow) },
                        shape = RectangleShape,
                    )
                    // The age question, drawn only for a user under 16. It is the one answer here they
                    // can still change — somebody answers at 15 and is 16 the year after — and to
                    // everyone else it is a question they cannot answer differently, which is clutter.
                    //
                    // The parent or guardian's agreement is not offered here at all: for an under-16 it
                    // is locked in once it has been given, and for everyone else it does not exist.
                    // Withdrawing it would lock the switches from a screen with no way to unlock them.
                    if (state.underConsentAge) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                        AgeAnswerRow(
                            answer = state.overConsentAge,
                            onAnswer = { onEvent(SettingsUiEvent.SetOverConsentAge(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        Text(
                            strings.consentAgeUnder16Reason,
                            style = Naj.type.rowSubtitle,
                            color = colors.muted,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsSwitchRow(
                        title = strings.consentCrashReportingTitle,
                        description = strings.consentCrashReportingDescription,
                        checked = state.crashReportingEnabled,
                        enabled = state.canAnswerSwitches,
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
                        enabled = state.canAnswerSwitches,
                        icon = Icons.Outlined.Analytics,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetAnalytics(it)) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsSwitchRow(
                        title = strings.consentAdsTitle,
                        description = strings.consentAdsDescription,
                        checked = state.adsEnabled,
                        enabled = state.canAnswerSwitches,
                        // The megaphone, because this row is about what the app shows the user rather
                        // than about what leaves the device.
                        icon = Icons.Outlined.Campaign,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetAds(it)) },
                    )
                }

                SectionLabel(strings.settingsApp, Modifier.padding(start = 6.dp))
                NajCard {
                    SettingsNavRow(
                        title = strings.feedbackTitle,
                        onClick = onOpenFeedback,
                        // The envelope, because what the screen does is open the user's mail app on
                        // the report they write.
                        leading = { SettingsRowIcon(Icons.Outlined.MailOutline, colors.primaryDeep, colors.mintGlow) },
                        // Square, so it reads as the first row of this card rather than a button
                        // laid on top of it, the same as the privacy rows above.
                        shape = RectangleShape,
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    SettingsNavRow(
                        title = strings.aboutApp,
                        // Its subtitle, because the version is part of what this entry is rather than a
                        // note under it. A platform that reports none leaves the row without a
                        // subtitle, the same as any other row without one.
                        subtitle = state.appVersion?.let { strings.aboutVersion(it) },
                        onClick = onOpenAbout,
                        leading = { SettingsRowIcon(Icons.Outlined.Info, colors.primaryDeep, colors.mintGlow) },
                        shape = RectangleShape,
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    // Last, and not a link to anywhere: what the app files what it holds about this
                    // device under, which is the one thing a user cannot read off their own device.
                    SettingsIdentifierRow(
                        identifier = state.deviceIdentifier,
                        copied = state.deviceIdentifierCopied,
                        onCopy = onCopyIdentifier,
                    )
                }
            }
        }
    }
}

/**
 * A setting that opens another screen: a mark, what it is, and the chevron that says it goes
 * somewhere. The whole row is the target.
 *
 * It brings its own card, because most of these stand alone in the column. A row that is going
 * inside a card already drawn passes [shape], or its card nests in that one and the row reads as a
 * rounded button floating on the card rather than as part of it. The outer card clips to its own
 * corners, so the top row of a group still follows the card's curve.
 */
@Composable
private fun SettingsNavRow(
    title: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
    shape: Shape = RoundedCornerShape(CardRadius),
) {
    val colors = Naj.colors
    NajCard(shape = shape) {
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

/** The square mark a setting leads with, on the color the setting is about. */
@Composable
private fun SettingsRowIcon(icon: ImageVector, tint: Color, background: Color) {
    IconTile(size = 40.dp, background = Brush.linearGradient(listOf(background, background))) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * One setting with its switch; the whole row toggles it. [enabled] is false for a setting that
 * another one rules out, which grays the row out and leaves the switch off.
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
            // The row is the target, not the switch: the title and the description are what the user
            // reads, and the switch is a small thing to hit. The switch gets no handler of its own, so
            // a press cannot be answered twice.
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
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
        NajSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * What the statistics and crash reports from this device are filed under, with the press that copies
 * it.
 *
 * It is the one piece of what we hold that a user cannot read off their own device, which is what
 * makes it worth a row: quoting the identifier is what lets us find their rows, so the press that
 * puts it on the clipboard is the point of the row. The identifier is drawn in a selection as well,
 * the same way the feedback screen hands over what it would send, so there is a way through on a
 * platform whose clipboard refuses.
 *
 * [identifier] is `null` until something has been allowed to leave, and the row then says what would
 * put one there rather than showing an empty value.
 */
@Composable
private fun SettingsIdentifierRow(identifier: String?, copied: Boolean, onCopy: (String) -> Unit) {
    val colors = Naj.colors
    NajCard(shape = RectangleShape) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingsRowIcon(Icons.Outlined.Fingerprint, colors.primaryDeep, colors.mintGlow)
                Column(Modifier.weight(1f)) {
                    Text(strings.settingsDeviceIdentifier, style = Naj.type.rowTitle, color = colors.ink)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (identifier == null) {
                            strings.settingsDeviceIdentifierEmpty
                        } else {
                            strings.settingsDeviceIdentifierDescription
                        },
                        style = Naj.type.rowSubtitle,
                        color = colors.muted,
                    )
                }
                if (identifier != null) {
                    // The words the feedback screen already uses for the same press: one button, one
                    // label, kept in one place per language.
                    OutlinePill(
                        text = if (copied) strings.feedbackCopied else strings.feedbackCopy,
                        icon = if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                        onClick = { onCopy(identifier) },
                        height = 36.dp,
                    )
                }
            }
            if (identifier != null) {
                Spacer(Modifier.height(10.dp))
                SelectionContainer {
                    Text(identifier, style = Naj.type.option, color = colors.ink)
                }
            }
        }
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
                adsEnabled = true,
                overConsentAge = true,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = null,
                deviceIdentifierCopied = false,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
            onCopyIdentifier = {},
        )
    }
}

/** Checking answers as they're picked, which grays out the setting it rules out. */
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
                adsEnabled = false,
                overConsentAge = true,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = null,
                deviceIdentifierCopied = false,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
            onCopyIdentifier = {},
        )
    }
}

/** No version to report, which leaves the row in place with no subtitle. */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun SettingsScreenNoVersionPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.English,
                checkAnswersImmediately = false,
                advanceAfterAnswer = false,
                crashReportingEnabled = false,
                analyticsEnabled = false,
                adsEnabled = false,
                overConsentAge = true,
                parentalConsent = false,
                exam = previewExam,
                appVersion = null,
                deviceIdentifier = null,
                deviceIdentifierCopied = false,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
            onCopyIdentifier = {},
        )
    }
}

/**
 * The exam row before its bank resolves, and after an update has taken that bank away: no
 * category to name and no mark to draw. Not a state the user can reach by their own choice.
 */
@Preview(widthDp = 400, heightDp = 700)
@Composable
private fun SettingsScreenNoExamYetPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.English,
                checkAnswersImmediately = false,
                advanceAfterAnswer = false,
                crashReportingEnabled = true,
                analyticsEnabled = true,
                adsEnabled = true,
                overConsentAge = true,
                parentalConsent = false,
                exam = null,
                appVersion = "1.4.2",
                deviceIdentifier = null,
                deviceIdentifierCopied = false,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
            onCopyIdentifier = {},
        )
    }
}

/** An identifier exists, which is what the App section's last row shows once one does. */
@Preview(widthDp = 400, heightDp = 1400)
@Composable
private fun SettingsScreenIdentifierPreview() {
    AppPreview {
        SettingsScreen(
            state = SettingsUiState(
                language = AppLanguage.English,
                checkAnswersImmediately = true,
                advanceAfterAnswer = false,
                crashReportingEnabled = false,
                analyticsEnabled = true,
                adsEnabled = false,
                overConsentAge = true,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = PREVIEW_IDENTIFIER,
                deviceIdentifierCopied = false,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
            onCopyIdentifier = {},
        )
    }
}

/** Shaped like a real one, and long enough that the row has to wrap it rather than cut it short. */
private const val PREVIEW_IDENTIFIER = "0195f2c4-3b7e-7a41-9c6d-1f0a4e8b7c22"

/** The row's three shapes: nothing yet, an identifier with the press, and the press answered. */
@Preview(widthDp = 400, heightDp = 460)
@Composable
private fun SettingsIdentifierRowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("Nothing to show yet")
            SettingsIdentifierRow(identifier = null, copied = false, onCopy = {})
            PreviewLabel("An identifier to copy")
            SettingsIdentifierRow(identifier = PREVIEW_IDENTIFIER, copied = false, onCopy = {})
            PreviewLabel("Copied")
            SettingsIdentifierRow(identifier = PREVIEW_IDENTIFIER, copied = true, onCopy = {})
        }
    }
}

/**
 * The Privacy card under the age question: the same state, changed in the one way that matters, drawn
 * through a board of its own so the states are not copies of the whole screen call.
 */
@Composable
private fun SettingsAgeBoard(state: SettingsUiState) {
    SettingsScreen(
        state = state,
        onEvent = {},
        onChangeExam = {},
        onOpenAbout = {},
        onOpenFeedback = {},
        onOpenDocument = {},
        onCopyIdentifier = {},
    )
}

/** The state the three below are built from: an adult, who is the one that sees no age question. */
private val previewPrivacyState = SettingsUiState(
    language = AppLanguage.English,
    checkAnswersImmediately = false,
    advanceAfterAnswer = false,
    crashReportingEnabled = false,
    analyticsEnabled = false,
    adsEnabled = false,
    overConsentAge = true,
    parentalConsent = false,
    exam = previewExam,
    appVersion = "1.4.2",
    deviceIdentifier = null,
    deviceIdentifierCopied = false,
)

/**
 * The age question unanswered, which the consent screen makes sure cannot happen: kept as the state
 * this screen has to draw something sane for rather than one a user reaches.
 */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenAgeUnansweredPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(overConsentAge = null))
    }
}

/**
 * Under 16: the reason and the three switches locked. The question is drawn, because they are the one
 * user who can still answer it differently — and the guardian's agreement is not, because for them it
 * is locked in.
 */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenUnder16Preview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(overConsentAge = false))
    }
}

/** Under 16 with the agreement given on the consent screen, which is what unlocks the switches here. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenUnder16AgreedPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(overConsentAge = false, parentalConsent = true))
    }
}
