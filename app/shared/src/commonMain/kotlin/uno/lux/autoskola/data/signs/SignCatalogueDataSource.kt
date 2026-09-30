package uno.lux.autoskola.data.signs

import uno.lux.autoskola.signs.Sign
import uno.lux.autoskola.signs.SignCatalogue
import uno.lux.autoskola.signs.SignCatalogueInfo

/**
 * Where road-sign catalogues come from. Today the catalogue is bundled with the app, generated
 * from the traffic-sign regulation by `tools/import/import_znacky.py`.
 */
interface SignCatalogueDataSource {
    /** The latest version of every catalogue. */
    suspend fun readIndex(): List<SignCatalogueInfo>

    /** One catalogue version, or `null` if it doesn't exist. */
    suspend fun readCatalogue(id: String, version: Int): SignCatalogue?

    /** URI of a sign's picture, for the image loader (Coil). */
    fun imageUri(imageDir: String, path: String): String
}
