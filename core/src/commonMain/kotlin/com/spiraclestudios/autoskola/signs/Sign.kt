package com.spiraclestudios.autoskola.signs

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One sign of the road-sign catalog: what it is called, what it means, and the picture of it.
 *
 * The catalog is generated from the traffic-sign regulation by `tools/import`; see
 * docs/data-sources.md. [name] is empty for the few signs whose picture *is* their name (the
 * route-number plates), which is how the regulation's own table has them.
 */
@Serializable
data class Sign(
    /** The regulation's number of the sign, e.g. "140". */
    val number: String,
    val name: String,
    val description: String,
    /**
     * The part of the annex the sign is listed in: "VÝSTRAŽNÉ ZNAČKY", "REGULAČNÉ ZNAČKY",
     * "INFORMAČNÉ ZNAČKY", "DODATKOVÉ TABUĽKY", "VODOROVNÉ ZNAČKY" or "DOPRAVNÉ ZARIADENIA".
     */
    val kind: String,
    /** The heading inside that part, e.g. "I. 4. Pohyb na ceste", or empty where it has none. */
    val group: String,
    /** Path relative to the catalog's image directory. */
    val image: String,
)

/** One language's road-sign catalog, as written by `tools/import/import_znacky.py`. */
@Serializable
data class SignCatalog(
    val id: String,
    val version: Int,
    /** ISO 3166-1 alpha-2 code of the country whose catalog this is. */
    val country: String,
    /** ISO 639-1 code of the language the names and meanings are written in. */
    val language: String,
    /** Directory under `files/images/` holding the pictures the signs reference. */
    val imageDir: String,
    /** The regulation the catalog was read from. */
    val source: String,
    val signs: List<Sign>,
) {
    companion object {
        fun decode(json: String): SignCatalog = SignJson.decodeFromString(serializer(), json)
    }
}

/** Entry of `signs/index.json`: what a catalog contains without loading the whole catalog. */
@Serializable
data class SignCatalogInfo(
    val id: String,
    val version: Int,
    val country: String,
    val language: String,
    val signCount: Int,
) {
    companion object {
        fun decodeList(json: String): List<SignCatalogInfo> =
            SignJson.decodeFromString(ListSerializer(serializer()), json)
    }
}

private val SignJson = Json { ignoreUnknownKeys = true }
