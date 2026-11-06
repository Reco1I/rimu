package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.math.Interpolation
import com.osudroid.math.LinePath
import com.osudroid.math.Precision
import com.osudroid.math.Vector2
import com.osudroid.math.roundExactHalfUp
import com.osudroid.mods.*
import com.osudroid.utils.PathApproximation
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.Constants

object GameHelper {
    private var overallDifficulty: Float = 1f

    private var originalTimePreempt: Float = 0f

    private var healthDrain: Float = 0f

    private var speedMultiplier: Float = 0f

    private var hidden: ModHidden? = null

    private var traceable: ModTraceable? = null

    private var flashlight: ModFlashlight? = null

    private var hardRock: ModHardRock? = null

    private var relax: ModRelax? = null

    private var doubleTime: ModDoubleTime? = null

    private var nightCore: ModNightCore? = null

    private var halfTime: ModHalfTime? = null

    private var autopilot: ModAutopilot? = null

    private var suddenDeath: ModSuddenDeath? = null

    private var perfect: ModPerfect? = null

    private var synesthesia: ModSynesthesia? = null

    private var scoreV2: ModScoreV2? = null

    private var easy: ModEasy? = null

    private var muted: ModMuted? = null

    private var freezeFrame: ModFreezeFrame? = null

    private var approachDifferent: ModApproachDifferent? = null

    private var isKiai: Boolean = false

    private var autoplay: ModAutoplay? = null

    private var beatLength: Double = 0.0

    private var currentBeatTime: Double = 0.0

    private var samplesMatchPlaybackRate: Boolean = false

    private var replayVersion: Int = 0

    @JvmStatic
    fun getHealthDrain(): Float {
        return healthDrain
    }

    @JvmStatic
    fun setHealthDrain(healthDrain: Float) {
        GameHelper.healthDrain = healthDrain
    }

    @JvmStatic
    fun getOverallDifficulty(): Float {
        return overallDifficulty
    }

    @JvmStatic
    fun setOverallDifficulty(overallDifficulty: Float) {
        GameHelper.overallDifficulty = overallDifficulty
    }

    @JvmStatic
    fun getOriginalTimePreempt(): Float {
        return originalTimePreempt
    }

    @JvmStatic
    fun setOriginalTimePreempt(originalTimePreempt: Float) {
        GameHelper.originalTimePreempt = originalTimePreempt
    }

    /**
     * Converts a [SliderPath] to a [LinePath] that can be used to render the slider path.
     *
     * @param sliderPath The [SliderPath] to convert.
     * @return The converted [LinePath].
     */
    @JvmStatic
    fun convertSliderPath(sliderPath: SliderPath, scope: CoroutineScope?): LinePath {
        val renderPath = LinePath()

        if (sliderPath.anchorCount == 0) {
            return renderPath
        }

        var distanceThreshold = 0

        if (!Settings[OsuSettings.SnakingOutSliders]) {
            // osu!stable optimizes gameplay path rendering by only including points that are 6 osu!pixels apart.
            // In linear paths, the distance threshold is further extended to 32 osu!pixels.
            // However, we exclude this optimization if snaking out sliders is enabled as slider velocity may not be
            // reliably computed with respect to slider ball travel velocity.
            distanceThreshold = if (sliderPath.pathType == SliderPathType.Linear) 32 else 6
        }

        val distanceThresholdSquared = (distanceThreshold * distanceThreshold).toFloat()

        // Invert the scale to convert from screen pixels to osu!pixels.
        val invertedScale = Vector2(
            Constants.MAP_WIDTH.toFloat() / Constants.MAP_ACTUAL_WIDTH,
            Constants.MAP_HEIGHT.toFloat() / Constants.MAP_ACTUAL_HEIGHT
        )

        // Additional consideration for Catmull sliders that form "bulbs" around points with identical positions.
        val isCatmull = sliderPath.pathType == SliderPathType.Catmull
        val catmullSegmentLength = PathApproximation.CATMULL_DETAIL * 2

        for (i in 0 until sliderPath.anchorCount) {
            if (scope != null) {
                scope.coroutineContext.ensureActive()
            }

            val x = sliderPath.getX(i)
            val y = sliderPath.getY(i)
            val vec = Vector2(x, y)

            if (renderPath.size() == 0) {
                renderPath.add(vec)
                continue
            }

            val last = renderPath.getLast()
            val dx = (vec.x - last.x) * invertedScale.x
            val dy = (vec.y - last.y) * invertedScale.y
            val distanceFromLastSquared = dx * dx + dy * dy

            if (distanceFromLastSquared > distanceThresholdSquared || i == sliderPath.anchorCount - 1 ||
                    (isCatmull && (i + 1) % catmullSegmentLength == 0)) {
                renderPath.add(vec)
            }
        }

        // The render path may under-measure the true length of the slider due to the optimization.
        // Normally, the path would need to be extended to account for the true length of the slider.
        // However, in this case we simply let it be as the path is still rendered correctly (its points
        // are still in the correct positions).
        renderPath.measure()

        return renderPath
    }

