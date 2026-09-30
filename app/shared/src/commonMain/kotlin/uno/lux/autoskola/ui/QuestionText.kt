package uno.lux.autoskola.ui

private val KmPerHour = Regex("""km ?[.·] ?h-1(?!\d)""")

/**
 * Question text as it's shown. The ministry writes speeds as `km.h-1` or `km · h-1`; we show the usual `km/h`.
 * The bank keeps the original text, so this only changes the presentation.
 */
fun displayText(text: String): String = text.replace(KmPerHour, "km/h")
