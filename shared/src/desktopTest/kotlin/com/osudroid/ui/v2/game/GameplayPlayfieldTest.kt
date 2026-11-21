package com.osudroid.ui.v2.game

import com.osudroid.game.Playfield
import com.osudroid.math.Vector2
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.ui.container.Container
import org.junit.Assert.assertEquals
import org.junit.Test

class GameplayPlayfieldTest {

    // The heights of a 1280 wide space on common screens: 16:9, 18:9, 19.5:9, 20:9 and 4:3.
    private val heights = listOf(720f, 640f, 590.7692f, 576f, 960f)

    // A playfield laid out in a space 1280 wide and [height] tall, drawn at twice its size in the scene.
    private fun layOut(height: Float): GameplayPlayfield {
        val playfield = GameplayPlayfield()
        val space = Container().apply {
            setSize(Playfield.SPACE_WIDTH, height)
            scale = 2f
            scaleOrigin = Anchor.TopLeft
            +playfield
        }

        space.measure(LayoutConstraints(0f, Playfield.SPACE_WIDTH, 0f, height))
        space.layout(Vec4(0f, 0f, Playfield.SPACE_WIDTH, height))
        space.calculateTransformations()
        return playfield
    }

    // As osu!droid's Constants.MAP_ACTUAL_HEIGHT and MAP_ACTUAL_WIDTH, from its integer resolution.
    private fun playfieldHeight(height: Float) = (height.toInt() * 0.8f).toInt()
    private fun playfieldWidth(height: Float) = playfieldHeight(height) / 3 * 4

    @Test
    fun `The playfield is rounded as osu!droid rounds it`() {
        for (height in heights) {
            val scale = GameplayPlayfield.roundedScale(height)

            assertEquals(playfieldWidth(height) / 512f, scale.x, 1e-6f)
            assertEquals(playfieldHeight(height) / 384f, scale.y, 1e-6f)
        }
    }

    @Test
    fun `Positions in the scene are converted through the playfield as osu!droid maps them`() {
        for (height in heights) {
            val playfield = layOut(height)
            val width = playfieldWidth(height)
            val playfieldHeight = playfieldHeight(height)

            for (point in listOf(Vector2(0f), Vector2(256f, 192f), Vector2(512f, 384f), Vector2(37.5f, 301.25f))) {
                // As osu!droid's HitObject.convertPositionToRealCoordinates, in the space, twice as big in the scene.
                val spaceX = point.x * width / 512f + (1280f - width) / 2f
                val spaceY = point.y * playfieldHeight / 384f + (height - playfieldHeight) / 2f

                val position = playfield.toPlayfield(spaceX * 2f, spaceY * 2f)
                assertEquals(point.x, position.x, 1e-2f)
                assertEquals(point.y, position.y, 1e-2f)
            }
        }
    }

    @Test
    fun `The visible area is the space, in osu!pixels`() {
        for (height in heights) {
            val playfield = layOut(height)
            val area = playfield.visibleArea

            val topLeft = playfield.toPlayfield(0f, 0f)
            val bottomRight = playfield.toPlayfield(1280f * 2f, height * 2f)

            assertEquals(topLeft.x, area.x, 1e-2f)
            assertEquals(topLeft.y, area.y, 1e-2f)
            assertEquals(bottomRight.x, area.z, 1e-2f)
            assertEquals(bottomRight.y, area.w, 1e-2f)
        }
    }

    @Test
    fun `Hard Rock flips the playfield vertically`() {
        assertEquals(Vector2(100f, 284f), Playfield.flipVertically(Vector2(100f, 100f)))
    }
}
