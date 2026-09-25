package uno.lux.autoskola.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {

    @Test
    fun deviceLanguageIsUsedWhenSupported() {
        assertEquals(AppLanguage.Slovak, AppLanguage.forLocale("sk"))
        assertEquals(AppLanguage.Czech, AppLanguage.forLocale("cs"))
        assertEquals(AppLanguage.Hungarian, AppLanguage.forLocale("hu"))
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToEnglish() {
        assertEquals(AppLanguage.English, AppLanguage.forLocale("de"))
        assertEquals(AppLanguage.English, AppLanguage.forLocale(""))
    }

    @Test
    fun slovakAndCzechPointsUseThreePluralForms() {
        val sk = AppLanguage.Slovak.strings.points
        assertEquals(listOf("1 bod", "2 body", "4 body", "5 bodov", "0 bodov"), listOf(1, 2, 4, 5, 0).map(sk))
        val cs = AppLanguage.Czech.strings.points
        assertEquals(listOf("1 bod", "3 body", "5 bodů"), listOf(1, 3, 5).map(cs))
    }

    @Test
    fun englishAndHungarianPoints() {
        assertEquals(listOf("1 point", "2 points"), listOf(1, 2).map(AppLanguage.English.strings.points))
        assertEquals(listOf("1 pont", "4 pont"), listOf(1, 4).map(AppLanguage.Hungarian.strings.points))
    }

    @Test
    fun everyLanguageNamesTheCountriesWeSupport() {
        AppLanguage.entries.forEach { language ->
            listOf("SK", "CZ").forEach { code ->
                val name = language.strings.countryName(code)
                assert(name != code) { "${language.name} has no name for $code" }
            }
        }
    }

    @Test
    fun hungarianPutsTheNumberFirst() {
        assertEquals("3. teszt", AppLanguage.Hungarian.strings.testName(3))
        assertEquals("Test 3", AppLanguage.Slovak.strings.testName(3))
    }

    @Test
    fun appNameIsTranslated() {
        assertEquals("Naj Autoškola", AppLanguage.Slovak.strings.appName)
        assertEquals("Naj Autoškola", AppLanguage.English.strings.appName)
        assertEquals("Nej Autoškola", AppLanguage.Czech.strings.appName)
        assertEquals("Legjobb Autósiskola", AppLanguage.Hungarian.strings.appName)
    }

    @Test
    fun disclaimerUsesTheTranslatedAppName() {
        AppLanguage.entries.forEach { language ->
            val strings = language.strings
            assert(strings.appName in strings.disclaimerIndependent) { "${language.name} disclaimer" }
        }
    }
}
