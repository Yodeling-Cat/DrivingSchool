package com.spiraclestudios.autoskola.data.premium

/** No billing on iOS yet: StoreKit comes with the rest of `plans/premium.md`. Nothing was ever sold there. */
actual fun platformBillingDataSource(): BillingDataSource = UnavailableBillingDataSource()
