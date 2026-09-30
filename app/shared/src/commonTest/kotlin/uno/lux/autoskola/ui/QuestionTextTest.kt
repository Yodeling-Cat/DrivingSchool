package uno.lux.autoskola.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionTextTest {

    @Test
    fun speedInTheMinistrysNotationReadsAsKmPerHour() {
        assertEquals("najviac 50 km/h.", displayText("najviac 50 km.h-1."))
        assertEquals("90 km/h, na diaľnici", displayText("90 km.h-1, na diaľnici"))
    }

    @Test
    fun theSpacedDotVariantReadsAsKmPerHourToo() {
        assertEquals("ako 25 km/h.", displayText("ako 25 km · h-1."))
    }

    @Test
    fun everyOccurrenceIsChanged() {
        assertEquals("30 km/h a 50 km/h", displayText("30 km.h-1 a 50 km.h-1"))
    }

    @Test
    fun textWithoutTheNotationIsLeftAlone() {
        assertEquals("najviac 40 km/h.", displayText("najviac 40 km/h."))
        assertEquals("Vodič má 1 h-2 pauzu", displayText("Vodič má 1 h-2 pauzu"))
    }

    @Test
    fun aLongerNumberAfterTheExponentIsNotTouched() {
        assertEquals("km.h-10", displayText("km.h-10"))
    }
}
