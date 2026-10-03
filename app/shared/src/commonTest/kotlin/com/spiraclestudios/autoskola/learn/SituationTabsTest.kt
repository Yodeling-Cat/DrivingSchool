package com.spiraclestudios.autoskola.learn

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import com.spiraclestudios.autoskola.ui.ChipRadius
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The questions of a situation are drawn as one bar of tabs, not as separate pills. That bar is what
 * says which tab is one tab and where the next begins, so only the tabs at either end of the row
 * carry its own corners: the ones between them are square and sit flush against their neighbors.
 */
class SituationTabsTest {

    @Test
    fun theTabsAtTheEndsOfTheRowKeepTheBarsCorners() {
        assertEquals(
            RoundedCornerShape(topStart = ChipRadius, bottomStart = ChipRadius),
            tabShape(index = 0, count = 3),
        )
        assertEquals(
            RoundedCornerShape(topEnd = ChipRadius, bottomEnd = ChipRadius),
            tabShape(index = 2, count = 3),
        )
    }

    @Test
    fun aTabWithATabOnBothSidesIsSquare() {
        // A square tab joins the one before it and the one after it: the track between them is the
        // only line the driver needs, and a gap would read as another answer under the drawing.
        assertEquals(RectangleShape, tabShape(index = 1, count = 3))
    }

    @Test
    fun theOnlyTabOfASituationIsRoundedOnEveryCorner() {
        assertEquals(RoundedCornerShape(ChipRadius), tabShape(index = 0, count = 1))
    }
}
