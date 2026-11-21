package com.osudroid.game.replay

import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.toFixed
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.GradientBox
import com.reco1l.verktex.ui.IconButton
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.control.SeekBar
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.form.FormSeekBar
import com.reco1l.verktex.ui.icon
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.ui.transitionSlot
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The controls of a replay (or autoplay), laid out as a video player's: a bar at the bottom with
 * play/pause, the seek bar, the speed and the options, and gestures on the rest of the screen.
 * Double tapping a half of the screen seeks 10 seconds back (left) or forward (right), further taps
 * keep seeking, and holding plays at 2x until released.
 *
 * The controls fade to 25% while the screen isn't touched.
 *
 * It fills the scene and takes every pointer event, gameplay doesn't read touches while replaying.
 */
class ReplayControls : Container() {

    /**
     * The rate gameplay should progress at: 2x while the screen is held, otherwise the chosen speed.
     */
    val rate
        get() = if (isHolding) HOLD_RATE else selectedRate

    /**
     * Whether playback was paused with the play/pause button.
     */
    var isPlaybackPaused = false
        private set

    /**
     * Called when the play/pause button is pressed, with `true` when pausing.
     */
    var onPauseToggle: ((Boolean) -> Unit)? = null

    /**
     * Called when the user seeks, with the time to seek to in seconds.
     */
    var onSeek: ((Float) -> Unit)? = null

    /**
     * Called when the background brightness was changed, from 0 to 1.
     */
    var onBackgroundBrightnessChanged: ((Float) -> Unit)? = null

    /**
     * The default background brightness, from 0 to 1. Setting it also sets the slider's value.
     */
    var defaultBackgroundBrightness = OsuSettings.backgroundBrightness
        set(value) {
            field = value
            brightnessSlider.defaultValue = value * 100
            brightnessSlider.value = value * 100
        }


    private var selectedRate = 1f

    // The engine's time, accumulated on update, gestures are timed with it.
    private var time = 0f

    // The seekable range and the position, in seconds.
    private var minSeconds = 0f
    private var maxSeconds = 0f
    private var currentSeconds = 0f
    private var displayedSecond = Int.MIN_VALUE
    private var isDraggingSeekBar = false


    //region Gestures state

    // The fingers whose press it took.
    private val ownedPointers = mutableSetOf<Int>()

    // The finger pressing the screen outside of the controls, others are ignored.
    private var gesturePointerId = -1
    private var gesturePressTime = 0f
    private var isHolding = false

    // When the last tap ended, and on which half.
    private var lastTapTime = -1f
    private var lastTapSide = 0

    // The seek streak started by a double tap: its side, its accumulated seconds, when its last tap was and
    // where it seeks to, which the session may not have reached yet.
    private var streakSide = 0
    private var streakSeconds = 0
    private var lastStreakTime = -1f
    private var streakTarget = 0f

    //endregion


    //region Idle state

    private var idleTime = 0f
    private var isIdle = false
    private val idleTransition = transitionSlot()

    //endregion


    private val playPauseButton = IconButton().apply {
        icon = FAIcon.Pause
        variant = Button.Variant.Ghost
        onActionUp = { togglePause() }
    }

    private val timeText = Text().apply {
        width = 104f.dip
        alignment = Anchor.Center
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        text = formatTime(0, 0)
    }

    private val seekBar = SeekBar().apply {
        width = fill()
        height = 12f.dip
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        min = 0f
        max = 0f

        onValueChange = {
            if (isDraggingSeekBar) {
                currentSeconds = it
                updateTimeText()
            }
        }
        onStartDragging = { isDraggingSeekBar = true }
        onStopDragging = {
            isDraggingSeekBar = false
            onSeek?.invoke(value)
        }
    }

    // Taller than the thin seek bar, so it is easy to press, presses anywhere in it go to the seek bar.
    private val seekBarTouchArea = object : Container() {
        override fun onInputEvent(event: InputEvent) = seekBar.onInputEvent(event)
    }.apply {
        width = fill()
        height = 40f.dip
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        seekBar.anchor = Anchor.CenterLeft
        +seekBar
    }

    private val speedButton: TextButton = TextButton().apply {
        text = formatRate(selectedRate)
        variant = Button.Variant.Ghost
        onActionUp = { togglePopup(speedPopup) }
    }

