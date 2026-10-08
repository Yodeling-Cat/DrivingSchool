package com.spiraclestudios.autoskola.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Nothing on iOS yet, and nothing rather than a placeholder.
 *
 * The SDK is a Swift package linked from `app/iosApp`, which needs a Mac (`plans/ads.md` step 13).
 * Drawing a gap where a banner will go would be worse than drawing nothing: the space would be
 * reserved, the user would see it, and no ad would ever arrive to fill it.
 */
@Composable
internal actual fun PlatformAdBanner(state: AdBannerState, modifier: Modifier) = Unit
