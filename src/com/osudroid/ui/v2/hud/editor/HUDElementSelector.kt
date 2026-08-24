package com.osudroid.ui.v2.hud.editor

import com.reco1l.verktex.*
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import com.reco1l.toolkt.kotlin.fastForEach
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.reflect.full.primaryConstructor

class HUDElementSelector(private val hud: GameplayHUD) : UIContainer(), IGameplayEvents {


    /**
     * Whether the element selector is expanded.
     */
    val isExpanded
        get() = x >= 0f


    private val elements = HUDElements.entries.map { it.type.primaryConstructor!!.call() }

    private val elementList = UIScrollableContainer().apply {

        scrollAxes = Axis.Y
        height = Dimension.FillAvailable
        width = SELECTOR_WIDTH
        backgroundColor = Color4(0xFF1E1E2E)

        attachChild(UILinearContainer().apply {
            width = Dimension.FillAvailable
            padding = Vec4(16f)
            spacing = 12f
            orientation = Orientation.Vertical

            elements.forEach { element ->
                attachChild(HUDElementPreview(element, hud))
            }
        })

    }


    init {
        height = Dimension.FillAvailable

        x = -SELECTOR_WIDTH

        // The button to show/hide the element selector
        attachChild(object : UIContainer() {

            init {
                backgroundColor = Color4(0xFF181825)
                radius = BUTTON_RADIUS

                setSize(BUTTON_WIDTH, 150f)
                x = SELECTOR_WIDTH - BUTTON_RADIUS

                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                attachChild(UIText().apply {
                    rotationZ = -90f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    fontSize = FontSize.SM
                    text = "Elements"

                    x = BUTTON_RADIUS / 2
                })
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionDown) {
                    return true
                }

                if (event.isActionUp) {
                    if (isExpanded) {
                        collapse()
                    } else {
                        expand()
                    }
                    return false
                }
                return false
            }

        })

        attachChild(elementList)
    }


    fun expand() {
        if (isExpanded) {
            return
        }

        hud.selected = null

        clearEntityModifiers()
        elementList.isVisible = true
        moveToX(0f, 0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }
        clearEntityModifiers()
        moveToX(-SELECTOR_WIDTH, 0.2f).after { elementList.isVisible = false }
    }


    //region Gameplay Events

    override fun onNoteHit(statistics: StatisticV2) {
        elements.fastForEach { it.onNoteHit(statistics) }
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        elements.fastForEach { it.onBreakStateChange(isBreak) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        elements.fastForEach { it.onGameplayUpdate(gameScene, secondsElapsed) }
    }

    override fun onGameplayTouchDown(time: Float) {
        elements.fastForEach { it.onGameplayTouchDown(time) }
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        elements.fastForEach { it.onHitObjectLifetimeStart(obj) }
    }

    override fun onAccuracyRegister(type: HitObjectType, accuracy: Float) {
        elements.fastForEach { it.onAccuracyRegister(type, accuracy) }
    }

    //endregion


    companion object {

        const val SELECTOR_WIDTH = 340f
        const val BUTTON_WIDTH = 48f
        const val BUTTON_RADIUS = 12f

    }

}

