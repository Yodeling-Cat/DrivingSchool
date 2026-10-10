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
     * Where a country has units they have to be complete, or some placement would quietly resolve
     * nothing in that country's app.
     */
    @Test
    fun aCountrysReleaseUnitsAreComplete() {
        for ((country, units) in ReleaseAdUnits) {
            assertEquals(AdSlot.entries.toSet(), units.keys, "a partly filled map for $country")
        }
    }

    @Test
    fun everyDemoUnitIsADemoUnit() {
        for ((slot, id) in TestAdUnits) {
            assertTrue(id.startsWith(demoPrefix), "$slot is not a demo unit: $id")
        }
    }

    @Test
    fun noIdIsBlank() {
        for ((slot, id) in TestAdUnits + ReleaseAdUnits.values.flatMap { it.entries.map { e -> e.toPair() } }) {
            assertTrue(id.isNotBlank(), "$slot is blank")
        }
    }

    @Test
    fun aDebugBuildResolvesDemoUnitsInEveryCountry() {
        for (country in listOf("SK", "CZ")) {
            for (slot in AdSlot.entries) {
                val id = adUnitId(slot, country, debug = true)
                assertNotNull(id, "$slot resolved nothing in a debug build")
                assertTrue(id.startsWith(demoPrefix), "$slot served $id in a debug build")
            }
        }
    }

    @Test
    fun aReleaseBuildResolvesItsCountrysUnits() {
        for ((country, units) in ReleaseAdUnits) {
            for (slot in AdSlot.entries) {
                val id = adUnitId(slot, country, debug = false)
                assertEquals(units[slot], id)
                assertTrue(id!!.startsWith(accountPrefix), "$slot is not this account's unit: $id")
            }
        }
    }

    /**
     * A unit serves only the app it was made under, so the app of a country without units of its
     * own - the Czech one, so far - and a target with no ads at all get none, never another's.
     */
    @Test
    fun aCountryWithoutUnitsOfItsOwnResolvesNone() {
        for (slot in AdSlot.entries) assertNull(adUnitId(slot, "CZ", debug = false), "$slot")
    }
}
