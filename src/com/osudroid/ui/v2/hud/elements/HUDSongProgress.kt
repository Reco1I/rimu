package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState

import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.data.Color4
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import ru.nsu.ccfit.zuev.osu.game.GameScene
import kotlin.math.roundToInt

sealed class HUDSongProgress : HUDElement() {
    abstract fun setProgress(progress: Float, isIntro: Boolean)

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        if (gameScene.elapsedTime < gameScene.firstObjectStartTime) {
            setProgress((gameScene.elapsedTime - gameScene.initialStartTime) / (gameScene.firstObjectStartTime - gameScene.initialStartTime), true)
        } else {
            setProgress((gameScene.elapsedTime - gameScene.firstObjectStartTime) / (gameScene.lastObjectEndTime - gameScene.firstObjectStartTime), false)
        }
    }
}

class HUDLinearSongProgress : HUDSongProgress() {

    private val backgroundRect = UIBox().apply {
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        width = Dimension.FillAvailable
        height = BAR_HEIGHT.px
        backgroundColor = Color4.Black
        alpha = 0.3f
    }

    private val progressRect = UIBox().apply {
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        width = (0f).px
        height = (BAR_HEIGHT).px
        alpha = 0.4f
    }


    init {
        // Adding a 20epx padding so the user can grab the progress bar more easily.
        width = Dimension.FillAvailable
        height = (BAR_HEIGHT + 20f).px
        +backgroundRect
        +progressRect
    }


    override fun setProgress(progress: Float, isIntro: Boolean) {

        if (isIntro) {
            progressRect.backgroundColor = Color4(153f / 255f, 204f / 255f, 51f / 255f)
        } else {
            progressRect.backgroundColor = Color4(1f, 1f, 150f / 255f)
        }

        progressRect.width = (bounds.width * progress).px
    }


    companion object {
        private const val BAR_HEIGHT = 7f
    }

}

class HUDPieSongProgress : HUDSongProgress() {

    private val circularProgress: UICircle


    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent

        // Reference: https://github.com/ppy/osu/blob/6455c0583b5e607baeca7f584410bc63515aa619/osu.Game/Skinning/LegacySongProgress.cs

        UICircle().also { clear ->

            clear.width = 30f.px
            clear.height = 30f.px
            clear.anchor = Anchor.Center
            clear.origin = Anchor.Center
            clear.color = Color4.Transparent
            clear.clearDepthBeforeDraw = true
            clear.depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)

            +clear
        }

        UICircle().also { background ->

            background.width = 33f.px
            background.height = 33f.px
            background.anchor = Anchor.Center
            background.origin = Anchor.Center
            background.color = Color4.White
            background.depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)

            +background
        }

        circularProgress = UICircle().also { progress ->

            progress.width = 30f.px
            progress.height = 30f.px
            progress.anchor = Anchor.Center
            progress.origin = Anchor.Center
            progress.alpha = 0.6f

            +progress
        }


        UICircle().also { dot ->

            dot.width = 4f.px
            dot.height = 4f.px
            dot.anchor = Anchor.Center
            dot.origin = Anchor.Center
            dot.color = Color4.White

            +dot
        }
    }


    override fun setProgress(progress: Float, isIntro: Boolean) {
        // Snapping the progress to fixed steps lets UICircle skip rebuilding its vertex buffer on frames where the
        // visible arc would not change.
        val snappedProgress = (progress * PROGRESS_STEPS).roundToInt() / PROGRESS_STEPS

        if (isIntro) {
            circularProgress.color = Color4(199f / 255f, 1f, 47f / 255f, circularProgress.alpha)
            circularProgress.setPortion(-1f + snappedProgress)
        } else {
            circularProgress.color = Color4(1f, 1f, 1f, circularProgress.alpha)
            circularProgress.setPortion(snappedProgress)
        }
    }

    companion object {
        /**
         * The number of steps a full circle is divided into (0.5 degrees per step).
         */
        private const val PROGRESS_STEPS = 720f
    }

}