package com.spiraclestudios.autoskola.screenstate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * What `rememberSaveable` holds - the page of a pager, how far a screen was scrolled, the tab that
 * was open - written as text, so it can be kept where a process that was killed cannot reach it
 * (see [StoredScreenState]).
 *
 * Written are the values Compose's savers make: numbers, strings, booleans, characters, lists, maps
 * and the pairs Navigation 3 tells the screens of one back stack apart by. A value that cannot be
 * written is **left out** rather than making the whole state fail to write: the map Navigation 3
 * keeps every screen's state in carries one entry - the platform's own saved state for that screen
 * - that is none of ours, and losing one screen its place is a much smaller thing than losing every
 * screen's, or than refusing to write any of it.
 *
 * A state that cannot be read is read as no state at all rather than as the wrong one: text written
 * by another shape of the app, or a value the app no longer uses, costs a position rather than
 * crashing the screen it belongs to.
 */
internal object ScreenStateText {

    /** The shape of the text [write] makes; a state written in another shape is not read. */
    private const val VERSION = 1

    private const val VERSION_KEY = "version"
    private const val STATE_KEY = "state"
    private const val TAG = "t"
    private const val VALUE = "v"
    private const val MAP_TAG = "map"
    private const val PAIR_TAG = "pair"
    private const val LONG_TAG = "long"
    private const val FLOAT_TAG = "float"
    private const val DOUBLE_TAG = "double"
    private const val CHAR_TAG = "char"

    /**
     * Whether [value] is a value a state registry is worth being asked about at all. A `null` is: a
     * saver that returns nothing has nothing to save. So is any map, since what a map holds that
     * cannot be written is left out of it.
     */
    fun holds(value: Any?): Boolean = when (value) {
        null, is Boolean, is String, is Int, is Long, is Float, is Double, is Char -> true

        is Pair<*, *> -> holds(value.first) && holds(value.second)

        // Half a list is not the list that was saved: a pager's page and its offset are one value.
        is List<*> -> value.all(::holds)

        is Map<*, *> -> true

        else -> false
    }

    /** [state] as text, with the values it cannot write left out. */
    fun write(state: Map<String, List<Any?>>): String {
        val entries = state.entries.mapNotNull { (key, values) -> entry(key, values) }
        return Json.encodeToString(
            JsonElement.serializer(),
            JsonObject(
                mapOf(
                    VERSION_KEY to JsonPrimitive(VERSION),
                    STATE_KEY to tagged(MAP_TAG, JsonArray(entries)),
                ),
            ),
        )
    }

    /** The state [text] holds, or an empty state when it is not text this wrote. */
    fun read(text: String): Map<String, List<Any?>> = runCatching { decodeState(text) }.getOrElse { emptyMap() }

    private fun decodeState(text: String): Map<String, List<Any?>> {
        val payload = Json.parseToJsonElement(text) as? JsonObject ?: error("not a saved state")
        check((payload[VERSION_KEY] as? JsonPrimitive)?.intOrNull == VERSION) { "not this version" }
        val state = payload[STATE_KEY] ?: error("a saved state without a state")
        val saved = decode(state) as? Map<*, *> ?: error("a saved state that is not a map")
        return saved.entries.associate { (key, values) ->
            val name = key as? String ?: error("a screen that is not named")
            name to (values as? List<Any?> ?: error("a screen without values"))
        }
    }

    private fun encode(value: Any?): JsonElement? = when (value) {
        null -> JsonNull

        is Boolean -> JsonPrimitive(value)

        is String -> JsonPrimitive(value)

        is Int -> JsonPrimitive(value)

        is Long -> tagged(LONG_TAG, JsonPrimitive(value))

        is Float -> tagged(FLOAT_TAG, JsonPrimitive(value))

        is Double -> tagged(DOUBLE_TAG, JsonPrimitive(value))

        is Char -> tagged(CHAR_TAG, JsonPrimitive(value.toString()))

        is Pair<*, *> -> {
            val first = encode(value.first) ?: return null
            val second = encode(value.second) ?: return null
            tagged(PAIR_TAG, JsonArray(listOf(first, second)))
        }

        is List<*> -> JsonArray(value.map { encode(it) ?: return null })

        is Map<*, *> -> tagged(MAP_TAG, JsonArray(value.entries.mapNotNull { (key, held) -> entry(key, held) }))

        else -> null
    }

    /** One key and what it holds, or `null` when that cannot be written and is left out. */
    private fun entry(key: Any?, held: Any?): JsonElement? {
        val encodedKey = encode(key) ?: return null
        val encodedValue = encode(held) ?: return null
        return JsonArray(listOf(encodedKey, encodedValue))
    }

    private fun tagged(tag: String, value: JsonElement): JsonElement =
        JsonObject(mapOf(TAG to JsonPrimitive(tag), VALUE to value))

    private fun decode(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> decodeValue(element)
        is JsonArray -> element.map(::decode)
        is JsonObject -> decodeTagged(element)
    }

    private fun decodeValue(primitive: JsonPrimitive): Any = when {
        primitive.isString -> primitive.content
        primitive.content == "true" -> true
        primitive.content == "false" -> false
        else -> primitive.intOrNull ?: primitive.doubleOrNull ?: error("a number it isn't")
    }

    private fun decodeTagged(element: JsonObject): Any? {
        val tag = (element[TAG] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: error("a value without a name")
        val value = element[VALUE] ?: error("no $tag value")
        return when (tag) {
            LONG_TAG -> value.primitive()?.longOrNull ?: error("a long it isn't")

            FLOAT_TAG -> value.primitive()?.floatOrNull ?: error("a float it isn't")

            DOUBLE_TAG -> value.primitive()?.doubleOrNull ?: error("a double it isn't")

            CHAR_TAG -> value.primitive()?.content?.singleOrNull() ?: error("a character it isn't")

            PAIR_TAG -> {
                val (first, second) = value.entry() ?: error("a pair it isn't")
                decode(first) to decode(second)
            }

            MAP_TAG -> (value as? JsonArray)?.associate { entry ->
                val (key, held) = entry.entry() ?: error("an entry that is not a pair")
                (decode(key) ?: error("an entry without a key")) to decode(held)
            } ?: error("a map it isn't")

            else -> error("a $tag, which is not ours")
        }
    }

    private fun JsonElement.primitive(): JsonPrimitive? = this as? JsonPrimitive

    /** A pair in the way this writes one: a key and what it holds, or the two values of a pair. */
    private fun JsonElement.entry(): Pair<JsonElement, JsonElement>? {
        val pair = this as? JsonArray ?: return null
        return if (pair.size == 2) pair[0] to pair[1] else null
    }
}
