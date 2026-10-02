package com.spiraclestudios.autoskola.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.openEmail
import com.spiraclestudios.autoskola.practice.IconArrowBack
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.PrimaryPill
import com.spiraclestudios.autoskola.ui.RoundButton
import com.spiraclestudios.autoskola.ui.theme.Naj
import com.spiraclestudios.autoskola.ui.theme.paperBackground
import com.spiraclestudios.autoskola.ui.theme.paperGlows
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [FeedbackScreen] to its [FeedbackViewModel], and hands the platform the report the
 * screen has put together.
 *
 * The words of the report are the screen's, because they are the app's translations; the facts in
 * it, and whether the press found anything to send, come from the state. Opening the composer is
 * the only side effect, and it is the route's rather than the screen's - exactly as the documents
 * in Settings are opened.
 */
@Composable
fun FeedbackRoute(onBack: () -> Unit, viewModel: FeedbackViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Read here, not inside the press below: the press is not a composable scope.
    val words = strings
    FeedbackScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onSend = {
            if (!state.canSend) {
                // The refusal is the feedback, so the button stays pressable and the press is
                // what says what is missing.
                viewModel.onEvent(FeedbackUiEvent.MessageMissing)
            } else {
                val mailto = feedbackMailto(words, state.message, state.deviceToSend, state.appVersion)
                if (!openEmail(mailto)) viewModel.onEvent(FeedbackUiEvent.NoEmailApp)
            }
        },
    )
}

/**
 * Writing a report: what the user wants to say, whether the device it was written on goes with it,
 * and the press that opens their email app on it.
 *
 * Nothing is sent from here. The composer opens with the report already written, and the user
 * reads it, changes it, sends it or closes it - which is also why a written report is kept after
 * the press rather than cleared: it may be opened twice, and it is theirs.
 *
 * If the platform has no email app at all, the screen says so rather than leaving the press
 * looking like it worked.
 */
@Composable
fun FeedbackScreen(
    state: FeedbackUiState,
    onEvent: (FeedbackUiEvent) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = Naj.colors
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors, mintY = 0.7f))) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentWidth.Narrow)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp + LocalNavBarInset.current),
            ) {
                FeedbackHeader(onBack)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(strings.feedbackIntro, style = Naj.type.body, color = colors.muted)
                    MessageField(state, onEvent)
                    DeviceInfoRow(state, onEvent)
                    SendButton(state, onSend)
                }
            }
        }
    }
}

/** Back, and what the screen is for. */
@Composable
private fun FeedbackHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundButton(icon = IconArrowBack, contentDescription = strings.back, onClick = onBack)
        Text(strings.feedbackTitle, style = Naj.type.screenTitle, color = Naj.colors.ink)
    }
}

/**
 * The report itself, on a card tall enough to write a paragraph in.
 *
 * A press with nothing written marks the field and says what is missing: the mark alone would
 * reach only the users who can see it, and the announcement is what reaches the rest.
 */
