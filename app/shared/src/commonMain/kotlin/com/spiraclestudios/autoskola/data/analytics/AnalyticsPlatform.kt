package com.spiraclestudios.autoskola.data.analytics

import com.posthog.kmp.PostHogContext

/**
 * The platform value PostHog needs to start, or `null` where there is none.
 *
 * Only Android has something to pass: PostHog's Android context wraps the `Application`, and its
 * no-argument factory throws. Everything else — iOS, JS, Wasm — is handed an empty context. A host
 * test or a Compose preview has no application at all, answers `null`, and analytics stays inert.
 */
internal expect fun postHogContext(): PostHogContext?
