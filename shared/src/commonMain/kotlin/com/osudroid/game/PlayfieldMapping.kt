package com.osudroid.game

import com.osudroid.math.Vector2
import kotlin.jvm.JvmStatic
import kotlin.math.floor

/**
 * How osu!droid maps the playfield, in osu!pixels, into the space gameplay is laid out in: [width] by [height], a
 * screen [REFERENCE_WIDTH] wide.
 *
 * The playfield is 80% of the height, 4:3 and centered, with the sizes rounded to whole pixels of that space as
 * osu!droid rounds them, so its horizontal and vertical scales differ slightly ([scaleX], [scaleY]). Hit objects
 * are scaled by [objectScale], the height over 480.
 */
class PlayfieldMapping(val width: Float, val height: Float) {

    /**
     * The height of the playfield, in the pixels of the space.
     */
    val playfieldHeight = (floor(height) * 0.8f).toInt()

    /**
     * The width of the playfield, in the pixels of the space.
     */
    // The integer division is osu!droid's.
    val playfieldWidth = playfieldHeight / 3 * 4

    /**
     * The horizontal scale from osu!pixels.
     */
    val scaleX = playfieldWidth / PLAYFIELD_WIDTH

    /**
     * The vertical scale from osu!pixels.
     */
    val scaleY = playfieldHeight / PLAYFIELD_HEIGHT

    /**
     * Where the playfield starts horizontally.
     */
    val offsetX = (width - playfieldWidth) / 2f

    /**
     * Where the playfield starts vertically.
     */
    val offsetY = (height - playfieldHeight) / 2f

    /**
     * The scale of hit objects in the space, from their scale in osu!pixels.
     */
    val objectScale = height / 480f


    /**
     * Converts a position in osu!pixels to the space.
     */
    fun toResolution(x: Float, y: Float) = Vector2(x * scaleX + offsetX, y * scaleY + offsetY)

    /**
     * Converts a position in osu!pixels to the space.
     */
    fun toResolution(position: Vector2) = toResolution(position.x, position.y)

    /**
     * Converts a position in the space to osu!pixels.
     */
    fun toPlayfield(x: Float, y: Float) = Vector2((x - offsetX) / scaleX, (y - offsetY) / scaleY)


    override fun equals(other: Any?) = other is PlayfieldMapping && other.width == width && other.height == height

    override fun hashCode() = 31 * width.hashCode() + height.hashCode()


    companion object {

        /**
         * The width of the space gameplay is laid out in, osu!droid's fixed resolution.
         */
        const val REFERENCE_WIDTH = 1280f

        /**
         * The size of the playfield, in osu!pixels.
         */
        const val PLAYFIELD_WIDTH = 512f
        const val PLAYFIELD_HEIGHT = 384f

        /**
         * Flips a position in osu!pixels vertically, as Hard Rock flips the playfield.
         */
        @JvmStatic
        fun flipVertically(position: Vector2): Vector2 = position.set(position.x, PLAYFIELD_HEIGHT - position.y)
    }
}
