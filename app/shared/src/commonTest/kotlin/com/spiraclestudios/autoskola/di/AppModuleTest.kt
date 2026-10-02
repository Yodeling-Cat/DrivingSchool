package com.spiraclestudios.autoskola.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.AppViewModel
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.signs.FakeSignCatalogueDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogueDataSource
import com.spiraclestudios.autoskola.data.signs.testSignCatalogue
import com.spiraclestudios.autoskola.home.HomeViewModel
import com.spiraclestudios.autoskola.learn.IntersectionsViewModel
import com.spiraclestudios.autoskola.learn.SignsViewModel
import com.spiraclestudios.autoskola.learn.SituationViewModel
import com.spiraclestudios.autoskola.onboarding.DisclaimerViewModel
import com.spiraclestudios.autoskola.onboarding.ExamSelectionViewModel
import com.spiraclestudios.autoskola.practice.PracticeViewModel
import com.spiraclestudios.autoskola.results.ResultsViewModel
import com.spiraclestudios.autoskola.settings.SettingsViewModel
import com.spiraclestudios.autoskola.testing.ViewModelTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
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
