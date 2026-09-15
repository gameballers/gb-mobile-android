package com.gameball.gameball.inappmessaging.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Total accessors over Gson's tree model.
 *
 * The sync payload is walked by hand rather than bound reflectively: the parser's job is a
 * long list of asymmetric leniency rules, and reflective binding gives nulls and exceptions
 * where decisions are needed. Every accessor here returns null instead of throwing, so a
 * field of the wrong type behaves exactly like an absent one.
 */

internal fun JsonObject.child(name: String): JsonElement? =
    get(name)?.takeUnless { it.isJsonNull }

internal fun JsonObject.obj(name: String): JsonObject? =
    child(name)?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonObject.arr(name: String): JsonArray? =
    child(name)?.takeIf { it.isJsonArray }?.asJsonArray

/** Blank strings are normalised to null: an empty URL otherwise reaches the image loader. */
internal fun JsonObject.str(name: String): String? =
    child(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }

internal fun JsonObject.int(name: String): Int? = longOrNull(name)?.toInt()

internal fun JsonObject.long(name: String): Long? = longOrNull(name)

private fun JsonObject.longOrNull(name: String): Long? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isNumber -> primitive.asNumber.toLong()
        primitive.isString -> primitive.asString.trim().toLongOrNull()
        else -> null
    }
}

internal fun JsonObject.bool(name: String): Boolean? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isString -> primitive.asString.trim().lowercase().toBooleanStrictOrNull()
        else -> null
    }
}

internal fun JsonObject.double(name: String): Double? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isNumber -> primitive.asNumber.toDouble()
        primitive.isString -> primitive.asString.trim().toDoubleOrNull()
        else -> null
    }
}

/**
 * A Long, Double, Boolean or String — whatever the primitive actually is. Used for colours,
 * filter values and extras, where the wire type is not fixed.
 */
internal fun JsonElement?.scalar(): Any? {
    val primitive = this?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }
        ?.asJsonPrimitive ?: return null
    return primitive.toScalar()
}

internal fun JsonObject.scalar(name: String): Any? = child(name).scalar()

private fun JsonPrimitive.toScalar(): Any = when {
    isBoolean -> asBoolean
    isNumber -> {
        val text = asNumber.toString()
        text.toLongOrNull() ?: text.toDouble()
    }
    else -> asString
}
