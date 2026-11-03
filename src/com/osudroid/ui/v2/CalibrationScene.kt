package com.osudroid.ui.v2

import android.text.InputType
import androidx.annotation.IdRes
import com.osudroid.beatmaps.DroidHitWindow
import com.osudroid.math.Interpolation
import com.osudroid.resources.R.string
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.utils.mainThread
import com.osudroid.utils.median
import com.osudroid.utils.standardDeviation
import com.osudroid.utils.updateThread
import com.reco1l.osu.ui.PromptDialog
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UICheckbox
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.shape.circle
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable

object CalibrationScene : UIScene() {
    internal var OFFSET_MIN = -500
    internal var OFFSET_MAX = 500

    private const val APPROACH_SCALE_START = 3f
    private const val CIRCLE_DIAMETER = 130f
    private const val STEP_MS = 1
    private const val STEP_MS_LONG = 10
    private const val STEP_BPM = 1
    private const val STEP_BPM_LONG = 10
    private const val MAX_TAP_SAMPLES = 20

    private var currentBpm = 60

    private val beatInterval
        get() = 60f / currentBpm

    private var beatTimer = 0f
    private var metronomeTime = 0.0
    private var pendingOffset = 0

    private val judgementHitWindow = DroidHitWindow(10f)
    private val tapOffsets = mutableListOf<Double>()
    private var wasMusicPlaying = false
    private var successfulTapStreak = 0

    /**
     * Invoked on the main thread after the overlay closes (Back or SET).
     * Set by the caller so that the current settings can re-show itself (e.g. Settings → Audio).
     */
    internal var settingsFragment: SettingsFragment? = null

    private lateinit var approachCircle: UIBox
    private lateinit var hitCircleFill: UICircle
    private lateinit var rippleCircle: UIBox
    private var circleContainer: UIContainer
    private lateinit var bpmValueText: UITextButton
    private lateinit var offsetValueText: UITextButton
    private lateinit var tapFeedbackText: UIText
    private lateinit var judgementText: UIText
    private lateinit var streakText: UIText
    private lateinit var highPrecisionToggle: UICheckbox

    init {
        // Background
        background = UIBox().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            backgroundColor = Verktex.theme.palette.accent * 0.08f
        }

        textButton {
            anchor = Anchor.TopLeft
            origin = Anchor.TopLeft
            translationX = (60f).px
            translationY = (12f).px
            text = "Back"
            leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
            onActionUp = {
                playShortClickConfirmSound()
                back()
            }
            onActionCancel = { playShortClickSound() }
        }

