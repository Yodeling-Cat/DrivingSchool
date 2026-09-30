package uno.lux.autoskola

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uno.lux.autoskola.di.initKoin
import uno.lux.autoskola.i18n.LocalStrings
import uno.lux.autoskola.navigation.ExamNavigation
import uno.lux.autoskola.onboarding.DisclaimerRoute
import uno.lux.autoskola.onboarding.ExamSelectionRoute
import uno.lux.autoskola.ui.LoadingScreen
import uno.lux.autoskola.ui.ProvideWindowSizeClasses
import uno.lux.autoskola.ui.theme.NajAutoskolaTheme

@Composable
@Preview
fun App() {
    remember { initKoin() }
    NajAutoskolaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val strings = state.language.strings
        LaunchedEffect(strings.appName) { setWindowTitle(strings.appName) }
        CompositionLocalProvider(LocalStrings provides strings) {
            ProvideWindowSizeClasses {
                when (state.destination) {
                    AppDestination.Loading -> LoadingScreen()

                    AppDestination.Disclaimer -> DisclaimerRoute()

                    // Picking an exam here changes the destination, so there's nothing else to do when done.
                    AppDestination.ChooseExam -> ExamSelectionRoute(onBack = null, onDone = {})

                    AppDestination.Exam -> ExamNavigation()
                }
            }
        }
    }
}
