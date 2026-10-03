package com.spiraclestudios.autoskola.data.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The ad unit ids, and the two mistakes they would otherwise let through: a real unit in a debug
 * build, which serves a paid impression to a developer and is an AdMob policy problem, and a demo
 * unit left in a release build, which serves nothing and shows up only as no-fill in production.
 *
 * Banners are the reason this is per platform: AdMob registers an app per platform and an ad unit
 * belongs to its app, so the Android ids and the iOS ids are different units with different numbers.
 */
class AdUnitsTest {

    /** Google's demo units, which belong to no account and can be used by anyone. */
    private val demoPrefix = "ca-app-pub-3940256099942544/"

    /** The account the release units belong to. */
    private val accountPrefix = "ca-app-pub-7362127401681337/"

    @Test
    fun everySlotHasADemoUnit() {
        assertEquals(AdSlot.entries.toSet(), TestAdUnits.keys)
    }

    /**
     * A target that cannot serve ads has no release units at all, rather than blank ones - the web
     * build is the case, and an empty map is what lets [adUnitId] answer null instead of failing.
     * Where the map is not empty it has to be complete, or some placement would quietly resolve
     * nothing on that platform.
     */
    @Test
    fun theReleaseUnitsAreEitherCompleteOrAbsent() {
        assertTrue(
            ReleaseAdUnits.isEmpty() || ReleaseAdUnits.keys == AdSlot.entries.toSet(),
            "a partly filled release map is worse than an empty one: ${ReleaseAdUnits.keys}",
        )
    }

    @Test
    fun everyDemoUnitIsADemoUnit() {
        for ((slot, id) in TestAdUnits) {
            assertTrue(id.startsWith(demoPrefix), "$slot is not a demo unit: $id")
        }
    }

    @Test
    fun noIdIsBlank() {
        for ((slot, id) in TestAdUnits + ReleaseAdUnits) {
            assertTrue(id.isNotBlank(), "$slot is blank")
        }
    }

    @Test
    fun aDebugBuildResolvesDemoUnits() {
        for (slot in AdSlot.entries) {
            val id = adUnitId(slot, debug = true)
            assertNotNull(id, "$slot resolved nothing in a debug build")
            assertTrue(id.startsWith(demoPrefix), "$slot served $id in a debug build")
        }
    }

    @Test
    fun aReleaseBuildResolvesTheAccountsUnitsWhenThereAreAny() {
        for (slot in AdSlot.entries) {
            val id = adUnitId(slot, debug = false)
            if (id == null) {
                assertTrue(ReleaseAdUnits.isEmpty(), "$slot resolved nothing but the map has entries")
            } else {
                assertTrue(id.startsWith(accountPrefix), "$slot is not this account's unit: $id")
            }
        }
    }

    /** The web targets ask for no unit, and must get a null rather than an exception. */
    @Test
    fun aTargetWithNoAdsAnswersNullRatherThanFailing() {
        if (ReleaseAdUnits.isNotEmpty()) return
        for (slot in AdSlot.entries) assertNull(adUnitId(slot, debug = false), "$slot")
    }
}
