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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Verified
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
import com.spiraclestudios.autoskola.data.preferences.AgeBand
import com.spiraclestudios.autoskola.data.preferences.LegalDocument
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.onboarding.LicenseIcon
import com.spiraclestudios.autoskola.openUrl
import com.spiraclestudios.autoskola.ui.AgeAnswerRow
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.CardRadius
import com.spiraclestudios.autoskola.ui.ConfirmDialog
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

/** The two ages the confirmation asks about: where agreeing alone starts, and where personalized ads do. */
private const val CONSENT_AGE = 16
private const val ADULT_AGE = 18

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

                // Only where the store sells it and has said what it costs; on iOS and the web there is
                // nothing here to buy, so there is no section either.
                if (state.removeAds != RemoveAdsOffer.None) {
                    SectionLabel(strings.settingsPurchases, Modifier.padding(start = 6.dp))
                    RemoveAdsRow(state.removeAds, onBuy = { onEvent(SettingsUiEvent.BuyRemoveAds) })
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
                    // The age question, drawn only for a user under 18. It is the one answer here they
                    // can still change — somebody answers at 15 and is 16 the year after, and 18 the year
                    // after that — and to an adult it is a question they cannot answer differently, which
                    // is clutter.
                    //
                    // The parent or guardian's agreement is not offered here at all: for an under-16 it
                    // is locked in once it has been given, and for everyone else it does not exist.
                    // Withdrawing it would lock the switches from a screen with no way to unlock them.
                    if (state.canCorrectAge) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                        AgeAnswerRow(
                            answer = state.ageBand,
                            onAnswer = { onEvent(SettingsUiEvent.SetAgeBand(it)) },
                            // Nobody gets younger: a 16 or 17-year-old is not offered "under 16".
                            options = state.ageOptions,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        // Only an under-16 is told why the first two switches are locked: from 16 they are
                        // theirs, and the ads row says its own reason.
                        if (state.underConsentAge) {
                            Text(
                                strings.consentAgeUnder16Reason,
                                style = Naj.type.rowSubtitle,
                                color = colors.muted,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            )
                        }
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
                        title = strings.consentPersonalizedAdsTitle,
                        // Locked under 18, with the reason where the description would be: no parent or
                        // guardian's agreement opens this one.
                        description = if (state.canPersonalizeAds) {
                            strings.consentPersonalizedAdsDescription
                        } else {
                            strings.consentPersonalizedAdsUnder18Reason
                        },
                        checked = state.personalizedAdsEnabled && state.canPersonalizeAds,
                        enabled = state.canPersonalizeAds,
                        // The megaphone, because this row is about which ads the app shows the user
                        // rather than about what leaves the device.
                        icon = Icons.Outlined.Campaign,
                        iconTint = colors.primaryDeep,
                        iconBackground = colors.mintGlow,
                        onCheckedChange = { onEvent(SettingsUiEvent.SetPersonalizedAds(it)) },
                    )
                    // Google requires a way back into its consent form wherever it shows one, so the row
                    // exists only where the form does. It opens the form rather than answering anything
                    // here: what the user agreed to there is Google's record, not one of the app's.
                    if (state.adPrivacyOptionsRequired) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                        SettingsNavRow(
                            title = strings.settingsAdPrivacyOptions,
                            subtitle = strings.settingsAdPrivacyOptionsDescription,
                            onClick = { onEvent(SettingsUiEvent.OpenAdPrivacyOptions) },
                            leading = {
                                SettingsRowIcon(Icons.Outlined.PrivacyTip, colors.primaryDeep, colors.mintGlow)
                            },
                            shape = RectangleShape,
                        )
                    }
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
                    // Hidden for now: the feedback screen already puts the identifier in the email it
                    // writes, which is the one place a user needs it.
                    // Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    // SettingsIdentifierRow(identifier = state.deviceIdentifier)
                }

                // The one question this screen asks before it changes something. It writes nothing on
                // its own: the answer is applied by the ViewModel when it is confirmed, and backing out
                // leaves the stored answer exactly as it was.
                state.confirmingAge?.let { band ->
                    // The band's lower bound is what the user is being asked about: 16 or 18.
                    val age = if (band == AgeBand.Adult) ADULT_AGE else CONSENT_AGE
                    ConfirmDialog(
                        title = strings.settingsAgeConfirmTitle(age),
                        message = strings.settingsAgeConfirmMessage,
                        confirmLabel = strings.settingsAgeConfirmAction(age),
                        onConfirm = { onEvent(SettingsUiEvent.ConfirmAgeBand) },
                        dismissLabel = strings.settingsAgeConfirmDismiss,
                        onDismiss = { onEvent(SettingsUiEvent.DismissAgeConfirmation) },
                        onDismissRequest = { onEvent(SettingsUiEvent.DismissAgeConfirmation) },
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
 * The "remove ads" purchase: what it is, and the store's price on the button that opens the store's
 * own sheet. Once the ads are off it says so and thanks the user, so a buyer can see that it worked.
 */
@Composable
private fun RemoveAdsRow(offer: RemoveAdsOffer, onBuy: () -> Unit) {
    val colors = Naj.colors
    NajCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (offer == RemoveAdsOffer.Owned) {
                SettingsRowIcon(Icons.Outlined.Verified, colors.primaryDeep, colors.mintGlow)
            } else {
                SettingsRowIcon(Icons.Outlined.Block, colors.primaryDeep, colors.mintGlow)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (offer == RemoveAdsOffer.Owned) strings.settingsAdsRemoved else strings.settingsRemoveAds,
                    style = Naj.type.rowTitle,
                    color = colors.ink,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when ((offer as? RemoveAdsOffer.ForSale)?.phase) {
                        null -> strings.settingsAdsRemovedDescription
                        PurchasePhase.Pending -> strings.settingsRemoveAdsPending
                        PurchasePhase.Failed -> strings.settingsRemoveAdsFailed
                        PurchasePhase.Idle, PurchasePhase.Buying -> strings.settingsRemoveAdsDescription
                    },
                    style = Naj.type.rowSubtitle,
                    color = colors.muted,
                )
            }
            if (offer is RemoveAdsOffer.ForSale) {
                OutlinePill(
                    text = offer.price,
                    onClick = onBuy,
                    // While the store's sheet is open: one press, one sheet.
                    enabled = offer.phase != PurchasePhase.Buying,
                    height = 36.dp,
                )
            }
        }
    }
}

