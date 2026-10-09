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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.spiraclestudios.autoskola.copyToClipboard
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.openEmail
import com.spiraclestudios.autoskola.ui.AppPreview
import com.spiraclestudios.autoskola.ui.BackHeader
import com.spiraclestudios.autoskola.ui.ChoicePill
import com.spiraclestudios.autoskola.ui.ContentWidth
import com.spiraclestudios.autoskola.ui.IconArrowBack
import com.spiraclestudios.autoskola.ui.LocalNavBarInset
import com.spiraclestudios.autoskola.ui.NajCard
import com.spiraclestudios.autoskola.ui.OutlinePill
import com.spiraclestudios.autoskola.ui.PreviewBoard
import com.spiraclestudios.autoskola.ui.PreviewLabel
import com.spiraclestudios.autoskola.ui.PrimaryButtonHeight
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
    // Read here, not inside the presses below: a press is not a composable scope.
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
                val mailto = feedbackMailto(
                    words,
                    state.message,
                    state.deviceToSend,
                    state.appVersion,
                    state.identifierToSend,
                    state.kind,
                )
                if (!openEmail(mailto)) viewModel.onEvent(FeedbackUiEvent.NoEmailApp)
            }
        },
        onCopy = { details ->
            copyToClipboard(details)
            viewModel.onEvent(FeedbackUiEvent.Copied)
        },
    )
}

/**
 * Writing a report, and getting it to us.
 *
 * On the phones that is a press that opens their email app on the report already written: nothing
 * is sent from here, the composer opens in front of the user, and they read it, change it, send it
 * or close it. That press is why a written report is kept after it rather than cleared - it may be
 * opened twice, and it is theirs. The press sits at the bottom of the screen while what is written
 * scrolls above it: a message worth sending is longer than a phone in landscape has room for, and
 * the way out of the screen should not be the thing that scrolled away. A device with no email app
 * at all is told about rather than left looking like the press worked.
 *
 * A browser is not offered that press: it cannot open a mail app the way a phone does, so the
 * screen says where to write to us instead, and draws the block of build and device the user
 * pastes into their own mail app - selectable, and copyable with one press.
 */
@Composable
fun FeedbackScreen(
    state: FeedbackUiState,
    onEvent: (FeedbackUiEvent) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val colors = Naj.colors
    // The same block the phones write into the mail, drawn here for the user to paste themselves.
    // The device is named in it whether or not it would be in a mail (a browser's user has no switch
    // to ask, see below), and the identifier is there when what is being written is a request about
    // the reader's own data.
    val words = strings
    val details = remember(words, state.device, state.appVersion, state.identifierToSend) {
        feedbackDetails(words, state.device, state.appVersion, state.identifierToSend)
    }
    Box(Modifier.fillMaxSize().paperBackground(paperGlows(colors, mintY = 0.7f))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ContentWidth.Narrow)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BackHeader(title = strings.feedbackTitle, onBack = onBack)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                // The press brings its own room when it is there; without one the
                                // column ends the screen and has to keep clear of the edge itself.
                                bottom = if (state.hasEmailApp) 8.dp else 24.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(strings.feedbackIntro, style = Naj.type.body, color = colors.muted)
                        // Each part is handed only what it draws, so typing - a new state with every
                        // letter - recomposes the field being typed in and nothing else.
                        KindPicker(state.kind, hasIdentifier = state.deviceIdentifier != null, onEvent)
                        if (state.hasEmailApp) {
                            MessageField(state.message, state.kind, state.messageMissing, onEvent)
                            DeviceInfoRow(state.includeDeviceInfo, onEvent)
                        } else {
                            // No field at all in a browser: the report is written in the mail app
                            // the user already has open, and a field here would only be a place to
                            // type something this screen cannot carry anywhere.
                            //
                            // The switch below is the phones' too, and for the same reason it is
                            // not drawn here: it keeps the device out of a report the app writes
                            // into a composer itself, while a browser's user pastes the block by
                            // hand - and the card is how they see what they are pasting, so a
                            // switch that hid it would hide the point of the screen.
                            SelectionContainer {
                                Text(
                                    strings.feedbackSendTo(FEEDBACK_ADDRESS),
                                    style = Naj.type.body,
                                    color = colors.ink,
                                )
                            }
                            if (details.isNotEmpty()) {
                                DetailsCard(details, copied = state.copied, onCopy = { onCopy(details) })
                            }
                        }
                    }
                }
            }
            if (state.hasEmailApp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = ContentWidth.Narrow)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + LocalNavBarInset.current),
                    ) {
                        SendButton(noEmailApp = state.noEmailApp, onSend = onSend)
                    }
                }
            }
        }
    }
}

