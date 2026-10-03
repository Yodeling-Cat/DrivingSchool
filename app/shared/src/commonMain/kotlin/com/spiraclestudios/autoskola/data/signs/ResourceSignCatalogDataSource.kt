package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.data.banks.readOrNull
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import najautoskola.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Reads the sign catalog that `tools/import` writes into the app's Compose resources. */
@OptIn(ExperimentalResourceApi::class)
class ResourceSignCatalogDataSource : SignCatalogDataSource {

    override suspend fun readIndex(): List<SignCatalogInfo> =
        decode("files/signs/index.json", SignCatalogInfo::decodeList)

    override suspend fun readCatalog(id: String, version: Int): SignCatalog? =
        readOrNull { decode("files/signs/$id-v$version.json", SignCatalog::decode) }

    override fun imageUri(imageDir: String, path: String): String = Res.getUri("files/images/$imageDir/$path")

    private suspend fun <T> decode(path: String, parse: (String) -> T): T {
        val json = Res.readBytes(path).decodeToString()
        return withContext(Dispatchers.Default) { parse(json) }
    }
}
