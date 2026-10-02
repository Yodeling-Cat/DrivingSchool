package com.spiraclestudios.autoskola.signs

/**
 * The signs whose name, number or meaning contains [query], in the catalogue's own order.
 *
 * Diacritics and case are folded on both sides, so "zakaz" finds "Zákaz vjazdu" — a driver types
 * what is easiest on the keyboard. An empty query keeps every sign.
 */
fun searchSigns(signs: List<Sign>, query: String): List<Sign> {
    val needle = foldSignText(query)
    if (needle.isEmpty()) return signs
    return signs.filter { sign ->
        foldSignText(sign.name).contains(needle) ||
            foldSignText(sign.number).contains(needle) ||
            foldSignText(sign.description).contains(needle)
    }
}

/**
 * [text] with case, diacritics and punctuation taken out, so that two spellings of the same word
 * compare equal. Words are separated by a single space.
 */
fun foldSignText(text: String): String = buildString(text.length) {
    var afterSpace = true
    for (char in text) {
        val lower = char.lowercaseChar()
        val folded = DIACRITICS[lower] ?: lower
        if (folded.isLetterOrDigit()) {
            append(folded)
            afterSpace = false
        } else if (!afterSpace) {
            append(' ')
            afterSpace = true
        }
    }
    if (isNotEmpty() && last() == ' ') deleteAt(lastIndex)
}

/** The diacritics of Slovak, Czech and Hungarian, each paired with the letter it folds to. */
private val DIACRITICS: Map<Char, Char> = mapOf(
    'á' to 'a', 'ä' to 'a', 'č' to 'c', 'ď' to 'd', 'é' to 'e', 'ě' to 'e', 'í' to 'i',
    'ĺ' to 'l', 'ľ' to 'l', 'ň' to 'n', 'ó' to 'o', 'ô' to 'o', 'ö' to 'o', 'ő' to 'o',
    'ř' to 'r', 'š' to 's', 'ť' to 't', 'ú' to 'u', 'ů' to 'u', 'ü' to 'u', 'ű' to 'u',
    'ý' to 'y', 'ž' to 'z', 'ź' to 'z',
)
