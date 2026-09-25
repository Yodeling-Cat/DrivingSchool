package uno.lux.autoskola.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveTest {

    @Test
    fun widthClassesFollowMaterialBreakpoints() {
        assertEquals(WidthClass.Compact, WidthClass.of(599.dp))
        assertEquals(WidthClass.Medium, WidthClass.of(600.dp))
        assertEquals(WidthClass.Medium, WidthClass.of(839.dp))
        assertEquals(WidthClass.Expanded, WidthClass.of(840.dp))
    }

    @Test
    fun phoneInLandscapeHasCompactHeight() {
        assertEquals(HeightClass.Compact, HeightClass.of(390.dp))
        assertEquals(HeightClass.Medium, HeightClass.of(480.dp))
        assertEquals(HeightClass.Expanded, HeightClass.of(900.dp))
    }
}
