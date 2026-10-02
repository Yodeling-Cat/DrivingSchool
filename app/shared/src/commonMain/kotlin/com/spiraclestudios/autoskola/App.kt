package com.spiraclestudios.autoskola

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.di.initKoin
import com.spiraclestudios.autoskola.i18n.LocalStrings
import com.spiraclestudios.autoskola.navigation.ExamNavigation
import com.spiraclestudios.autoskola.onboarding.ConsentRoute
import com.spiraclestudios.autoskola.onboarding.DisclaimerRoute
import com.spiraclestudios.autoskola.onboarding.ExamSelectionRoute
import com.spiraclestudios.autoskola.ui.LoadingScreen
import com.spiraclestudios.autoskola.ui.ProvideWindowSizeClasses
import com.spiraclestudios.autoskola.ui.theme.NajAutoskolaTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    remember { initKoin() }
    NajAutoskolaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val analytics = koinInject<AnalyticsRepository>()
        val strings = state.language.strings
        // Reading the disclaimer is not agreeing to anything, so the consent screen follows it and
        // is where the documents and the two sharing questions are answered. It is pure
        // navigation, so it lives here rather than in the ViewModel.
        var consentStarted by remember { mutableStateOf(false) }
        LaunchedEffect(strings.appName) { setWindowTitle(strings.appName) }
        // Onboarding is not on the back stack, so it reports itself from here; every screen after
        // it reports itself from `ExamNavigation`, which sees each route change.
        LaunchedEffect(state.destination, consentStarted) {
            when (state.destination) {
                AppDestination.Disclaimer -> {
                    analytics.screen(if (consentStarted) "consent" else "disclaimer")
                }

                AppDestination.ChooseExam -> analytics.screen("choose_exam")

                // A frame between the other two rather than a screen, and the exam's own screens
                // are already being reported.
                AppDestination.Loading, AppDestination.Exam -> Unit
            }
        }
        CompositionLocalProvider(LocalStrings provides strings) {
            ProvideWindowSizeClasses {
                when (state.destination) {
                    AppDestination.Loading -> LoadingScreen()

                    AppDestination.Disclaimer -> if (consentStarted) {
                        ConsentRoute()
                    } else {
                        DisclaimerRoute(onContinue = { consentStarted = true })
                    }

                    // Picking an exam here changes the destination, so there's nothing else to do when done.
                    AppDestination.ChooseExam -> ExamSelectionRoute(onBack = null, onDone = {})

                    AppDestination.Exam -> ExamNavigation()
                }
            }
        }
    }
}