    private val optionsButton = IconButton().apply {
        icon = FAIcon.Sliders
        variant = Button.Variant.Ghost
        onActionUp = { togglePopup(optionsPopup) }
    }

    private val bar: LinearContainer = object : LinearContainer() {
        // Presses between the buttons are the bar's, they aren't screen gestures.
        override fun onInputEvent(event: InputEvent) =
            super.onInputEvent(event) || event is PointerEvent && event.action == PointerEvent.Action.Down
    }.apply {
        width = fill()
        anchor = Anchor.BottomCenter
        origin = Anchor.BottomCenter
        spacing = 8f.dip
        padding = Padding(16f.dip, 12f.dip)

        +playPauseButton
        +timeText
        +seekBarTouchArea
        +speedButton
        +optionsButton
    }

    private val speedSlider: FormSeekBar = FormSeekBar(selectedRate).apply {
        label = "Playback speed"
        control.min = MIN_RATE
        control.max = MAX_RATE
        valueFormatter = { formatRate(it) }
        onValueChanged = {
            selectedRate = it
            speedButton.text = formatRate(it)
        }
    }

    private val brightnessSlider = FormSeekBar(defaultBackgroundBrightness * 100).apply {
        label = Res.string.opt_bgbrightness_title
        control.min = 0f
        control.max = 100f
        valueFormatter = { "${it.roundToInt()}%" }
        onValueChanged = { onBackgroundBrightnessChanged?.invoke(it / 100f) }
    }

    private val speedPopup: ReplayPopup = ReplayPopup(speedSlider)
    private val optionsPopup: ReplayPopup = ReplayPopup(brightnessSlider)

    private val leftSeekFeedback = SeekFeedback(-1)
    private val rightSeekFeedback = SeekFeedback(1)

    private val holdIndicator = Container().apply {
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        y = 24f.dip
        padding = Padding(16f.dip, 8f.dip)
        radius = CornerRadius(999f.px)
        background = Background.Color(Color4(0f, 0f, 0f, 0.6f))
        alpha = 0f

        +LinearContainer().apply {
            spacing = 8f.dip

            text {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                text = "2x"
            }
            icon {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                iconCode = FAIcon.Forward
                color = Color4.White
            }
        }
    }

    private val holdTransition = holdIndicator.transitionSlot()


    init {
        width = fill()
        height = fill()

        +GradientBox().apply {
            width = fill()
            height = 160f.dip
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            colorStart = Color4.Transparent
            colorEnd = Color4(0f, 0f, 0f, 0.6f)
        }

        +leftSeekFeedback
        +rightSeekFeedback
        +holdIndicator
        +bar
        +speedPopup
        +optionsPopup
    }


    /**
     * Shows the playback position. Called by the session on each update.
     *
     * @param currentSeconds The position, in seconds.
     * @param minSeconds The earliest position to seek to (the first object's start time), in seconds.
     * @param maxSeconds The latest position to seek to (the last object's end time), in seconds.
     */
    fun updateSeekPosition(currentSeconds: Float, minSeconds: Float, maxSeconds: Float) {
        if (isDraggingSeekBar) {
            return
        }

        this.minSeconds = minSeconds
        this.maxSeconds = maxSeconds
        this.currentSeconds = currentSeconds

        seekBar.min = minSeconds
        seekBar.max = maxSeconds
        // Pinned to the start during the lead-in, whose time is still shown.
        seekBar.value = currentSeconds.coerceIn(minSeconds, maxSeconds.coerceAtLeast(minSeconds))
        updateTimeText()
    }

    private fun updateTimeText() {
        val second = (currentSeconds - minSeconds).toInt()
        if (second == displayedSecond) {
            return
        }
        displayedSecond = second
        timeText.text = formatTime(second, (maxSeconds - minSeconds).toInt().coerceAtLeast(0))
    }

    private fun togglePause() {
        isPlaybackPaused = !isPlaybackPaused
        playPauseButton.icon = if (isPlaybackPaused) FAIcon.Play else FAIcon.Pause
        onPauseToggle?.invoke(isPlaybackPaused)
    }


    //region Popups

    private val openPopup
        get() = when {
            speedPopup.isOpen -> speedPopup
            optionsPopup.isOpen -> optionsPopup
            else -> null
        }

    private fun togglePopup(popup: ReplayPopup) {
        if (popup.isOpen) {
            popup.close()
            return
        }
        openPopup?.close()
        popup.open()
    }

