package com.spiraclestudios.autoskola.data.signs

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignCatalogueRepositoryTest {

    @Test
    fun theCatalogueIsTheLatestVersionOfTheIndex() = runTest {
        val dataSource = FakeSignCatalogueDataSource.of(testSignCatalogue(version = 2))
        val repository = SignCatalogueRepository(dataSource)

        assertEquals(2, repository.latest("sk")?.version)
    }

    @Test
    fun theCatalogueIsReadOnceAndThenKept() = runTest {
        val dataSource = FakeSignCatalogueDataSource.of(testSignCatalogue())
        val repository = SignCatalogueRepository(dataSource)

        repository.latest("sk")
        repository.latest("sk")

        assertEquals(1, dataSource.indexReads)
        assertEquals(listOf("sk-v1"), dataSource.catalogueReads)
    }

    @Test
    fun aCatalogueThatIsNotBundledIsMissing() = runTest {
        val repository = SignCatalogueRepository(FakeSignCatalogueDataSource.of(testSignCatalogue()))

        assertNull(repository.latest("cz"))
    }

    @Test
    fun theBundledCatalogueIsTheOneTheIndexLists() = runTest {
        val repository = SignCatalogueRepository(FakeSignCatalogueDataSource.of(testSignCatalogue()))

        assertEquals("sk", repository.bundled()?.id)
    }

    @Test
    fun signPicturesComeFromTheImageDirectoryOfTheCatalogue() = runTest {
        val repository = SignCatalogueRepository(FakeSignCatalogueDataSource.of(testSignCatalogue()))
        val catalogue = repository.latest("sk")!!

        assertEquals("test://minv-sk/obr3/dz/140.jpg", repository.imageUri(catalogue, catalogue.signs.first()))
    }
}
