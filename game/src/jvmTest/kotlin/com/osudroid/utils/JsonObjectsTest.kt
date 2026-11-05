package com.osudroid.utils

import com.osudroid.ui.skinning.mergeMissing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonObjectsTest {
    private fun json(text: String) = LenientJson.parseToJsonElement(text).jsonObject

    @Test
    fun `Test lenient parsing`() {
        val json = json(
            """
            {
                // A comment
                "a": 1,
                b: "two",
            }
            """
        )

        assertEquals(1, json.optInt("a"))
        assertEquals("two", json.optString("b"))
    }

    @Test
    fun `Test strings`() {
        val json = json("""{"text": "a", "number": 1.5, "null": null}""")

        assertEquals("a", json.optString("text"))
        assertEquals("1.5", json.optString("number"))
        // As org.json does.
        assertEquals("null", json.optString("null"))
        assertEquals("fallback", json.optString("missing", "fallback"))
    }

    @Test
    fun `Test numbers`() {
        val json = json("""{"int": 3, "double": 2.75, "numberString": "4.5", "text": "a", "boolean": true}""")

        assertEquals(3, json.optInt("int"))
        assertEquals(2, json.optInt("double"))
        assertEquals(4, json.optInt("numberString"))
        assertEquals(7, json.optInt("text", 7))
        assertEquals(2.75, json.optDouble("double"), 0.0)
        assertEquals(4.5, json.optDouble("numberString"), 0.0)
        assertTrue(json.optDouble("boolean").isNaN())
        assertTrue(json.optDouble("missing").isNaN())
    }

    @Test
    fun `Test booleans`() {
        val json = json("""{"true": true, "string": "TRUE", "false": "false", "number": 1}""")

        assertTrue(json.optBoolean("true"))
        assertTrue(json.optBoolean("string"))
        assertFalse(json.optBoolean("false", true))
        assertTrue(json.optBoolean("number", true))
        assertFalse(json.optBoolean("number"))
    }

    @Test
    fun `Test null and missing`() {
        val json = json("""{"null": null, "value": 1}""")

        assertTrue(json.isNull("null"))
        assertTrue(json.isNull("missing"))
        assertFalse(json.isNull("value"))
        assertNull(json.optJsonObject("value"))
    }

    @Test
    fun `Test merging missing keys`() {
        val target = json("""{"a": 1, "nested": {"b": 2}}""")
        val source = json("""{"a": 5, "c": 3, "nested": {"b": 9, "d": 4}}""")

        val merged = mergeMissing(target, source)

        assertEquals(1, merged.optInt("a"))
        assertEquals(3, merged.optInt("c"))
        val nested = merged.optJsonObject("nested") as JsonObject
        assertEquals(2, nested.optInt("b"))
        assertEquals(4, nested.optInt("d"))
    }
}
