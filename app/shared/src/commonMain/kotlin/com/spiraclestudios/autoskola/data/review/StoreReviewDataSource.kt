package com.spiraclestudios.autoskola.data.review

import com.spiraclestudios.autoskola.StoreListing
import com.spiraclestudios.autoskola.openUrl

/**
 * The store's own way of rating the app.
 *
 * Google Play has one that opens over the app: a sheet with the stars and a box for a review, so
 * the user rates without leaving the screen they are on. A store without one is sent to its
 * listing instead ([ListingStoreReviewDataSource]).
 *
 * One implementation per platform behind [platformStoreReviewDataSource].
 */
interface StoreReviewDataSource {

    /**
     * Asks the store to let the user rate the app, in [listing] if it has no better way. Suspends
     * until the store is done, which says nothing about whether the user rated: no store tells.
     */
    suspend fun requestReview(listing: StoreListing)
}

/** Opens the store's page for the app, where the user can rate it among everything else there. */
class ListingStoreReviewDataSource : StoreReviewDataSource {
    override suspend fun requestReview(listing: StoreListing) = openUrl(listing.url)
}

/**
 * The store's rating for the target the app is running on, in the same shape as
 * `platformBillingDataSource` and for the same reason: the difference is a build target, not a
 * runtime choice.
 *
 * Android asks Google Play for its in-app review sheet. iOS opens the App Store page, and the web
 * is never asked, because it has no store to rate the app in.
 */
expect fun platformStoreReviewDataSource(): StoreReviewDataSource
