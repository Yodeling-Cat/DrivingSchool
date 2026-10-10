package com.spiraclestudios.autoskola

private val releaseApplicationId: String =
    (appBuildConfig("APPLICATION_ID") as? String)?.removeSuffix(".dev") ?: "com.spiraclestudios.autoskola"

/**
 * The Play listing, which is addressed by the released application id: each country's app has its
 * own. A debug build installs beside the released app under its own id (the release id with `.dev`
 * after it), and that one has no listing: the page named here is the released app's.
 */
actual val storeListing: StoreListing? = StoreListing(
    name = "Play Store",
    url = "https://play.google.com/store/apps/details?id=$releaseApplicationId",
)
