package uno.lux.autoskola.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AccentTextTest {
    @Test
    fun splitsALabelAroundItsNumber() {
        assertEquals(
            listOf("Test " to false, "3" to true),
            accentNumber("Test 3"),
        )
    }

    @Test
    fun keepsTheNumberFirstWhenTheLanguagePutsItThere() {
        assertEquals(
            listOf("3" to true, ". teszt" to false),
            accentNumber("3. teszt"),
        )
    }

    @Test
    fun takesEveryDigitOfTheNumber() {
        assertEquals(
            listOf("Test " to false, "12" to true),
            accentNumber("Test 12"),
        )
    }

    @Test
    fun accentsOnlyTheFirstNumberInALabel() {
        assertEquals(
            listOf("Test " to false, "12" to true, " of 50" to false),
            accentNumber("Test 12 of 50"),
        )
    }

    @Test
    fun leavesALabelWithoutDigitsAlone() {
        assertEquals(listOf("Mistakes" to false), accentNumber("Mistakes"))
    }

    @Test
    fun hasNothingToDrawForAnEmptyLabel() {
        assertEquals(emptyList(), accentNumber(""))
    }

    @Test
    fun keepsASpaceBeforeTheWordItAccents() {
        assertEquals(
            listOf("Which country's test are you" to false, " taking?" to true),
            accentWord("Which country's test are you", "taking?"),
        )
    }

    @Test
    fun joinsEveryRunOfATitleWithOneSpace() {
        assertEquals(
            listOf("Test" to false, " 3" to true, " results" to false),
            accentWord("Test", "3", "results"),
        )
    }

    @Test
    fun doesNotLeadWithASpaceWhenTheWordStartsTheTitle() {
        assertEquals(listOf("Start" to true), accentWord("", "Start"))
    }

    @Test
    fun marksTheLastWordOfTheWordmarkAsTheAccent() {
        assertEquals(
            listOf("Naj " to false, "Autoškola" to true),
            brandParts("Naj Autoškola"),
        )
    }

    @Test
    fun keepsTheWordmarkWholeWhenItHasOneWordOrNone() {
        assertEquals(listOf("Autoškola" to false), brandParts("Autoškola"))
        assertEquals(listOf("" to false), brandParts(""))
    }
}
