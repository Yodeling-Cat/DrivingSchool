package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.signs.Sign
import com.spiraclestudios.autoskola.signs.SignCatalogue
import com.spiraclestudios.autoskola.signs.SignCatalogueInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The sign catalogue, read once and then kept in memory. */
class SignCatalogueRepository(private val dataSource: SignCatalogueDataSource) {
    private val mutex = Mutex()
    private var index: List<SignCatalogueInfo>? = null
    private val catalogues = mutableMapOf<String, SignCatalogue>()

    suspend fun index(): List<SignCatalogueInfo> = mutex.withLock {
        index ?: dataSource.readIndex().also { index = it }
    }

    /** The latest version of the catalogue [id], or `null` if there's no such catalogue. */
    suspend fun latest(id: String): SignCatalogue? {
        val info = index().firstOrNull { it.id == id } ?: return null
        return catalogue(id, info.version)
    }

    /**
     * The catalogue the app offers today. Only one country's catalogue is bundled, so this is the
     * one listed in the index; when a second country arrives, this picks by the exam the user is
     * preparing for instead.
     */
    suspend fun bundled(): SignCatalogue? = index().firstOrNull()?.let { catalogue(it.id, it.version) }

    /**
     * A specific version. As with the question banks, only catalogues that were read successfully
     * are remembered, so a hiccup while loading is tried again rather than leaving the catalogue
     * "missing" for the rest of the process.
     */
    suspend fun catalogue(id: String, version: Int): SignCatalogue? = mutex.withLock {
        val key = "$id-v$version"
        catalogues[key] ?: dataSource.readCatalogue(id, version)?.also { catalogues[key] = it }
    }

    /** URI of the picture of [sign], for the image loader. */
    fun imageUri(catalogue: SignCatalogue, sign: Sign): String = dataSource.imageUri(catalogue.imageDir, sign.image)
}
