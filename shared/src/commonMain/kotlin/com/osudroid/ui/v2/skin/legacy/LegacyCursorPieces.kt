package com.osudroid.ui.v2.skin.legacy

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skin.CursorPiece
import com.osudroid.ui.v2.skin.CursorTrailStyle
import com.osudroid.ui.v2.skin.FollowPointPiece
import com.osudroid.ui.v2.skinFrames
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.ui.Sprite
import kotlin.math.max
import kotlin.math.min
import com.osudroid.ui.skinning.OsuSkin

/**
 * The follow point of a texture skin, `followpoint`, animated or not depending on the skin, which may change while
 * it's pooled.
 *
 * As in osu!lazer, its animation loops and starts when the follow point starts to fade in, on the gameplay clock, so
 * it's in the same frame whenever it's seen, seeking included.
 */
class LegacyFollowPointPiece : FollowPointPiece() {

    private val sprite = object : AnimatedSprite(skinFrames("followpoint", orStatic = true), OsuSkin.current.utils.animationFramerate) {

        init {
            anchor = Anchor.Center
            origin = Anchor.Center
            isLoop = true
            // The frame follows the clock instead of advancing on its own.
            pause()
            // Cropping from the center is what this scale type does when the size is smaller than the texture.
            scaleType = ScaleType.Crop
        }

        override fun onUpdate(clock: FrameClock) {
            val duration = duration

            elapsedSec = if (duration > 0f) max(0f, currentClockTime - animationStartTime) % duration else 0f

            super.onUpdate(clock)
        }

        // The size follows the texture, at most this.
        // Reference: https://github.com/ppy/osu/blob/0811de728e4205a45e485d53ccdaf19a937c6033/osu.Game.Rulesets.Osu/Skinning/Legacy/OsuLegacySkinTransformer.cs#L95-L97
        override fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {
            val size = super.onMeasure(parentConstraints, contentConstraints)

            return Vec2(
                x = min(size.x, HitObject.OBJECT_RADIUS * 2),
                y = min(size.y, HitObject.OBJECT_RADIUS)
            )
        }
    }

    init {
        +sprite
    }
}

/**
 * The cursor of a texture skin, `cursor`, placed from its centre or its top left corner as the skin asks.
 */
class LegacyCursorPiece : CursorPiece() {

    override val isCentred = OsuSkin.current.cursor.centreCursor

    init {
        +Sprite().apply { texture = skinTexture("cursor") }
    }
}

/**
 * The trail of a texture skin, as osu!stable's: separate `cursortrail` images every frame for skins without a
 * `cursormiddle`, a continuous trail for skins with one.
 */
object LegacyCursorTrailStyle : CursorTrailStyle {

    override val texture: Texture.Region?
        get() = SkinTextures["cursortrail"]

    override val isContinuous: Boolean
        get() = SkinTextures["cursormiddle"] != null

    override val isCentred: Boolean
        get() = isContinuous || OsuSkin.current.cursor.centreCursor

    override val rotates: Boolean
        get() = OsuSkin.current.cursor.rotateCursorTrail
}
