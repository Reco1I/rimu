package com.osudroid.game.objects

import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.game.SpinnerRpmCounter
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.resetAnimatedState
import com.reco1l.verktex.ui.shape.PathShape
import kotlin.jvm.JvmStatic
import kotlin.math.max

/**
 * The pieces of the hit objects of gameplay, reused from one object to the next: building one while a beatmap plays
 * stalls the frame, so the pool is filled before it starts, see [preload]. The objects themselves are cheap, each hit
 * object gets a new one, which borrows its pieces here and gives them back as it expires (see [GameObject.obtain]).
 *
 * The pieces of the skin are kept by their [SkinComponent], and a piece given back is reset ([resetAnimatedState]),
 * so nothing of its last use is drawn: the object using it next sets what it shows.
 */
class PiecePool private constructor() {

    private val pieces = HashMap<SkinComponent<*>, ArrayDeque<Any>>()

    // The components no skin draws, e.g. the hit lighting without its texture, not looked up again.
    private val unavailable = HashSet<SkinComponent<*>>()

    // The components of the judgements, by their texture, not to make one for each judgement.
    private val judgementComponents = HashMap<String, SkinComponent.Judgement>()

    private val sliderBodies = ArrayDeque<PathShape>()
    private val rpmCounters = ArrayDeque<SpinnerRpmCounter>()

    // The revision of the skin textures the pieces were built with, see preload.
    private var skinRevision = -1

    // What was created since the pool was last filled, as it had none left: preload should have made it.
    private val misses = LinkedHashMap<String, Int>()


    /**
     * A piece of [component], made if the pool has none left.
     */
    fun <T : Any> obtain(component: SkinComponent<T>): T =
        obtainOrNull(component) ?: error("No skin can draw $component")

    /**
     * A piece of [component], made if the pool has none left, or null if no skin draws it, as the hit lighting of a
     * skin without its texture.
     */
    fun <T : Any> obtainOrNull(component: SkinComponent<T>): T? {
        @Suppress("UNCHECKED_CAST")
        pieces[component]?.removeFirstOrNull()?.let { return it as T }

        if (component in unavailable) {
            return null
        }

        val piece = SkinPieces.current.createOrNull(component)
        if (piece == null) {
            unavailable += component
            return null
        }

        miss(component.toString())
        return piece
    }

    /**
     * Gives back [piece], a piece of [component], taken out of its parent and reset.
     */
    fun release(component: SkinComponent<*>, piece: Any) {
        if (piece is Component) {
            piece.removeSelf()
            piece.resetAnimatedState()
        }
        pieces.getOrPut(component) { ArrayDeque() }.add(piece)
    }

    /**
     * The component of the judgement with the texture [textureName] (`hit300`, `hit0`, `sliderpoint30`…).
     */
    fun judgement(textureName: String): SkinComponent.Judgement =
        judgementComponents.getOrPut(textureName) { SkinComponent.Judgement(textureName) }

    /**
     * A body for a slider, drawn as the skin's [SkinComponent.SliderBody] styles it.
     */
    fun obtainSliderBody(): PathShape = sliderBodies.removeFirstOrNull() ?: PathShape().also { miss("SliderBody shape") }

    fun releaseSliderBody(body: PathShape) {
        body.removeSelf()
        body.resetAnimatedState()
        sliderBodies.add(body)
    }

    /**
     * The spins per minute shown under a spinner.
     */
    fun obtainRpmCounter(): SpinnerRpmCounter = rpmCounters.removeFirstOrNull() ?: SpinnerRpmCounter().also { miss("SpinnerRpmCounter") }

    fun releaseRpmCounter(counter: SpinnerRpmCounter) {
        counter.detach()
        rpmCounters.add(counter)
    }


    fun purge() {
        pieces.clear()
        unavailable.clear()
        sliderBodies.clear()
        rpmCounters.clear()

        skinRevision = -1
        misses.clear()
    }

    private fun miss(name: String) {
        misses[name] = (misses[name] ?: 0) + 1
    }

    /**
     * Fills the pool with the pieces of the objects a play has alive at once, given by [sizes], and their judgements.
     * What it has is kept if the skin is the same as when it was last filled (a retry), otherwise it's built again.
     *
     * @param isTraceable Whether the play has Traceable, whose objects have their own approach circles.
     * @return Whether what the pool had was kept: the other pools of gameplay can keep theirs too then.
     */
    fun preload(sizes: PoolSizes, isTraceable: Boolean): Boolean {
        val revision = SkinTextures.revision
        val keep = revision == skinRevision

        if (!keep) {
            purge()
        }
        skinRevision = revision
        misses.clear()

        val circles = sizes.circles + MARGIN
        val sliders = sizes.sliders + MARGIN

        fill(SkinComponent.HitCircle, circles)
        fill(SkinComponent.ApproachCircle, circles + sliders)
        if (isTraceable) {
            fill(SkinComponent.TraceableApproachCircle, circles + sliders)
        }

        // The head and the tail of a slider, and their bursts.
        fill(SkinComponent.SliderHeadCircle, sliders * 2)
        fill(SkinComponent.SliderTailCircle, sliders * 2)
        // The arrows at both ends and one bursting.
        fill(SkinComponent.ReverseArrow, sliders * 3)
        fill(SkinComponent.SliderBall, sliders)
        fill(SkinComponent.FollowCircle, sliders)
        fill(SkinComponent.SliderBody, sliders)
        repeat(sliders - sliderBodies.size) { sliderBodies.add(PathShape()) }

        val spinners = max(1, sizes.spinners)
        fill(SkinComponent.Spinner, spinners)
        repeat(spinners - rpmCounters.size) { rpmCounters.add(SpinnerRpmCounter()) }

        if (Settings[OsuSettings.HitLighting]) {
            fill(SkinComponent.HitLighting, sizes.activeObjects + MARGIN)
        }

        // Each object shows its judgement until it expires, a slider one for each of its parts.
        val fewJudgements = max(MARGIN, sizes.activeObjects / 4)
        fill(judgement("hit300"), sizes.activeObjects + MARGIN)
        fill(judgement("sliderpoint30"), sizes.sliders * 2 + MARGIN)
        fill(judgement("sliderpoint10"), sizes.sliderTicks + MARGIN)
        for (name in FEW_JUDGEMENTS) {
            fill(judgement(name), fewJudgements)
        }

        return keep
    }

    private fun fill(component: SkinComponent<*>, count: Int) {
        if (component in unavailable) {
            return
        }

        val pool = pieces.getOrPut(component) { ArrayDeque() }

        repeat(count - pool.size) {
            val piece = SkinPieces.current.createOrNull(component)
            if (piece == null) {
                unavailable += component
                return
            }
            pool.add(piece)
        }
    }

    /**
     * What had to be created while playing, for the log.
     */
    fun missSummary() = if (misses.isEmpty()) "nothing" else misses.entries.joinToString { "${it.key}: ${it.value}" }

    companion object {
        private const val MARGIN = 2

        private val FEW_JUDGEMENTS = arrayOf("hit300k", "hit300g", "hit100", "hit100k", "hit50", "hit0")

        @JvmStatic
        val instance = PiecePool()
    }
}
