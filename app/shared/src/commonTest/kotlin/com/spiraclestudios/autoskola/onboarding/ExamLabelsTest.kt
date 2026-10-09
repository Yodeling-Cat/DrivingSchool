package com.spiraclestudios.autoskola.onboarding

import com.spiraclestudios.autoskola.bank.LicenseCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ExamLabelsTest {

    @Test
    fun aCzechExamIsNamedInCzech() {
        val label = ExamLabel(LicenseCategory.CE, bankLanguage = "cs")

        assertEquals("Náklaďák s přívěsem (C+E)", label.name)
        assertEquals("Řidičský průkaz · Česky", label.title)
    }

    @Test
    fun everyGroupHasANameInTheLanguageOfEveryBank() {
        // A missing name falls back to English, which a driver reading Czech should never meet.
        for (language in listOf("sk", "hu", "cs")) {
            for (category in LicenseCategory.entries) {
                assertNotEquals(category.displayName("en"), category.displayName(language), "$language $category")
            }
        }
    }
}
