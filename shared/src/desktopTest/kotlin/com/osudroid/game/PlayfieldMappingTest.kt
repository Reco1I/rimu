package com.osudroid.game

import com.osudroid.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayfieldMappingTest {

    // The heights of a 1280 wide space on common screens: 16:9, 18:9, 19.5:9, 20:9 and 4:3.
    private val heights = listOf(720f, 640f, 590.7692f, 576f, 960f)

    @Test
    fun `The playfield is rounded as osu!droid rounds it`() {
        for (height in heights) {
            val mapping = PlayfieldMapping(1280f, height)

            // As osu!droid's Constants.MAP_ACTUAL_HEIGHT and MAP_ACTUAL_WIDTH, from its integer resolution.
            val expectedHeight = (height.toInt() * 0.8f).toInt()
            val expectedWidth = expectedHeight / 3 * 4

            assertEquals(expectedHeight, mapping.playfieldHeight)
            assertEquals(expectedWidth, mapping.playfieldWidth)
        }
    }

    @Test
    fun `Positions are mapped as osu!droid maps them`() {
        for (height in heights) {
            val mapping = PlayfieldMapping(1280f, height)

            for (point in listOf(Vector2(0f), Vector2(256f, 192f), Vector2(512f, 384f), Vector2(37.5f, 301.25f))) {
                // As osu!droid's HitObject.convertPositionToRealCoordinates.
                val expectedX = point.x * mapping.playfieldWidth / 512f + (1280f - mapping.playfieldWidth) / 2f
                val expectedY = point.y * mapping.playfieldHeight / 384f + (height - mapping.playfieldHeight) / 2f

                val position = mapping.toResolution(point)
                assertEquals(expectedX, position.x, 1e-3f)
                assertEquals(expectedY, position.y, 1e-3f)

                val back = mapping.toPlayfield(position.x, position.y)
                assertEquals(point.x, back.x, 1e-3f)
                assertEquals(point.y, back.y, 1e-3f)
            }
        }
    }

    @Test
    fun `The center of the playfield is the center of the space`() {
        for (height in heights) {
            val mapping = PlayfieldMapping(1280f, height)
            val center = mapping.toResolution(256f, 192f)

            assertEquals(640f, center.x, 1e-3f)
            assertEquals(height / 2f, center.y, 1e-3f)
        }
    }

    @Test
    fun `Hard Rock flips the playfield vertically`() {
        assertEquals(Vector2(100f, 284f), PlayfieldMapping.flipVertically(Vector2(100f, 100f)))
    }
}
