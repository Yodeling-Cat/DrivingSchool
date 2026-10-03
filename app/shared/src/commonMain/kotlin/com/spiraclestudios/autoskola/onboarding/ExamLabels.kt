package com.spiraclestudios.autoskola.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.spiraclestudios.autoskola.bank.LicenseCategory
import com.spiraclestudios.autoskola.bank.LicenseCategory.A
import com.spiraclestudios.autoskola.bank.LicenseCategory.B
import com.spiraclestudios.autoskola.bank.LicenseCategory.BE
import com.spiraclestudios.autoskola.bank.LicenseCategory.C
import com.spiraclestudios.autoskola.bank.LicenseCategory.CE
import com.spiraclestudios.autoskola.bank.LicenseCategory.D
import com.spiraclestudios.autoskola.bank.LicenseCategory.DE
import com.spiraclestudios.autoskola.bank.LicenseCategory.T
import com.spiraclestudios.autoskola.i18n.AppLanguage

// Exam names are shown in the language of the question bank, so people can find the exam
// in their own language even before the app UI is localized.

private val categoryNames: Map<String, Map<LicenseCategory, String>> = mapOf(
    "sk" to mapOf(
        A to "Motorka",
        B to "Auto",
        BE to "Auto s prívesom",
        C to "Nákladiak",
        CE to "Nákladiak s prívesom",
        D to "Autobus",
        DE to "Autobus s prívesom",
        T to "Traktor",
    ),
    "hu" to mapOf(
        A to "Motorkerékpár",
        B to "Autó",
        BE to "Autó utánfutóval",
        C to "Tehergépkocsi",
        CE to "Tehergépkocsi pótkocsival",
        D to "Autóbusz",
        DE to "Autóbusz pótkocsival",
        T to "Traktor",
    ),
    "en" to mapOf(
        A to "Motorcycle",
        B to "Car",
        BE to "Car with trailer",
        C to "Truck",
        CE to "Truck with trailer",
        D to "Bus",
        DE to "Bus with trailer",
        T to "Tractor",
    ),
)

private val bankTitles = mapOf(
    "sk" to "Vodičský preukaz · Slovensky",
    "hu" to "Vezetői engedély · Magyarul",
    "en" to "Driver's license · English",
)

/** E.g. "Auto (B)" for Slovak, "Car with trailer (B+E)" for English. */
fun LicenseCategory.displayName(language: String): String {
    val name = (categoryNames[language] ?: categoryNames.getValue("en")).getValue(this)
    return "$name ($code)"
}

fun bankTitle(language: String): String = bankTitles[language] ?: language

internal val LicenseCategory.icon: ImageVector
    get() = when (this) {
        A -> Icons.Filled.TwoWheeler
        B, BE -> Icons.Filled.DirectionsCar
        C, CE -> Icons.Filled.LocalShipping
        D, DE -> Icons.Filled.DirectionsBus
        T -> Icons.Filled.Agriculture
    }

internal val LicenseCategory.tint: Color
    get() = when (this) {
        A -> Color(0xFF1E88E5)
        B, BE -> Color(0xFF43A047)
        C, CE -> Color(0xFFFB8C00)
        D, DE -> Color(0xFFE53935)
        T -> Color(0xFF6D4C41)
    }

/** Which exam, named in the language of its question bank (see [displayName] and [bankTitle]). */
data class ExamLabel(val category: LicenseCategory, val bankLanguage: String) {
    /** E.g. "Auto (B)". */
    val name: String get() = category.displayName(bankLanguage)

    /** E.g. "Vodičský preukaz · Slovensky". */
    val title: String get() = bankTitle(bankLanguage)

    /**
     * E.g. "Auto (B) · Slovenčina": the category and the language it is taken in, short enough for
     * a row or a subtitle. [title] is the bank's own name for itself and too long for either.
     */
    val shortTitle: String
        get() = "$name · ${AppLanguage.fromCode(bankLanguage)?.nativeName ?: bankLanguage}"
}