    /**
     * Converts an osu!pixels-based path of a [Slider] to one that can be used in gameplay.
     *
     * @return The converted [SliderPath].
     */
    @JvmStatic
    fun convertSliderPath(slider: Slider, scope: CoroutineScope?): SliderPath {
        val calculatedPath = slider.path.calculatedPath
        val cumulativeLength = slider.path.cumulativeLength
        val path = SliderPath(slider.path.pathType, calculatedPath.size)

        val widthScale = Constants.MAP_ACTUAL_WIDTH.toFloat() / Constants.MAP_WIDTH
        val heightScale = Constants.MAP_ACTUAL_HEIGHT.toFloat() / Constants.MAP_HEIGHT

        for (i in calculatedPath.indices) {
            if (scope != null) {
                scope.coroutineContext.ensureActive()
            }

            val p = calculatedPath[i]

            path.set(i, p.x * widthScale, p.y * heightScale, cumulativeLength[i].toFloat())
        }

        return path
    }

    /**
     * Gets the rate at which gameplay progresses in terms of time.
     */
    @JvmStatic
    fun getSpeedMultiplier(): Float {
        return speedMultiplier
    }

    /**
     * Sets the rate at which gameplay progresses in terms of time.
     */
    @JvmStatic
    fun setSpeedMultiplier(speedMultiplier: Float) {
        GameHelper.speedMultiplier = speedMultiplier
    }

    @JvmStatic
    fun getEasy(): ModEasy? {
        return easy
    }

    @JvmStatic
    fun isEasy(): Boolean {
        return easy != null
    }

    @JvmStatic
    fun setEasy(isEasy: ModEasy?) {
        GameHelper.easy = isEasy
    }

    @JvmStatic
    fun getHardRock(): ModHardRock? {
        return hardRock
    }

    @JvmStatic
    fun isHardRock(): Boolean {
        return hardRock != null
    }

    @JvmStatic
    fun setHardRock(hardRock: ModHardRock?) {
        GameHelper.hardRock = hardRock
    }

    @JvmStatic
    fun getHidden(): ModHidden? {
        return hidden
    }

    @JvmStatic
    fun isHidden(): Boolean {
        return hidden != null
    }

    @JvmStatic
    fun setHidden(hidden: ModHidden?) {
        GameHelper.hidden = hidden
    }

    @JvmStatic
    fun getTraceable(): ModTraceable? {
        return traceable
    }

    @JvmStatic
    fun isTraceable(): Boolean {
        return traceable != null
    }

    @JvmStatic
    fun setTraceable(traceable: ModTraceable?) {
        GameHelper.traceable = traceable
    }

    @JvmStatic
    fun getFlashlight(): ModFlashlight? {
        return flashlight
    }

    @JvmStatic
    fun isFlashlight(): Boolean {
        return flashlight != null
    }

    @JvmStatic
    fun setFlashlight(flashlight: ModFlashlight?) {
        GameHelper.flashlight = flashlight
    }

    @JvmStatic
    fun getHalfTime(): ModHalfTime? {
        return halfTime
    }

    @JvmStatic
    fun isHalfTime(): Boolean {
        return halfTime != null
    }

    @JvmStatic
    fun setHalfTime(halfTime: ModHalfTime?) {
        GameHelper.halfTime = halfTime
    }

