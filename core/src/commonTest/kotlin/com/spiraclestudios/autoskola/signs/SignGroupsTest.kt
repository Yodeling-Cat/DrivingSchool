package com.spiraclestudios.autoskola.signs

import kotlin.test.Test
import kotlin.test.assertEquals

class SignGroupsTest {

    @Test
    fun signsUnderOneHeadingBecomeOneGroup() {
        val signs = listOf(
            sign("140", "Chodci", "VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"),
            sign("141", "Deti", "VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"),
        )

        val groups = groupSigns(signs)

        assertEquals(1, groups.size)
        assertEquals("VÝSTRAŽNÉ ZNAČKY", groups[0].kind)
        assertEquals("I. 4. Pohyb na ceste", groups[0].group)
        assertEquals(listOf("140", "141"), groups[0].signs.map { it.number })
    }

    @Test
    fun aNewHeadingOpensANewGroup() {
        val signs = listOf(
            sign("140", "Chodci", "VÝSTRAŽNÉ ZNAČKY", "I. 4. Pohyb na ceste"),
            sign("152", "Výstražný kríž", "VÝSTRAŽNÉ ZNAČKY", "II. Priecestné značky"),
        )

        val groups = groupSigns(signs)

        assertEquals(listOf("I. 4. Pohyb na ceste", "II. Priecestné značky"), groups.map { it.group })
        assertEquals(listOf(listOf("140"), listOf("152")), groups.map { g -> g.signs.map { it.number } })
    }

    @Test
    fun aNewKindOpensANewGroupEvenUnderTheSameHeading() {
        // "I. Značky o prednosti v jazde" is a heading of the informative signs, and the regulatory
        // signs have one that reads almost the same: the kind is what tells them apart.
        val signs = listOf(
            sign("201", "Daj prednosť v jazde!", "REGULAČNÉ ZNAČKY", "I. Značky o dávaní prednosti"),
            sign("301", "Križovatka s prednosťou", "INFORMAČNÉ ZNAČKY", "I. Značky o prednosti"),
        )

        assertEquals(2, groupSigns(signs).size)
    }

    @Test
    fun aHeadingThatComesBackKeepsTheCatalogOrder() {
        val signs = listOf(
            sign("601", "Pozdĺžna súvislá čiara", "VODOROVNÉ ZNAČKY", "I. Pozdĺžne čiary"),
            sign("604", "Pozdĺžna prerušovaná čiara", "VODOROVNÉ ZNAČKY", "II. Priečne čiary"),
            sign("603", "Dvojitá čiara", "VODOROVNÉ ZNAČKY", "I. Pozdĺžne čiary"),
        )

        val groups = groupSigns(signs)

        assertEquals(listOf("I. Pozdĺžne čiary", "II. Priečne čiary", "I. Pozdĺžne čiary"), groups.map { it.group })
        assertEquals(listOf(listOf("601"), listOf("604"), listOf("603")), groups.map { g -> g.signs.map { it.number } })
    }

    @Test
    fun aSignWithoutAHeadingStillHasAGroup() {
        // A kind whose signs carry no inner heading: the group is named by the kind itself.
        val groups = groupSigns(listOf(sign("355", "Kilometrovník", "INFORMAČNÉ ZNAČKY", "")))

        assertEquals("", groups[0].group)
        assertEquals(1, groups[0].signs.size)
    }

    @Test
    fun noSignsLeaveNoGroups() {
        assertEquals(emptyList(), groupSigns(emptyList()))
    }

    private fun sign(
        number: String,
        name: String,
        kind: String,
        group: String,
    ) = Sign(
        number = number,
        name = name,
        description = "Značka.",
        kind = kind,
        group = group,
        image = "obr3/dz/$number.jpg",
    )
}
