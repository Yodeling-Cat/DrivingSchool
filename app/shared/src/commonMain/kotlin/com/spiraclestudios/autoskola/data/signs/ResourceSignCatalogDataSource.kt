package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.data.banks.bundledImageUri
import com.spiraclestudios.autoskola.data.banks.readOrNull
import com.spiraclestudios.autoskola.data.banks.readResource
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo

/** Reads the sign catalog that `tools/import` writes into the app's Compose resources. */
class ResourceSignCatalogDataSource : SignCatalogDataSource {

    override suspend fun readIndex(): List<SignCatalogInfo> =
        readResource("files/signs/index.json", SignCatalogInfo::decodeList)

    override suspend fun readCatalog(id: String, version: Int): SignCatalog? =
        readOrNull { readResource("files/signs/$id-v$version.json", SignCatalog::decode) }

    override fun imageUri(imageDir: String, path: String): String = bundledImageUri(imageDir, path)
}
