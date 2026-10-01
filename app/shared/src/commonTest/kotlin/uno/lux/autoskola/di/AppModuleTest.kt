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
import uno.lux.autoskola.data.signs.FakeSignCatalogueDataSource
import uno.lux.autoskola.data.signs.SignCatalogueDataSource
import uno.lux.autoskola.data.signs.testSignCatalogue
import uno.lux.autoskola.home.HomeViewModel
import uno.lux.autoskola.learn.IntersectionsViewModel
import uno.lux.autoskola.learn.SignsViewModel
import uno.lux.autoskola.learn.SituationViewModel
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
                    single<SignCatalogueDataSource> { FakeSignCatalogueDataSource.of(testSignCatalogue()) }
                },
            )
        }.koin

        koin.get<AppViewModel>()
        koin.get<DisclaimerViewModel>()
        koin.get<ExamSelectionViewModel>()
        koin.get<HomeViewModel>()
        koin.get<SignsViewModel>()
        koin.get<IntersectionsViewModel>()
        koin.get<SituationViewModel> { parametersOf(1) }
        koin.get<SettingsViewModel>()
        koin.get<PracticeViewModel> { parametersOf("attempt-id") }
        koin.get<ResultsViewModel> { parametersOf("attempt-id") }
    }
}
