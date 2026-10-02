package com.spiraclestudios.autoskola.feedback

import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedbackViewModelTest : ViewModelTest() {

    private val viewModel by lazy {
        FeedbackViewModel(version = "1.4.2", device = "Pixel 7 · Android 14")
    }

    @Test
    fun whatIsWrittenIsWhatIsSent() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(FeedbackUiEvent.Write("The timer keeps counting after I finish."))

        assertEquals("The timer keeps counting after I finish.", state.value.message)
        assertTrue(state.value.canSend)
        assertEquals("1.4.2", state.value.appVersion)
    }

    @Test
    fun theDeviceIsInTheReportOnlyWhenItIsAskedFor() = runTest {
        val state = observe(viewModel.uiState)

        assertFalse(state.value.includeDeviceInfo)
        assertNull(state.value.deviceToSend)

        viewModel.onEvent(FeedbackUiEvent.IncludeDeviceInfo(true))
        assertEquals("Pixel 7 · Android 14", state.value.deviceToSend)

        viewModel.onEvent(FeedbackUiEvent.IncludeDeviceInfo(false))
        assertNull(state.value.deviceToSend)
    }

    @Test
    fun aPressWithNothingWrittenIsAnsweredRatherThanSilentlyDoingNothing() = runTest {
        val state = observe(viewModel.uiState)

        assertFalse(state.value.canSend)
        viewModel.onEvent(FeedbackUiEvent.MessageMissing)
        assertTrue(state.value.messageMissing)
    }

    @Test
    fun typingTakesTheComplaintAway() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(FeedbackUiEvent.MessageMissing)

        viewModel.onEvent(FeedbackUiEvent.Write("a"))

        assertFalse(state.value.messageMissing)
    }

    @Test
    fun whitespaceIsNotAMessage() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(FeedbackUiEvent.Write("   \n "))

        assertFalse(state.value.canSend)
    }

    @Test
    fun aDeviceWithNoEmailAppIsSaidRatherThanIgnored() = runTest {
        val state = observe(viewModel.uiState)

        viewModel.onEvent(FeedbackUiEvent.NoEmailApp)

        assertTrue(state.value.noEmailApp)
    }

    @Test
    fun aBuildWithNothingToReportStillSends() = runTest {
        // The web build: no version of its own, and a device only if the user asks for it.
        val web = FeedbackViewModel(version = null, device = null)
        val state = observe(web.uiState)

        assertNull(state.value.appVersion)
        assertNull(state.value.deviceToSend)

        web.onEvent(FeedbackUiEvent.Write("Hello"))
        assertTrue(state.value.canSend)
    }
}
