package uno.lux.autoskola.signs

/** One group of a catalogue: the kind of sign and the heading inside it, with the signs under it. */
data class SignGroup(
    /** The part of the catalogue the signs are listed in, e.g. "VÝSTRAŽNÉ ZNAČKY". */
    val kind: String,
    /** The heading inside it, e.g. "I. 4. Pohyb na ceste". */
    val group: String,
    val signs: List<Sign>,
)

/**
 * The catalogue's signs gathered under the headings the regulation lists them under, in the order
 * they come in: a group is a run of neighbouring signs that share a kind and a heading, so a
 * narrowed list - a search's results - keeps the headings of the signs it still has.
 *
 * Two headings that share only their name are not merged: a heading is the run of signs that
 * carried it, which is what the regulation draws.
 */
fun groupSigns(signs: List<Sign>): List<SignGroup> {
    val groups = mutableListOf<SignGroup>()
    for (sign in signs) {
        val last = groups.lastOrNull()
        if (last != null && last.kind == sign.kind && last.group == sign.group) {
            groups[groups.lastIndex] = last.copy(signs = last.signs + sign)
        } else {
            groups += SignGroup(kind = sign.kind, group = sign.group, signs = listOf(sign))
        }
    }
    return groups
}