/**
 * What the statistics and crash reports from this device are filed under. The identifier is drawn
 * in a selection, so it can still be copied by hand.
 *
 * [identifier] is `null` until something has been allowed to leave, and the row then says what would
 * put one there rather than showing an empty value.
 */
@Composable
private fun SettingsIdentifierRow(identifier: String?) {
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
                personalizedAdsEnabled = true,
                ageBand = AgeBand.Adult,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = null,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
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
                personalizedAdsEnabled = false,
                ageBand = AgeBand.Adult,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = null,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
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
                personalizedAdsEnabled = false,
                ageBand = AgeBand.Adult,
                parentalConsent = false,
                exam = previewExam,
                appVersion = null,
                deviceIdentifier = null,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
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
                personalizedAdsEnabled = true,
                ageBand = AgeBand.Adult,
                parentalConsent = false,
                exam = null,
                appVersion = "1.4.2",
                deviceIdentifier = null,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
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
                personalizedAdsEnabled = false,
                ageBand = AgeBand.Adult,
                parentalConsent = false,
                exam = previewExam,
                appVersion = "1.4.2",
                deviceIdentifier = PREVIEW_IDENTIFIER,
            ),
            onEvent = {},
            onChangeExam = {},
            onOpenAbout = {},
            onOpenFeedback = {},
            onOpenDocument = {},
        )
    }
}

