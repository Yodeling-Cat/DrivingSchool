package com.spiraclestudios.autoskola.data.ads

/**
 * The consent form for the target the app is running on, in the same shape as [platformAdDataSource]
 * and for the same reason: the difference is a build target, not a runtime choice.
 *
 * Android has Google's form; iOS and the web have none yet and answer [NoAdConsent], which asks for
 * nothing. The iOS form arrives with the iOS SDK (`plans/ads.md` step 13), and a browser has a
 * different mechanism altogether (step 15).
 */
expect fun platformAdConsent(): AdConsent
