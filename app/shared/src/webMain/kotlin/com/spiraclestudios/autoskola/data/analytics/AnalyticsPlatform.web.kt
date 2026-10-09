package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.PostHogContext

/** The browser needs nothing beyond the empty context PostHog provides. */
internal actual fun postHogContext(): PostHogContext? = PostHogContext()
