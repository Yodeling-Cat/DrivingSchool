package com.spiraclestudios.autoskola

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.content.BundledContent
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.di.initKoin
import com.spiraclestudios.autoskola.i18n.LocalStrings
import com.spiraclestudios.autoskola.navigation.ExamNavigation
import com.spiraclestudios.autoskola.onboarding.ConsentRoute
import com.spiraclestudios.autoskola.onboarding.DisclaimerRoute
import com.spiraclestudios.autoskola.onboarding.ExamSelectionRoute
import com.spiraclestudios.autoskola.screenstate.ProvideStoredScreenState
import com.spiraclestudios.autoskola.screenstate.ScreenStateStorage
import com.spiraclestudios.autoskola.ui.LoadingScreen
import com.spiraclestudios.autoskola.ui.ProvideWindowSizeClasses
import com.spiraclestudios.autoskola.ui.theme.NajAutoskolaTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The whole app, over the [content] of the country it is built for: each country's app passes its
 * own (see docs/countries.md).
 */
@Composable
fun App(content: BundledContent) {
    remember { initKoin(content) }
    NajAutoskolaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val analytics = koinInject<AnalyticsRepository>()
        val screenState = koinInject<ScreenStateStorage>()
        val strings = state.language.strings
        LaunchedEffect(strings.appName) { setWindowTitle(strings.appName) }
        // Every time the app comes to the front the store is asked again what the user owns: a refund can
        // have taken "remove ads" back while it was away, and a purchase made elsewhere can have arrived.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(AppUiEvent.Resumed) }
        // Onboarding is not on the back stack, so it reports itself from here; every screen after it
        // reports itself from `ExamNavigation`, which sees each route change. Which of the two
        // onboarding screens is showing is the ViewModel's, decided from stored preferences, so a
        // rotation comes back to the same one rather than to the first.
        LaunchedEffect(state.destination) {
            when (state.destination) {
                AppDestination.Disclaimer -> analytics.screen("disclaimer")

                AppDestination.Consent -> analytics.screen("consent")

                AppDestination.ChooseExam -> analytics.screen("choose_exam")

                // A frame between the other two rather than a screen, and the exam's own screens
                // are already being reported.
                AppDestination.Loading, AppDestination.Exam -> Unit
            }
        }
        CompositionLocalProvider(LocalStrings provides strings) {
            ProvideWindowSizeClasses {
                // Everything inside - onboarding and every screen after it - keeps what
                // rememberSaveable holds where a killed process can't reach it (see
                // ProvideStoredScreenState).
                ProvideStoredScreenState(screenState) {
                    when (state.destination) {
                        AppDestination.Loading -> LoadingScreen()

                        // Reading the disclaimer is not agreeing to anything, so the consent screen
                        // follows it and is where the documents and the two sharing questions are
                        // answered.
                        AppDestination.Disclaimer -> DisclaimerRoute(
                            onContinue = { viewModel.onEvent(AppUiEvent.DisclaimerRead) },
                        )

                        AppDestination.Consent -> ConsentRoute()

                        // Picking an exam here changes the destination, so there's nothing else to do when done.
                        AppDestination.ChooseExam -> ExamSelectionRoute(onBack = null, onDone = {})

                        AppDestination.Exam -> ExamNavigation()
                    }
                }
            }
        }
    }
}

/**
 * The whole app, as a smoke check. A preview bundles no country, so the content it stands on lists
 * no banks, and the app opens on its first screen.
 */
@Preview
@Composable
private fun AppPreview() {
    App(PreviewContent)
}

private object PreviewContent : BundledContent {
    override val country = "SK"

    override suspend fun readBytes(path: String): ByteArray = "[]".encodeToByteArray()

    override fun uri(path: String): String = "preview://$path"
}
