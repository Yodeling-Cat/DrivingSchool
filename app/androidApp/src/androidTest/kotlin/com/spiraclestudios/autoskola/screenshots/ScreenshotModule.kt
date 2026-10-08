package com.spiraclestudios.autoskola.screenshots

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.spiraclestudios.autoskola.data.ads.AdConsent
import com.spiraclestudios.autoskola.data.ads.AdDataSource
import com.spiraclestudios.autoskola.data.ads.NoAdConsent
import com.spiraclestudios.autoskola.data.ads.NoOpAdDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsDataSource
import com.spiraclestudios.autoskola.data.analytics.AnalyticsEvent
import com.spiraclestudios.autoskola.data.premium.BillingDataSource
import com.spiraclestudios.autoskola.data.premium.ProductOffer
import com.spiraclestudios.autoskola.data.premium.PurchaseQuery
import com.spiraclestudios.autoskola.data.premium.PurchaseResult
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import org.koin.dsl.module

/** The preferences file the screenshots run in, so the app's own data on the device is left alone. */
private const val SCREENSHOT_PREFERENCES = "store_screenshots"

/** The screenshots' own [Settings]: empty at the start of every screen. */
fun screenshotSettings(context: Context): Settings {
    val preferences = context.getSharedPreferences(SCREENSHOT_PREFERENCES, Context.MODE_PRIVATE)
    preferences.edit().clear().commit()
    return SharedPreferencesSettings(preferences)
}

/**
 * What the store screenshots replace in `appModule`: a store listing shows the app, not an ad, and a
 * screenshot run must not report itself to anyone.
 *
 * - No ad SDK and no consent form, so nothing is requested and no banner is drawn.
 * - A store that says "remove ads" has been bought, so nothing offers to sell it either.
 * - Analytics that send nothing.
 */
fun screenshotModule(settings: Settings) = module {
    single<Settings> { settings }
    single<AdDataSource> { NoOpAdDataSource() }
    single<AdConsent> { NoAdConsent() }
    single<BillingDataSource> { AdsRemovedBillingDataSource }
    single<AnalyticsDataSource> { SilentAnalyticsDataSource }
}

private object AdsRemovedBillingDataSource : BillingDataSource {
    private val adsRemoved = PremiumStatus.AdsRemoved(PurchaseSource.Play)

    override suspend fun query(): PurchaseQuery = PurchaseQuery.Answered(adsRemoved)

    override suspend fun removeAdsOffer(): ProductOffer? = null

    override suspend fun buyRemoveAds(): PurchaseResult = PurchaseResult.AlreadyOwned
}

private object SilentAnalyticsDataSource : AnalyticsDataSource {
    override fun capture(event: AnalyticsEvent, properties: Map<String, Any>) = Unit

    override fun screen(name: String) = Unit

    override fun deviceIdentifier(): String? = null

    override fun setConsent(crashReporting: Boolean, analytics: Boolean) = Unit
}
