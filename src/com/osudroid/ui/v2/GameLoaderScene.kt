package com.osudroid.ui.v2

import com.edlplan.framework.easing.*
import com.osudroid.data.*
import com.osudroid.multiplayer.*
import com.osudroid.resources.R
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.shape.*
import com.reco1l.verktex.ui.sprite.*
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.vw
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.ui.sprite.ScaleType
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.shape.UIBox
import com.rian.andengine.modifier.ModifierType
import kotlin.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.helper.StringTable

class GameLoaderScene(private val gameScene: GameScene, private val beatmapInfo: BeatmapInfo, mods: ModHashMap, private val isRestart: Boolean) : Scene() {

    private var lastTimeTouched = System.currentTimeMillis()
    private var isStarting = false

    private val dimBox: UIBox
    private val mainContainer: UIContainer

    private val beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)
        ?: BeatmapOptions(beatmapInfo.setDirectory)

    private var fadeTimeout = if (isRestart) 500L else 2000L
    private var minimumTimeout = if (isRestart) 500L else 2000L

    init {
        Textures.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")

        sprite {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            scaleType = ScaleType.Crop
            textureRegion = Textures.getInstance().getTexture(if (Config.isSafeBeatmapBg()) "menu-background" else "::background")
        }

        dimBox = box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            color = Color4.Black
            alpha = 0.7f
        }

        mainContainer = fillContainer {
            orientation = Orientation.Horizontal
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleOrigin = Anchor.Center
            style = {
                padding = Engine.current.safeArea.copy(
                    y = 4f.srem,
                    w = 4f.srem + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)
                )
                spacing = 4f.srem
            }

            container {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                if (beatmapInfo.epilepsyWarning) {
                    compoundText {
                        leadingIcon = UIIcon(FAIcon.TriangleExclamation)
                        text = StringTable.get(R.string.epilepsy_warning)
                    }
                }

                linearContainer {
                    width = Dimension.FillAvailable
                    orientation = Orientation.Vertical
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = {
                        spacing = 2f.srem
                    }

                    text {
                        width = Dimension.FillAvailable
                        fontSize = FontSize.XL
                        wrapText = true
                        text = beatmapInfo.titleText
                        style = { color = it.accentColor }
                    }

                    text {
                        width = Dimension.FillAvailable
                        fontSize = FontSize.XL
                        wrapText = true
                        text = beatmapInfo.version
                        style = { color = it.accentColor }
                    }

                    text {
                        width = Dimension.FillAvailable
                        fontSize = FontSize.LG
                        wrapText = true
                        text = "by ${beatmapInfo.artistText}"
                        style = { color = it.accentColor * 0.9f }
                    }

                    if (mods.isNotEmpty()) {
                        +ModsIndicator().also { it.mods = mods.values }
                    }
                }

                linearContainer {
                    orientation = Orientation.Vertical
                    anchor = Anchor.BottomLeft
                    origin = Anchor.BottomLeft
                    style = {
                        spacing = 4f.srem
                    }

                    +UILoadingIndicator()

                    if (!Multiplayer.isMultiplayer) {
                        textButton {
                            text = "Back"
                            leadingIcon = UIIcon(FAIcon.ArrowLeft)
                            onActionUp = {
                                Textures.getInstance().getSound("click-short-confirm")?.play()
                                cancel()
                            }
                            onActionCancel = { Textures.getInstance().getSound("click-short")?.play() }
                        }
                    }
                }
            }

            +QuickSettingsLayout()
        }
    }

    /**
     * Cancels loading and goes back to the song menu.
     */
    fun cancel() {
        if (Multiplayer.isMultiplayer) {
            return
        }

        gameScene.cancelLoading()

        val global = GlobalManager.getInstance()
        val songMenu = global.songMenu
        val selectedBeatmap = songMenu.selectedBeatmap

        global.engine.scene = songMenu.scene

        if (selectedBeatmap != null) {
            songMenu.playMusic(selectedBeatmap.audioPath, selectedBeatmap.previewTime)
        }
    }


    override fun onAttached() {
        super.onAttached()

        mainContainer.paddingBottom = if (Multiplayer.isConnected) Multiplayer.roomScene!!.chat.buttonHeight + 12f else 0f
    }

    override fun onLoadComplete() {
        mainContainer.fadeIn(0.2f, Easing.OutCubic)
        mainContainer.scaleTo(1f, 0.2f, Easing.OutCubic)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isStarting) {

            if (gameScene.isReadyToStart) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (System.currentTimeMillis() - lastTimeTouched > minimumTimeout || Multiplayer.isMultiplayer) {
                    isStarting = true

                    // This is used instead of getBackgroundBrightness to directly obtain the
                    // updated value from the brightness slider.
                    val backgroundBrightness = Config.getInt("bgbrightness", 25)

                    mainContainer.fadeOut(0.1f, Easing.OutExpo)

                    dimBox.clearModifiers(ModifierType.Alpha)
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

        super.onManagedUpdate(deltaTimeSec)
    }


    private inner class QuickSettingsLayout : UIScrollableContainer() {

        init {
            height = Dimension.FillAvailable
            scrollAxes = Axis.Y
            alpha = 0.5f

            style = {
                width = 0.3f.vw
            }

            linearContainer {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                style = {
                    spacing = 4f.srem
                }

                collapsibleCard {
                    width = Dimension.FillAvailable
                    title = "Beatmap"

                    content.apply {
                        val offsetSlider = FormSlider().apply {
                            label = StringTable.get(R.string.opt_category_offset)
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
                            style = {
                                spacing = 3f.srem
                                padding = Vec4(2f.srem)
                            }

                            fun StepButton(step: Int) = textButton {
                                text = abs(step).toString()
                                leadingIcon = UIIcon(if (step >= 0) FAIcon.Plus else FAIcon.Minus)
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
                        collapse(true)
                    }
                }

                collapsibleCard {
                    width = Dimension.FillAvailable
                    title = "Settings"

                    content.apply {

                        +IntPreferenceSlider("bgbrightness", 25).apply {
                            label = StringTable.get(R.string.opt_bgbrightness_title)
                            control.min = 0f
                            control.max = 100f
                            control.onStopDragging = {
                                if (!isStarting) {
                                    dimBox.fadeTo(0.7f, 0.1f)
                                }
                            }
                            valueFormatter = { "${it.roundToInt()}%" }
                            onValueChanged = {
                                Config.setBackgroundBrightness(it / 100f)

                                // Storyboard and video should not be enabled if the background brightness is too low,
                                // so we trigger a reload when changing brightness.
                                gameScene.loadStoryboard(beatmapInfo)
                                gameScene.loadVideo(beatmapInfo)

                                if (!isStarting) {
                                    dimBox.alpha = 1f - it / 100f
                                }
                            }
                        }

                        +PreferenceCheckbox("enableStoryboard").apply {
                            label = StringTable.get(R.string.opt_enableStoryboard_title)
                            onValueChanged = {
                                gameScene.loadStoryboard(beatmapInfo)
                            }
                        }

                        +PreferenceCheckbox("enableVideo").apply {
                            label = StringTable.get(R.string.opt_video_title)
                            onValueChanged = {
                                gameScene.loadVideo(beatmapInfo)
                            }
                        }

                        +PreferenceCheckbox("showscoreboard").apply {
                            label = StringTable.get(R.string.opt_show_scoreboard_title)
                        }
                    }

                    onExpandStatusChange = {
                        settingsCardCollapsed = !it
                    }

                    if (settingsCardCollapsed) {
                        collapse(true)
                    }
                }
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            alpha = 1f
            lastTimeTouched = System.currentTimeMillis()

            // When the player is restarting, and they touch the layout, assume they want to change settings.
            // In that case, show this loading scene longer.
            if (isRestart) {
                fadeTimeout = 1500L
                minimumTimeout = 1500L
            }

            return super.onAreaTouched(event, localX, localY)
        }

        override fun onManagedUpdate(deltaTimeSec: Float) {

            val elapsed = System.currentTimeMillis() - lastTimeTouched

            if (alpha > 0.5f && elapsed > fadeTimeout) {
                alpha -= deltaTimeSec * 1.5f
            }

            super.onManagedUpdate(deltaTimeSec)
        }

    }

    companion object {
        private var beatmapCardCollapsed = false
        private var settingsCardCollapsed = false
    }
}