package com.osudroid.ui.v2

import android.text.InputType
import androidx.annotation.IdRes
import com.edlplan.framework.easing.Easing
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.utils.mainThread
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.Engine
import com.reco1l.verktex.ui.box
import com.reco1l.verktex.ui.circle
import com.reco1l.verktex.ui.setText
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.linearContainer
import com.reco1l.verktex.ui.PaintStyle
import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.text
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.control.UICheckbox
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.reco1l.osu.ui.PromptDialog
import com.osudroid.beatmaps.DroidHitWindow
import com.osudroid.math.Interpolation
import com.osudroid.utils.median
import com.osudroid.utils.standardDeviation
import com.reco1l.verktex.ui.fillContainer
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import com.rian.andengine.modifier.ModifierType
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R.string

object CalibrationScene : Scene() {
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

    private lateinit var approachCircle: UICircle
    private lateinit var hitCircleFill: UICircle
    private lateinit var rippleCircle: UICircle
    private var circleContainer: UIContainer
    private lateinit var bpmValueText: UITextButton
    private lateinit var offsetValueText: UITextButton
    private lateinit var tapFeedbackText: UIText
    private lateinit var judgementText: UIText
    private lateinit var streakText: UIText
    private lateinit var highPrecisionToggle: UICheckbox

    init {
        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")

        isBackgroundEnabled = false

        // Background
        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            style = { color = it.accentColor * 0.08f }
        }

        val tapAreaSize = CIRCLE_DIAMETER * APPROACH_SCALE_START

        textButton {
            anchor = Anchor.TopLeft
            origin = Anchor.TopLeft
            translationX = 60f
            translationY = 12f
            text = "Back"
            leadingIcon = UISprite().apply {
                textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
                width = 28f
                height = 28f
            }
            onActionUp = {
                playShortClickConfirmSound()
                back()
            }
            onActionCancel = { playShortClickSound() }
        }

