package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.signs.Sign
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo

/**
 * Where road-sign catalogs come from. Today the catalog is bundled with the app, generated
 * from the traffic-sign regulation by `tools/import/import_znacky.py`.
 */
interface SignCatalogDataSource {
    /** The latest version of every catalog. */
    suspend fun readIndex(): List<SignCatalogInfo>

    /** One catalog version, or `null` if it doesn't exist. */
    suspend fun readCatalog(id: String, version: Int): SignCatalog?

    /** URI of a sign's picture, for the image loader (Coil). */
    fun imageUri(imageDir: String, path: String): String
}
