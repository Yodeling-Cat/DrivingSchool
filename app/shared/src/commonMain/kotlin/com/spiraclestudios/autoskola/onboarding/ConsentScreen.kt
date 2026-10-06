package com.spiraclestudios.autoskola.onboarding

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
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
 * **The age question comes first.** Under 16 the three answers are not the user's to give, so the
 * switches stay locked until the user has said which side of 16 they are on — and, if under 16, until
 * a parent or guardian's agreement is recorded. An unanswered question is not an answer, and neither
 * button commits anything before it is answered (`plans/age-gate.md`).
 */
@Composable
fun ConsentScreen(state: ConsentUiState, onEvent: (ConsentUiEvent) -> Unit, onOpenDocument: (LegalDocument) -> Unit) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader(
                trailing = {
                    LanguageMenu(
                        current = state.language,
                        onSelect = { onEvent(ConsentUiEvent.SelectLanguage(it)) },
                    )
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 52.dp),
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
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    Text(strings.consentTitle, style = Naj.type.screenTitle, color = colors.ink)
                    Spacer(Modifier.height(12.dp))
                    Text(strings.consentSubtitle, style = Naj.type.body, color = colors.muted)
                    Spacer(Modifier.height(24.dp))

                    NajCard {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            // The question comes first, because it decides whether the three answers
                            // below are the user's to give at all (see `plans/age-gate.md`).
                            AgeAnswerRow(
                                answer = state.overConsentAge,
                                missing = state.ageError,
                                onAnswer = { onEvent(ConsentUiEvent.SetOverConsentAge(it)) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                            if (state.asksForParentalConsent) {
                                // Under 16 the switches are locked, and this is what says why and what
                                // unlocks them. Both are drawn only for the answer that needs them, so
                                // the screen an adult sees is the screen it always was.
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
                            // Last, and about showing rather than sending: refusing it costs the user
                            // nothing but the ads, which is why it sits after the two that decide
                            // what leaves the device.
                            ConsentSwitchRow(
                                title = strings.consentAdsTitle,
                                description = strings.consentAdsDescription,
                                checked = state.adsEnabled,
                                enabled = state.switchesEnabled,
                                onCheckedChange = { onEvent(ConsentUiEvent.SetAds(it)) },
                            )
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = ContentWidth.Narrow).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrimaryPill(
                        text = strings.consentAcceptAll,
                        onClick = { onEvent(ConsentUiEvent.AcceptAll) },
                        height = 56.dp,
                        fillWidth = true,
                    )
                    OutlinePill(
                        text = strings.consentAcceptChosen,
                        onClick = { onEvent(ConsentUiEvent.AcceptChosen) },
                        height = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Naj.type.body, color = Naj.colors.ink, fontWeight = FontWeight.Medium)
            Text(description, style = Naj.type.rowSubtitle, color = Naj.colors.muted)
        }
        NajSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // Locked, not hidden, for an under-16 without a parent or guardian's agreement: a switch
            // that vanished would look like a bug, and one that stayed live would take an answer the
            // user may not give.
            enabled = enabled,
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
    ageError = false,
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

/** The screen as it opens now: the age question unanswered, and the three switches locked under it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAgeUnansweredPreview() {
    AppPreview {
        ConsentScreen(previewConsentState.copy(overConsentAge = null), onEvent = {}, onOpenDocument = {})
    }
}

/** A button pressed before the question was answered: the question is marked, nothing was committed. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAgeMissingPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(overConsentAge = null, ageError = true),
            onEvent = {},
            onOpenDocument = {},
        )
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