        circleContainer = object : UIContainer() {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (event.isActionDown) {
                    val cx = tapAreaSize / 2f
                    val cy = tapAreaSize / 2f
                    val dx = localX - cx
                    val dy = localY - cy

                    if (dx * dx + dy * dy <= cx * cx) {
                        onPlayfieldTap()
                    }
                }

                return true
            }
        }.apply {
            width = CIRCLE_DIAMETER
            height = CIRCLE_DIAMETER
            anchor = Anchor.Center
            origin = Anchor.Center

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                style = { color = it.accentColor * 0.35f }
                hitCircleFill = this
            }

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                paintStyle = PaintStyle.Outline
                lineWidth = 8f
                style = { color = it.accentColor }
            }

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                paintStyle = PaintStyle.Outline
                lineWidth = 6f
                scaleOrigin = Anchor.Center
                setScale(APPROACH_SCALE_START)
                style = { color = it.accentColor * 0.85f }
                approachCircle = this
            }

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                paintStyle = PaintStyle.Outline
                lineWidth = 6f
                scaleOrigin = Anchor.Center
                alpha = 0f
                style = { color = it.accentColor }
                rippleCircle = this
            }
        }

        attachChild(circleContainer)

        text {
            fontSize = FontSize.SM
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.BottomCenter
            translationY = -(CIRCLE_DIAMETER / 2 + 20f)
            scaleOrigin = Anchor.Center
            alpha = 0f
            judgementText = this
        }

        text {
            fontSize = FontSize.SM
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.TopCenter
            translationY = CIRCLE_DIAMETER / 2 + 24f
            tapFeedbackText = this
        }

        text {
            fontSize = FontSize.SM
            alignment = Anchor.Center
            anchor = Anchor.Center
            origin = Anchor.TopCenter
            translationY = CIRCLE_DIAMETER / 2 + 56f
            scaleOrigin = Anchor.Center
            streakText = this
        }

        text {
            setText(string.opt_offset_calibration_tap_hint)
            fontSize = FontSize.SM
            alignment = Anchor.BottomCenter
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            translationY = -28f
            style = { color = it.accentColor * 0.45f }
        }

        fillContainer {
            width = 300f
            anchor = Anchor.CenterRight
            origin = Anchor.CenterRight
            translationX = -20f
            orientation = Orientation.Vertical
            padding = Vec4(24f)

            style = {
                radius = 16f
                backgroundColor = it.accentColor * 0.15f
            }

            text {
                setText(string.opt_offset_calibration_calibration)
                fontSize = FontSize.SM
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = { color = it.accentColor }
            }

            box {
                width = Dimension.FillAvailable
                height = 1f
                style = { color = it.accentColor * 0.2f }
            }

            text {
                text = "BPM"
                fontSize = FontSize.SM
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = { color = it.accentColor * 0.6f }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                spacing = 12f

                textButton {
                    leadingIcon = UIIcon(FAIcon.CaretDown).apply {
                        style = { color = it.accentColor }
                    }
                    onActionUp = { changeBpm(-STEP_BPM) }
                    onActionLongPress = { changeBpm(-STEP_BPM_LONG) }
                }

                textButton {
                    text = currentBpm.toString()
                    alignment = Anchor.Center
                    minWidth = 110f
                    style = {
                        radius = 8f
                        backgroundColor = it.accentColor * 0.12f
                        color = it.accentColor
                    }
                    onActionUp = { showBpmInputDialog() }
                    bpmValueText = this
                }

                textButton {
                    leadingIcon = UIIcon(FAIcon.CaretUp).apply {
                        style = { color = it.accentColor }
                    }
                    onActionUp = { changeBpm(STEP_BPM) }
                    onActionLongPress = { changeBpm(STEP_BPM_LONG) }
                }
            }

            box {
                width = Dimension.FillAvailable
                height = 1f
                style = { color = it.accentColor * 0.2f }
            }

            text {
                setText(string.opt_offset_calibration_offset)
                fontSize = FontSize.SM
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = { color = it.accentColor * 0.6f }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                spacing = 12f

                textButton {
                    leadingIcon = UIIcon(FAIcon.CaretDown).apply {
                        style = { color = it.accentColor }
                    }
                    onActionUp = { changeOffset(-STEP_MS) }
                    onActionLongPress = { changeOffset(-STEP_MS_LONG) }
                }

                textButton {
                    text = formatOffset(pendingOffset)
                    alignment = Anchor.Center
                    minWidth = 110f
                    style = {
                        color = it.accentColor
                        radius = 8f
                        backgroundColor = it.accentColor * 0.12f
                    }
                    onActionUp = { showOffsetInputDialog() }
                    offsetValueText = this
                }

                textButton {
                    leadingIcon = UIIcon(FAIcon.CaretUp).apply {
                        style = { color = it.accentColor }
                    }
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
                style = {}
                color = Color4(0xFFFFBFBF)
                backgroundColor = Color4(0xFF342121)
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

        fillContainer {
            width = 400f
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            translationX = 20f
            translationY = -20f
            orientation = Orientation.Vertical
            padding = Vec4(14f)
            style = {
                radius = 16f
                backgroundColor = it.accentColor * 0.15f
            }

            // Header
            text {
                setText(string.opt_offset_calibration_settings)
                fontSize = FontSize.SM
                alignment = Anchor.TopCenter
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = { color = it.accentColor }
            }

            // Divider
            box {
                width = Dimension.FillAvailable
                height = 1f
                style = { color = it.accentColor * 0.2f }
            }

            // Setting row: label (left) + ON/OFF toggle (right)
            linearContainer {
                orientation = Orientation.Horizontal
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                spacing = 8f
                width = Dimension.FillAvailable

                text {
                    setText(string.opt_highPrecisionInput_title)
                    fontSize = FontSize.SM
                    alignment = Anchor.CenterLeft
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = { color = it.accentColor * 0.9f }
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
                fontSize = FontSize.SM
                width = Dimension.FillAvailable
                clipToBounds = true
                alignment = Anchor.TopLeft
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                style = { color = it.accentColor * 0.5f }
            }
        }
    }

    override fun show() {
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

        Engine.current.scene.setChildScene(this, false, false, true)
    }

    override fun back() {
        if (wasMusicPlaying) {
            GlobalManager.getInstance().songService.play()
        }

        mainThread {
            settingsFragment?.show()
            settingsFragment = null
        }

        super.back()
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        super.onManagedUpdate(deltaTimeSec)

        metronomeTime += deltaTimeSec.toDouble()
        beatTimer += deltaTimeSec

        val progress = (beatTimer / beatInterval).coerceIn(0f, 1f)
        approachCircle.setScale(Interpolation.linear(APPROACH_SCALE_START, 1f, progress))

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
            clearModifiers(ModifierType.Alpha)
            alpha = 0.7f
            fadeTo(0.35f, beatInterval * 0.5f, Easing.Out)
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
            clearModifiers(false, ModifierType.Alpha, ModifierType.ScaleXY)
            setText(judgement.label)
            color = judgement.color
            setScale(0.7f)
            alpha = 1f

            scaleTo(1.1f, 0.1f, Easing.Out).after {
                scaleTo(1f, 0.05f)
                fadeOut(0.45f, Easing.In)
            }
        }
    }

    /** Expanding outline ring that bursts outward and fades – colored by judgement. */
    private fun triggerRipple(judgement: Judgement) {
        rippleCircle.apply {
            clearModifiers(false, ModifierType.Alpha, ModifierType.ScaleXY)
            color = judgement.color
            setScale(1f)
            alpha = 0.75f

            scaleTo(1.5f, 0.2f)
            fadeOut(0.2f)
        }
    }

    /** Brief scale-up of the whole circle group – the classic osu! hit punch. */
    private fun punchCircle() {
        circleContainer.apply {
            clearModifiers(ModifierType.ScaleXY)
            setScale(1f)

            scaleTo(1.12f, 0.08f, Easing.Out).after {
                scaleTo(1f, 0.12f, Easing.In)
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
                        clearModifiers(ModifierType.ScaleXY)
                        scaleTo(1.3f, 0.1f, Easing.Out).after {
                            scaleTo(1f, 0.1f, Easing.In)
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
