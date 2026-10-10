package com.spiraclestudios.autoskola.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.spiraclestudios.autoskola.AppViewModel
import com.spiraclestudios.autoskola.about.AboutViewModel
import com.spiraclestudios.autoskola.content.BundledContent
import com.spiraclestudios.autoskola.data.ads.AdRepository
import com.spiraclestudios.autoskola.data.analytics.AnalyticsDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsRepository
import com.spiraclestudios.autoskola.data.analytics.RecordingAnalyticsDataSource
import com.spiraclestudios.autoskola.data.banks.FakeQuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.QuestionBankDataSource
import com.spiraclestudios.autoskola.data.banks.testBank
import com.spiraclestudios.autoskola.data.premium.PremiumRepository
import com.spiraclestudios.autoskola.data.premium.PremiumSync
import com.spiraclestudios.autoskola.data.signs.FakeSignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.SignCatalogDataSource
import com.spiraclestudios.autoskola.data.signs.testSignCatalog
import com.spiraclestudios.autoskola.feedback.FeedbackViewModel
import com.spiraclestudios.autoskola.home.HomeViewModel
import com.spiraclestudios.autoskola.learn.IntersectionsViewModel
import com.spiraclestudios.autoskola.learn.LearnViewModel
import com.spiraclestudios.autoskola.learn.SignsViewModel
import com.spiraclestudios.autoskola.learn.SituationViewModel
import com.spiraclestudios.autoskola.onboarding.ConsentViewModel
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
                    // The country's content an entry point hands in; the data sources below stand in
                    // for what is read from it.
                    single<BundledContent> { TestContent }
                    single<QuestionBankDataSource> { FakeQuestionBankDataSource.of(testBank()) }
                    single<SignCatalogDataSource> { FakeSignCatalogDataSource.of(testSignCatalog()) }
                    // The real one never sends without a platform context, but a test should not
                    // depend on that: the sink makes "nothing was sent" assertable.
                    single<AnalyticsDataSource> { RecordingAnalyticsDataSource() }
                },
            )
        }.koin

        koin.get<AppViewModel>()
        koin.get<DisclaimerViewModel>()
        koin.get<ConsentViewModel>()
        koin.get<ExamSelectionViewModel>()
        koin.get<HomeViewModel>()
        koin.get<LearnViewModel>()
        koin.get<SignsViewModel>()
        koin.get<IntersectionsViewModel>()
        koin.get<SituationViewModel> { parametersOf(1) }
        koin.get<SettingsViewModel>()
        koin.get<AboutViewModel>()
        koin.get<FeedbackViewModel>()
        koin.get<PracticeViewModel> { parametersOf("attempt-id") }
        koin.get<ResultsViewModel> { parametersOf("attempt-id") }
        koin.get<AnalyticsRepository>()
        koin.get<PremiumRepository>()
        koin.get<PremiumSync>()
        koin.get<AdRepository>()
    }
}

/** A Slovak app's content with no files, which nothing here reads: the data sources are fakes. */
private object TestContent : BundledContent {
    override val country = "SK"

    override suspend fun readBytes(path: String): ByteArray = error("no file is read in this test: $path")

    override fun uri(path: String): String = "test://$path"
}