/** Shaped like a real one, and long enough that the row has to wrap it rather than cut it short. */
private const val PREVIEW_IDENTIFIER = "0195f2c4-3b7e-7a41-9c6d-1f0a4e8b7c22"

/** The row's two shapes: nothing yet, and an identifier. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun SettingsIdentifierRowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("Nothing to show yet")
            SettingsIdentifierRow(identifier = null)
            PreviewLabel("An identifier")
            SettingsIdentifierRow(identifier = PREVIEW_IDENTIFIER)
        }
    }
}

/** The purchase row's states: for sale, the sheet open, a pending payment, a failure, and bought. */
@Preview(widthDp = 400, heightDp = 640)
@Composable
private fun RemoveAdsRowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("For sale")
            RemoveAdsRow(RemoveAdsOffer.ForSale("€0.99"), onBuy = {})
            PreviewLabel("The store's sheet is open")
            RemoveAdsRow(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Buying), onBuy = {})
            PreviewLabel("Payment pending")
            RemoveAdsRow(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Pending), onBuy = {})
            PreviewLabel("Failed")
            RemoveAdsRow(RemoveAdsOffer.ForSale("€0.99", PurchasePhase.Failed), onBuy = {})
            PreviewLabel("Bought")
            RemoveAdsRow(RemoveAdsOffer.Owned, onBuy = {})
        }
    }
}

/** The longest translation of the row, with a price in the format Play gives a Slovak user. */
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun RemoveAdsRowSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        PreviewBoard {
            RemoveAdsRow(RemoveAdsOffer.ForSale("0,99 €"), onBuy = {})
            RemoveAdsRow(RemoveAdsOffer.ForSale("0,99 €", PurchasePhase.Pending), onBuy = {})
        }
    }
}

/** Where the store sells "remove ads", Settings carries it in a section of its own above Privacy. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenRemoveAdsPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(removeAds = RemoveAdsOffer.ForSale("€0.99")))
    }
}

/** On a tablet or a desktop browser width, the column stays capped and centered. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun SettingsScreenRemoveAdsDesktopPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(removeAds = RemoveAdsOffer.Owned))
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
    )
}

/** The state the three below are built from: an adult, who is the one that sees no age question. */
private val previewPrivacyState = SettingsUiState(
    language = AppLanguage.English,
    checkAnswersImmediately = false,
    advanceAfterAnswer = false,
    crashReportingEnabled = false,
    analyticsEnabled = false,
    personalizedAdsEnabled = false,
    ageBand = AgeBand.Adult,
    parentalConsent = false,
    exam = previewExam,
    appVersion = "1.4.2",
    deviceIdentifier = null,
)

/**
 * The age question unanswered, which the consent screen makes sure cannot happen: kept as the state
 * this screen has to draw something sane for rather than one a user reaches.
 */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenAgeUnansweredPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = null))
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
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = AgeBand.Under16))
    }
}

/** Under 16 with the agreement given on the consent screen, which is what unlocks the switches here. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenUnder16AgreedPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = AgeBand.Under16, parentalConsent = true))
    }
}

/**
 * 16 to 17: the age row is still drawn, because they can still turn 18, and the personalized-ads row is
 * locked with its reason while the two above it are theirs to answer.
 */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenTeenPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = AgeBand.From16To17, crashReportingEnabled = true))
    }
}

/** Where Google shows its consent form, Settings carries the way back into it, as the card's last row. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenAdPrivacyOptionsPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(adPrivacyOptionsRequired = true))
    }
}

/** The question before turning 18 counts, because that press unlocks the personalized-ads switch. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenAdultConfirmationPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = AgeBand.From16To17, confirmingAge = AgeBand.Adult))
    }
}

/** The question before turning 16 counts, because that press unlocks the crash-report and statistics switches. */
@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenAgeConfirmationPreview() {
    AppPreview {
        SettingsAgeBoard(previewPrivacyState.copy(ageBand = AgeBand.Under16, confirmingAge = AgeBand.From16To17))
    }
}
