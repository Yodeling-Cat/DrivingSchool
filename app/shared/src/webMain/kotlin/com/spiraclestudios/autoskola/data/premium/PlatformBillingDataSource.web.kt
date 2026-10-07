package com.spiraclestudios.autoskola.data.premium

/** No store in a browser, and nothing sold there: the phone stores own the entitlement. */
actual fun platformBillingDataSource(): BillingDataSource = UnavailableBillingDataSource()
