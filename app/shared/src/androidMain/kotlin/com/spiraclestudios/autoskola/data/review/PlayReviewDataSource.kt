package com.spiraclestudios.autoskola.data.review

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import com.spiraclestudios.autoskola.StoreListing
import com.spiraclestudios.autoskola.currentActivity
import com.spiraclestudios.autoskola.openUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play's in-app review: a sheet over the app with the stars and a box for a review, so the
 * user rates without leaving the home screen.
 *
 * Play decides whether the sheet is drawn, and says nothing either way: a user who has used up
 * Play's quota of asks sees nothing, and so does one who already rated. When Play can't even be
 * asked - no activity in front to draw over, no Play Store on the phone - the user is sent to the
 * [StoreListing] instead, which is where the card used to send everyone.
 *
 * It cannot be tried from a debug build installed straight onto a phone: Play only knows builds that
 * came through it, so there the sheet is never drawn. It has to be tried from an internal test track
 * or with internal app sharing.
 */
class PlayReviewDataSource(context: Context) : StoreReviewDataSource {

    private val manager = ReviewManagerFactory.create(context)

    override suspend fun requestReview(listing: StoreListing) {
        val activity = currentActivity()
        val review = activity?.let { manager.requestReviewFlow().awaitOrNull() }
        if (activity == null || review == null) {
            openUrl(listing.url)
            return
        }
        manager.launchReviewFlow(activity, review).awaitOrNull()
    }
}

/** What the task ended with, or `null` when it failed: neither failure is worth telling the user about. */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task -> continuation.resume(if (task.isSuccessful) task.result else null) }
}
