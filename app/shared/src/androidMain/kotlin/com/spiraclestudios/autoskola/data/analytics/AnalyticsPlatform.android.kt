package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.PostHogContext
import com.spiraclestudios.autoskola.applicationContext

/** PostHog's Android context wraps the `Application`, which the entry point installs. */
actual fun postHogContext(): PostHogContext? = applicationContext()?.let { PostHogContext(it) }
