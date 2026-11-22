package com.osudroid.ui.v2

import com.osudroid.game.Gameplay
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
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
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.card
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.delay
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import kotlin.math.*
import com.reco1l.verktex.ui.themed
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.localization.formatString
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.Button.Density
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.icon

class GameLoaderScene(private val gameplay: Gameplay, private val beatmapInfo: BeatmapInfo, mods: ModHashMap, private val isRestart: Boolean) : Scene() {

    // Back cancels the play, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            cancel()
            return true
        }
        return false
    }

    private var lastTimeTouched = currentTimeMillis()
    private var isStarting = false
    private var multiplayerLoadElapsed = 0f

    private val mainContainer: Container

    // The options of the beatmap set, read from the database once the scene opens. Gameplay starts
    // once they're read, with their offset.
    private var beatmapOptions = BeatmapOptions(beatmapInfo.setDirectory)
    private var isBeatmapOptionsLoaded = false

    // Shows the offset of the options once they're read.
    private var offsetSlider: FormSeekBar? = null

    private var fadeTimeout = if (isRestart) 500L else 2000L
    private var minimumTimeout = if (isRestart) 500L else 2000L

    init {
        mainContainer = container {
            width = fill()
            height = fill()
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            scaleOrigin = Anchor.Center
            padding = Padding(16f.dip)

            container {
                width = fill()
                height = fill()

                if (beatmapInfo.epilepsyWarning) {
                    linearContainer {
                        spacing = 12f.dip
                        icon {
                            iconCode = FAIcon.TriangleExclamation
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                        }
                        text {
                            text = Res.string.epilepsy_warning
                        }
                    }
                }

                linearContainer {
                    width = fill()
                    orientation = Orientation.Vertical
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 8f.dip

                    text {
                        width = fill()
                        wrapText = true
                        text = beatmapInfo.titleText
                        fontSettings = themed { typography.h1 }
                        color = themed { palette.accent }
                    }

                    text {
                        width = fill()
                        wrapText = true
                        text = beatmapInfo.version
                        fontSettings = themed { typography.h3 }
                        color = themed { palette.accent }
                    }

                    text {
                        width = fill()
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
                    orientation = Orientation.Vertical
                    anchor = Anchor.BottomLeft
                    origin = Anchor.BottomLeft
                    spacing = 16f.dip

                    +LoadingIndicator()

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
        gameplay.cancelLoading()

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

        BeatmapBackground.fadeBrightness(LOADING_BRIGHTNESS)

        mainContainer.fadeIn(0.2f, Easing.OutCubic)
        mainContainer.scaleTo(1f, 0.2f, Easing.OutCubic)
    }

    init {
        async {
            val options = DatabaseManager.beatmapOptionsTable.getOptions(beatmapInfo.setDirectory)

            updateThread {
                if (options != null) {
                    beatmapOptions = options
                    offsetSlider?.apply {
                        value = options.offset.toFloat()
                        defaultValue = options.offset.toFloat()
                    }
                }
                isBeatmapOptionsLoaded = true
            }
        }
    }

    override fun onUpdate(clock: FrameClock) {

        if (!isStarting) {

            if (Multiplayer.isMultiplayer && !gameplay.isReadyToStart) {
                multiplayerLoadElapsed += clock.elapsedFrameTime

                if (multiplayerLoadElapsed >= MULTIPLAYER_LOAD_TIMEOUT_SEC) {
                    Toasts.showLong("You have been moved back as you took too long to load the beatmap.")

                    cancel()
                    return
                }
            }

            if (gameplay.isReadyToStart && isBeatmapOptionsLoaded) {

                // Multiplayer will skip the minimum timeout if it's ready to start.
                if (currentTimeMillis() - lastTimeTouched > minimumTimeout || Multiplayer.isMultiplayer) {
                    isStarting = true

                    val backgroundBrightness = Settings[OsuSettings.BackgroundBrightness]

                    mainContainer.fadeOut(0.1f, Easing.OutExpo)

                    BeatmapBackground.fadeBrightness(backgroundBrightness / 100f, START_DURATION)

                    // Timed by the scene, gameplay changing the background mustn't keep it from starting.
                    delay(START_DURATION).after {

                        gameplay.session!!.hudLayer.apply {
                            alpha = 0f
                            scaleX = 0.9f
                            scaleY = 0.9f
                            scaleOrigin = Anchor.Center

                            scaleTo(1f, 0.2f, Easing.OutCubic)
                            fadeIn(0.1f, Easing.OutExpo)
                        }

                        gameplay.begin(beatmapOptions.offset)
                    }
                }

            } else {
                lastTimeTouched = currentTimeMillis()
            }
        }

        super.onUpdate(clock)
    }


    private inner class QuickSettingsLayout : ScrollableContainer() {

        init {
            width = wrap()
            height = fill()
            maxWidth = fill(0.35f)
            anchor = Anchor.TopRight
            origin = Anchor.TopRight
            scrollAxes = Axis.Y
            alpha = 0.5f

            linearContainer {
                width = fill()
                orientation = Orientation.Vertical
                spacing = 16f.dip

                card {
                    width = fill()
                    title = "Beatmap"
                    isCollapsible = true

                    content.apply {
                        val offsetSeekbar = FormSeekBar().apply {
                            label = Res.string.opt_category_offset
                            control.min = -250f
                            control.max = 250f
                            value = beatmapOptions.offset.toFloat()
                            defaultValue = beatmapOptions.offset.toFloat()
                            valueFormatter = { "${it.roundToInt()}ms" }

                            onValueChanged = {
                                // Not before the options are read, which would replace them.
                                if (isBeatmapOptionsLoaded) {
                                    beatmapOptions.offset = it.roundToInt()
                                    val options = beatmapOptions
                                    async { DatabaseManager.beatmapOptionsTable.upsert(options) }
                                }
                            }
                        }
                        +offsetSeekbar
                        this@GameLoaderScene.offsetSlider = offsetSeekbar

                        linearContainer {
                            anchor = Anchor.TopCenter
                            origin = Anchor.TopCenter
                            spacing = 12f.dip
                            padding = com.reco1l.verktex.data.Padding(8f.dip)

                            fun stepButton(step: Int) = textButton {
                                text = abs(step).toString()
                                density = Density.Small
                                leadingIcon = faSolidIcon(if (step >= 0) FAIcon.Plus else FAIcon.Minus)
                                onActionUp = {
                                    offsetSeekbar.value += step
                                }
                            }

                            stepButton(-5)
                            stepButton(-1)
                            stepButton(1)
                            stepButton(5)
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
                    width = fill()
                    title = "Settings"
                    isCollapsible = true

                    content.apply {

                        +FloatPreferenceSlider(OsuSettings.BackgroundBrightness).apply {
                            label = Res.string.opt_bgbrightness_title
                            control.min = 0f
                            control.max = 100f
                            control.step = 0.1f
                            control.precision = 1
                            control.onStopDragging = {
                                if (!isStarting) {
                                    BeatmapBackground.fadeBrightness(LOADING_BRIGHTNESS, 0.1f)
                                }
                            }
                            valueFormatter = { formatString("%.1f%%", it) }
                            onValueChanged = {
                                // Storyboard and video should not be enabled if the background brightness is too low,
                                // so we trigger a reload when changing brightness.
                                gameplay.session?.loadVideo()

                                if (!isStarting) {
                                    BeatmapBackground.setBrightness(it / 100f)
                                }
                            }
                        }

                        // TODO: Add Storyboard support.
                        /*+PreferenceCheckbox(OsuSettings.EnableStoryboard).apply {
                            label = Res.string.opt_enableStoryboard_title
                            onValueChanged = {
                            }
                        }*/

                        +PreferenceCheckbox(OsuSettings.EnableVideo).apply {
                            label = Res.string.opt_video_title
                            onValueChanged = {
                                gameplay.session?.loadVideo()
                            }
                        }

                        +PreferenceCheckbox(OsuSettings.ShowScoreboard).apply {
                            label = Res.string.opt_show_scoreboard_title
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
            lastTimeTouched = currentTimeMillis()

            // When the player is restarting, and they touch the layout, assume they want to change settings.
            // In that case, show this loading scene longer.
            if (isRestart) {
                fadeTimeout = 1500L
                minimumTimeout = 1500L
            }

            return super.onInputEvent(event)
        }

        override fun onUpdate(clock: FrameClock) {

            val elapsed = currentTimeMillis() - lastTimeTouched

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

        // How bright the background is behind the loader, and how long it takes to fade to the
        // gameplay's brightness once it starts.
        private const val LOADING_BRIGHTNESS = 0.3f
        private const val START_DURATION = 0.2f
    }
}