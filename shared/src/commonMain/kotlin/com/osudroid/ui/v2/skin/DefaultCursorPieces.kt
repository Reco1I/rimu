package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.PixelsSource
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.container.Container

/**
 * The follow point of the default skin, as osu!lazer's default `FollowPoint`: a white dot 8 across, half transparent
 * and added to what's below it. Without osu!lazer's soft glow, whose quad would be four times the dot's: many are shown
 * at once and it's barely visible at their size.
 */
class DefaultFollowPointPiece : FollowPointPiece() {

    init {
        width = SIZE.px
        height = SIZE.px

        +disc(SIZE).apply {
            color = Color4.White.alpha(0.5f)
            blendMode = BlendMode.Additive
        }
    }

    private companion object {
        // In osu!pixels, before the objects' scale.
        const val SIZE = 8f
    }
}

/**
 * The cursor of the default skin, as osu!lazer's `ArgonCursor`: a white ring with a glow around a dark disc and a
 * white dot, centred on the cursor's position, at the size of the default `cursor`.
 */
class DefaultCursorPiece : CursorPiece() {

    override val isCentred = true

    init {
        val size = SkinDimensions.CURSOR.width

        width = size.px
        height = size.px

        +disc(size).apply { color = Color4.Black.alpha(0.4f) }
        +ring(size, size * 0.12f).apply {
            shadow = Shadow(Color4.White.alpha(0.5f), blur = (size * 0.15f).px, cutout = true)
        }
        +disc(size * 0.2f).apply { color = Color4.White }
    }
}

/**
 * The trail of the default skin: a continuous trail of soft white dots, made once in code.
 */
object DefaultCursorTrailStyle : CursorTrailStyle {

    private const val KEY = "default-skin:cursortrail"

    override val texture: Texture.Region?
        // Made again if the textures were unloaded.
        get() = Textures[KEY] ?: Textures.load(KEY, PixelsSource.softDot(SIZE))

    override val isContinuous = true
    override val isCentred = true
    override val rotates = false

    // The size of the default `cursortrail`, twice as large as it's a soft dot rather than a solid one.
    private val SIZE = (SkinDimensions.CURSOR_TRAIL.width * 2).toInt()
}