/**
 * The two things a user can write to us about: what the app did, or what we hold about them.
 *
 * The choice is not decoration. It decides what the mail is called, which is what puts it in the
 * right place in our inbox, and it is the only thing that puts the identifier in it - attaching that
 * to an ordinary report would join the sender's address to everything filed under the identifier,
 * which is the one thing the analytics are built never to do.
 *
 * The line under the pills says what the request carries, and says it differently when the app is
 * showing no identifier: the reader who wonders why is told where one appears rather than left with
 * an email that cannot be matched to anything.
 */
@Composable
private fun KindPicker(kind: FeedbackKind, hasIdentifier: Boolean, onEvent: (FeedbackUiEvent) -> Unit) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill(
                strings.feedbackKindFeedback,
                selected = kind == FeedbackKind.Feedback,
                height = 36.dp,
            ) {
                onEvent(FeedbackUiEvent.SetKind(FeedbackKind.Feedback))
            }
            ChoicePill(
                strings.feedbackKindPrivacyRequest,
                selected = kind == FeedbackKind.PrivacyRequest,
                height = 36.dp,
            ) {
                onEvent(FeedbackUiEvent.SetKind(FeedbackKind.PrivacyRequest))
            }
        }
        if (kind == FeedbackKind.PrivacyRequest) {
            Text(
                text = if (hasIdentifier) {
                    strings.feedbackPrivacyRequestNote
                } else {
                    strings.feedbackPrivacyRequestNoIdentifier
                },
                style = Naj.type.rowSubtitle,
                color = colors.muted,
            )
        }
    }
}

/**
 * The report itself, on a card tall enough to write a paragraph in.
 *
 * A press with nothing written marks the field and says what is missing: the mark alone would
 * reach only the users who can see it, and the announcement is what reaches the rest.
 */
@Composable
private fun MessageField(
    message: String,
    kind: FeedbackKind,
    /** A press found nothing written, which the field is marked with until the next thing typed. */
    messageMissing: Boolean,
    onEvent: (FeedbackUiEvent) -> Unit,
) {
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
                        if (messageMissing) {
                            // `this.` on purpose: `error` alone would be ambiguous with
                            // kotlin.error, which throws instead.
                            this.error(missingMessage)
                            liveRegion = LiveRegionMode.Polite
                        }
                    },
            ) {
                if (message.isEmpty()) {
                    Text(
                        // The same field, asked differently: a report says what happened, a request
                        // says what the reader wants done.
                        text = if (kind == FeedbackKind.PrivacyRequest) {
                            strings.feedbackRequestHint
                        } else {
                            strings.feedbackMessageHint
                        },
                        style = Naj.type.option,
                        color = colors.muted,
                    )
                }
                BasicTextField(
                    value = message,
                    onValueChange = { onEvent(FeedbackUiEvent.Write(it)) },
                    textStyle = Naj.type.option.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.primaryDeep),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (messageMissing) {
            Spacer(Modifier.height(8.dp))
            // In the same color as the mark, for anyone the mark alone does not reach.
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
private fun DeviceInfoRow(includeDeviceInfo: Boolean, onEvent: (FeedbackUiEvent) -> Unit) {
    val colors = Naj.colors
    NajCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(FeedbackUiEvent.IncludeDeviceInfo(!includeDeviceInfo)) }
                .padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = includeDeviceInfo,
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
private fun SendButton(
    /** The last press found no email app to open, which the button says under it. */
    noEmailApp: Boolean,
    onSend: () -> Unit,
) {
    val colors = Naj.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PrimaryPill(
            text = strings.feedbackSend,
            onClick = onSend,
            height = PrimaryButtonHeight,
            fillWidth = true,
        )
        if (noEmailApp) {
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

/**
 * The block a browser's user pastes into their own email: what the report says about the build and
 * the device, with the press that takes it.
 *
 * The text is in a selection as well as on the clipboard, because a page's clipboard is the one
 * thing the app cannot check it wrote to, and taking it by hand is the way through for anyone the
 * press does not reach.
 */
@Composable
private fun DetailsCard(
    details: String,
    copied: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Naj.colors
    NajCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    strings.feedbackIncludeDetails,
                    style = Naj.type.rowSubtitle,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
                // The corner of the card, where the thing it does to the text below it belongs.
                OutlinePill(
                    text = if (copied) strings.feedbackCopied else strings.feedbackCopy,
                    icon = if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                    onClick = onCopy,
                    height = 36.dp,
                )
            }
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                Text(details, style = Naj.type.option, color = colors.ink)
            }
        }
    }
}

// ── Previews ──

/** The phone the screen is designed around, and what its report would name. */
private val previewPhone = "Pixel 7 · Android 14 (API 34)"

/** What a browser can report instead, which is all the web build has to name a device with. */
private val previewBrowser =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

