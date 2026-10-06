package com.spiraclestudios.autoskola.feedback

import com.spiraclestudios.autoskola.testing.TestRepositories
import com.spiraclestudios.autoskola.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedbackViewModelTest : ViewModelTest() {

    private val repositories = TestRepositories()

    private val viewModel by lazy {
        FeedbackViewModel(
            repositories.analytics,
            version = "1.4.2",
            device = "Pixel 7 · Android 14",
        )
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
    fun thePlatformSaysWhetherTheReportGoesThroughAMailApp() = runTest {
        // The phones: the press opens an email app on the report, already written.
        assertTrue(observe(viewModel.uiState).value.hasEmailApp)

        // A browser: it says where to write, and offers the details to paste.
        val browser = FeedbackViewModel(repositories.analytics, version = null, device = "a browser", emailApp = false)
        assertFalse(observe(browser.uiState).value.hasEmailApp)
    }

    @Test
    fun theCopyOfTheDetailsIsSaidBackOnceItWasMade() = runTest {
        val state = observe(viewModel.uiState)

        assertFalse(state.value.copied)

        viewModel.onEvent(FeedbackUiEvent.Copied)

        assertTrue(state.value.copied)
    }

    @Test
    fun changingWhatGoesInTheReportForgetsTheCopyThatWasMade() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(FeedbackUiEvent.Copied)

        viewModel.onEvent(FeedbackUiEvent.IncludeDeviceInfo(false))

        assertFalse(state.value.copied)
    }

    @Test
    fun aBuildWithNothingToReportStillSends() = runTest {
        // A build with no version to name, and a device only if the user asks for it.
        val web = FeedbackViewModel(repositories.analytics, version = null, device = null)
        val state = observe(web.uiState)

        assertNull(state.value.appVersion)
        assertNull(state.value.deviceToSend)

        web.onEvent(FeedbackUiEvent.Write("Hello"))
        assertTrue(state.value.canSend)
    }

    @Test
    fun aRequestAboutDataCarriesTheIdentifierAndAReportDoesNot() = runTest {
        repositories.preferences.setAnalyticsEnabled(true)
        repositories.analytics.syncConsent()
        repositories.analyticsSink.identifier = "ident-5"
        repositories.analytics.refreshDeviceIdentifier()
        val state = observe(viewModel.uiState)

        // A report is not about their data: there is nothing to match, so nothing is attached.
        assertEquals(FeedbackKind.Feedback, state.value.kind)
        assertEquals("ident-5", state.value.deviceIdentifier)
        assertNull(state.value.identifierToSend)

        viewModel.onEvent(FeedbackUiEvent.SetKind(FeedbackKind.PrivacyRequest))

        assertEquals("ident-5", state.value.identifierToSend)
    }

    @Test
    fun aRequestWithNothingToMatchItToStillSends() = runTest {
        // A device the app is showing no identifier for. The request goes anyway; what tells the
        // reader that nothing will be found is the line under the picker, not the press.
        val state = observe(viewModel.uiState)

        viewModel.onEvent(FeedbackUiEvent.SetKind(FeedbackKind.PrivacyRequest))
        viewModel.onEvent(FeedbackUiEvent.Write("Please delete everything you hold about me."))

        assertNull(state.value.identifierToSend)
        assertTrue(state.value.canSend)
    }

    @Test
    fun pickingADifferentKindForgetsTheCopyThatWasMade() = runTest {
        val state = observe(viewModel.uiState)
        viewModel.onEvent(FeedbackUiEvent.Copied)

        viewModel.onEvent(FeedbackUiEvent.SetKind(FeedbackKind.PrivacyRequest))

        assertFalse(state.value.copied)
    }
}