@Composable
private fun MessageField(state: FeedbackUiState, onEvent: (FeedbackUiEvent) -> Unit) {
    val colors = Naj.colors
    // Read out here: the semantics block below is not a composable scope.
    val missingMessage = strings.feedbackMessageMissing
    Column(Modifier.fillMaxWidth()) {
        NajCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .padding(16.dp)
                    .semantics {
                        if (state.messageMissing) {
                            // `this.` on purpose: `error` alone would be ambiguous with
                            // kotlin.error, which throws instead.
                            this.error(missingMessage)
                            liveRegion = LiveRegionMode.Polite
                        }
                    },
            ) {
                if (state.message.isEmpty()) {
                    Text(strings.feedbackMessageHint, style = Naj.type.option, color = colors.muted)
                }
                BasicTextField(
                    value = state.message,
                    onValueChange = { onEvent(FeedbackUiEvent.Write(it)) },
                    textStyle = Naj.type.option.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.primaryDeep),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.messageMissing) {
            Spacer(Modifier.height(8.dp))
            // In the same colour as the mark, for anyone the mark alone does not reach.
            Text(
                missingMessage,
                style = Naj.type.rowSubtitle,
                color = colors.wrongOn,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/**
 * Whether the report carries the device it was written on. The whole row answers it, which is what
 * a thumb on a phone reaches; the words say what the answer covers, so the user is not agreeing to
 * send something they were not told about.
 */
@Composable
private fun DeviceInfoRow(state: FeedbackUiState, onEvent: (FeedbackUiEvent) -> Unit) {
    val colors = Naj.colors
    NajCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(FeedbackUiEvent.IncludeDeviceInfo(!state.includeDeviceInfo)) }
                .padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = state.includeDeviceInfo,
                onCheckedChange = { onEvent(FeedbackUiEvent.IncludeDeviceInfo(it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primaryDeep,
                    uncheckedColor = colors.muted,
                    checkmarkColor = colors.mintGlow,
                ),
            )
            Column(Modifier.weight(1f)) {
                Text(strings.feedbackIncludeDeviceInfo, style = Naj.type.rowTitle, color = colors.ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    strings.feedbackIncludeDeviceInfoDescription,
                    style = Naj.type.rowSubtitle,
                    color = colors.muted,
                )
            }
        }
    }
}

/** The press that opens the email app, and what the screen says when there was none to open. */
@Composable
private fun SendButton(state: FeedbackUiState, onSend: () -> Unit) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PrimaryPill(
            text = strings.feedbackSend,
            onClick = onSend,
            height = 56.dp,
            fillWidth = true,
        )
        if (state.noEmailApp) {
            Spacer(Modifier.height(10.dp))
            // Read out here: the semantics block below is not a composable scope. And announced,
            // because the press that produced it left the focus up on the button.
            val noEmailApp = strings.feedbackNoEmailApp
            Text(
                noEmailApp,
                style = Naj.type.rowSubtitle,
                color = colors.wrongOn,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

// ── Previews ──

/** A report half written, on the phone the screen is designed around. */
private fun previewFeedback(
    message: String = "",
    includeDeviceInfo: Boolean = false,
    messageMissing: Boolean = false,
    noEmailApp: Boolean = false,
): FeedbackUiState = FeedbackUiState(
    message = message,
    includeDeviceInfo = includeDeviceInfo,
    device = "Pixel 7 · Android 14 (API 34)",
    appVersion = "1.4.2",
    messageMissing = messageMissing,
    noEmailApp = noEmailApp,
)

/** The screen as it opens: nothing written, and the device left out of the report. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenPreview() {
    AppPreview { FeedbackScreen(previewFeedback(), onEvent = {}, onBack = {}, onSend = {}) }
}

/** A report being written, with the device it was written on going in with it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenWrittenPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(
                message = "The timer on question 12 kept counting after I answered it.",
                includeDeviceInfo = true,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
        )
    }
}

/** Send pressed with nothing written: the field is marked, and the answer sits under it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenMessageMissingPreview() {
    AppPreview { FeedbackScreen(previewFeedback(messageMissing = true), onEvent = {}, onBack = {}, onSend = {}) }
}

/** A device with no email app: the press has nowhere to go, so the screen says so. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenNoEmailAppPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(message = "The timer kept counting.", noEmailApp = true),
            onEvent = {},
            onBack = {},
            onSend = {},
        )
    }
}

/** The longest of the four translations, where the checkbox and its description are wordiest. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        FeedbackScreen(
            previewFeedback(message = "Časovač na otázke 12 sa stále počítal.", includeDeviceInfo = true),
            onEvent = {},
            onBack = {},
            onSend = {},
        )
    }
}

/** A desktop browser, where the column is capped rather than stretched. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun FeedbackScreenDesktopPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(message = "The timer kept counting after I answered.", includeDeviceInfo = true),
            onEvent = {},
            onBack = {},
            onSend = {},
        )
    }
}
