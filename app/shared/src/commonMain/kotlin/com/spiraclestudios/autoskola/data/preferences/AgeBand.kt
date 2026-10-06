package com.spiraclestudios.autoskola.data.preferences

/**
 * Which age band the user says they are in, from lowest to highest.
 *
 * Three bands because the app has two different lines to hold, and a yes/no answer can only hold one:
 *
 * - **16** is the age at which a person can agree alone, in Slovakia, to crash reports and usage
 *   statistics. Below it only a parent or guardian can.
 * - **18** is the line for personalized ads. Profiling-based advertising to minors is restricted well
 *   beyond the age of consent, so a 16 or 17-year-old can agree to the first two and not to the third
 *   (`plans/age-gate.md`).
 *
 * The order is the order of the ages, which is what [rises] relies on.
 */
enum class AgeBand {
    Under16,
    From16To17,
    Adult,
    ;

    /** Whether answering [other] instead of this band is moving *up* — to more of the choices. */
    fun rises(other: AgeBand): Boolean = other.ordinal > ordinal

    companion object {
        /** The band stored under [name], or `null` for nothing stored or a name this version does not know. */
        fun fromName(name: String?): AgeBand? = entries.firstOrNull { it.name == name }
    }
}
