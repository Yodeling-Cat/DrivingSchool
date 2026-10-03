package com.spiraclestudios.autoskola.signs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignSearchTest {

    private val signs = listOf(
        sign("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov na vozovke."),
        sign("142", "Priechod pre chodcov", "Značka upozorňuje na priechod pre chodcov."),
        sign("230", "Zákaz vjazdu", "Značka zakazuje vjazd všetkými vozidlami."),
        sign("601", "Pozdĺžna súvislá čiara", "Značka vyznačuje jazdný pruh alebo okraj vozovky."),
    )

    @Test
    fun anEmptyQueryKeepsEverySign() {
        assertEquals(signs, searchSigns(signs, ""))
    }

    @Test
    fun whitespaceOnlyCountsAsEmpty() {
        assertEquals(signs, searchSigns(signs, "   "))
    }

    @Test
    fun matchesTheSignsName() {
        assertEquals(listOf("140", "142"), searchSigns(signs, "chodc").map { it.number })
    }

    @Test
    fun matchesWithoutDiacritics() {
        assertEquals(listOf("230"), searchSigns(signs, "zakaz").map { it.number })
    }

    @Test
    fun matchesWithDiacritics() {
        assertEquals(listOf("230"), searchSigns(signs, "zákaz").map { it.number })
    }

    @Test
    fun matchesTheSignsNumber() {
        assertEquals(listOf("601"), searchSigns(signs, "601").map { it.number })
    }

    @Test
    fun matchesAWordOfTheMeaning() {
        assertEquals(listOf("140"), searchSigns(signs, "vozovke").map { it.number })
    }

    @Test
    fun matchesInTheOrderTheCatalogLists() {
        assertEquals(listOf("140", "601"), searchSigns(signs, "vozovk").map { it.number })
    }

    @Test
    fun aQueryThatMatchesNothingGivesNothing() {
        assertTrue(searchSigns(signs, "vrtuľník").isEmpty())
    }

    @Test
    fun findsANamelessSignByItsNumber() {
        val nameless = listOf(sign("350", "", "Značka informuje o čísle cesty I. triedy."))

        assertEquals(1, searchSigns(nameless, "350").size)
    }

    @Test
    fun aSearchDoesNotMatchTheHeadingASignIsListedUnder() {
        val grouped = listOf(sign("140", "Chodci", "Značka upozorňuje na zvýšený pohyb chodcov."))

        assertTrue(searchSigns(grouped, "VÝSTRAŽNÉ ZNAČKY").isEmpty())
    }

    @Test
    fun foldsEveryDiacriticWeTranslateInto() {
        // Slovak, Czech and Hungarian, which the app is written in.
        assertEquals("aacdeeiillnoooouuuursstzz", foldSignText("áäčďéěííĺľňóôöőúůüűřššťžź"))
    }

    @Test
    fun foldingLeavesPunctuationAndCaseOutOfTheWay() {
        assertEquals("zakaz vjazdu", foldSignText("Zákaz  vJAZDU!"))
    }

    private fun sign(number: String, name: String, description: String) = Sign(
        number = number,
        name = name,
        description = description,
        kind = "VÝSTRAŽNÉ ZNAČKY",
        group = "I. 1. Stavebno-technické vlastnosti cesty",
        image = "obr3/dz/$number.jpg",
    )
}
