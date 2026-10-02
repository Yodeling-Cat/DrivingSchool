package com.spiraclestudios.autoskola.home

import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.AppLanguage
import com.spiraclestudios.autoskola.ui.WidthClass
import kotlin.test.Test
import kotlin.test.assertEquals

class PillLabelsTest {

    private val english = AppLanguage.English.strings

    @Test
    fun aPhoneDropsTheNounFromTheRandomPill() {
        assertEquals("Random", randomPillLabel(WidthClass.of(400.dp), english))
        assertEquals("Náhodný", randomPillLabel(WidthClass.Compact, AppLanguage.Slovak.strings))
    }

    @Test
    fun aTabletAndUpKeepIt() {
        // 600dp is where Material stops calling the window a phone.
        assertEquals("Random test", randomPillLabel(WidthClass.of(600.dp), english))
        assertEquals("Random test", randomPillLabel(WidthClass.of(700.dp), english))
        assertEquals("Random test", randomPillLabel(WidthClass.of(1100.dp), english))
        assertEquals("Véletlen teszt", randomPillLabel(WidthClass.of(700.dp), AppLanguage.Hungarian.strings))
    }
}
