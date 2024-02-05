package com.reco1l.rimu.data

import androidx.annotation.IntRange
import androidx.core.graphics.drawable.toDrawable
import com.badlogic.gdx.graphics.Color
import com.reco1l.skindecoder.serializers.ColorSerializer
import kotlinx.serialization.Serializable

/**
 * [Color] extension that allows to pass a non-component color (8 bit 0-255) in its constructor,
 * default color is Black (0, 0, 0, 255).
 *
 * Keep in mind this converts the 4 channels to component, in order to use 0-255 format use [red8bit],
 * [green8bit], [blue8bit] and [alpha8bit].
 */
@Serializable(ColorSerializer::class)
open class Color4 @JvmOverloads constructor(

    @IntRange(0, 255) r: Int,

    @IntRange(0, 255) g: Int,

    @IntRange(0, 255) b: Int,

    @IntRange(0, 255) alpha: Int = 255

) : Color(r / 255f, g / 255f, b / 255f, alpha / 255f)
{

    /**
     * Create a [Color4] from an HEX Integer.
     */
    constructor(argb: Long) : this(
        r = ((argb shr 16) and 0xFF).toInt(),
        g = ((argb shr 8) and 0xFF).toInt(),
        b = (argb and 0xFF).toInt(),
        alpha = ((argb shr 24) and 0xFF).toInt()
    )

    /**
     * Clone from another [Color4] instance.
     */
    constructor(copy: Color4) : this(
        copy.red8bit,
        copy.green8bit,
        copy.blue8bit,
        copy.alpha8bit
    )

    /**
     * `getRed() * 255`
     */
    var red8bit
        get() = (255f * r).toInt()
        set(value)
        {
            r = value.coerceIn(0, 255) / 255f
        }

    /**
     * `getGreen() * 255`
     */
    var green8bit
        get() = (255f * g).toInt()
        set(value)
        {
            g = value.coerceIn(0, 255) / 255f
        }

    /**
     * `getBlue() * 255`
     */
    var blue8bit
        get() = (255f * b).toInt()
        set(value)
        {
            b = value.coerceIn(0, 255) / 255f
        }

    /**
     * `getAlpha() * 255`
     */
    var alpha8bit
        get() = (255f * a).toInt()
        set(value)
        {
            a = value.coerceIn(0, 255) / 255f
        }


    // Transformations

    fun factor(factor: Float) = Color4(
        (red8bit * factor).toInt(),
        (green8bit * factor).toInt(),
        (blue8bit * factor).toInt(),
        alpha8bit
    )



    // Conversion

    /**
     * Returns the color in a hexadecimal integer format.
     */
    fun toInt(
        r: Float = this.r,
        g: Float = this.g,
        b: Float = this.b,
        alpha: Float = this.a,
        factor: Float = 1f
    ): Int
    {
        return ((255 * alpha).toInt() shl 24)
            .or((255 * r * factor).toInt() shl 16)
            .or((255 * g * factor).toInt() shl 8)
            .or((255 * b * factor).toInt() shl 0)
    }

    fun toDrawable(
        r: Float = this.r,
        g: Float = this.g,
        b: Float = this.b,
        alpha: Float = this.a,
        factor: Float = 1f
    ) = toInt(r, g, b, alpha, factor).toDrawable()


    // Generated

    override fun equals(other: Any?) = other === this || other is Color4
            && r == other.r
            && g == other.g
            && b == other.b
            && a == other.a


    override fun hashCode(): Int
    {
        var result = super.hashCode()
        result = 31 * result + red8bit
        result = 31 * result + green8bit
        result = 31 * result + blue8bit
        result = 31 * result + alpha8bit
        return result
    }

}


