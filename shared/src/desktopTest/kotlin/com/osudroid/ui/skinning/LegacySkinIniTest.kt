package com.osudroid.ui.skinning

import com.reco1l.verktex.data.Color4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySkinIniTest {

    private fun convert(ini: String) = LegacySkinIni.decode(ini).toConfiguration()

    private fun versionOf(general: String) = convert("[General]\n$general\n").utils.version

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
        assertEquals(SkinConfiguration.LATEST_VERSION, versionOf("Version: latest"))
    }

    @Test
    fun `An osu!stable skin_ini is read`() {
        val configuration = convert(
            """
            // A comment
            [General]
            Name: Test skin
            Author: Someone
            Version: 2.7
            CursorRotate: 0
            CursorExpand: 0
            CursorCentre: 1
            SliderBallFlip: 0
            AllowSliderBallTint: 1
            SpinnerNoBlink: 1
            ComboBurstRandom: 1
            LayeredHitSounds: 0
            AnimationFramerate: 30

            [Colours]
            Combo1: 255,192,0
            Combo2 : 0, 202, 0 // green
            Combo3: 18,124,255,255
            Combo5: not a colour
            SliderBorder: 255,255,255
            SpinnerBackground: 10,20,30

            [Fonts]
            HitCirclePrefix: fonts\default
            HitCircleOverlap: 3
            ScorePrefix: fonts\score
            ComboOverlap: -1

            [Mania]
            Keys: 4
            """.trimIndent()
        )

        assertEquals(2.7f, configuration.utils.version)
        assertFalse(configuration.cursor.rotateCursor)
        assertTrue(configuration.cursor.rotateCursorTrail)
        assertFalse(configuration.cursor.expandCursor)
        assertTrue(configuration.cursor.centreCursor)
        assertFalse(configuration.slider.ballFlip)
        assertTrue(configuration.utils.allowSliderBallTint)
        assertTrue(configuration.utils.spinnerNoBlink)
        assertTrue(configuration.utils.comboBurstRandom)
        assertFalse(configuration.utils.layeredHitSounds)
        assertEquals(30f, configuration.utils.animationFramerate)
        assertEquals(0.8f, configuration.utils.comboTextScale)

        assertEquals(listOf(Color4(255, 192, 0), Color4(0, 202, 0), Color4(18, 124, 255)), configuration.comboColors.colors)
        assertTrue(configuration.comboColors.forceOverride)
        assertEquals(Color4(255, 255, 255), configuration.slider.borderColor)
        assertEquals(Color4(10, 20, 30), configuration.utils.spinnerBackgroundColor)

        assertEquals("fonts\\default", configuration.fonts.hitCirclePrefix)
        assertEquals(3f, configuration.fonts.hitCircleOverlap)
        assertEquals("fonts\\score", configuration.fonts.scorePrefix)
        assertEquals("score", configuration.fonts.comboPrefix)
        assertEquals(-1f, configuration.fonts.comboOverlap)
    }

    @Test
    fun `A skin_ini without keys has osu!stable's defaults`() {
        val configuration = convert("")

        assertTrue(configuration.cursor.rotateCursor)
        assertTrue(configuration.cursor.expandCursor)
        assertTrue(configuration.utils.hitCircleOverlayAboveNumber)
        assertTrue(configuration.utils.layeredHitSounds)
        assertFalse(configuration.comboColors.forceOverride)
        assertEquals(ComboColors.DEFAULT, configuration.comboColors.resolved)
        assertTrue(configuration.slider.followComboColor)
        assertNull(configuration.slider.borderColor)
        assertEquals(-1f, configuration.utils.animationFramerate)
        assertEquals("default", configuration.fonts.hitCirclePrefix)
        assertEquals(-2f, configuration.fonts.hitCircleOverlap)
    }

    @Test
    fun `The misspelled HitCircleOverlayAboveNumer is read`() {
        assertFalse(convert("[General]\nHitCircleOverlayAboveNumer: 0").utils.hitCircleOverlayAboveNumber)
        assertTrue(convert("[General]\nHitCircleOverlayAboveNumer: 0\nHitCircleOverlayAboveNumber: 1").utils.hitCircleOverlayAboveNumber)
    }

    @Test
    fun `A slider track colour replaces the combo colour of the body, opaque`() {
        val slider = convert("[Colours]\nSliderTrackOverride: 10,20,30").slider

        assertEquals(Color4(10, 20, 30), slider.bodyColor)
        assertFalse(slider.followComboColor)
        assertEquals(1f, slider.bodyBaseAlpha)
    }

    @Test
    fun `Invalid values keep their defaults`() {
        val configuration = convert("[General]\nCursorRotate: maybe\nAnimationFramerate: fast\n[Colours]\nSliderBorder: 1,2")

        assertTrue(configuration.cursor.rotateCursor)
        assertEquals(-1f, configuration.utils.animationFramerate)
        assertNull(configuration.slider.borderColor)
    }
}