    //endregion


    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        if (event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        wake()

        if (event.action != PointerEvent.Action.Down) {
            // Only the fingers it took the press of, the others' moves and releases belong elsewhere.
            if (event.pointerId !in ownedPointers) {
                return false
            }
            if (event.action != PointerEvent.Action.Move) {
                ownedPointers -= event.pointerId
            }
        }

        if (event.action == PointerEvent.Action.Down) {
            ownedPointers += event.pointerId
            val popup = openPopup

            // A press outside of the open popup closes it, unless it's on the bar, whose buttons toggle them.
            if (popup != null && !popup.containsScenePoint(event.x, event.y) && !bar.containsScenePoint(event.x, event.y)) {
                popup.close()
                return true
            }

            if (super.onInputEvent(event)) {
                return true
            }

            if (gesturePointerId == -1) {
                gesturePointerId = event.pointerId
                gesturePressTime = 0f
            }
            return true
        }

        if (event.pointerId == gesturePointerId) {
            when (event.action) {
                PointerEvent.Action.Up -> {
                    gesturePointerId = -1
                    if (isHolding) stopHold() else onTap(event.x, event.y)
                }
                PointerEvent.Action.Cancel, PointerEvent.Action.Unknown -> {
                    gesturePointerId = -1
                    stopHold()
                }
                else -> Unit
            }
            return true
        }

        super.onInputEvent(event)
        return true
    }

    private fun onTap(sceneX: Float, sceneY: Float) {
        val side = if (toLocalSpaceFromScene(sceneX, sceneY).x < bounds.width / 2f) -1 else 1

        // Once a double tap seeked, each further tap seeks again.
        val continuesStreak = streakSide != 0 && time - lastStreakTime <= STREAK_WINDOW
        val isDoubleTap = lastTapTime >= 0f && time - lastTapTime <= DOUBLE_TAP_WINDOW && side == lastTapSide

        lastTapTime = time
        lastTapSide = side

        if (continuesStreak || isDoubleTap) {
            seekBy(side)
            // A double tap doesn't start a new one with its next tap.
            lastTapTime = -1f
        }
    }

    private fun seekBy(side: Int) {
        val isSameStreak = streakSide == side && time - lastStreakTime <= STREAK_WINDOW

        if (!isSameStreak) {
            streakSide = side
            streakSeconds = 0
            streakTarget = currentSeconds
        }

        streakSeconds += SEEK_SECONDS
        streakTarget = (streakTarget + side * SEEK_SECONDS).coerceIn(minSeconds, maxSeconds.coerceAtLeast(minSeconds))
        lastStreakTime = time

        val feedback = if (side < 0) leftSeekFeedback else rightSeekFeedback
        (if (side < 0) rightSeekFeedback else leftSeekFeedback).dismiss()
        feedback.show(streakSeconds)

        onSeek?.invoke(streakTarget)
    }

    private fun startHold() {
        isHolding = true
        holdTransition.run { fadeIn(0.15f) }
    }

    private fun stopHold() {
        if (!isHolding) {
            return
        }
        isHolding = false
        holdTransition.run { fadeOut(0.2f) }
    }

    private fun wake() {
        idleTime = 0f

        if (isIdle) {
            isIdle = false
            idleTransition.run { fadeTo(1f, 0.15f) }
        }
    }

    //endregion


    override fun onUpdate(clock: FrameClock) {
        val elapsed = clock.elapsedFrameTime
        time += elapsed

        if (gesturePointerId != -1 && !isHolding) {
            gesturePressTime += elapsed

            if (gesturePressTime >= HOLD_DELAY) {
                startHold()
                // A tap that became a hold doesn't count towards a double tap.
                lastTapTime = -1f
            }
        }

        if (streakSide != 0 && time - lastStreakTime > STREAK_WINDOW) {
            streakSide = 0
            leftSeekFeedback.dismiss()
            rightSeekFeedback.dismiss()
        }

        val isEngaged = gesturePointerId != -1 || isDraggingSeekBar || openPopup != null
        if (isEngaged) {
            idleTime = 0f
        } else {
            idleTime += elapsed
        }

        if (!isIdle && idleTime >= IDLE_DELAY) {
            isIdle = true
            idleTransition.run { fadeTo(IDLE_ALPHA, 0.4f) }
        }

        super.onUpdate(clock)
    }


