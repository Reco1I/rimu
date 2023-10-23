package com.reco1l.framework.graphics

import android.content.res.ColorStateList
import androidx.annotation.IntRange
import androidx.core.graphics.drawable.toDrawable
import com.reco1l.skindecoder.serializers.ColorSerializer
import kotlinx.serialization.Serializable
import org.andengine.util.adt.color.Color

/**
 * [Color] extension that allows to pass a non-component color (8 bit 0-255) in its constructor,
 * default color is Black (0, 0, 0, 255).
 *
 * Keep in mind this converts the 4 channels to component, in order to use 0-255 format use [red8bit],
 * [green8bit], [blue8bit] and [alpha8bit].
 */
@Serializable(ColorSerializer::class)
open class Color4 @JvmOverloads constructor(

    @IntRange(0, 255) red: Int,

    @IntRange(0, 255) green: Int,

    @IntRange(0, 255) blue: Int,

    @IntRange(0, 255) alpha: Int = 255

) : Color(red / 255f, green / 255f, blue / 255f, alpha / 255f)
{

    /**
     * Create a [Color4] from an HEX Integer.
     */
    constructor(argb: Long) : this(
        red = ((argb shr 16) and 0xFF).toInt(),
        green = ((argb shr 8) and 0xFF).toInt(),
        blue = (argb and 0xFF).toInt(),
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

    /**`getRed() * 255`*/
    var red8bit
        get() = (255f * red).toInt()
        set(value)
        {
            red = value.coerceIn(0, 255) / 255f
        }

    /**`getGreen() * 255`*/
    var green8bit
        get() = (255f * green).toInt()
        set(value)
        {
            green = value.coerceIn(0, 255) / 255f
        }

    /**`getBlue() * 255`*/
    var blue8bit
        get() = (255f * blue).toInt()
        set(value)
        {
            blue = value.coerceIn(0, 255) / 255f
        }

    /**`getAlpha() * 255`*/
    var alpha8bit
        get() = (255f * alpha).toInt()
        set(value)
        {
            alpha = value.coerceIn(0, 255) / 255f
        }


    // Transformations

    fun factor(factor: Float) = Color4(
        (red8bit * factor).toInt(),
        (green8bit * factor).toInt(),
        (blue8bit * factor).toInt(),
        alpha8bit
    )


    // Conversion

    fun set(hex: Int)
    {
        red8bit = (hex shr 16) and 0xFF
        green8bit = (hex shr 8) and 0xFF
        blue8bit = hex and 0xFF
        alpha8bit = (hex shr 24) and 0xFF
    }


    /**
     * Returns the color in a hexadecimal integer format.
     */
    fun toInt(
        red: Float = getRed(),
        blue: Float = getBlue(),
        green: Float = getGreen(),
        alpha: Float = getAlpha(),
        factor: Float = 1f
    ): Int
    {
        return ((255 * alpha).toInt() shl 24)
            .or((255 * red * factor).toInt() shl 16)
            .or((255 * green * factor).toInt() shl 8)
            .or((255 * blue * factor).toInt() shl 0)
    }

    fun toDrawable(
        red: Float = getRed(),
        blue: Float = getBlue(),
        green: Float = getGreen(),
        alpha: Float = getAlpha(),
        factor: Float = 1f
    ) = toInt(red, blue, green, alpha, factor).toDrawable()

    fun toColorStateList(
        red: Float = getRed(),
        blue: Float = getBlue(),
        green: Float = getGreen(),
        alpha: Float = getAlpha(),
        factor: Float = 1f
    ) = ColorStateList.valueOf(toInt(red, blue, green, alpha, factor))


    // Generated

    override fun equals(other: Any?) = other === this || other is Color4
            && red == other.red
            && green == other.green
            && blue == other.blue
            && alpha == other.alpha


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