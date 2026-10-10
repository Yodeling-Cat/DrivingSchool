package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.signs.Sign
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The sign catalog, read once and then kept in memory. */
class SignCatalogRepository(private val dataSource: SignCatalogDataSource) {
    private val mutex = Mutex()
    private var index: List<SignCatalogInfo>? = null
    private val catalogs = mutableMapOf<String, SignCatalog>()

    suspend fun index(): List<SignCatalogInfo> = mutex.withLock {
        index ?: dataSource.readIndex().also { index = it }
    }

    /** The latest version of the catalog [id], or `null` if there's no such catalog. */
    suspend fun latest(id: String): SignCatalog? {
        val info = index().firstOrNull { it.id == id } ?: return null
        return catalog(id, info.version)
    }

    /** Whether the app bundles a road-sign catalog. Asks the index alone, not the catalog. */
    suspend fun hasBundled(): Boolean = index().isNotEmpty()

    /**
     * The road-sign catalog the app bundles, or `null` when it has none. An app bundles its own
     * country's content and no other's (see AGENTS.md, "Countries"), so this is the catalog of the
     * country whose exam the driver takes.
     */
    suspend fun bundled(): SignCatalog? = index().firstOrNull()?.let { catalog(it.id, it.version) }

    /**
     * A specific version. As with the question banks, only catalogs that were read successfully
     * are remembered, so a hiccup while loading is tried again rather than leaving the catalog
     * "missing" for the rest of the process.
     */
    suspend fun catalog(id: String, version: Int): SignCatalog? = mutex.withLock {
        val key = "$id-v$version"
        catalogs[key] ?: dataSource.readCatalog(id, version)?.also { catalogs[key] = it }
    }

    /** URI of the picture of [sign], for the image loader. */
    fun imageUri(catalog: SignCatalog, sign: Sign): String = dataSource.imageUri(catalog.imageDir, sign.image)
}
