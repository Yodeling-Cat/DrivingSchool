package com.spiraclestudios.autoskola.data.review

/**
 * Never asked: the web has no store, so the rating card is not shown there. The listing is the
 * answer only because something has to be.
 */
actual fun platformStoreReviewDataSource(): StoreReviewDataSource = ListingStoreReviewDataSource()
