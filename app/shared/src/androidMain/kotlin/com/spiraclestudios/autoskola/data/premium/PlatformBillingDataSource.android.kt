package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.applicationContext
import com.spiraclestudios.autoskola.isDebugBuild

/**
 * Google Play, once the entry point has handed over an application.
 *
 * Without one - in a host test, or in a preview - this answers [UnavailableBillingDataSource], the same
 * refusal the ad data source makes: nothing can be asked of a store with no context.
 *
 * A debug build (the `.dev` one) gets [DebugBillingDataSource] instead: Play sells nothing to a build
 * that did not come through it, so the real store would never show the "remove ads" row there.
 */
actual fun platformBillingDataSource(): BillingDataSource {
    if (isDebugBuild()) return DebugBillingDataSource()
    val context = applicationContext() ?: return UnavailableBillingDataSource()
    return PlayBillingDataSource(context)
}
