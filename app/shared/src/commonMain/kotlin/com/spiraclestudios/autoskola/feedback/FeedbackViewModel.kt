package com.spiraclestudios.autoskola.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.appVersion
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.deviceDescription
import com.spiraclestudios.autoskola.hasEmailApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * What the user is writing about, which decides what the mail is called and whether the identifier
 * goes in it.
 */
enum class FeedbackKind {
    /** Something the app did, or did not do, which is what a report is for. */
    Feedback,

    /**
     * A request about the user's own data. The only kind that carries the identifier, because it is
     * the only kind where matching matters: we cannot find what we hold without it, and it is the one
     * thing about our side of the app that a user cannot find on their own device.
     */
    PrivacyRequest,
}

data class FeedbackUiState(
    /** What the user has written, which is what the report will carry. */
    val message: String,
    /** Whether the device the report was written on goes in with it. */
    val includeDeviceInfo: Boolean,
    /** What the device says about itself, or `null` where it says nothing. */
    val device: String?,
    /** What the platform says this build is, or `null` where it says nothing. */
    val appVersion: String?,
    /** Whether this platform can open an email app on the report - the phones can, a browser cannot. */
    val hasEmailApp: Boolean,
    /** What is being written: a report about the app, or a request about their own data. */
    val kind: FeedbackKind,
    /**
     * The identifier Settings is showing for this device, or `null` while the app has none to show.
     * Published by [AnalyticsRepository], which has none until the SDK is running.
     */
    val deviceIdentifier: String?,
    /** The press that found nothing written, until the next thing typed. */
    val messageMissing: Boolean,
    /** The platform has no email app to open, which there is nothing to do about but say. */
    val noEmailApp: Boolean,
    /** The details block has been put on the clipboard, which the press on a browser says. */
    val copied: Boolean,
) {
    /** Nothing written is nothing to send, so the press says so instead of the button doing nothing. */
    val canSend: Boolean get() = message.isNotBlank()

    /** The device the report names, only while the user has asked for it. */
    val deviceToSend: String? get() = device?.takeIf { includeDeviceInfo }

    /**
     * The identifier the mail carries. Only a request about data gets one: in an ordinary report it
     * would join the sender's address to everything filed under the identifier, which is the one
     * thing the analytics are built never to do.
     */
    val identifierToSend: String? get() = deviceIdentifier?.takeIf { kind == FeedbackKind.PrivacyRequest }
}

sealed interface FeedbackUiEvent {
    /** Something was typed, or deleted. */
    data class Write(val text: String) : FeedbackUiEvent

    /** The answer to whether the device goes in the report. */
    data class IncludeDeviceInfo(val included: Boolean) : FeedbackUiEvent

    /** What is being written: a report about the app, or a request about the user's own data. */
    data class SetKind(val kind: FeedbackKind) : FeedbackUiEvent

    /** Send was pressed with nothing written, which is the correction the user has to make. */
    data object MessageMissing : FeedbackUiEvent

    /** The platform had no email app to hand the report to. */
    data object NoEmailApp : FeedbackUiEvent

    /** The details block a browser's user pastes was put on the clipboard. */
    data object Copied : FeedbackUiEvent
}

/**
 * What the feedback screen holds: what has been written, and what the app would add to it.
 *
 * The report itself is written out where the words for it are - in the screen, which has the
 * app's translations - and this holds the facts it is built from. [version], [device] and
 * [hasEmailApp] are read once, because the platform's answers cannot change while the screen is
 * up, and taking them as parameters is also what lets a test say which build, which phone, and
 * whether a mail app, a report would be written against. The identifier is the exception: it comes
 * from [analytics] as a flow, because it exists only while the SDK is running rather than whenever
 * a screen is built.
 */
class FeedbackViewModel(
    private val analytics: AnalyticsRepository,
    private val version: String? = appVersion(),
    private val device: String? = deviceDescription(),
    private val emailApp: Boolean = hasEmailApp(),
) : ViewModel() {

    /** Everything the screen holds that is its own; only the identifier comes from elsewhere. */
    private val local = MutableStateFlow(
        FeedbackUiState(
            message = "",
            includeDeviceInfo = false,
            device = device,
            appVersion = version,
            hasEmailApp = emailApp,
            kind = FeedbackKind.Feedback,
            deviceIdentifier = null,
            messageMissing = false,
            noEmailApp = false,
            copied = false,
        ),
    )

    val uiState: StateFlow<FeedbackUiState> =
        combine(local, analytics.deviceIdentifier) { state, identifier -> state.copy(deviceIdentifier = identifier) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                local.value.copy(deviceIdentifier = analytics.deviceIdentifier.value),
            )

    init {
        // The identifier is there only while the SDK is running and has one to report, so the screen
        // asks for the current one instead of trusting what was read when something else last
        // changed. Reading it starts nothing.
        analytics.refreshDeviceIdentifier()
    }

    fun onEvent(event: FeedbackUiEvent) {
        when (event) {
            // Writing is the correction the complaint asked for, so it is what takes it away.
            is FeedbackUiEvent.Write -> local.update { it.copy(message = event.text, messageMissing = false) }

            // What was copied is no longer what is shown, so the copy is forgotten with it.
            is FeedbackUiEvent.IncludeDeviceInfo -> local.update {
                it.copy(includeDeviceInfo = event.included, copied = false)
            }

            // A different kind of message is a different mail, and the copy that was made was of the
            // one that is no longer being written.
            is FeedbackUiEvent.SetKind -> local.update { it.copy(kind = event.kind, copied = false) }

            FeedbackUiEvent.MessageMissing -> local.update { it.copy(messageMissing = true) }

            FeedbackUiEvent.NoEmailApp -> local.update { it.copy(noEmailApp = true) }

            FeedbackUiEvent.Copied -> local.update { it.copy(copied = true) }
        }
    }
}
