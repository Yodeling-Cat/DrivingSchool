package com.spiraclestudios.autoskola.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.data.preferences.LegalDocument
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.openUrl
import com.spiraclestudios.autoskola.settings.LanguageMenu
import com.spiraclestudios.autoskola.ui.AgeAnswerRow
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ConsentCheckRow
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.HeightClass
import com.spiraclestudios.autoskola.ui.LocalHeightClass
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.NajSwitch
import com.spiraclestudios.autoskola.ui.OutlinePill
import com.spiraclestudios.autoskola.ui.PrimaryPill
import com.spiraclestudios.autoskola.ui.ScreenHeader
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConsentRoute(viewModel: ConsentViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConsentScreen(state, viewModel::onEvent, onOpenDocument = { openUrl(it.url) })
}

/**
 * How long the card takes to open, matching the screen slide rather than racing it.
 */
private const val CARD_REVEAL_MILLIS = 240

/** The card's own growth: the vertical half of the reveal. */
private val CardRevealSpec =
    tween<IntSize>(durationMillis = CARD_REVEAL_MILLIS, easing = FastOutSlowInEasing)

/** And its colour, which does not have to travel the same distance to arrive at the same time. */
private val CardRevealFadeSpec =
    tween<Float>(durationMillis = CARD_REVEAL_MILLIS, easing = FastOutSlowInEasing)

/**
 * The second screen after install, and the one that actually asks: what the app shows and sends,
 * three switches to answer with, and the documents to read before agreeing to them.
 *
 * The continue button stays disabled until the agreement is ticked. The switches do not gate it —
 * refusing both is a valid answer, because neither is required to use the app.
 *
 * Two buttons, not one. "Accept all" answers all three questions in a single act and is always live;
 * "Accept chosen" keeps whatever the switches say and waits for the documents to be ticked. That
 * leaves the wary user a way through with both answers off, which has to be as easy as agreeing —
 * and either way the answer comes from a press, which is what the pre-ticked switch it replaces
 * could never do.
 *
 * All three switches start off, and that is a requirement rather than a default:
 * a pre-ticked box is not valid consent under the GDPR or the ePrivacy Directive, so the answer has
 * to come from the user acting rather than from the user not objecting. `plans/privacy-and-terms.md`
 * records the same thing from the other side.
 *
 * **The card opens on the age question.** It starts collapsed, showing only that question: under 16 the
 * three answers are not the user's to give, and a form they cannot fill in says that worse than a card
 * still closed. Answering it expands the card to the choices — the three switches, and the parent or
 * guardian's agreement when the answer is under 16 — while both buttons are drawn dead until then,
 * because neither can mean anything before the question is answered. An answer of under 16 leaves the
 * card open with the switches locked, which says that the choices exist and are not the user's to make.
 */
