package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.Container
import kotlin.jvm.JvmOverloads

/**
 * A number in the skin's score font ([SkinComponent.ScoreFont]): its texture digits, or Fredoka in the default skin
 * and in skins without them. Placed from its top left corner, its top centre if [isCentered], where it pops each time
 * it changes, or its top right corner if [isRightAligned].
 */
class ScoreNumber @JvmOverloads constructor(
    x: Float,
    y: Float,
    text: String,
    private val letterScale: Float,
    private val isCentered: Boolean = false,
    isRightAligned: Boolean = false
) : Container() {

    private val number = SkinPieces.current.create(SkinComponent.ScoreFont)

    /**
     * The height of the letters.
     */
    val letterHeight: Float
        get() = number.digitHeight * letterScale

    init {
        val placement = when {
            isCentered -> Anchor.TopCenter
            isRightAligned -> Anchor.TopRight
            else -> Anchor.TopLeft
        }

        origin = placement
        number.anchor = placement
        number.origin = placement
        number.scaleOrigin = placement
        number.scale = letterScale
        +number

        setPosition(x, y)
        setText(text)
    }


    fun setText(text: String) {
        number.text = text

        if (isCentered) {
            number.choreography?.clear()
            number.scale = letterScale
            number.choreograph {
                at(number.currentClockTime)
                scaleTo(letterScale * 1.5f, 0.2f)
                then()
                scaleTo(letterScale, 0.4f)
            }
        }
    }
}
