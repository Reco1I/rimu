package com.osudroid.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonGettersTest {

    private val json = LenientJson.parseToJsonElement(
        """{"s": "text", "n": 12, "ns": "34", "d": 1.5, "b": "TRUE", "o": {"k": 1}, "a": [{"x": 1}, "y"], "nul": null}"""
    ) as JsonObject

    @Test
    fun `Test strict getters coerce like org json`() {
        assertEquals("text", json.getString("s"))
        assertEquals("12", json.getString("n"))
        assertEquals(12, json.getInt("n"))
        assertEquals(34L, json.getLong("ns"))
        assertEquals(1.5, json.getDouble("d"), 0.0)
        assertEquals(1, json.getInt("d"))
        assertEquals(true, json.getBoolean("b"))
        assertEquals(1, json.getJsonObject("o").getInt("k"))
        assertEquals(1, json.getJsonArray("a").getJsonObject(0).getInt("x"))
        assertEquals("y", json["a"]!!.jsonArray.getString(1))
    }

    @Test
    fun `Test strict getters throw on missing or mismatched values`() {
        assertThrows(JsonException::class.java) { json.getString("missing") }
        assertThrows(JsonException::class.java) { json.getString("nul") }
        assertThrows(JsonException::class.java) { json.getInt("s") }
        assertThrows(JsonException::class.java) { json.getJsonObject("a") }
        assertThrows(JsonException::class.java) { json.getBoolean("n") }
    }
}
