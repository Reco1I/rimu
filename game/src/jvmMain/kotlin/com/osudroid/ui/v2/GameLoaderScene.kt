package com.osudroid.ui.v2

import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.ui.text.spans
import com.osudroid.data.*
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.resources.Res
import com.osudroid.ui.v2.settings.FloatPreferenceSlider
import com.osudroid.ui.v2.settings.PreferenceCheckbox
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.card
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import kotlin.math.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import com.reco1l.verktex.ui.themed

class GameLoaderScene(private val gameScene: GameScene, private val beatmapInfo: BeatmapInfo, mods: ModHashMap, private val isRestart: Boolean) : UIScene() {

    private var lastTimeTouched = System.currentTimeMillis()
    private var isStarting = false
    private var multiplayerLoadElapsed = 0f

    private val dimBox: UIBox
    private val mainContainer: UIContainer

    private val beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)
        ?: BeatmapOptions(beatmapInfo.setDirectory)

    private var fadeTimeout = if (isRestart) 500L else 2000L
    private var minimumTimeout = if (isRestart) 500L else 2000L

    init {
        background = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
                textureRegion = if (Config.isSafeBeatmapBg()) SkinTextures["menu-background"] else Textures["::background"]
            }

            dimBox = box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                backgroundColor = Color4.Black
                alpha = 0.7f
            }
        }

        mainContainer = container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleOrigin = Anchor.Center
            padding = com.reco1l.verktex.data.Padding(
                top = 16f.dip,
                bottom = (16f.dip.toPixels() + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)).px
            )

            container {
                width = 0.67f.pct
                height = Dimension.FillAvailable

                if (beatmapInfo.epilepsyWarning) {
                    text {
                        spans {
                            icon(FAIcon.TriangleExclamation)
                            space(8f.dip)
                            +StringTable.get(Res.string.epilepsy_warning)
                        }
                    }
                }

                linearContainer {
                    width = Dimension.FillAvailable
                    orientation = UILinearContainer.Orientation.Vertical
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 8f.dip

                    text {
                        width = Dimension.FillAvailable
                        wrapText = true
                        text = beatmapInfo.titleText
                        fontSize = themed { typography.h2.size }
                        color = themed { palette.accent }
                    }

                    text {
                        width = Dimension.FillAvailable
                        wrapText = true
                        text = beatmapInfo.version
                        fontSize = themed { typography.h2.size }
                        color = themed { palette.accent }
                    }

                    text {
                        width = Dimension.FillAvailable
                        wrapText = true
                        text = "by ${beatmapInfo.artistText}"
                        fontSize = themed { typography.h3.size }
                        color = themed { palette.accent.shade(0.9f) }
                    }

                    if (mods.isNotEmpty()) {
                        +ModsIndicator().also {
                            // Next to the headings, the default size of the theme's body text is too small.
                            it.iconSize = 32f.dip
                            it.mods = mods.values
                        }
                    }
                }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    anchor = Anchor.BottomLeft
                    origin = Anchor.BottomLeft
                    spacing = 16f.dip

                    +UILoadingIndicator()

                    textButton {
                        text = "Back"
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        onActionUp = {
                            SkinSounds["click-short-confirm"].play()
                            cancel()
                        }
                        onActionCancel = { SkinSounds["click-short"].play() }
                    }
                }
            }

            +QuickSettingsLayout()
        }
    }

    /**
     * Cancels loading and goes back to the song menu, or to the multiplayer room if in multiplayer.
     */
    fun cancel() {
        gameScene.cancelLoading()

        if (Multiplayer.isMultiplayer) {
            // Inform that the player has canceled loading.
            RoomAPI.setPlayerStatus(PlayerStatus.NotReady)
            Verktex.withTreeLock { Verktex.popScene() }
            Multiplayer.roomScene?.show()
            return
        }

        Verktex.withTreeLock { Verktex.popScene() }
    }


    // Scenes are never attached to a parent, onAttached doesn't run for them.
    override fun onEnter() {
        super.onEnter()

        mainContainer.fadeIn(0.2f, Easing.OutCubic)
        mainContainer.scaleTo(1f, 0.2f, Easing.OutCubic)
    }

    override fun onUpdate(clock: FrameClock) {

        if (!isStarting) {

            if (Multiplayer.isMultiplayer && !gameScene.isReadyToStart) {
                multiplayerLoadElapsed += clock.elapsedFrameTime

                if (multiplayerLoadElapsed >= MULTIPLAYER_LOAD_TIMEOUT_SEC) {
                    Toasts.showLong("You have been moved back as you took too long to load the beatmap.")

                    cancel()
                    return
                }
            }

            if (gameScene.isReadyToStart) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (System.currentTimeMillis() - lastTimeTouched > minimumTimeout || Multiplayer.isMultiplayer) {
                    isStarting = true

                    // This is used instead of getBackgroundBrightness to directly obtain the
                    // updated value from the brightness slider.
                    val backgroundBrightness = Config.getNumber("bgbrightness", 25f)

                    mainContainer.fadeOut(0.1f, Easing.OutExpo)

                    dimBox.clearTransforms()
                    dimBox.fadeTo(1f - backgroundBrightness / 100f, 0.2f).after {

                        gameScene.hud.apply {
                            alpha = 0f
                            scaleX = 0.9f
                            scaleY = 0.9f
                            scaleOrigin = Anchor.Center

                            scaleTo(1f, 0.2f, Easing.OutCubic)
                            fadeIn(0.1f, Easing.OutExpo)
                        }

                        gameScene.start()
                    }
                }

            } else {
                lastTimeTouched = System.currentTimeMillis()
            }
        }

        super.onUpdate(clock)
    }


    private inner class QuickSettingsLayout : UIScrollableContainer() {

        init {
            width = 0.3f.pct
            height = Dimension.FillAvailable
            anchor = Anchor.TopRight
            origin = Anchor.TopRight
            scrollAxes = Axis.Y
            alpha = 0.5f

            linearContainer {
                width = Dimension.FillAvailable
                orientation = UILinearContainer.Orientation.Vertical
                spacing = 16f.dip

                card {
                    width = Dimension.FillAvailable
                    title = "Beatmap"
                    isCollapsible = true

                    content.apply {
                        val offsetSlider = FormSlider().apply {
                            label = StringTable.get(Res.string.opt_category_offset)
                            control.min = -250f
                            control.max = 250f
                            value = beatmapOptions.offset.toFloat()
                            defaultValue = beatmapOptions.offset.toFloat()
                            valueFormatter = { "${it.roundToInt()}ms" }

                            onValueChanged = {
                                beatmapOptions.offset = it.roundToInt()
                                DatabaseManager.beatmapOptionsTable.upsert(beatmapOptions)
                            }
                        }
                        +offsetSlider

                        linearContainer {
                            anchor = Anchor.TopCenter
                            origin = Anchor.TopCenter
                            spacing = 12f.dip
                            padding = com.reco1l.verktex.data.Padding(8f.dip)

                            fun StepButton(step: Int) = textButton {
                                text = abs(step).toString()
                                leadingIcon = faSolidIcon(if (step >= 0) FAIcon.Plus else FAIcon.Minus)
                                onActionUp = {
                                    offsetSlider.value += step
                                }
                            }

                            StepButton(-5)
                            StepButton(-1)
                            StepButton(1)
                            StepButton(5)
                        }
                    }

                    onExpandStatusChange = {
                        beatmapCardCollapsed = !it
                    }

                    if (beatmapCardCollapsed) {
                        collapse()
                    }
                }

                card {
                    width = Dimension.FillAvailable
                    title = "Settings"
                    isCollapsible = true

                    content.apply {

                        +FloatPreferenceSlider("bgbrightness", 25f).apply {
                            label = StringTable.get(Res.string.opt_bgbrightness_title)
                            control.min = 0f
                            control.max = 100f
                            control.step = 0.1f
                            control.precision = 1
                            control.onStopDragging = {
                                if (!isStarting) {
                                    dimBox.fadeTo(0.7f, 0.1f)
                                }
                            }
                            valueFormatter = { "%.1f%%".format(it) }
                            onValueChanged = {
                                Config.setBackgroundBrightness(it / 100f)

                                // Storyboard and video should not be enabled if the background brightness is too low,
                                // so we trigger a reload when changing brightness.
                                gameScene.loadVideo(beatmapInfo)

                                if (!isStarting) {
                                    dimBox.alpha = 1f - it / 100f
                                }
                            }
                        }

                        +PreferenceCheckbox("enableStoryboard").apply {
                            label = StringTable.get(Res.string.opt_enableStoryboard_title)
                            onValueChanged = {
                            }
                        }

                        +PreferenceCheckbox("enableVideo").apply {
                            label = StringTable.get(Res.string.opt_video_title)
                            onValueChanged = {
                                gameScene.loadVideo(beatmapInfo)
                            }
                        }

                        +PreferenceCheckbox("showscoreboard").apply {
                            label = StringTable.get(Res.string.opt_show_scoreboard_title)
                        }
                    }

                    onExpandStatusChange = {
                        settingsCardCollapsed = !it
                    }

                    if (settingsCardCollapsed) {
                        collapse()
                    }
                }
            }
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            alpha = 1f
            lastTimeTouched = System.currentTimeMillis()

            // When the player is restarting, and they touch the layout, assume they want to change settings.
            // In that case, show this loading scene longer.
            if (isRestart) {
                fadeTimeout = 1500L
                minimumTimeout = 1500L
            }

            return super.onInputEvent(event)
        }

        override fun onUpdate(clock: FrameClock) {

            val elapsed = System.currentTimeMillis() - lastTimeTouched

            if (alpha > 0.5f && elapsed > fadeTimeout) {
                alpha -= clock.elapsedFrameTime * 1.5f
            }

            super.onUpdate(clock)
        }

    }

    companion object {
        private var beatmapCardCollapsed = false
        private var settingsCardCollapsed = false

        private const val MULTIPLAYER_LOAD_TIMEOUT_SEC = 60f
    }
}