    /**
     * A popup above the bar, at its right, holding one control.
     */
    private inner class ReplayPopup(content: Container) : Container() {

        var isOpen = false
            private set

        private val visibility = transitionSlot()

        init {
            width = 320f.dip
            anchor = Anchor.BottomRight
            origin = Anchor.BottomRight
            x = (-16f).dip
            padding = Padding(16f.dip, 12f.dip)
            background = Background.Color(themed { palette.surface })
            radius = themed { CornerRadius(radius.lg) }
            border = themed { cardStyle.elevation.border }
            shadow = themed { cardStyle.elevation.shadow }

            isVisible = false
            alpha = 0f
            scale = HIDDEN_SCALE
            scaleOrigin = Anchor.BottomRight

            content.width = fill()
            +content
        }

        fun open() {
            if (isOpen) return
            isOpen = true
            isVisible = true

            visibility.run {
                fadeIn(0.15f, Easing.OutQuint)
                scaleTo(1f, 0.15f, Easing.OutQuint)
            }
        }

        fun close() {
            if (!isOpen) return
            isOpen = false

            visibility.run {
                scaleTo(HIDDEN_SCALE, 0.15f, Easing.OutQuint)
                fadeOut(0.15f, Easing.OutQuint).after { if (!isOpen) isVisible = false }
            }
        }

        override fun onUpdate(clock: FrameClock) {
            // Above the bar, whose height is only known once laid out.
            y = (-(bar.bounds.height + 4f.dip.toPixels())).px
            super.onUpdate(clock)
        }

        // Presses on the popup are its own.
        override fun onInputEvent(event: InputEvent): Boolean {
            if (!isOpen) return false
            return super.onInputEvent(event) || event is PointerEvent && event.action == PointerEvent.Action.Down
        }
    }

    /**
     * Shows on a side how many seconds a seek streak seeked.
     */
    private class SeekFeedback(private val side: Int) : Container() {

        private val label: Text
        private var isShown = false
        private val visibility = transitionSlot()

        init {
            anchor = if (side < 0) Anchor.CenterLeft else Anchor.CenterRight
            origin = Anchor.Center
            x = (if (side < 0) 160f else -160f).dip
            width = 140f.dip
            height = 140f.dip
            radius = CornerRadius(70f.dip)
            background = Background.Color(Color4(1f, 1f, 1f, 0.15f))
            alpha = 0f

            val column = LinearContainer().apply {
                orientation = LinearContainer.Orientation.Vertical
                anchor = Anchor.Center
                origin = Anchor.Center
                spacing = 6f.dip
            }

            column.icon {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                iconCode = if (side < 0) FAIcon.Backward else FAIcon.Forward
                color = Color4.White
            }
            label = column.text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
            }
            +column
        }

        fun show(seconds: Int) {
            label.text = (if (side < 0) "-" else "+") + "${seconds}s"

            if (!isShown) {
                isShown = true
                visibility.run { fadeIn(0.1f) }
            }
        }

        fun dismiss() {
            if (!isShown) return
            isShown = false
            visibility.run { fadeOut(0.3f) }
        }
    }


    companion object {
        private const val MIN_RATE = 0.05f
        private const val MAX_RATE = 2f
        private const val HOLD_RATE = 2f

        private const val SEEK_SECONDS = 10

        // How long a press must be held to play at 2x, in seconds.
        private const val HOLD_DELAY = 0.4f
        // How soon after a tap another one is a double tap, in seconds.
        private const val DOUBLE_TAP_WINDOW = 0.3f
        // How soon after a seek another tap seeks again, in seconds.
        private const val STREAK_WINDOW = 0.6f

        // How long without touches the controls fade, in seconds, and their opacity once faded.
        private const val IDLE_DELAY = 2.5f
        private const val IDLE_ALPHA = 0.25f

        private const val HIDDEN_SCALE = 0.95f

        private fun formatRate(rate: Float) = rate.toFixed(2) + "x"

        private fun formatTime(seconds: Int, duration: Int): String {
            fun format(value: Int): String {
                val absolute = abs(value)
                return (if (value < 0) "-" else "") + "${absolute / 60}:" + (absolute % 60).toString().padStart(2, '0')
            }
            return "${format(seconds)} / ${format(duration)}"
        }
    }
}
