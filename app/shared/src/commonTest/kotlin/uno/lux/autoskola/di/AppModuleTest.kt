package uno.lux.autoskola.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import uno.lux.autoskola.AppViewModel
import uno.lux.autoskola.data.banks.FakeQuestionBankDataSource
import uno.lux.autoskola.data.banks.QuestionBankDataSource
import uno.lux.autoskola.data.banks.testBank
import uno.lux.autoskola.home.HomeViewModel
import uno.lux.autoskola.onboarding.DisclaimerViewModel
import uno.lux.autoskola.onboarding.ExamSelectionViewModel
import uno.lux.autoskola.practice.PracticeViewModel
import uno.lux.autoskola.results.ResultsViewModel
import uno.lux.autoskola.settings.SettingsViewModel
import uno.lux.autoskola.testing.ViewModelTest
import kotlin.test.Test

class AppModuleTest : ViewModelTest() {

    @Test
    fun everyViewModelCanBeCreated() {
        val koin = koinApplication {
            modules(
                appModule,
                module {
                    single<Settings> { MapSettings() }
                    single<QuestionBankDataSource> { FakeQuestionBankDataSource.of(testBank()) }
                },
            )
        }.koin

        koin.get<AppViewModel>()
        koin.get<DisclaimerViewModel>()
        koin.get<ExamSelectionViewModel>()
        koin.get<HomeViewModel>()
        koin.get<SettingsViewModel>()
        koin.get<PracticeViewModel> { parametersOf("attempt-id") }
        koin.get<ResultsViewModel> { parametersOf("attempt-id") }
    }
}
