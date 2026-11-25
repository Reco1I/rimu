package com.osudroid.game.objects

import com.osudroid.ui.v2.skin.JudgementPiece
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.osudroid.ui.v2.SkinTextures
import kotlin.math.max
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The hit objects and judgements of gameplay, reused from one object to the next: building one while a beatmap plays
 * stalls the frame, so the pool is filled before it starts, see [preload].
 */
class GameObjectPool private constructor() {
    @JvmField
    var circles = ArrayDeque<GameplayHitCircle>()
    private val judgements = HashMap<String, ArrayDeque<JudgementPiece>>()
    @JvmField
    var sliders = ArrayDeque<GameplaySlider>()
    @JvmField
    var spinners = ArrayDeque<GameplaySpinner>()

    // The revision of the skin textures the pieces were built with, see preload.
    private var skinRevision = -1

    /**
     * The objects created since the pool was last filled, as it had none left: [preload] should have made them.
     */
    var circleMisses = 0
        private set
    var sliderMisses = 0
        private set
    var spinnerMisses = 0
        private set
    var judgementMisses = 0
        private set

    val circle: GameplayHitCircle
        get() {
            if (!circles.isEmpty()) {
                return circles.removeFirst()
            }

            circleMisses++
            return GameplayHitCircle()
        }

    fun putCircle(circle: GameplayHitCircle) {
        circles.add(circle)
    }

    val spinner: GameplaySpinner
        get() {
            if (!spinners.isEmpty()) {
                return spinners.removeFirst()
            }

            spinnerMisses++
            return GameplaySpinner()
        }

    fun putSpinner(spinner: GameplaySpinner) {
        spinners.add(spinner)
    }

    /**
     * A judgement piece of the skin for the texture [textureName], see [JudgementPiece].
     */
    fun getJudgement(textureName: String): JudgementPiece {
        judgements[textureName]?.removeFirstOrNull()?.let { return it }

        judgementMisses++
        return createJudgement(textureName)
    }

    private fun createJudgement(textureName: String): JudgementPiece =
        SkinPieces.current.create(SkinComponent.Judgement(textureName))

    fun putJudgement(piece: JudgementPiece) {
        judgements.getOrPut(piece.textureName) { ArrayDeque() }.add(piece)
    }

    val slider: GameplaySlider
        get() {
            if (!sliders.isEmpty()) {
                return sliders.removeFirst()
            }

            sliderMisses++
            return GameplaySlider()
        }

    fun putSlider(slider: GameplaySlider) {
        sliders.add(slider)
    }

    fun purge() {
        judgements.clear()
        circles.clear()
        sliders.clear()
        spinners.clear()

        skinRevision = -1
        resetMisses()
    }

    private fun resetMisses() {
        circleMisses = 0
        sliderMisses = 0
        spinnerMisses = 0
        judgementMisses = 0
    }

    /**
     * Fills the pool with the objects a play has alive at once, given by [sizes], and their judgements. What it has
     * is kept if the skin is the same as when it was last filled (a retry), otherwise it's built again.
     *
     * @return Whether what the pool had was kept: the other pools of gameplay can keep theirs too then.
     */
    fun preload(sizes: PoolSizes): Boolean {
        val revision = SkinTextures.revision
        val keep = revision == skinRevision

        if (!keep) {
            purge()
        }
        skinRevision = revision
        resetMisses()

        // A few more than estimated, for what the estimate misses (the lifetime of effects can be longer).
        repeat(sizes.circles + MARGIN - circles.size) { putCircle(GameplayHitCircle()) }
        repeat(sizes.sliders + MARGIN - sliders.size) { putSlider(GameplaySlider()) }
        repeat(max(1, sizes.spinners) - spinners.size) { putSpinner(GameplaySpinner()) }

        // Each object shows its judgement until it expires, a slider one for each of its parts.
        val fewJudgements = max(MARGIN, sizes.activeObjects / 4)
        preloadJudgements("hit300", sizes.activeObjects + MARGIN)
        preloadJudgements("sliderpoint30", sizes.sliders * 2 + MARGIN)
        preloadJudgements("sliderpoint10", sizes.sliderTicks + MARGIN)
        for (name in FEW_JUDGEMENTS) {
            preloadJudgements(name, fewJudgements)
        }

        return keep
    }

    private fun preloadJudgements(textureName: String, count: Int) {
        val pieces = judgements.getOrPut(textureName) { ArrayDeque() }
        repeat(count - pieces.size) { pieces.add(createJudgement(textureName)) }
    }

    /**
     * What had to be created while playing, for the log.
     */
    fun missSummary() =
        "circles: $circleMisses, sliders: $sliderMisses, spinners: $spinnerMisses, judgements: $judgementMisses"

    companion object {
        private const val MARGIN = 2

        private val FEW_JUDGEMENTS = arrayOf("hit300k", "hit300g", "hit100", "hit100k", "hit50", "hit0")

        @JvmField
        var instance = GameObjectPool()

        @JvmStatic
        fun getInstance(): GameObjectPool {
            return instance
        }
    }
}