        circleContainer = object : UIContainer() {
            override fun onInputEvent(event: InputEvent): Boolean {
                if (event !is PointerEvent || event.action != PointerEvent.Action.Down) {
                    return false
                }

                if (!containsScenePoint(event.x, event.y)) {
                    return false
                }

                onPlayfieldTap()
                return true
            }
        }.apply {
            width = CIRCLE_DIAMETER.px
            height = CIRCLE_DIAMETER.px
            anchor = Anchor.Center
            origin = Anchor.Center

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                style = Style { theme -> color = theme.palette.accent * 0.35f }
                hitCircleFill = this
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                radius = (CIRCLE_DIAMETER / 2f).px
                borderWidth = 8f.px
                style = Style { theme -> borderColor = theme.palette.accent }
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                radius = (CIRCLE_DIAMETER / 2f).px
                borderWidth = 6f.px
                scaleOrigin = Anchor.Center
                scale = APPROACH_SCALE_START
                style = Style { theme -> borderColor = theme.palette.accent * 0.85f }
                approachCircle = this
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                radius = (CIRCLE_DIAMETER / 2f).px
                borderWidth = 6f.px
                scaleOrigin = Anchor.Center
                alpha = 0f
                style = Style { theme -> borderColor = theme.palette.accent }
                rippleCircle = this
            }
        }

        +circleContainer

        text {
            fontSize = 14f.dip
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.BottomCenter
            translationY = (-(CIRCLE_DIAMETER / 2 + 20f)).px
            scaleOrigin = Anchor.Center
            alpha = 0f
            judgementText = this
        }

        text {
            fontSize = 14f.dip
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.TopCenter
            translationY = (CIRCLE_DIAMETER / 2 + 24f).px
            tapFeedbackText = this
        }

        text {
            fontSize = 14f.dip
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.TopCenter
            translationY = (CIRCLE_DIAMETER / 2 + 56f).px
            scaleOrigin = Anchor.Center
            streakText = this
        }

        text {
            setText(string.opt_offset_calibration_tap_hint)
            fontSize = 14f.dip
            alignment = Anchor.BottomCenter
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            translationY = (-28f).px
            style = Style { theme -> color = theme.palette.accent * 0.45f }
        }

        linearContainer {
            width = 300f.px
            anchor = Anchor.CenterRight
            origin = Anchor.CenterRight
            translationX = (-20f).px
            orientation = UILinearContainer.Orientation.Vertical
            padding = Padding(24f.px)
            radius = 16f.px

            style = Style { theme ->
                backgroundColor = theme.palette.accent * 0.15f
            }

            text {
                setText(string.opt_offset_calibration_calibration)
                fontSize = 14f.dip
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = Style { theme -> color = theme.palette.accent }
            }

            box {
                width = Dimension.FillAvailable
                height = 1f.px
                style = Style { theme -> backgroundColor = theme.palette.accent * 0.2f }
            }

            text {
                text = "BPM"
                fontSize = 14f.dip
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = Style { theme -> color = theme.palette.accent * 0.6f }
            }

            linearContainer {
                orientation = UILinearContainer.Orientation.Horizontal
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                spacing = 12f.px

                textButton {
                    leadingIcon = faSolidIcon(FAIcon.CaretDown)
                    onActionUp = { changeBpm(-STEP_BPM) }
                    onActionLongPress = { changeBpm(-STEP_BPM_LONG) }
                }

                textButton {
                    text = currentBpm.toString()
                    alignment = Anchor.Center
                    width = 110f.px
                    tintChildren = true
                    style += Style { theme ->
                        radius = 8f.px
                        backgroundColor = theme.palette.accent * 0.12f
                        color = theme.palette.accent
                    }
                    onActionUp = { showBpmInputDialog() }
                    bpmValueText = this
                }

                textButton {
                    leadingIcon = faSolidIcon(FAIcon.CaretUp)
                    onActionUp = { changeBpm(STEP_BPM) }
                    onActionLongPress = { changeBpm(STEP_BPM_LONG) }
                }
            }

            box {
                width = Dimension.FillAvailable
                height = 1f.px
                style = Style { theme -> backgroundColor = theme.palette.accent * 0.2f }
            }

            text {
                setText(string.opt_offset_calibration_offset)
                fontSize = 14f.dip
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = Style { theme -> color = theme.palette.accent * 0.6f }
            }

            linearContainer {
                orientation = UILinearContainer.Orientation.Horizontal
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                spacing = 12f.px

                textButton {
                    leadingIcon = faSolidIcon(FAIcon.CaretDown)
                    onActionUp = { changeOffset(-STEP_MS) }
                    onActionLongPress = { changeOffset(-STEP_MS_LONG) }
                }

                textButton {
                    text = formatOffset(pendingOffset)
                    alignment = Anchor.Center
                    width = 110f.px
                    tintChildren = true
                    style += Style { theme ->
                        radius = 8f.px
                        backgroundColor = theme.palette.accent * 0.12f
                        color = theme.palette.accent
                    }
                    onActionUp = { showOffsetInputDialog() }
                    offsetValueText = this
                }

                textButton {
                    leadingIcon = faSolidIcon(FAIcon.CaretUp)
                    onActionUp = { changeOffset(STEP_MS) }
                    onActionLongPress = { changeOffset(STEP_MS_LONG) }
                }
            }

            textButton {
                setText(string.opt_offset_calibration_set)
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                onActionUp = {
                    playShortClickConfirmSound()
                    applyOffset()
                    back()
                }
                onActionCancel = { playShortClickSound() }
            }

            textButton {
                setText(string.opt_offset_calibration_reset)
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                tintChildren = true
                style += Style {
                    color = Color4(0xFFFFBFBF)
                    backgroundColor = Color4(0xFF342121)
                }
                onActionUp = {
                    playShortClickSound()
                    pendingOffset = 0
                    successfulTapStreak = 0
                    tapOffsets.clear()
                    updateOffsetDisplay()
                    updateTapFeedback()
                    updateStreakDisplay()
                }
                onActionCancel = { playShortClickSound() }
            }

        }

        linearContainer {
            width = 400f.px
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            translationX = (20f).px
            translationY = (-20f).px
            orientation = UILinearContainer.Orientation.Vertical
            padding = Padding(14f.px)
            radius = 16f.px
            style = Style { theme ->
                backgroundColor = theme.palette.accent * 0.15f
            }

            // Header
            text {
                setText(string.opt_offset_calibration_settings)
                fontSize = 14f.dip
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = Style { theme -> color = theme.palette.accent }
            }

            // Divider
            box {
                width = Dimension.FillAvailable
                height = 1f.px
                style = Style { theme -> backgroundColor = theme.palette.accent * 0.2f }
            }

            // Setting row: label (left) + ON/OFF toggle (right)
            linearContainer {
                orientation = UILinearContainer.Orientation.Horizontal
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                spacing = 8f.px
                width = Dimension.FillAvailable

                text {
                    setText(string.opt_highPrecisionInput_title)
                    fontSize = 14f.dip
                    alignment = Anchor.CenterLeft
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = Style { theme -> color = theme.palette.accent * 0.9f }
                }

                highPrecisionToggle = UICheckbox(Config.isHighPrecisionInput()).apply {
                    onValueChange =  {
                        Config.setBoolean("highPrecisionInput", value)
                    }
                }
                +highPrecisionToggle
            }

            // Description below the row
            text {
                setText(string.opt_highPrecisionInput_summary)
                fontSize = 14f.dip
                width = Dimension.FillAvailable
                clipping = ClipMode.Bounds
                alignment = Anchor.TopLeft
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                style = Style { theme -> color = theme.palette.accent * 0.5f }
            }
        }
    }

    fun show() {
        pendingOffset = Config.getOffset().toInt()
        beatTimer = 0f
        metronomeTime = 0.0
        successfulTapStreak = 0
        tapOffsets.clear()
        updateOffsetDisplay()
        updateTapFeedback()
        highPrecisionToggle.value = Config.isHighPrecisionInput()

        val songService = GlobalManager.getInstance().songService
        wasMusicPlaying = songService.status == Status.PLAYING

        if (wasMusicPlaying) {
            songService.pause()
        }

        Verktex.withTreeLock { Verktex.pushScene(this) }
    }

    fun back() {
        if (wasMusicPlaying) {
            GlobalManager.getInstance().songService.play()
        }

        mainThread {
            settingsFragment?.show()
            settingsFragment = null
        }

        Verktex.withTreeLock { Verktex.popScene() }
    }

    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)

        val deltaTimeSec = clock.elapsedFrameTime

        metronomeTime += deltaTimeSec.toDouble()
        beatTimer += deltaTimeSec

        val progress = (beatTimer / beatInterval).coerceIn(0f, 1f)
        approachCircle.scale = Interpolation.linear(APPROACH_SCALE_START, 1f, progress)

        while (beatTimer >= beatInterval) {
            beatTimer -= beatInterval
            onBeat()
        }
    }

    // -------------------------------------------------------------------------
    // Beat / animation
    // -------------------------------------------------------------------------

    private fun onBeat() {
        ResourceManager.getInstance().getSound("drum-hitclap")?.play()

        hitCircleFill.apply {
            clearTransforms()
            alpha = 0.7f
            fadeTo(0.35f, beatInterval * 0.5f, Easing.OutQuad)
        }
    }

    // -------------------------------------------------------------------------
    // Tap calibration
    // -------------------------------------------------------------------------

    private fun onPlayfieldTap() {
        val beatMs = beatInterval * 1000
        val currentMs = metronomeTime * 1000
        val phase = currentMs % beatMs
        val error = if (phase <= beatMs / 2) phase else phase - beatMs
        val absErr = abs(error).roundToInt()

        tapOffsets.add(error)

        if (tapOffsets.size > MAX_TAP_SAMPLES) {
            tapOffsets.removeAt(0)
        }

        pendingOffset = computePendingOffset()

        val judgement = when {
            absErr < judgementHitWindow.greatWindow -> Judgement.PERFECT
            absErr < judgementHitWindow.okWindow -> Judgement.GOOD
            absErr < judgementHitWindow.mehWindow -> Judgement.MEH
            else -> Judgement.MISS
        }

        when (judgement) {
            Judgement.PERFECT, Judgement.GOOD -> {
                successfulTapStreak++
                playShortClickConfirmSound()
            }

            else -> {
                successfulTapStreak = 0
                playShortClickSound()
            }
        }

        updateOffsetDisplay()
        updateTapFeedback(error)
        updateStreakDisplay(judgement)

        showJudgement(judgement)
        triggerRipple(judgement)
        punchCircle()
    }

    private enum class Judgement(@field:IdRes val label: Int, val color: Color4) {
        PERFECT(string.opt_offset_calibration_judgement_perfect, Color4(0xFF6ECFFF)),
        GOOD(string.opt_offset_calibration_judgement_good, Color4(0xFF64DC28)),
        MEH(string.opt_offset_calibration_judgement_meh, Color4(0xFFc8b46e)),
        MISS(string.opt_offset_calibration_judgement_miss, Color4(0xFFFF4444))
    }

    /** Pop-in judgement badge above the hit circle: scale 0.7 → 1.1 → fade out. */
    private fun showJudgement(judgement: Judgement) {
        judgementText.apply {
            clearTransforms()
            setText(judgement.label)
            color = judgement.color
            scale = 0.7f
            alpha = 1f

            scaleTo(1.1f, 0.1f, Easing.OutQuad).after {
                scaleTo(1f, 0.05f)
                fadeOut(0.45f, Easing.InQuad)
            }
        }
    }

    /** Expanding outline ring that bursts outward and fades – colored by judgement. */
    private fun triggerRipple(judgement: Judgement) {
        rippleCircle.apply {
            clearTransforms()
            borderColor = judgement.color
            scale = 1f
            alpha = 0.75f

            scaleTo(1.5f, 0.2f)
            fadeOut(0.2f)
        }
    }

    /** Brief scale-up of the whole circle group – the classic osu! hit punch. */
    private fun punchCircle() {
        circleContainer.apply {
            clearTransforms()
            scale = 1f

            scaleTo(1.12f, 0.08f, Easing.OutQuad).after {
                scaleTo(1f, 0.12f, Easing.InQuad)
            }
        }
    }


    private fun updateStreakDisplay(judgement: Judgement? = null) {
        streakText.apply {
            when {
                successfulTapStreak >= 10 -> {
                    text = StringTable.format(string.opt_offset_calibration_streak, successfulTapStreak)
                    color = Color4(0xFFFFAA00)

                    // tiny pulse on milestone
                    if (judgement != null && successfulTapStreak % 10 == 0) {
                        clearTransforms()
                        scaleTo(1.3f, 0.1f, Easing.OutQuad).after {
                            scaleTo(1f, 0.1f, Easing.InQuad)
                        }
                    }
                }

                successfulTapStreak >= 3 -> {
                    text = StringTable.format(string.opt_offset_calibration_streak_combo, successfulTapStreak)
                    color = Color4(0xFF88FF88)
                }

                else -> text = ""
            }
        }
    }

    private fun updateTapFeedback(lastError: Double? = null) {
        if (lastError != null) {
            val absErr = abs(lastError).roundToInt()

            val label = StringTable.get(when {
                absErr < judgementHitWindow.greatWindow -> string.opt_offset_calibration_feedback_perfect
                lastError > 0 -> string.opt_offset_calibration_feedback_late
                else -> string.opt_offset_calibration_feedback_early
            })

            val detail =
                if (absErr < 16) label
                else "$label (${if (lastError > 0) "+" else ""}${lastError.roundToInt()} ms)"

            tapFeedbackText.text = "$detail  •  ${tapOffsets.size} ${StringTable.get(string.opt_offset_calibration_feedback_tap_count)}"
            tapFeedbackText.color = when {
                absErr < judgementHitWindow.greatWindow -> Color4(0xFF88FF88)
                absErr < judgementHitWindow.okWindow -> Color4(0xFFFFFF88)
                else -> Color4(0xFFFF8888)
            }
        } else {
            tapFeedbackText.text = if (tapOffsets.isNotEmpty()) "${tapOffsets.size} ${StringTable.get(string.opt_offset_calibration_feedback_tap_count)}" else ""
            tapFeedbackText.color = Color4(0xFFAAAAAA)
        }
    }

    // -------------------------------------------------------------------------
    // Offset / BPM helpers
    // -------------------------------------------------------------------------

    private fun changeOffset(delta: Int) {
        tapOffsets.clear()
        pendingOffset = (pendingOffset + delta).coerceIn(OFFSET_MIN, OFFSET_MAX)
        updateOffsetDisplay()
        updateTapFeedback()
        playShortClickSound()
    }

    private fun changeBpm(delta: Int) {
        currentBpm = (currentBpm + delta).coerceIn(30, 300)
        beatTimer = 0f
        metronomeTime = 0.0
        updateBpmDisplay()
        playShortClickSound()
    }

    private fun showOffsetInputDialog() {
        mainThread {
            PromptDialog().apply {
                setTitle(StringTable.get(com.osudroid.resources.R.string.opt_offset_title))
                setMessage(StringTable.get(com.osudroid.resources.R.string.opt_offset_summary))
                setInput(pendingOffset.toString())
                setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

                addButton("OK") { dialog ->
                    dialog as PromptDialog

                    val newValue = dialog.input?.toIntOrNull() ?: pendingOffset
                    pendingOffset = newValue.coerceIn(OFFSET_MIN, OFFSET_MAX)

                    updateThread {
                        tapOffsets.clear()
                        updateOffsetDisplay()
                        updateTapFeedback()
                    }

                    dismiss()
                }

                addButton("Cancel") { dialog ->
                    dialog.dismiss()
                }

                show()
            }
        }
    }

    private fun showBpmInputDialog() {
        mainThread {
            PromptDialog().apply {
                setTitle("BPM")
                setInput(currentBpm.toString())
                setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)

                addButton("OK") { dialog ->
                    dialog as PromptDialog

                    val newValue = dialog.input?.toIntOrNull() ?: currentBpm

                    updateThread {
                        changeBpm(newValue - currentBpm)
                    }

                    dismiss()
                }

                addButton("Cancel") { dialog ->
                    dialog.dismiss()
                }

                show()
            }
        }
    }

    private fun updateOffsetDisplay() {
        offsetValueText.text = formatOffset(pendingOffset)
    }

    private fun updateBpmDisplay() {
        bpmValueText.text = currentBpm.toString()
    }

    private fun formatOffset(ms: Int) = when {
        ms > 0 -> "+${ms} ms"
        ms < 0 -> "$ms ms"
        else -> "0 ms"
    }

    private fun playShortClickSound() {
        ResourceManager.getInstance().getSound("click-short")?.play()
    }

    private fun playShortClickConfirmSound() {
        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
    }

    private fun applyOffset() {
        Config.setOffset(pendingOffset.toFloat())
        Config.setInt("offset", pendingOffset)
    }

    private fun computePendingOffset(): Int {
        if (tapOffsets.isEmpty()) {
            return 0
        }

        val unstableRate = tapOffsets.standardDeviation()
        var offset = tapOffsets.median()

        if (unstableRate >= 90) {
            // A demonstrative graph of this algorithm is embedded in https://github.com/ppy/osu/discussions/30521.
            // This prevents high unstable rate from suggesting potentially invalid offsets.
            offset *= exp(-0.0116 * (unstableRate - 90))
        }

        return offset.roundToInt().coerceIn(OFFSET_MIN, OFFSET_MAX)
    }
}
