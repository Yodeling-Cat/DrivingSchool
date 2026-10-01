package uno.lux.autoskola.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MiddleEllipsizedTest {

    /** Every character measures the same, so the rule has no font to lean on - and no font to blame. */
    private val widthOf: (String) -> Float = { it.length * 10f }

    @Test
    fun aLabelThatFitsIsLeftAsItIs() {
        assertEquals("Question 2", middleEllipsized("Question 2", maxWidth = 100f, widthOf = widthOf))
    }

    @Test
    fun whatDoesNotFitLosesItsMiddleAndKeepsBothEnds() {
        // Room for six characters of "Question 2": two from the front, three from the back, and the
        // ellipsis between them - so the number, with the space before it, is still there.
        assertEquals("Qu…n 2", middleEllipsized("Question 2", maxWidth = 60f, widthOf = widthOf))
    }

    @Test
    fun itKeepsShavingUntilWhatIsLeftFits() {
        assertEquals("Qu… 2", middleEllipsized("Question 2", maxWidth = 50f, widthOf = widthOf))
        assertEquals("Q… 2", middleEllipsized("Question 2", maxWidth = 40f, widthOf = widthOf))
    }

    @Test
    fun aLabelWithNoRoomForAnythingButTheEllipsisIsTheEllipsis() {
        assertEquals("…", middleEllipsized("Question 2", maxWidth = 10f, widthOf = widthOf))
        assertEquals("…", middleEllipsized("Question 2", maxWidth = 0f, widthOf = widthOf))
    }

    @Test
    fun aLabelShorterThanTheEllipsisItselfIsNotWorthShortening() {
        // One character with no room is the ellipsis; there is nothing else to keep.
        assertEquals("…", middleEllipsized("7", maxWidth = 5f, widthOf = widthOf))
    }
}