    @JvmStatic
    fun getNightCore(): ModNightCore? {
        return nightCore
    }

    @JvmStatic
    fun isNightCore(): Boolean {
        return nightCore != null
    }

    @JvmStatic
    fun setNightCore(nightCore: ModNightCore?) {
        GameHelper.nightCore = nightCore
    }

    @JvmStatic
    fun getDoubleTime(): ModDoubleTime? {
        return doubleTime
    }

    @JvmStatic
    fun isDoubleTime(): Boolean {
        return doubleTime != null
    }

    @JvmStatic
    fun setDoubleTime(doubleTime: ModDoubleTime?) {
        GameHelper.doubleTime = doubleTime
    }

    @JvmStatic
    fun getSuddenDeath(): ModSuddenDeath? {
        return suddenDeath
    }

    @JvmStatic
    fun isSuddenDeath(): Boolean {
        return suddenDeath != null
    }

    @JvmStatic
    fun setSuddenDeath(suddenDeath: ModSuddenDeath?) {
        GameHelper.suddenDeath = suddenDeath
    }

    @JvmStatic
    fun getPerfect(): ModPerfect? {
        return perfect
    }

    @JvmStatic
    fun isPerfect(): Boolean {
        return perfect != null
    }

    @JvmStatic
    fun setPerfect(perfect: ModPerfect?) {
        GameHelper.perfect = perfect
    }

    @JvmStatic
    fun getSynesthesia(): ModSynesthesia? {
        return synesthesia
    }

    @JvmStatic
    fun isSynesthesia(): Boolean {
        return synesthesia != null
    }

    @JvmStatic
    fun setSynesthesia(synesthesia: ModSynesthesia?) {
        GameHelper.synesthesia = synesthesia
    }

    @JvmStatic
    fun getMuted(): ModMuted? {
        return muted
    }

    @JvmStatic
    fun isMuted(): Boolean {
        return muted != null
    }

    @JvmStatic
    fun setMuted(muted: ModMuted?) {
        GameHelper.muted = muted
    }

    @JvmStatic
    fun getFreezeFrame(): ModFreezeFrame? {
        return freezeFrame
    }

    @JvmStatic
    fun isFreezeFrame(): Boolean {
        return freezeFrame != null
    }

    @JvmStatic
    fun setFreezeFrame(freezeFrame: ModFreezeFrame?) {
        GameHelper.freezeFrame = freezeFrame
    }

    @JvmStatic
    fun getApproachDifferent(): ModApproachDifferent? {
        return approachDifferent
    }

    @JvmStatic
    fun isApproachDifferent(): Boolean {
        return approachDifferent != null
    }

    @JvmStatic
    fun setApproachDifferent(approachDifferent: ModApproachDifferent?) {
        GameHelper.approachDifferent = approachDifferent
    }

    @JvmStatic
    fun getScoreV2(): ModScoreV2? {
        return scoreV2
    }

    @JvmStatic
    fun isScoreV2(): Boolean {
        return scoreV2 != null
    }

    @JvmStatic
    fun setScoreV2(scoreV2: ModScoreV2?) {
        GameHelper.scoreV2 = scoreV2
    }

    @JvmStatic
    fun isKiai(): Boolean {
        return isKiai
    }

    @JvmStatic
    fun setKiai(isKiai: Boolean) {
        GameHelper.isKiai = isKiai
    }

    @JvmStatic
    fun getCurrentBeatTime(): Double {
        return currentBeatTime
    }

    @JvmStatic
    fun setCurrentBeatTime(currentBeatTime: Double) {
        GameHelper.currentBeatTime = currentBeatTime
    }

    @JvmStatic
    fun getBeatLength(): Double {
        return beatLength
    }

    @JvmStatic
    fun setBeatLength(beatLength: Double) {
        GameHelper.beatLength = beatLength
    }

    @JvmStatic
    fun getRelax(): ModRelax? {
        return relax
    }

    @JvmStatic
    fun isRelax(): Boolean {
        return relax != null
    }

    @JvmStatic
    fun setRelax(relax: ModRelax?) {
        GameHelper.relax = relax
    }

