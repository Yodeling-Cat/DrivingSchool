package com.spiraclestudios.autoskola.premium

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PremiumRulesTest {

    /**
     * The split, one row per feature, written out rather than computed: a feature that becomes free
     * has to be edited here, which is the point. `plans/premium.md` calls for exactly this, so that
     * changing what Premium includes changes a test instead of sliding through unnoticed.
     */
    private val unlockedForFree = mapOf(
        PremiumFeature.NoAds to false,
        PremiumFeature.AllLicenseGroups to false,
        PremiumFeature.AllCountries to false,
        PremiumFeature.LearnMaterial to false,
        PremiumFeature.MistakeStatistics to false,
    )

    /** A new feature is a decision about the free tier, not something to leave undecided. */
    @Test
    fun everyFeatureHasItsFreeTierDecided() {
        assertEquals(
            PremiumFeature.entries.toSet(),
            unlockedForFree.keys,
            "every feature needs a row above; a new one has to be decided, not defaulted",
        )
    }

    @Test
    fun freeUnlocksExactlyWhatTheTableSays() {
        for (feature in PremiumFeature.entries) {
            assertEquals(
                unlockedForFree.getValue(feature),
                PremiumRules.isUnlocked(feature, PremiumStatus.Free),
                "$feature for Free",
            )
        }
    }

    @Test
    fun premiumUnlocksEverything() {
        val premium = PremiumStatus.Premium(PurchaseSource.Play)

        for (feature in PremiumFeature.entries) {
            assertTrue(PremiumRules.isUnlocked(feature, premium), "$feature for Premium")
        }
    }

    /**
     * The "remove ads" product: ads go and nothing else changes. Written as a table so that a
     * feature added later has to be decided for these buyers too, rather than inheriting Premium.
     */
    @Test
    fun theRemoveAdsProductUnlocksNothingButNoAds() {
        val adsRemoved = PremiumStatus.AdsRemoved(PurchaseSource.Play)

        for (feature in PremiumFeature.entries) {
            assertEquals(
                feature == PremiumFeature.NoAds,
                PremiumRules.isUnlocked(feature, adsRemoved),
                "$feature for AdsRemoved",
            )
        }
    }

    /** Which store granted it does not change what is unlocked. */
    @Test
    fun premiumUnlocksEverythingWhicheverStoreItCameFrom() {
        val sources = listOf(PurchaseSource.Play, PurchaseSource.AppStore, PurchaseSource.Unknown)

        for (source in sources) {
            for (feature in PremiumFeature.entries) {
                assertTrue(
                    PremiumRules.isUnlocked(feature, PremiumStatus.Premium(source)),
                    "$feature for Premium from $source",
                )
            }
        }
    }
}
