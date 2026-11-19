package ru.nsu.ccfit.zuev.skins

import com.reco1l.verktex.data.Anchor
import com.osudroid.utils.optDouble
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.Sprite
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonObject

class SkinLayout {

    @JvmField
    var property: JsonObject? = null

    @JvmField
    var width = 0f
    @JvmField
    var height = 0f
    @JvmField
    var xPosition = 0f
    @JvmField
    var yPosition = 0f

    @JvmField
    var scale = 1f

    @JvmOverloads
    fun apply(sprite: Sprite, previousSprite: Sprite? = null) {
        if (scale != -1f) {
            sprite.scale = scale
        }
        if (width != -1f) {
            sprite.width = Dimension.Fixed.Pixels(width)
        }
        if (height != -1f) {
            sprite.height = Dimension.Fixed.Pixels(height)
        }

        // Positions are from the bottom left corner of the sprite's parent, up and to the right.
        var x = xPosition
        val y = if (yPosition.isNaN()) 0f else -yPosition

        if (x.isNaN()) {
            x = if (previousSprite != null) previousSprite.x.resolve(0f) + previousSprite.transformedWidth else 0f
        }

        sprite.anchor = Anchor.BottomLeft
        sprite.origin = Anchor.BottomLeft
        sprite.setPosition(x, y)
    }

    companion object {
        @JvmStatic
        fun load(`object`: JsonObject): SkinLayout {
            val layout = SkinLayout()
            layout.property = `object`
            layout.width = `object`.optDouble("w", -1.0).toFloat()
            layout.height = `object`.optDouble("h", -1.0).toFloat()
            layout.xPosition = `object`.optDouble("x").toFloat()
            layout.yPosition = `object`.optDouble("y").toFloat()
            layout.scale = `object`.optDouble("scale", -1.0).toFloat()
            return layout
        }
    }
}