    @JvmStatic
    fun getAutopilot(): ModAutopilot? {
        return autopilot
    }

    @JvmStatic
    fun isAutopilot(): Boolean {
        return autopilot != null
    }

    @JvmStatic
    fun setAutopilot(autopilot: ModAutopilot?) {
        GameHelper.autopilot = autopilot
    }

    @JvmStatic
    fun getAutoplay(): ModAutoplay? {
        return autoplay
    }

    @JvmStatic
    fun isAutoplay(): Boolean {
        return autoplay != null
    }

    @JvmStatic
    fun setAutoplay(autoplay: ModAutoplay?) {
        GameHelper.autoplay = autoplay
    }

    @JvmStatic
    fun Round(value: Double, digits: Int): Float {
        if (abs(value) < Double.MAX_VALUE) {
            var f1 = 0f
            f1 = roundExactHalfUp(value, digits)
            return f1
        } else {
            return if (value > 0) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
        }
    }

    @JvmStatic
    fun isSamplesMatchPlaybackRate(): Boolean {
        return samplesMatchPlaybackRate
    }

    @JvmStatic
    fun setSamplesMatchPlaybackRate(samplesMatchPlaybackRate: Boolean) {
        GameHelper.samplesMatchPlaybackRate = samplesMatchPlaybackRate
    }

    @JvmStatic
    fun getReplayVersion(): Int {
        return replayVersion
    }

    @JvmStatic
    fun setReplayVersion(replayVersion: Int) {
        GameHelper.replayVersion = replayVersion
    }

    class SliderPath(@JvmField val pathType: SliderPathType, anchorPointCount: Int) {
        private val data = FloatArray(anchorPointCount * strip)
        @JvmField
        var anchorCount = 0

        fun set(index: Int, x: Float, y: Float, length: Float) {
            data[index * strip + offsetX] = x
            data[index * strip + offsetY] = y
            data[index * strip + offsetLength] = length
            anchorCount++
        }

        fun getX(index: Int): Float {
            return getData(index, offsetX)
        }

        fun getY(index: Int): Float {
            return getData(index, offsetY)
        }

        fun getLength(index: Int): Float {
            return getData(index, offsetLength)
        }

        fun positionAt(out: Vector2, percentage: Float): Int {
            if (anchorCount == 0) {
                out.set(0f, 0f)
                return 0
            }

            val distance = percentage * getLength(anchorCount - 1)
            val index = getIndexOfDistance(distance)

            interpolate(out, index, distance)

            return index
        }

        private fun interpolate(out: Vector2, index: Int, distance: Float) {
            if (index <= 0) {
                out.set(getX(0), getY(0))
                return
            }

            if (index >= anchorCount) {
                out.set(getX(anchorCount - 1), getY(anchorCount - 1))
                return
            }

            val d0 = getLength(index - 1)
            val d1 = getLength(index)

            if (Precision.almostEquals(d0, d1)) {
                out.set(getX(index - 1), getY(index - 1))
                return
            }

            val t = Interpolation.reverseLinear(distance, d0, d1)

            out.x = Interpolation.linear(getX(index - 1), getX(index), t)
            out.y = Interpolation.linear(getY(index - 1), getY(index), t)
        }

        private fun getIndexOfDistance(d: Float): Int {
            if (anchorCount == 0 || d < getLength(0)) {
                return 0
            }

            if (d >= getLength(anchorCount - 1)) {
                return anchorCount
            }

            var l = 0
            var r = anchorCount - 2

            while (l <= r) {
                val pivot = l + ((r - l) shr 1)
                val length = getLength(pivot)

                if (length < d) {
                    l = pivot + 1
                } else if (length > d) {
                    r = pivot - 1
                } else {
                    return pivot
                }
            }

            return l
        }

        private fun getData(index: Int, offset: Int): Float {
            if (data.size == 0) {
                return 0f
            }

            val idx = index * strip + offset

            if (idx < 0) {
                return 0f
            }

            if (idx >= data.size) {
                return data[data.size - strip + offset]
            }

            return data[idx]
        }

        companion object {
            private const val strip = 3
            private const val offsetX = 0
            private const val offsetY = 1
            private const val offsetLength = 2
        }
    }
}
