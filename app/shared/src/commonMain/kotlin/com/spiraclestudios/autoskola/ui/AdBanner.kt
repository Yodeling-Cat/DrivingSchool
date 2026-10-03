package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spiraclestudios.autoskola.data.ads.AdSlot

/**
 * One banner placement, drawn where it is put.
 *
 * It draws nothing when there is no unit to draw - the web build, and any target without an SDK - so
 * a placement can be written without asking whether the target supports ads.
 *
 * It does **not** decide whether ads are allowed. That belongs to the screen's own state, which is
 * where the consent answer and the entitlement have already been read into a flag: render this only
 * where the state says ads are on. Putting the check inside here instead would mean every placement
 * silently ignoring the entitlement it cannot see.
 *
 * The size is the request's, not the unit's: the implementation asks for an anchored adaptive banner
 * as wide as the space the placement has been given, which is why a banner belongs in a slot rather
 * than beside something with its own width.
 */
@Composable
expect fun AdBanner(slot: AdSlot, modifier: Modifier = Modifier)
