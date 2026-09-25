package uno.lux.autoskola.di

import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import uno.lux.autoskola.AppViewModel
import uno.lux.autoskola.data.attempts.AttemptDataSource
import uno.lux.autoskola.data.attempts.AttemptRepository
import uno.lux.autoskola.data.attempts.SettingsAttemptDataSource
import uno.lux.autoskola.data.banks.QuestionBankDataSource
import uno.lux.autoskola.data.banks.QuestionBankRepository
import uno.lux.autoskola.data.banks.ResourceQuestionBankDataSource
import uno.lux.autoskola.data.exam.ExamRepository
import uno.lux.autoskola.data.preferences.PreferencesRepository
import uno.lux.autoskola.home.HomeViewModel
import uno.lux.autoskola.onboarding.DisclaimerViewModel
import uno.lux.autoskola.onboarding.ExamSelectionViewModel
import uno.lux.autoskola.practice.PracticeViewModel
import uno.lux.autoskola.results.ResultsViewModel
import uno.lux.autoskola.settings.SettingsViewModel

/** Data sources and repositories are app-wide singletons; ViewModels are scoped to their screen. */
val appModule = module {
    // Data sources
    single<Settings> { Settings() }
    single<AttemptDataSource> { SettingsAttemptDataSource(get()) }
    single<QuestionBankDataSource> { ResourceQuestionBankDataSource() }

    // Repositories
    single { AttemptRepository(get()) }
    single { QuestionBankRepository(get()) }
    single { PreferencesRepository(get()) }
    single { ExamRepository(get(), get(), get()) }

    // ViewModels
    viewModelOf(::AppViewModel)
    viewModelOf(::DisclaimerViewModel)
    viewModelOf(::ExamSelectionViewModel)
    viewModel { HomeViewModel(get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModel { (attemptId: String) -> PracticeViewModel(attemptId, get(), get()) }
    viewModel { (attemptId: String) -> ResultsViewModel(attemptId, get()) }
}

/**
 * Starts Koin once per process. Safe to call again, e.g. when Android recreates the activity:
 * the repositories must outlive it, because retained ViewModels still use them.
 */
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() == null) startKoin { modules(appModule) }
}
