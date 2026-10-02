package com.spiraclestudios.autoskola.bank

import kotlinx.serialization.Serializable

/** Driver's license group a question bank's questions are assigned to. */
@Serializable
enum class LicenseCategory {
    A,
    B,
    BE,
    C,
    CE,
    D,
    DE,
    T,
    ;

    /** Display code, e.g. "B+E". */
    val code: String get() = if (name.length == 2) "${name[0]}+${name[1]}" else name

    companion object {
        val Default = B

        fun fromName(name: String?): LicenseCategory? = entries.firstOrNull { it.name == name }
    }
}
