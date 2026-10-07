package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumStatus
import kotlinx.coroutines.delay

/**
 * A store a test can script: what it answers the next time it is asked, how long it takes, and a
 * record of how often it was asked.
 *
 * It starts by answering "nothing bought", which is what a user who has never paid gets from the real
 * one. A test about a store that cannot be reached says [PurchaseQuery.Unavailable].
 */
class FakeBillingDataSource(
    var answer: PurchaseQuery = PurchaseQuery.Answered(PremiumStatus.Free),
    /** How long the store takes to answer, in the test's virtual time. */
    var delayMillis: Long = 0,
) : BillingDataSource {

    /** How many times the store was asked. */
    var queries = 0
        private set

    override suspend fun query(): PurchaseQuery {
        queries++
        if (delayMillis > 0) delay(delayMillis)
        return answer
    }
}
