package com.spiraclestudios.autoskola.di

import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.AppViewModel
import com.spiraclestudios.autoskola.about.AboutViewModel
import com.spiraclestudios.autoskola.data.ads.AdDataSource
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.ads.platformAdDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.PostHogAnalyticsDataSource
import com.spiraclestudios.autoskola.data.analytics.postHogContext
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
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.data.review.ReviewRepository
import com.spiraclestudios.autoskola.data.signs.ResourceSignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogRepository
import com.spiraclestudios.autoskola.feedback.FeedbackViewModel
import com.spiraclestudios.autoskola.home.HomeViewModel
import com.spiraclestudios.autoskola.isDebugBuild
import com.spiraclestudios.autoskola.learn.IntersectionsViewModel
import com.spiraclestudios.autoskola.learn.SignsViewModel
import com.spiraclestudios.autoskola.learn.SituationViewModel
import com.spiraclestudios.autoskola.navigation.navigationState
import com.spiraclestudios.autoskola.onboarding.ConsentViewModel
import com.spiraclestudios.autoskola.onboarding.DisclaimerViewModel
import com.spiraclestudios.autoskola.onboarding.ExamSelectionViewModel
import com.spiraclestudios.autoskola.practice.PracticeViewModel
import com.spiraclestudios.autoskola.results.ResultsViewModel
import com.spiraclestudios.autoskola.screenstate.ScreenStateStorage
import com.spiraclestudios.autoskola.screenstate.StoredScreenState
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
    single<SignCatalogDataSource> { ResourceSignCatalogDataSource() }
    single<PracticeAnswerDataSource> { SettingsPracticeAnswerDataSource(get()) }
    single<AnalyticsDataSource> { PostHogAnalyticsDataSource(postHogContext(), debug = isDebugBuild()) }
    // Android answers Google's SDK, which falls back to the no-op source until the entry point has
    // handed over an application; iOS and the web answer the no-op one outright.
    single<AdDataSource> { platformAdDataSource() }

    // Repositories
    single { AttemptRepository(get()) }
    single { QuestionBankRepository(get()) }
    single { SignCatalogRepository(get()) }
    single { PracticeAnswerRepository(get()) }
    single { PreferencesRepository(get()) }
    single { AnalyticsRepository(get(), get()) }
    single { ReviewRepository(get()) }
    single { ExamRepository(get(), get(), get(), get()) }
    // The store is not wired up yet, so this answers from one flag in code; see `plans/premium.md`.
    single { PremiumRepository() }
    single { AdRepository(get(), get(), get(), get()) }

    // Navigation
    single { navigationState(get()) }

    // The state of the screens: which question a test was on, how far each screen was scrolled.
    single<ScreenStateStorage> { StoredScreenState(get()) }

    // ViewModels
    viewModelOf(::AppViewModel)
    viewModelOf(::DisclaimerViewModel)
    viewModelOf(::ConsentViewModel)
    viewModelOf(::ExamSelectionViewModel)
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModelOf(::SignsViewModel)
    viewModelOf(::IntersectionsViewModel)
    viewModel { (number: Int) -> SituationViewModel(number, get(), get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModel { AboutViewModel() }
    // Both of these read the platform's own answers as their defaults, which Koin must not
    // resolve for them.
    viewModel { FeedbackViewModel() }
    viewModel { (attemptId: String) -> PracticeViewModel(attemptId, get(), get(), get(), get(), get()) }
    viewModel { (attemptId: String) -> ResultsViewModel(attemptId, get()) }
}

/**
 * Starts Koin once per process. Safe to call again, e.g. when Android recreates the activity:
 * the repositories must outlive it, because retained ViewModels still use them.
 */
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() == null) startKoin { modules(appModule) }
}
