package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.data.banks.readOrNull
import com.spiraclestudios.autoskola.signs.SignCatalogue
import com.spiraclestudios.autoskola.signs.SignCatalogueInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import najautoskola.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Reads the sign catalogue that `tools/import` writes into the app's Compose resources. */
@OptIn(ExperimentalResourceApi::class)
class ResourceSignCatalogueDataSource : SignCatalogueDataSource {

    override suspend fun readIndex(): List<SignCatalogueInfo> =
        decode("files/signs/index.json", SignCatalogueInfo::decodeList)

    override suspend fun readCatalogue(id: String, version: Int): SignCatalogue? =
        readOrNull { decode("files/signs/$id-v$version.json", SignCatalogue::decode) }

    override fun imageUri(imageDir: String, path: String): String = Res.getUri("files/images/$imageDir/$path")

    private suspend fun <T> decode(path: String, parse: (String) -> T): T {
        val json = Res.readBytes(path).decodeToString()
        return withContext(Dispatchers.Default) { parse(json) }
    }
}