/**
 * A report half written: on a phone unless asked for the browser, which is the screen's other
 * shape - the address it says to write to, and the block the user pastes into their own mail app.
 */
private fun previewFeedback(
    message: String = "",
    includeDeviceInfo: Boolean = false,
    messageMissing: Boolean = false,
    noEmailApp: Boolean = false,
    hasEmailApp: Boolean = true,
    copied: Boolean = false,
    device: String = previewPhone,
    appVersion: String? = "1.4.2",
    kind: FeedbackKind = FeedbackKind.Feedback,
    deviceIdentifier: String? = null,
): FeedbackUiState = FeedbackUiState(
    message = message,
    includeDeviceInfo = includeDeviceInfo,
    device = device,
    appVersion = appVersion,
    hasEmailApp = hasEmailApp,
    kind = kind,
    deviceIdentifier = deviceIdentifier,
    messageMissing = messageMissing,
    noEmailApp = noEmailApp,
    copied = copied,
)

/** Shaped like a real one: what the identifier in a request looks like when it is written out. */
private const val PREVIEW_IDENTIFIER = "0195f2c4-3b7e-7a41-9c6d-1f0a4e8b7c22"

/** The screen as it opens: nothing written, and the device left out of the report. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenPreview() {
    AppPreview { FeedbackScreen(previewFeedback(), onEvent = {}, onBack = {}, onSend = {}, onCopy = {}) }
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
            onCopy = {},
        )
    }
}

/** Send pressed with nothing written: the field is marked, and the answer sits under it. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenMessageMissingPreview() {
    AppPreview {
        FeedbackScreen(previewFeedback(messageMissing = true), onEvent = {}, onBack = {}, onSend = {}, onCopy = {})
    }
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
            onCopy = {},
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
            onCopy = {},
        )
    }
}

/**
 * The browser the web build actually is: the version its build stamped in, a device only the
 * user-agent string names, and no press - the address to write to, and the block to paste.
 */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun FeedbackScreenBrowserPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(
                message = "The timer kept counting after I answered.",
                hasEmailApp = false,
                device = previewBrowser,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** The copy taken: the press says so, and the block it took is on the clipboard. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun FeedbackScreenBrowserCopiedPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(
                message = "The timer kept counting after I answered.",
                hasEmailApp = false,
                copied = true,
                device = previewBrowser,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/**
 * The browser in the longest of the four translations, on a build that names no version of its
 * own: the card is the device line alone, and the address line is the one to watch for width.
 */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun FeedbackScreenBrowserSlovakPreview() {
    AppPreview(language = AppLanguage.Slovak) {
        FeedbackScreen(
            previewFeedback(
                message = "Časovač na otázke 12 sa stále počítal.",
                hasEmailApp = false,
                device = previewBrowser,
                appVersion = null,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** A phone in landscape, where the press holds its place below a message with little room left. */
@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun FeedbackScreenLandscapePreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(message = "The timer kept counting after I answered.", includeDeviceInfo = true),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** A request about the reader's own data, with the identifier the mail will carry. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenPrivacyRequestPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(
                message = "Please delete the statistics and crash reports you hold about me.",
                kind = FeedbackKind.PrivacyRequest,
                deviceIdentifier = PREVIEW_IDENTIFIER,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** The same request on a device the app has no identifier for, which the line under the pills says. */
@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun FeedbackScreenPrivacyRequestNoIdentifierPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(kind = FeedbackKind.PrivacyRequest),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** A request written in a browser, where the identifier travels in the block the reader pastes. */
@Preview(widthDp = 1100, heightDp = 900)
@Composable
private fun FeedbackScreenBrowserRequestPreview() {
    AppPreview {
        FeedbackScreen(
            previewFeedback(
                message = "Please delete the statistics you hold about me.",
                hasEmailApp = false,
                device = previewBrowser,
                kind = FeedbackKind.PrivacyRequest,
                deviceIdentifier = PREVIEW_IDENTIFIER,
            ),
            onEvent = {},
            onBack = {},
            onSend = {},
            onCopy = {},
        )
    }
}

/** The picker's three shapes: a report, a request with an identifier to quote, and one without. */
@Preview(widthDp = 400, heightDp = 320)
@Composable
private fun KindPickerPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("A report")
            KindPicker(FeedbackKind.Feedback, hasIdentifier = false, onEvent = {})
            PreviewLabel("A request, with an identifier to quote")
            KindPicker(FeedbackKind.PrivacyRequest, hasIdentifier = true, onEvent = {})
            PreviewLabel("A request, with no identifier to quote")
            KindPicker(FeedbackKind.PrivacyRequest, hasIdentifier = false, onEvent = {})
        }
    }
}
