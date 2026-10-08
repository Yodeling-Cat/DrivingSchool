package com.spiraclestudios.autoskola.data.review

/** The App Store page, which is where the rating card has always sent the user on iOS. */
actual fun platformStoreReviewDataSource(): StoreReviewDataSource = ListingStoreReviewDataSource()
