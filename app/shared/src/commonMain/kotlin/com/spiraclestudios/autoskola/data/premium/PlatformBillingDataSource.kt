package com.spiraclestudios.autoskola.data.premium

/**
 * The store for the target the app is running on, in the same shape as `platformAdDataSource` and
 * for the same reason: the difference is a build target, not a runtime choice.
 *
 * Android asks Google Play. iOS has no billing yet and the web has no store, so both answer
 * [UnavailableBillingDataSource], which never grants or takes anything away.
 */
expect fun platformBillingDataSource(): BillingDataSource
