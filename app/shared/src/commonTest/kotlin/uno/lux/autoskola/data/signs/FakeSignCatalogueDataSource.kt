package uno.lux.autoskola.data.signs

import uno.lux.autoskola.signs.Sign
import uno.lux.autoskola.signs.SignCatalogue
import uno.lux.autoskola.signs.SignCatalogueInfo

/** Serves [catalogues] from memory and records what was read. */
class FakeSignCatalogueDataSource(
    private val index: List<SignCatalogueInfo>,
    private val catalogues: List<SignCatalogue>,
) : SignCatalogueDataSource {
    val catalogueReads = mutableListOf<String>()
    var indexReads = 0

    override suspend fun readIndex(): List<SignCatalogueInfo> = index.also { indexReads++ }

    override suspend fun readCatalogue(id: String, version: Int): SignCatalogue? {
        catalogueReads += "$id-v$version"
        return catalogues.firstOrNull { it.id == id && it.version == version }
    }

    override fun imageUri(imageDir: String, path: String): String = "test://$imageDir/$path"

    companion object {
        /** A data source with just [catalogue], listed as the latest version. */
        fun of(catalogue: SignCatalogue) = FakeSignCatalogueDataSource(
            index = listOf(testSignCatalogueInfo(catalogue)),
            catalogues = listOf(catalogue),
        )
    }
}

fun testSignCatalogueInfo(catalogue: SignCatalogue) = SignCatalogueInfo(
    id = catalogue.id,
    version = catalogue.version,
    country = catalogue.country,
    language = catalogue.language,
    signCount = catalogue.signs.size,
)

/** Three signs: two of them warning signs under one heading, the third a road marking. */
fun testSignCatalogue(id: String = "sk", version: Int = 1) = SignCatalogue(
    id = id,
    version = version,
    country = "SK",
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