@Composable
fun ConsentScreen(state: ConsentUiState, onEvent: (ConsentUiEvent) -> Unit, onOpenDocument: (LegalDocument) -> Unit) {
    val colors = Naj.colors
    // A landscape phone, where the bars have to give up their height to the card rather than the
    // other way round.
    val compactHeight = LocalHeightClass.current == HeightClass.Compact
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader(
                trailing = {
                    LanguageMenu(
                        current = state.language,
                        onSelect = { onEvent(ConsentUiEvent.SelectLanguage(it)) },
                    )
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = if (compactHeight) 12.dp else 52.dp),
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ContentWidth.Narrow)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = if (compactHeight) 12.dp else 24.dp),
                ) {
                    Text(strings.consentTitle, style = Naj.type.screenTitle, color = colors.ink)
                    Spacer(Modifier.height(12.dp))
                    Text(strings.consentSubtitle, style = Naj.type.body, color = colors.muted)
                    Spacer(Modifier.height(24.dp))

                    NajCard {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            // The card opens on this one question, and everything below it arrives once
                            // it has been answered: the three answers are not the user's to give before
                            // that, and a form they cannot fill in says that worse than a card that is
                            // still closed (see `plans/age-gate.md`).
                            AgeAnswerRow(
                                answer = state.overConsentAge,
                                onAnswer = { onEvent(ConsentUiEvent.SetOverConsentAge(it)) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                            AnimatedVisibility(
                                visible = state.ageAnswered,
                                enter = expandVertically(animationSpec = CardRevealSpec) +
                                    fadeIn(animationSpec = CardRevealFadeSpec),
                                exit = shrinkVertically(animationSpec = CardRevealSpec) +
                                    fadeOut(animationSpec = CardRevealFadeSpec),
                            ) {
                                Column {
                                    if (state.asksForParentalConsent) {
                                        // Under 16 the switches are locked, and this says why and what
                                        // unlocks them. Both are drawn only for the answer that needs
                                        // them, so the screen an adult sees is the screen it always was.
                                        Text(
                                            strings.consentAgeUnder16Reason,
                                            style = Naj.type.rowSubtitle,
                                            color = Naj.colors.muted,
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                        )
                                        ConsentCheckRow(
                                            title = strings.consentParentalConsent,
                                            description = strings.consentParentalConsentDescription,
                                            checked = state.parentalConsent,
                                            error = state.parentalError,
                                            errorMessage = strings.consentAgreeRequired,
                                            onCheckedChange = { onEvent(ConsentUiEvent.SetParentalConsent(it)) },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                    }
                                    ConsentSwitchRow(
                                        title = strings.consentCrashReportingTitle,
                                        description = strings.consentCrashReportingDescription,
                                        checked = state.crashReportingEnabled,
                                        enabled = state.switchesEnabled,
                                        onCheckedChange = { onEvent(ConsentUiEvent.SetCrashReporting(it)) },
                                    )
                                    ConsentSwitchRow(
                                        title = strings.consentAnalyticsTitle,
                                        description = strings.consentAnalyticsDescription,
                                        checked = state.analyticsEnabled,
                                        enabled = state.switchesEnabled,
                                        onCheckedChange = { onEvent(ConsentUiEvent.SetAnalytics(it)) },
                                    )
                                    // Last, and about showing rather than sending: refusing it costs the
                                    // user nothing but the ads, which is why it sits after the two that
                                    // decide what leaves the device.
                                    ConsentSwitchRow(
                                        title = strings.consentAdsTitle,
                                        description = strings.consentAdsDescription,
                                        checked = state.adsEnabled,
                                        enabled = state.switchesEnabled,
                                        onCheckedChange = { onEvent(ConsentUiEvent.SetAds(it)) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConsentCheckRow(
                            title = strings.consentAgree,
                            checked = state.documentsAccepted,
                            error = state.agreementError,
                            errorMessage = strings.consentAgreeRequired,
                            onCheckedChange = { onEvent(ConsentUiEvent.SetDocumentsAccepted(it)) },
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    // Side by side and centerd, in a FlowRow so they can wrap: the Slovak and Czech
                    // names for these two documents are long enough to run past a phone together.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        DocumentLink(strings.legalPrivacyPolicy) { onOpenDocument(LegalDocument.PrivacyPolicy) }
                        Text(
                            "•",
                            style = Naj.type.body,
                            color = colors.muted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        DocumentLink(strings.legalTermsOfUse) { onOpenDocument(LegalDocument.TermsOfUse) }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (compactHeight) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val buttons = Modifier.widthIn(max = ContentWidth.Narrow).fillMaxWidth()
                // Stacked where there is height to spare, side by side where there is not: two 56dp
                // buttons are a third of a landscape phone, and the card they are asking about would
                // have nothing left to be drawn in. `PracticeScreen` slims its bars the same way.
                if (compactHeight) {
                    Row(buttons, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcceptAllButton(state, onEvent, Modifier.weight(1f), height = 48.dp)
                        AcceptChosenButton(state, onEvent, Modifier.weight(1f), height = 48.dp)
                    }
                } else {
                    Column(buttons, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AcceptAllButton(state, onEvent, Modifier.fillMaxWidth(), height = 56.dp)
                        AcceptChosenButton(state, onEvent, Modifier.fillMaxWidth(), height = 56.dp)
                    }
                }
            }
        }
    }
}

/** "Accept all", waiting like its twin for the age question to be answered. */
@Composable
private fun AcceptAllButton(
    state: ConsentUiState,
    onEvent: (ConsentUiEvent) -> Unit,
    modifier: Modifier,
    height: Dp,
) {
    PrimaryPill(
        text = strings.consentAcceptAll,
        onClick = { onEvent(ConsentUiEvent.AcceptAll) },
        // It waits for the age question. Nothing under it is answerable before that, so the button
        // cannot mean anything yet — and a button drawn dead says so without a word, which is what the
        // mark it replaces was for.
        enabled = state.ageAnswered,
        height = height,
        modifier = modifier,
        fillWidth = true,
    )
}

/** And "accept the chosen", which commits only what the switches say. */
@Composable
private fun AcceptChosenButton(
    state: ConsentUiState,
    onEvent: (ConsentUiEvent) -> Unit,
    modifier: Modifier,
    height: Dp,
) {
    OutlinePill(
        text = strings.consentAcceptChosen,
        onClick = { onEvent(ConsentUiEvent.AcceptChosen) },
        enabled = state.ageAnswered,
        height = height,
        modifier = modifier,
    )
}

/** One of the two answers, with the words that say what it actually covers. */
@Composable
private fun ConsentSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The whole row is the target, not the switch alone: the label is what the user reads, and
            // a switch is a small thing to hit. The switch is handed no handler of its own so the two
            // cannot both answer the same press.
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Naj.type.body, color = Naj.colors.ink, fontWeight = FontWeight.Medium)
            Text(description, style = Naj.type.rowSubtitle, color = Naj.colors.muted)
        }
        NajSwitch(
            checked = checked,
            // Locked, not hidden, for an under-16 without a parent or guardian's agreement: a switch
            // that vanished would look like a bug, and one that stayed live would take an answer the
            // user may not give.
            enabled = enabled,
            onCheckedChange = null,
            contentDescription = title,
        )
    }
}

/** The documents themselves live outside the app, so they open in a browser. */
@Composable
private fun DocumentLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = Naj.type.body,
        color = Naj.colors.primaryDeep,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

// ── Previews ──

private val previewConsentState = ConsentUiState(
    language = AppLanguage.English,
    documentsAccepted = false,
    agreementError = false,
    // An adult, which is what most users are: the question answered, and the switches live.
    overConsentAge = true,
    parentalConsent = false,
    parentalError = false,
    crashReportingEnabled = false,
    analyticsEnabled = false,
    adsEnabled = false,
)

/** The screen as it opens: nothing answered, and the button that cannot be pressed yet. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenPreview() {
    AppPreview {
        ConsentScreen(previewConsentState, onEvent = {}, onOpenDocument = {})
    }
}

/** All three answers turned on and the documents agreed to, so the action is available. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAnsweredPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(
                documentsAccepted = true,
                crashReportingEnabled = true,
                analyticsEnabled = true,
                adsEnabled = true,
            ),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** The other way through: the documents agreed to with both answers left off. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenRefusedBothPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(documentsAccepted = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/**
 * Ads allowed while both sharing answers are refused: the shape of "keep it free, don't measure me",
 * and the combination the third switch exists to make possible.
 */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAdsOnlyPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(documentsAccepted = true, adsEnabled = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** The narrower button pressed before agreeing: the box is marked, and nothing was committed. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAgreementMissingPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(agreementError = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** The longest of the four translations, to check the two button labels and the links still fit. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        ConsentScreen(
            previewConsentState.copy(language = AppLanguage.Slovak, documentsAccepted = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** A desktop browser, where the reading column is what keeps the text readable. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun ConsentScreenDesktopPreview() {
    AppPreview {
        ConsentScreen(previewConsentState, onEvent = {}, onOpenDocument = {})
    }
}

/** The card as it opens: the one question, closed, with both buttons drawn dead under it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenCollapsedPreview() {
    AppPreview {
        ConsentScreen(previewConsentState.copy(overConsentAge = null), onEvent = {}, onOpenDocument = {})
    }
}

/** The same card, opened by the answer, which is the animation's end state. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenOpenedPreview() {
    AppPreview {
        ConsentScreen(previewConsentState, onEvent = {}, onOpenDocument = {})
    }
}

/**
 * Landscape, closed: the buttons go side by side and the card keeps the height they gave up, which is
 * the whole reason for that split — the question has to be on the screen without scrolling to it.
 */
@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun ConsentScreenLandscapeCollapsedPreview() {
    AppPreview {
        ConsentScreen(previewConsentState.copy(overConsentAge = null), onEvent = {}, onOpenDocument = {})
    }
}

/** And landscape with the card open, which is the tallest thing this screen ever draws. */
@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun ConsentScreenLandscapeOpenedPreview() {
    AppPreview {
        ConsentScreen(previewConsentState, onEvent = {}, onOpenDocument = {})
    }
}

/**
 * The under-16 path: the reason, the parent or guardian's agreement, and the switches still locked
 * until it is given.
 */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenUnder16Preview() {
    AppPreview {
        ConsentScreen(previewConsentState.copy(overConsentAge = false), onEvent = {}, onOpenDocument = {})
    }
}

/** Under 16 with a button pressed and no agreement yet: the box is marked as the missing thing. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenUnder16UnagreedPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(overConsentAge = false, parentalError = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** The agreement given: an under-16 answers the three switches like anyone else. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenUnder16AgreedPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(overConsentAge = false, parentalConsent = true, adsEnabled = true),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}

/** The under-16 path in the longest of the four translations, where the pills and the box have to fit. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenUnder16SlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        ConsentScreen(
            previewConsentState.copy(language = AppLanguage.Slovak, overConsentAge = false),
            onEvent = {},
            onOpenDocument = {},
        )
    }
}
