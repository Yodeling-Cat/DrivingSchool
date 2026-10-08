package com.spiraclestudios.autoskola.data.review

import com.spiraclestudios.autoskola.StoreListing

/** A store that records each time it was asked for its rating dialog, and shows nothing. */
class FakeStoreReviewDataSource : StoreReviewDataSource {

    /** The listings the store was asked to rate, in order. */
    val requested = mutableListOf<StoreListing>()

    override suspend fun requestReview(listing: StoreListing) {
        requested += listing
    }
}
