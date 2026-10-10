package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.content.BundledContent
import com.spiraclestudios.autoskola.data.banks.imageUri
import com.spiraclestudios.autoskola.data.banks.readJson
import com.spiraclestudios.autoskola.data.banks.readOrNull
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo

/**
 * Reads the sign catalog that `tools/import` writes into the [content] of the country the app is
 * built for. A country without a catalog has no `signs/index.json`, which reads as no catalogs.
 */
class ResourceSignCatalogDataSource(private val content: BundledContent) : SignCatalogDataSource {

    override suspend fun readIndex(): List<SignCatalogInfo> =
        readOrNull { content.readJson("files/signs/index.json", SignCatalogInfo::decodeList) }.orEmpty()

    override suspend fun readCatalog(id: String, version: Int): SignCatalog? =
        readOrNull { content.readJson("files/signs/$id-v$version.json", SignCatalog::decode) }

    override fun imageUri(imageDir: String, path: String): String = content.imageUri(imageDir, path)
}
