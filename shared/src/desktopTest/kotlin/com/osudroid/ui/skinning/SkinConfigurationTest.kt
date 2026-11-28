package com.osudroid.ui.skinning

import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.hud.HUDSkinData
import com.osudroid.utils.LenientJson
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SkinConfigurationTest {

    private lateinit var folder: File

    @Before
    fun setUp() {
        folder = Files.createTempDirectory("skin").toFile()
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
    }

    private fun json(text: String) = LenientJson.parseToJsonElement(text).jsonObject

    private fun load(json: String? = null, ini: String? = null): SkinConfiguration {
        json?.let { File(folder, "skin.json").writeText(it) }
        ini?.let { File(folder, "skin.ini").writeText(it) }
        return SkinConfigurationLoader.load(folder.toOkioPath())
    }

    @Test
    fun `An osu!droid skin_json is read, the keys rimu! doesn't read ignored`() {
        val configuration = decodeSkinConfiguration(
            json(
                """
                {
                    "ComboColor": { "forceOverride": true, "colors": ["#FF0000", "#00FF00"] },
                    "Slider": { "sliderBodyWidth": 50, "sliderHintEnable": true, "sliderBorderColor": "#FFFFFF" },
                    "Utils": { "disableKiai": true, "comboTextScale": 0.5, "version": 2 },
                    "Cursor": { "rotateCursor": false },
                    "Fonts": { "scorePrefix": "numbers\\score" },
                    "Layout": { "useNewLayout": true, "BackButton": { "scaleWhenHold": false } },
                    "Color": { "MenuItemDefaultColor": "#EB4999" },
                    "Theme": { "accentColor": "#123456" },
                }
                """.trimIndent()
            )
        )

        assertTrue(configuration.comboColors.forceOverride)
        assertEquals(listOf(Color4("#FF0000"), Color4("#00FF00")), configuration.comboColors.resolved)
        assertEquals(50f, configuration.slider.bodyWidth)
        assertEquals(Color4("#FFFFFF"), configuration.slider.borderColor)
        assertTrue(configuration.utils.disableKiai)
        assertEquals(0.5f, configuration.utils.comboTextScale)
        assertEquals(2f, configuration.utils.version)
        assertFalse(configuration.cursor.rotateCursor)
        assertTrue(configuration.cursor.rotateCursorTrail)
        assertEquals("numbers\\score", configuration.fonts.scorePrefix)
        assertEquals(Color4("#123456"), configuration.theme.accentColor)
        assertEquals(HUDSkinData.Default, configuration.hud)
    }

    @Test
    fun `Values are coerced as org_json did`() {
        val configuration = decodeSkinConfiguration(
            json(
                """
                {
                    "Slider": { "sliderBodyWidth": "40", "sliderFollowComboColor": "FALSE", "sliderBodyColor": "not a colour" },
                    "Utils": { "disableKiai": 1, "comboTextScale": true, "spinnerBackgroundColor": "#12" },
                    "Cursor": "not a section",
                    "ComboColor": { "colors": ["#FF0000", "nope", 3] },
                }
                """.trimIndent()
            )
        )

        assertEquals(40f, configuration.slider.bodyWidth)
        assertFalse(configuration.slider.followComboColor)
        assertNull(configuration.slider.bodyColor)
        assertFalse(configuration.utils.disableKiai)
        assertEquals(1f, configuration.utils.comboTextScale)
        assertEquals(UtilsConfiguration().spinnerBackgroundColor, configuration.utils.spinnerBackgroundColor)
        assertEquals(CursorConfiguration(), configuration.cursor)
        assertEquals(listOf(Color4("#FF0000")), configuration.comboColors.colors)
    }

    @Test
    fun `A combo colour list without valid colours has the default ones`() {
        val configuration = decodeSkinConfiguration(json("""{ "ComboColor": { "colors": ["nope"] } }"""))
        assertEquals(ComboColors.DEFAULT, configuration.comboColors.resolved)
    }

    @Test
    fun `A skin without configuration has the default one`() {
        assertEquals(SkinConfiguration.Default, load())
    }

    @Test
    fun `A skin_json key wins over the skin_ini, and what it lacks comes from it`() {
        val configuration = load(
            json = """{ "Cursor": { "rotateCursor": true }, "Fonts": { "scorePrefix": "json" } }""",
            ini = "[General]\nCursorRotate: 0\nCursorExpand: 0\n[Fonts]\nScorePrefix: ini\nComboPrefix: ini\n[Colours]\nCombo1: 1,2,3",
        )

        assertTrue(configuration.cursor.rotateCursor)
        assertFalse(configuration.cursor.expandCursor)
        assertEquals("json", configuration.fonts.scorePrefix)
        assertEquals("ini", configuration.fonts.comboPrefix)
        assertEquals(listOf(Color4(1, 2, 3)), configuration.comboColors.colors)
        // What the skin.ini doesn't say is osu!stable's, not osu!droid's.
        assertEquals(1f, configuration.utils.version)
        assertEquals(0.8f, configuration.utils.comboTextScale)
    }

    @Test
    fun `A skin_json that can't be read falls back to the skin_ini`() {
        val configuration = load(json = "{ this isn't json", ini = "[General]\nCursorRotate: 0")
        assertFalse(configuration.cursor.rotateCursor)

        File(folder, "skin.json").writeText("""{ "HUD": [{ "x": 1 }] }""")
        assertFalse(SkinConfigurationLoader.load(folder.toOkioPath()).cursor.rotateCursor)
    }

    @Test
    fun `The HUD layout is read and written`() {
        val hud = HUDSkinData(
            listOf(
                HUDElementSkinData("score_counter", Vec2(-10f, 4f), Anchor.TopRight, Anchor.TopRight, 0.96f),
                HUDElementSkinData("unknown_element"),
            )
        )

        val encoded = SkinJson.encodeToJsonElement(HUDSkinData.serializer(), hud)
        assertEquals(
            json("""{ "HUD": [
                { "type": "score_counter", "x": -10.0, "y": 4.0, "anchor": "TopRight", "origin": "TopRight", "scale": 0.96 },
                { "type": "unknown_element", "x": 0.0, "y": 0.0, "anchor": "Center", "origin": "Center", "scale": 1.0 }
            ] }""")["HUD"],
            encoded,
        )

        val configuration = decodeSkinConfiguration(JsonObject(mapOf("HUD" to encoded)))
        assertEquals(hud, configuration.hud)
        assertTrue(configuration.hud.hasElement("score_counter"))
    }

    @Test
    fun `HUD elements default what they don't say`() {
        val hud = decodeSkinConfiguration(json("""{ "HUD": [{ "type": "health_bar", "x": "5" }] }""")).hud
        assertEquals(HUDSkinData(listOf(HUDElementSkinData("health_bar", Vec2(5f, 0f)))), hud)
    }

    @Test
    fun `Colours are written as osu!droid writes them`() {
        val json = SkinJson.encodeToJsonElement(SkinConfiguration.serializer(), SkinConfiguration.Default).jsonObject
        assertEquals("#C2CAFF", json["Theme"]!!.jsonObject["accentColor"]!!.toString().trim('"'))
        assertEquals("#646464", json["Utils"]!!.jsonObject["spinnerBackgroundColor"]!!.toString().trim('"'))

        val translucent = SkinJson.encodeToJsonElement(HexColorSerializer, Color4(255, 0, 0, 128)).toString().trim('"')
        assertEquals("#80FF0000", translucent)
        assertEquals(Color4(255, 0, 0, 128), Color4(translucent))
    }
}
