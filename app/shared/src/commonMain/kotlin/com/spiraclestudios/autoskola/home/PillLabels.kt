package com.spiraclestudios.autoskola.home

import com.spiraclestudios.autoskola.i18n.Strings
import com.spiraclestudios.autoskola.ui.WidthClass

/**
 * The label the random pill wears. On a phone it shares the row with the "not passed" pill, which
 * has the longer label of the two, so it keeps just the adjective; from a tablet up the row has room
 * for the noun, and "Random test" says what the pill starts.
 */
fun randomPillLabel(widthClass: WidthClass, strings: Strings): String =
    if (widthClass == WidthClass.Compact) strings.randomTestShort else strings.randomTest
