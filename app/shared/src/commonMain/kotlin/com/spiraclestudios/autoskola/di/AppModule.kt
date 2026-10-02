package com.spiraclestudios.autoskola.di

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.AppViewModel
import com.spiraclestudios.autoskola.about.AboutViewModel
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerDataSource
import com.spiraclestudios.autoskola.data.answers.PracticeAnswerRepository
import com.spiraclestudios.autoskola.data.answers.SettingsPracticeAnswerDataSource
import com.spiraclestudios.autoskola.data.attempts.AttemptDataSource
import com.spiraclestudios.autoskola.data.attempts.AttemptRepository
import com.spiraclestudios.autoskola.data.attempts.SettingsAttemptDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankRepository
import com.spiraclestudios.autoskola.data.banks.ResourceQuestionBankDataSource
import com.spiraclestudios.autoskola.data.exam.ExamRepository
import com.spiraclestudios.autoskola.data.preferences.PreferencesRepository
import com.spiraclestudios.autoskola.data.signs.ResourceSignCatalogueDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogueDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogueRepository
import com.spiraclestudios.autoskola.home.HomeViewModel
import com.spiraclestudios.autoskola.learn.IntersectionsViewModel
import com.spiraclestudios.autoskola.learn.SignsViewModel
import com.spiraclestudios.autoskola.learn.SituationViewModel
import com.spiraclestudios.autoskola.navigation.navigationState
import com.spiraclestudios.autoskola.onboarding.DisclaimerViewModel
import com.spiraclestudios.autoskola.onboarding.ExamSelectionViewModel
import com.spiraclestudios.autoskola.practice.PracticeViewModel
import com.spiraclestudios.autoskola.results.ResultsViewModel
import com.spiraclestudios.autoskola.settings.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/** Data sources and repositories are app-wide singletons; ViewModels are scoped to their screen. */
val appModule = module {
    // Data sources
    single<Settings> { Settings() }
    single<AttemptDataSource> { SettingsAttemptDataSource(get()) }
    single<QuestionBankDataSource> { ResourceQuestionBankDataSource() }
    single<SignCatalogueDataSource> { ResourceSignCatalogueDataSource() }
    single<PracticeAnswerDataSource> { SettingsPracticeAnswerDataSource(get()) }

    // Repositories
    single { AttemptRepository(get()) }
    single { QuestionBankRepository(get()) }
    single { SignCatalogueRepository(get()) }
    single { PracticeAnswerRepository(get()) }
    single { PreferencesRepository(get()) }
    single { ExamRepository(get(), get(), get()) }

    // Navigation
    single { navigationState(get()) }

    // ViewModels
    viewModelOf(::AppViewModel)
    viewModelOf(::DisclaimerViewModel)
    viewModelOf(::ExamSelectionViewModel)
    viewModel { HomeViewModel(get(), get()) }
    viewModelOf(::SignsViewModel)
    viewModelOf(::IntersectionsViewModel)
    viewModel { (number: Int) -> SituationViewModel(number, get(), get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModel { AboutViewModel() }
    viewModel { (attemptId: String) -> PracticeViewModel(attemptId, get(), get(), get(), get()) }
    viewModel { (attemptId: String) -> ResultsViewModel(attemptId, get()) }
}

/**
 * Starts Koin once per process. Safe to call again, e.g. when Android recreates the activity:
 * the repositories must outlive it, because retained ViewModels still use them.
 */
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() == null) startKoin { modules(appModule) }
}
