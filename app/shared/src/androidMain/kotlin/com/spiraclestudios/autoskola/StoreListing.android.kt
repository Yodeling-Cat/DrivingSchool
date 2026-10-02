package com.spiraclestudios.autoskola

/**
 * The Play listing, which is addressed by the released application id. A debug build installs
 * beside the released app under its own id, and that one has no listing: the page named here is
 * the released app's, whichever build asks for it.
 */
actual val storeListing: StoreListing? = StoreListing(
    name = "Play Store",
    url = "https://play.google.com/store/apps/details?id=com.spiraclestudios.autoskola",
)
