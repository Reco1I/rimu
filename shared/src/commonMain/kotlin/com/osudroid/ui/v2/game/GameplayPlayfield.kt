package com.osudroid.ui.v2.game

import com.osudroid.game.Playfield
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.ScalingContainer
import kotlin.math.floor

/**
 * The playfield, in osu!pixels, as osu!lazer's: hit objects, spinners and cursors are placed, drawn and judged in it.
 *
 * It fills the space gameplay is laid out in and scales what's in it as osu!droid does: by the height over 480,
 * which this [ScalingContainer] does, with the playfield in its center, 4:3 and 80% of the height. Its sizes are
 * rounded as osu!droid rounds them, so it isn't exactly 4:3: what's in it is stretched by as much, less than 0.3%
 * (see [roundedScale]).
 *
 * Positions are converted from the scene with [toPlayfield], through the transformations of the playfield and of
 * what holds it (e.g. the playfield size setting), and the part of the playfield that's shown is [visibleArea].
 *
 * Its layers, from the bottom: [spinners], [underlay], for what's below the hit objects (follow points, the hit
 * lighting), [objects], for the hit objects, [approachCircles], for the pieces of the objects drawn above every
 * object (their approach circles, the slider balls and follow circles, through proxies), [judgements], for the
 * effects of the judgements, and [cursors], for the cursors and Flashlight. None clips, what's in them can go past
 * the playfield; the playfield clips to the space instead.
 */
class GameplayPlayfield : ScalingContainer() {

    // The playfield, in osu!pixels.
    private val content = Container().apply {
        setSize(Playfield.WIDTH, Playfield.HEIGHT)
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
    }

    /**
     * The layer of the spinners, below everything else.
     */
    val spinners = createLayer()

    /**
     * The layer below the hit objects.
     */
    val underlay = createLayer()

    /**
     * The layer of the hit objects.
     */
    val objects = createLayer()

    /**
     * The layer of the pieces of the objects drawn above every object, through proxies.
     */
    val approachCircles = createLayer()

    /**
     * The layer of the effects of the judgements.
     */
    val judgements = createLayer()

    /**
     * The layer of the cursors and Flashlight, above everything else.
     */
    val cursors = createLayer()

    /**
     * The particles of the judgements, in [judgements], which every object emits into.
     */
    val particles = GameplayParticles(judgements)

    /**
     * The part of the playfield that's shown, the whole space it's in, in osu!pixels: its left, top, right and bottom
     * edges. It goes past the playfield, as the space is wider than it, and a little taller.
     */
    var visibleArea = visibleAreaFor(Playfield.SPACE_WIDTH, 720f)
        private set


    init {
        mode = Mode.Height
        referenceSize = Vec2(Playfield.SPACE_WIDTH, REFERENCE_HEIGHT)
        clipping = ClipMode.Bounds

        content += spinners
        content += underlay
        content += objects
        content += approachCircles
        content += judgements
        content += cursors
        +content
    }


    private fun createLayer() = Container().apply {
        setSize(Playfield.WIDTH, Playfield.HEIGHT)
    }

    /**
     * Converts a position in the scene, e.g. of a pointer, to the playfield, in osu!pixels.
     */
    fun toPlayfield(sceneX: Float, sceneY: Float): Vec2 = content.toLocalSpaceFromScene(sceneX, sceneY)

    /**
     * Sets the size of the space the playfield is in, before it's measured in it, so what's placed before the first
     * layout (e.g. a spinner) knows the [visibleArea].
     */
    fun setSpaceSize(width: Float, height: Float) = update(width, height)

    private fun update(width: Float, height: Float) {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            return
        }

        val area = visibleAreaFor(width, height)
        if (area != visibleArea) {
            visibleArea = area
        }

        // The factor of this container is the height over 480, the playfield's own scale makes up the rest.
        val factor = height / REFERENCE_HEIGHT
        val scale = roundedScale(height)
        content.scaleX = scale.x / factor
        content.scaleY = scale.y / factor
    }

    override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
        update(constraints.availableWidth, constraints.availableHeight)
        return super.adjustParentConstraints(constraints)
    }


    companion object {

        // The height of the space the playfield is scaled to, as osu!droid scales objects by the height over it.
        private const val REFERENCE_HEIGHT = 480f

        /**
         * The scale from osu!pixels to a space [height] tall, horizontally and vertically: the playfield is 80% of
         * the height, rounded to whole pixels as osu!droid rounds it, and 4:3 by an integer division, so the scales
         * differ slightly.
         */
        fun roundedScale(height: Float): Vec2 {
            val playfieldHeight = (floor(height) * 0.8f).toInt()
            val playfieldWidth = playfieldHeight / 3 * 4

            return Vec2(playfieldWidth / Playfield.WIDTH, playfieldHeight / Playfield.HEIGHT)
        }

        /**
         * The part of the playfield shown in a space [width] by [height], in osu!pixels: see [visibleArea].
         */
        fun visibleAreaFor(width: Float, height: Float): Vec4 {
            val scale = roundedScale(height)
            val halfWidth = width / 2f / scale.x
            val halfHeight = height / 2f / scale.y
            val centerX = Playfield.WIDTH / 2f
            val centerY = Playfield.HEIGHT / 2f

            return Vec4(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
        }
    }
}
