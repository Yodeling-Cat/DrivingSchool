package com.spiraclestudios.autoskola.data.ads

/**
 * The web build has no ad units, because AdMob does not serve browsers.
 *
 * Not a placeholder and not a stub that silently does nothing: an empty map is what makes
 * [adUnitId] answer null on this target, so a caller has nothing to pass to a request it should not
 * be making. When web ads are built they are AdSense, with a different unit type and a different
 * consent flow - see "The web targets" in `plans/ads.md` - and this file is where that starts.
 */
internal actual val ReleaseAdUnits: Map<String, Map<AdSlot, String>> = emptyMap()
