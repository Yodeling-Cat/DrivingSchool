package com.spiraclestudios.autoskola.bank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LicenseCategoryTest {

    @Test
    fun trailerGroupsAreShownWithPlus() {
        assertEquals("B", LicenseCategory.B.code)
        assertEquals("B+E", LicenseCategory.BE.code)
        assertEquals("D+E", LicenseCategory.DE.code)
    }

    @Test
    fun parsesStoredNames() {
        assertEquals(LicenseCategory.CE, LicenseCategory.fromName("CE"))
        assertNull(LicenseCategory.fromName("C+E"))
        assertNull(LicenseCategory.fromName(null))
    }
}
