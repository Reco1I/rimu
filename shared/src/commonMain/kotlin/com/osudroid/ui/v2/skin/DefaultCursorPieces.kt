package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.PixelsSource
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.Sprite
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import com.reco1l.verktex.ui.container.Container

/**
 * The follow point of the default skin, as osu!stable's default `followpoint`: a short white dash along the
 * connection, tapering to its ends, in a soft glow. Made once in code.
 */
class DefaultFollowPointPiece : FollowPointPiece() {

    init {
        width = LENGTH.px
        height = HEIGHT.px

        +Sprite(texture).apply {
            width = LENGTH.px
            height = HEIGHT.px
            scaleType = ScaleType.Stretch
        }
    }

    private companion object {
        const val KEY = "default-skin:followpoint"

        // In osu!pixels, before the objects' scale, glow included.
        const val LENGTH = 24f
        const val HEIGHT = 8f

        // The texture's pixels per osu!pixel.
        const val RESOLUTION = 4

        // In osu!pixels: half the length of the dash's core, the radius of its core and of its glow.
        const val CORE_HALF_LENGTH = 7f
        const val CORE_RADIUS = 1.1f
        const val GLOW_RADIUS = 4f
        const val GLOW_ALPHA = 0.35f

        // Made again if the textures were unloaded.
        val texture: Texture.Region?
            get() = Textures[KEY] ?: Textures.load(KEY, dash())

        fun dash(): PixelsSource {
            val width = (LENGTH * RESOLUTION).toInt()
            val height = (HEIGHT * RESOLUTION).toInt()
            val pixels = ByteArray(width * height * 4)

            for (py in 0 until height) {
                for (px in 0 until width) {
                    // In osu!pixels from the centre.
                    val x = (px + 0.5f) / RESOLUTION - LENGTH / 2
                    val y = (py + 0.5f) / RESOLUTION - HEIGHT / 2

                    // The distance to the segment of the core, so its ends are round.
                    val dx = (abs(x) - CORE_HALF_LENGTH).coerceAtLeast(0f)
                    val distance = sqrt(dx * dx + y * y)

                    // The core is solid, with an antialiased edge a pixel wide.
                    val core = ((CORE_RADIUS - distance) * RESOLUTION + 0.5f).coerceIn(0f, 1f)
                    val glow = (1f - distance / GLOW_RADIUS).coerceAtLeast(0f).pow(2) * GLOW_ALPHA

                    // Fading towards the ends, as the stable dash does.
                    val taper = 1f - (abs(x) / (LENGTH / 2)).pow(3)

                    val alpha = (core + glow * (1f - core)) * taper
                    val value = (alpha * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()

                    // Premultiplied white: every channel is the alpha.
                    val index = (py * width + px) * 4
                    pixels[index] = value
                    pixels[index + 1] = value
                    pixels[index + 2] = value
                    pixels[index + 3] = value
                }
            }

            return PixelsSource(width, height, pixels)
        }
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
