package com.spiraclestudios.autoskola.data.premium

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which store products take the ads away: the one on sale, and nothing else. */
class RemoveAdsProductTest {

    @Test
    fun theProductOnSaleIsRemoveAllAds() {
        assertEquals("remove_all_ads", PRODUCT_REMOVE_ADS)
    }

    @Test
    fun aPurchaseOfTheProductOnSaleRemovesTheAds() {
        assertTrue(removesAds(listOf("remove_all_ads")))
    }

    @Test
    fun theOldAppsProductNoLongerCounts() {
        // Dropped on 2026-10-08: nobody still uses a purchase of it.
        assertFalse(removesAds(listOf("remove_ads")))
    }

    @Test
    fun anyOtherProductDoesNot() {
        assertFalse(removesAds(listOf("premium_unlock")))
        assertFalse(removesAds(emptyList()))
    }
}
