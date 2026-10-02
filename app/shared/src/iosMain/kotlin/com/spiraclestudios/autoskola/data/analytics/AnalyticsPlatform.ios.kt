package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.PostHogContext

/** iOS needs nothing beyond the empty context PostHog provides. */
internal actual fun postHogContext(): PostHogContext? = PostHogContext()
