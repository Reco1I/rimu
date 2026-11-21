package com.osudroid.game

import com.osudroid.math.Vector2
import kotlin.jvm.JvmStatic

/**
 * The dimensions gameplay is laid out with.
 *
 * Hit objects, spinners and cursors are in the playfield, in osu!pixels, see
 * [com.osudroid.ui.v2.game.GameplayPlayfield]. The rest of gameplay (the HUD, the break and countdown screens) is in
 * a space [SPACE_WIDTH] wide, osu!droid's fixed resolution, in the proportions of the window.
 */
object Playfield {

    /**
     * The size of the playfield, in osu!pixels.
     */
    const val WIDTH = 512f
    const val HEIGHT = 384f

    /**
     * The width of the space what isn't in the playfield is laid out in, osu!droid's fixed resolution.
     */
    const val SPACE_WIDTH = 1280f

    /**
     * The osu!pixels in a pixel of osu!droid's 1280x720 screen, where the playfield is 576 pixels tall: what's in the
     * playfield but isn't a hit object (spinners, cursors, Flashlight) was made for that screen, and is scaled by it.
     */
    const val SCREEN_PIXEL = HEIGHT / 576f

    /**
     * Flips a position in osu!pixels vertically, as Hard Rock flips the playfield.
     */
    @JvmStatic
    fun flipVertically(position: Vector2): Vector2 = position.set(position.x, HEIGHT - position.y)
}
