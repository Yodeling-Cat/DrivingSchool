package com.spiraclestudios.autoskola.data.signs

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignCatalogRepositoryTest {

    @Test
    fun theCatalogIsTheLatestVersionOfTheIndex() = runTest {
        val dataSource = FakeSignCatalogDataSource.of(testSignCatalog(version = 2))
        val repository = SignCatalogRepository(dataSource)

        assertEquals(2, repository.latest("sk")?.version)
    }

    @Test
    fun theCatalogIsReadOnceAndThenKept() = runTest {
        val dataSource = FakeSignCatalogDataSource.of(testSignCatalog())
        val repository = SignCatalogRepository(dataSource)

        repository.latest("sk")
        repository.latest("sk")

        assertEquals(1, dataSource.indexReads)
        assertEquals(listOf("sk-v1"), dataSource.catalogReads)
    }

    @Test
    fun aCatalogThatIsNotBundledIsMissing() = runTest {
        val repository = SignCatalogRepository(FakeSignCatalogDataSource.of(testSignCatalog()))

        assertNull(repository.latest("cz"))
    }

    @Test
    fun theBundledCatalogIsTheOneTheIndexLists() = runTest {
        val repository = SignCatalogRepository(FakeSignCatalogDataSource.of(testSignCatalog()))

        assertEquals("sk", repository.bundled()?.id)
    }

    @Test
    fun signPicturesComeFromTheImageDirectoryOfTheCatalog() = runTest {
        val repository = SignCatalogRepository(FakeSignCatalogDataSource.of(testSignCatalog()))
        val catalog = repository.latest("sk")!!

        assertEquals("test://minv-sk/obr3/dz/140.jpg", repository.imageUri(catalog, catalog.signs.first()))
    }
}
