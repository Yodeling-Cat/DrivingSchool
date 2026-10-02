package com.spiraclestudios.autoskola.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spiraclestudios.autoskola.appVersion
import com.spiraclestudios.autoskola.deviceDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FeedbackUiState(
    /** What the user has written, which is what the report will carry. */
    val message: String,
    /** Whether the device the report was written on goes in with it. */
    val includeDeviceInfo: Boolean,
    /** What the device says about itself, or `null` where it says nothing. */
    val device: String?,
    /** What the platform says this build is, or `null` where it says nothing, as the web build does. */
    val appVersion: String?,
    /** The press that found nothing written, until the next thing typed. */
    val messageMissing: Boolean,
    /** The platform has no email app to open, which there is nothing to do about but say. */
    val noEmailApp: Boolean,
) {
    /** Nothing written is nothing to send, so the press says so instead of the button doing nothing. */
    val canSend: Boolean get() = message.isNotBlank()

    /** The device the report names, only while the user has asked for it. */
    val deviceToSend: String? get() = device?.takeIf { includeDeviceInfo }
}

sealed interface FeedbackUiEvent {
    /** Something was typed, or deleted. */
    data class Write(val text: String) : FeedbackUiEvent

    /** The answer to whether the device goes in the report. */
    data class IncludeDeviceInfo(val included: Boolean) : FeedbackUiEvent

    /** Send was pressed with nothing written, which is the correction the user has to make. */
    data object MessageMissing : FeedbackUiEvent

    /** The platform had no email app to hand the report to. */
    data object NoEmailApp : FeedbackUiEvent
}

/**
 * What the feedback screen holds: what has been written, and what the app would add to it.
 *
 * The report itself is written out where the words for it are - in the screen, which has the
 * app's translations - and this holds the facts it is built from. [version] and [device] are read
 * once, because the platform's answers cannot change while the screen is up, and taking them as
 * parameters is also what lets a test say which build and which phone a report would name.
 */
class FeedbackViewModel(
    private val version: String? = appVersion(),
    private val device: String? = deviceDescription(),
) : ViewModel() {

    private val message = MutableStateFlow("")
    private val includeDeviceInfo = MutableStateFlow(false)
    private val messageMissing = MutableStateFlow(false)
    private val noEmailApp = MutableStateFlow(false)

    val uiState: StateFlow<FeedbackUiState> =
        combine(message, includeDeviceInfo, messageMissing, noEmailApp) { message, include, missing, noApp ->
            FeedbackUiState(
                message = message,
                includeDeviceInfo = include,
                device = device,
                appVersion = version,
                messageMissing = missing,
                noEmailApp = noApp,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FeedbackUiState(
                message = "",
                includeDeviceInfo = false,
                device = device,
                appVersion = version,
                messageMissing = false,
                noEmailApp = false,
            ),
        )

    fun onEvent(event: FeedbackUiEvent) {
        when (event) {
            // Writing is the correction the complaint asked for, so it is what takes it away.
            is FeedbackUiEvent.Write -> {
                message.value = event.text
                messageMissing.value = false
            }

            is FeedbackUiEvent.IncludeDeviceInfo -> includeDeviceInfo.value = event.included

            FeedbackUiEvent.MessageMissing -> messageMissing.value = true

            FeedbackUiEvent.NoEmailApp -> noEmailApp.value = true
        }
    }
}
