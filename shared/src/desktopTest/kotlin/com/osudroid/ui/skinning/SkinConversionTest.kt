package com.osudroid.ui.skinning

import java.io.File
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.nsu.ccfit.zuev.skins.OsuSkin

class SkinConversionTest {

    private fun versionOf(general: String): Float {
        val file = File.createTempFile("skin", ".ini")

        try {
            file.writeText("[General]\n$general\n")
            val json = IniReader(file.path).use { convertToJson(it) }
            return json["Utils"]!!.jsonObject["version"]!!.jsonPrimitive.float
        } finally {
            file.delete()
        }
    }

    @Test
    fun `A skin without a version is made for 1_0`() {
        assertEquals(1f, versionOf("Name: Test"))
    }

    @Test
    fun `The version is read as written`() {
        assertEquals(1f, versionOf("Version: 1"))
        assertEquals(2f, versionOf("Version: 2"))
        assertEquals(2.5f, versionOf("Version: 2.5"))
    }

    @Test
    fun `Latest is the latest version`() {
        assertEquals(OsuSkin.LATEST_VERSION, versionOf("Version: latest"))
    }
}
