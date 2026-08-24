package com.osudroid.ui.v2.hud.editor

import com.reco1l.verktex.*
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.shape.*
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.input.touch.TouchEvent
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min

class HUDElementPreview(private val element: HUDElement, val hud: GameplayHUD): UIContainer() {


    private val label = UIText().apply {
        fontSize = FontSize.SM
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        text = element.name
        color = Color4.White
    }

    init {
        width = Dimension.FillAvailable
        height = 120f
        padding = Vec4(12f)
        scaleCenterX = 0.5f
        scaleCenterY = 0.5f
        backgroundColor = Color4(0xFF363653)
        radius = 12f

        attachChild(element)
        attachChild(label)
        element.setSkinData(
            HUDElementSkinData(
                type = element::class,
                anchor = Anchor.TopLeft,
                origin = Anchor.TopLeft,
            )
        )
    }

    override fun doDraw(gl: GL10, camera: Camera) {

        // Scaling the element inside the box
        element.setScaleCenter(0f, 0f)

        if (element.width > element.height) {
            element.setScale(min(1f, innerWidth / element.width))
        } else {
            element.setScale(min(1f, (innerHeight - label.height) / element.height))
        }

        super.doDraw(gl, camera)
    }

    //region Input handling
    private var initialX = 0f
    private var initialY = 0f

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (event.isActionDown) {
            initialX = localX
            initialY = localY
        }

        if (event.isActionUp) {

            if (abs(localX - initialX) < 1f && abs(localY - initialY) < 1f) {
                clearEntityModifiers()
                beginModifierSequence {
                    scaleTo(0.9f, 0.1f)
                    then()
                    scaleTo(1f, 0.1f)
                }
                hud.addElement(HUDElementSkinData(element::class))
                hud.elementSelector?.collapse()
            }
        }

        return false
    }
    //endregion

}