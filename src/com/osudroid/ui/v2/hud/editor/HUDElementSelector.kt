package com.osudroid.ui.v2.hud.editor

import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.moveToX

import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Color4
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.reco1l.verktex.data.Dimension
import com.reco1l.framework.forEachTolerant
import com.osudroid.math.Vector2
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.reflect.full.primaryConstructor

class HUDElementSelector(private val hud: GameplayHUD) : UIContainer(), IGameplayEvents {


    /**
     * Whether the element selector is expanded.
     */
    val isExpanded
        get() = translationX.resolve(0f) >= 0f


    private val elements = HUDElements.entries.map { it.type.primaryConstructor!!.call() }

    private val elementList = UIScrollableContainer().apply {

        scrollAxes = Axis.Y
        height = Dimension.FillAvailable
        width = SELECTOR_WIDTH.px
        backgroundColor = Color4(0xFF1E1E2E)

        +UILinearContainer().apply {
            width = Dimension.FillAvailable
            padding = Padding(16f.px)
            spacing = 12f.px
            orientation = UILinearContainer.Orientation.Vertical

            elements.forEach { element ->
                +HUDElementPreview(element, hud)
            }
        }

    }


    init {
        height = Dimension.FillAvailable

        translationX = (-SELECTOR_WIDTH).px

        // The button to show/hide the element selector
        +UIClickableContainer().apply {
            backgroundColor = Color4(0xFF181825)
            radius = BUTTON_RADIUS.px

            width = BUTTON_WIDTH.px
            height = 150f.px
            x = (SELECTOR_WIDTH - BUTTON_RADIUS).px

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            +UIText().apply {
                rotationZ = -90f
                anchor = Anchor.Center
                origin = Anchor.Center
                fontSize = 14f.dip
                text = "Elements"

                x = (BUTTON_RADIUS / 2).px
            }

            onActionUp = {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }
            }
        }

        +elementList
    }


    fun expand() {
        if (isExpanded) {
            return
        }

        hud.selected = null

        clearTransforms()
        elementList.isVisible = true
        moveToX(0f, 0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }
        clearTransforms()
        moveToX(-SELECTOR_WIDTH, 0.2f).after { elementList.isVisible = false }
    }


    //region Gameplay Events

    override fun onNoteHit(statistics: StatisticV2) {
        elements.forEach { it.onNoteHit(statistics) }
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        elements.forEach { it.onBreakStateChange(isBreak) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        elements.forEach { it.onGameplayUpdate(gameScene, secondsElapsed) }
    }

    override fun onGameplayTouchDown(time: Float) {
        elements.forEach { it.onGameplayTouchDown(time) }
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        elements.forEach { it.onHitObjectLifetimeStart(obj) }
    }

    override fun onAccuracyRegister(type: HitObjectType, accuracy: Float) {
        elements.forEach { it.onAccuracyRegister(type, accuracy) }
    }

    override fun onAimJudgement(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float) {
        elements.forEach { it.onAimJudgement(objectPosition, cursorPosition, objectRadius) }
    }

    override fun onSeek() {
        elements.forEach { it.onSeek() }
    }

    //endregion


    companion object {

        const val SELECTOR_WIDTH = 340f
        const val BUTTON_WIDTH = 48f
        const val BUTTON_RADIUS = 12f

    }

}

