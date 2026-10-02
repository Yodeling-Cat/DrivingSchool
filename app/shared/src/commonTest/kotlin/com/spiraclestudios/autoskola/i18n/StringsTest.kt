package com.spiraclestudios.autoskola.i18n

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
    fun pointsAreNamedAsWhatTheQuestionIsWorth() {
        assertEquals(
            listOf("Worth 1 point", "Worth 2 points"),
            listOf(1, 2).map(AppLanguage.English.strings.points),
        )
    }

    @Test
    fun slovakAndCzechPointsUseThreePluralForms() {
        val sk = AppLanguage.Slovak.strings.points
        assertEquals(
            listOf("za 1 bod", "za 2 body", "za 4 body", "za 5 bodov", "za 0 bodov"),
            listOf(1, 2, 4, 5, 0).map(sk),
        )
        val cs = AppLanguage.Czech.strings.points
        assertEquals(listOf("za 1 bod", "za 3 body", "za 5 bodů"), listOf(1, 3, 5).map(cs))
    }

    @Test
    fun hungarianPointsReadAsWhatTheAnswerIsWorth() {
        assertEquals(listOf("1 pontot ér", "4 pontot ér"), listOf(1, 4).map(AppLanguage.Hungarian.strings.points))
    }

    @Test
    fun everyLanguageNamesTheMinutesATestIsGiven() {
        AppLanguage.entries.forEach { language ->
            assert("30" in language.strings.overTime(30)) { "${language.name} over-time notice" }
        }
    }

    @Test
    fun everyLanguageReadsTheMinutesATestTook() {
        AppLanguage.entries.forEach { language ->
            assert("23" in language.strings.minutesShort(23)) { "${language.name} minutes" }
        }
    }

    @Test
    fun everyLanguageStampsATestAsPassedOrFailedInOneWord() {
        AppLanguage.entries.forEach { language ->
            val strings = language.strings
            listOf(strings.passedStamp, strings.failedStamp).forEach { word ->
                // The stamp is a fixed-size badge on the card: one word, no sentence in it.
                assert(' ' !in word && word.length <= 11) { "${language.name} stamp \"$word\"" }
            }
            assert(strings.passedStamp != strings.failedStamp) { "${language.name} stamps differ" }
        }
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
    fun everyLanguageCountsTheSigns() {
        assertEquals("1 sign", AppLanguage.English.strings.signsCount(1))
        assertEquals("120 signs", AppLanguage.English.strings.signsCount(120))
        assertEquals("120 tábla", AppLanguage.Hungarian.strings.signsCount(120))
    }

    @Test
    fun slovakAndCzechSignCountsUseThreePluralForms() {
        val sk = AppLanguage.Slovak.strings.signsCount
        assertEquals(listOf("1 značka", "2 značky", "5 značiek"), listOf(1, 2, 5).map(sk))
        val cs = AppLanguage.Czech.strings.signsCount
        assertEquals(listOf("1 značka", "3 značky", "120 značek"), listOf(1, 3, 120).map(cs))
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
