package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.scaleTo
import kotlin.jvm.JvmOverloads
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * A number drawn with the score font of the skin.
 */
class ScoreNumber @JvmOverloads constructor(
    x: Float,
    y: Float,
    text: String,
    private val letterScale: Float,
    private val isCentered: Boolean = false
) : UIContainer() {

    /**
     * The height of the letters.
     */
    var letterHeight = 0f
        private set


    init {
        setPosition(x, y)
        setText(text)
    }


    fun setText(text: String) {
        Verktex.withTreeLock {
            clearChildren()

            var totalWidth = 0f

            for (ch in text) {
                val name = when (ch) {
                    in '0'..'9' -> ch.toString()
                    '.', ',' -> "comma"
                    '%' -> "percent"
                    else -> "x"
                }

                val texture = SkinTextures[OsuSkin.get().scorePrefix, name] ?: continue
                val width = texture.displayWidth * letterScale

                letterHeight = texture.displayHeight * letterScale

                +UISprite(texture).apply {
                    setPosition(totalWidth, 0f)
                    setSize(width, letterHeight)
                }

                totalWidth += width
            }

            if (isCentered) {
                for (letter in this) {
                    letter.x = (letter.x.resolve(0f) - totalWidth / 2f).px
                    letter.choreograph {
                        at(letter.currentClockTime)
                        scaleTo(1.5f, 0.2f)
                        then()
                        scaleTo(1f, 0.4f)
                    }
                }
            }
        }
    }
}
