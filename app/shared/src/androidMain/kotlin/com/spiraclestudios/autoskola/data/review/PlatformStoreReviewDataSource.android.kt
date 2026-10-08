package com.spiraclestudios.autoskola.data.review

import com.spiraclestudios.autoskola.applicationContext

/**
 * Google Play's in-app review sheet, once the entry point has handed over an application.
 *
 * Without one - in a host test, or in a preview - this answers [ListingStoreReviewDataSource], which
 * opens the Play listing the way the card did before the sheet.
 */
actual fun platformStoreReviewDataSource(): StoreReviewDataSource {
    val context = applicationContext() ?: return ListingStoreReviewDataSource()
    return PlayReviewDataSource(context)
}
