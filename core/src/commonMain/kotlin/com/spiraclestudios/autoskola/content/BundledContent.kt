package com.spiraclestudios.autoskola.content

/**
 * One country's content as an app ships it: its question banks, its sign catalog, and their
 * pictures and videos, which `tools/import` writes into the Compose resources of that country's
 * module (`app/content/<country>`). Each country's app bundles its own and no other's, so it
 * installs no larger than it has to (see AGENTS.md, "Countries").
 *
 * Paths are relative to the module's Compose resources, as in `files/banks/index.json`.
 */
interface BundledContent {
    /** ISO 3166-1 alpha-2 code of the country whose content this is. */
    val country: String

    /** The bytes of the file at [path]. Fails when there is no such file. */
    suspend fun readBytes(path: String): ByteArray

    /** A URI the platform's image loader and video player can open the file at [path] from. */
    fun uri(path: String): String
}
