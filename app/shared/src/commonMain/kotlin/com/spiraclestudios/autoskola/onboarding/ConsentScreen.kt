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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.spiraclestudios.autoskola.ui.AppPreview
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
 * The second screen after install, and the one that actually asks: what the app sends, two
 * switches to answer with, and the documents to read before agreeing to them.
 *
 * The continue button stays disabled until the agreement is ticked. The switches do not gate it —
 * refusing both is a valid answer, because neither is required to use the app.
 *
 * Two buttons, not one. "Accept all" answers both questions in a single act and is always live;
 * "Accept chosen" keeps whatever the switches say and waits for the documents to be ticked. That
 * leaves the wary user a way through with both answers off, which has to be as easy as agreeing —
 * and either way the answer comes from a press, which is what the pre-ticked switch it replaces
 * could never do.
 *
 * Both switches start off, and that is a requirement rather than a default waiting to be improved:
 * a pre-ticked box is not valid consent under the GDPR or the ePrivacy Directive, so the answer has
 * to come from the user acting rather than from the user not objecting. `plans/privacy-and-terms.md`
 * records the same thing from the other side.
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
                            ConsentSwitchRow(
                                title = strings.consentCrashReportingTitle,
                                description = strings.consentCrashReportingDescription,
                                checked = state.crashReportingEnabled,
                                onCheckedChange = { onEvent(ConsentUiEvent.SetCrashReporting(it)) },
                            )
                            ConsentSwitchRow(
                                title = strings.consentAnalyticsTitle,
                                description = strings.consentAnalyticsDescription,
                                checked = state.analyticsEnabled,
                                onCheckedChange = { onEvent(ConsentUiEvent.SetAnalytics(it)) },
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.documentsAccepted,
                            onCheckedChange = { onEvent(ConsentUiEvent.SetDocumentsAccepted(it)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primaryDeep,
                                // The one cue that the narrower button needs this: it is enabled
                                // while unticked, so the press has to say what is missing.
                                uncheckedColor = if (state.agreementError) colors.wrongOn else colors.muted,
                                checkmarkColor = colors.mintGlow,
                            ),
                        )
                        Text(
                            strings.consentAgree,
                            style = Naj.type.body,
                            color = colors.ink,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    // Side by side and centred, in a FlowRow so they can wrap: the Slovak and Czech
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
    crashReportingEnabled = false,
    analyticsEnabled = false,
)

/** The screen as it opens: nothing answered, and the button that cannot be pressed yet. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenPreview() {
    AppPreview {
        ConsentScreen(previewConsentState, onEvent = {}, onOpenDocument = {})
    }
}

/** Both answers turned on and the documents agreed to, so the action is available. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun ConsentScreenAnsweredPreview() {
    AppPreview {
        ConsentScreen(
            previewConsentState.copy(
                documentsAccepted = true,
                crashReportingEnabled = true,
                analyticsEnabled = true,
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
