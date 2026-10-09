package com.spiraclestudios.autoskola.data.signs

import com.spiraclestudios.autoskola.signs.Sign
import com.spiraclestudios.autoskola.signs.SignCatalog
import com.spiraclestudios.autoskola.signs.SignCatalogInfo

/** Serves [catalogs] from memory and records what was read. */
class FakeSignCatalogDataSource(private val index: List<SignCatalogInfo>, private val catalogs: List<SignCatalog>) :
    SignCatalogDataSource {
    val catalogReads = mutableListOf<String>()
    var indexReads = 0

    override suspend fun readIndex(): List<SignCatalogInfo> = index.also { indexReads++ }

    override suspend fun readCatalog(id: String, version: Int): SignCatalog? {
        catalogReads += "$id-v$version"
        return catalogs.firstOrNull { it.id == id && it.version == version }
    }

    override fun imageUri(imageDir: String, path: String): String = "test://$imageDir/$path"

    companion object {
        /** A data source with just [catalog], listed as the latest version. */
        fun of(catalog: SignCatalog) = FakeSignCatalogDataSource(
            index = listOf(testSignCatalogInfo(catalog)),
            catalogs = listOf(catalog),
        )
    }
}

fun testSignCatalogInfo(catalog: SignCatalog) = SignCatalogInfo(
    id = catalog.id,
    version = catalog.version,
    country = catalog.country,
    language = catalog.language,
    signCount = catalog.signs.size,
)

/** Three signs: two of them warning signs under one heading, the third a road marking. */
fun testSignCatalog(id: String = "sk", version: Int = 1, country: String = "SK") = SignCatalog(
    id = id,
    version = version,
    country = country,
    language = "sk",
    imageDir = "minv-sk",
    source = "Vyhláška MV SR č. 30/2020 Z. z., príloha č. 1",
    signs = listOf(
        testSign("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov na vozovke."),
        testSign("142", "Priechod pre chodcov", "Značka upozorňuje na priechod pre chodcov."),
        testSign(
            "601",
            "Pozdĺžna súvislá čiara",
            "Značka vyznačuje jazdný pruh alebo okraj vozovky.",
            kind = "VODOROVNÉ ZNAČKY",
            group = "I. Pozdĺžne čiary",
        ),
    ),
)

fun testSign(
    number: String,
    name: String,
    description: String,
    kind: String = "VÝSTRAŽNÉ ZNAČKY",
    group: String = "I. 4. Pohyb na ceste",
) = Sign(
    number = number,
    name = name,
    description = description,
    kind = kind,
    group = group,
    image = "obr3/dz/$number.jpg",
)
