package uno.lux.autoskola.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashedCircleTest {
    @Test
    fun takesTheDesignsProportionsFromItsTwentyFourUnitBox() {
        val circle = dashedCircle(24f)

        assertEquals(9f, circle.radius, 1e-4f)
        assertEquals(2.2f, circle.strokeWidth, 1e-4f)
        assertEquals(3.5f, circle.dash, 1e-4f)
        assertEquals(3.6f, circle.gap, 1e-4f)
    }

    @Test
    fun scalesEveryMeasureWithTheIcon() {
        val small = dashedCircle(20f)
        val large = dashedCircle(40f)

        assertEquals(7.5f, small.radius, 1e-4f)
        assertEquals(small.radius * 2f, large.radius, 1e-4f)
        assertEquals(small.strokeWidth * 2f, large.strokeWidth, 1e-4f)
        assertEquals(small.dash * 2f, large.dash, 1e-4f)
    }

    @Test
    fun leavesMoreOfTheCircleDrawnThanOpen() {
        val circle = dashedCircle(24f)

        assertTrue(circle.strokeWidth < circle.dash, "a dash is longer than the line is thick")
    }
}
