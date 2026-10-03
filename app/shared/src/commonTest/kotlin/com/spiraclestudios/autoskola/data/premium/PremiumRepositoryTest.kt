package com.spiraclestudios.autoskola.data.premium

import com.spiraclestudios.autoskola.premium.PremiumFeature
import com.spiraclestudios.autoskola.premium.PremiumStatus
import com.spiraclestudios.autoskola.premium.PurchaseSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The entitlement before there is a store to ask. What matters here is that the answer comes from
 * the status the repository was given and nowhere else, so wiring billing in later is a change to
 * where the status comes from and not to how the app asks about it.
 */
class PremiumRepositoryTest {

    @Test
    fun freeLocksEveryFeature() {
        val premium = PremiumRepository(PremiumStatus.Free)

        for (feature in PremiumFeature.entries) {
            assertFalse(premium.isUnlocked(feature), "$feature should be locked for Free")
        }
    }

    @Test
    fun premiumUnlocksEveryFeature() {
        val premium = PremiumRepository(PremiumStatus.Premium(PurchaseSource.Unknown))

        for (feature in PremiumFeature.entries) {
            assertTrue(premium.isUnlocked(feature), "$feature should be unlocked for Premium")
        }
    }

    /**
     * The dud the app actually ships, and the test that tells you [PREMIUM_UNLOCKED] is still flipped
     * on. That flag is a local, temporary change - flip it to walk the premium paths by hand - so it
     * failing is the reminder to put it back, not a broken test.
     */
    @Test
    fun theShippedDudIsFree() {
        assertEquals(PremiumStatus.Free, PremiumRepository().entitlement.value)
    }
}
