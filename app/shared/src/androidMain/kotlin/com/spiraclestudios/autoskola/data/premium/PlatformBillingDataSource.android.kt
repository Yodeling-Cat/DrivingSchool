package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.applicationContext

/**
 * Google Play, once the entry point has handed over an application.
 *
 * Without one - in a host test, or in a preview - this answers [UnavailableBillingDataSource], the same
 * refusal the ad data source makes: nothing can be asked of a store with no context.
 */
actual fun platformBillingDataSource(): BillingDataSource {
    val context = applicationContext() ?: return UnavailableBillingDataSource()
    return PlayBillingDataSource(context)
}
