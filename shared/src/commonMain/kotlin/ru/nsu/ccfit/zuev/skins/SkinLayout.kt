package ru.nsu.ccfit.zuev.skins

import com.osudroid.settings.GameResolution
import com.osudroid.utils.optDouble
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.UISprite
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
    fun apply(sprite: UISprite, previousSprite: UISprite? = null) {
        if (scale != -1f) {
            sprite.scale = scale
        }
        if (width != -1f) {
            sprite.width = Dimension.Fixed.Pixels(width)
        }
        if (height != -1f) {
            sprite.height = Dimension.Fixed.Pixels(height)
        }

        // The origin of the sprite that is using this class is in bottom left, but without its
        // origin being set explicitly in ExtendedSprite to bottom left as touch area does not account for
        // different origins yet. To work around this, we need to flip the position in the Y axis.
        // TODO: When ExtendedSprite supports touch area properly, this should be changed to use bottom-left origin
        var x = xPosition
        var y = GameResolution.height - sprite.transformedHeight - yPosition

        if (x.isNaN()) {
            x = if (previousSprite != null) previousSprite.x.resolve(0f) + previousSprite.transformedWidth else 0f
        }

        if (y.isNaN()) {
            y = GameResolution.height - sprite.transformedHeight
        }

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
