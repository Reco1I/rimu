package com.osudroid.ui.v2.game

import com.osudroid.game.PlayfieldMapping
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.ScalingContainer

/**
 * The playfield hit objects are drawn in, in osu!pixels, as osu!lazer's.
 *
 * It fills the space gameplay is laid out in and scales what's in it as osu!droid does: by the height over 480,
 * which this [ScalingContainer] does, with the playfield in its center, 4:3 and 80% of the height, its sizes
 * rounded as osu!droid rounds them (see [PlayfieldMapping]). Positions are exactly osu!droid's; as the rounded
 * playfield isn't exactly 4:3, objects are stretched by as much, less than 0.3%.
 *
 * Its layers, from the bottom: [underlay], for what's below the hit objects (follow points, the hit lighting),
 * [objects], for the hit objects, [approachCircles], for the pieces of the objects drawn above every object (their
 * approach circles, the slider balls and follow circles, through proxies), and [judgements], for the effects of
 * the judgements. None clips, objects can go past the playfield; the playfield clips to the space instead.
 */
class GameplayPlayfield : ScalingContainer() {

    /**
     * The mapping of the playfield, from the size of the space it was last measured in.
     */
    var mapping = PlayfieldMapping(PlayfieldMapping.REFERENCE_WIDTH, 720f)
        private set

    // The playfield, in osu!pixels.
    private val content = Container().apply {
        setSize(PlayfieldMapping.PLAYFIELD_WIDTH, PlayfieldMapping.PLAYFIELD_HEIGHT)
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
    }

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
     * The layer of the effects of the judgements, above everything else.
     */
    val judgements = createLayer()


    init {
        mode = Mode.Height
        referenceSize = Vec2(PlayfieldMapping.REFERENCE_WIDTH, 480f)
        clipping = ClipMode.Bounds

        content += underlay
        content += objects
        content += approachCircles
        content += judgements
        +content
    }


    private fun createLayer() = Container().apply {
        setSize(PlayfieldMapping.PLAYFIELD_WIDTH, PlayfieldMapping.PLAYFIELD_HEIGHT)
    }

    /**
     * Sets the size of the space the playfield is in, before it's measured in it.
     */
    fun setSpaceSize(width: Float, height: Float) = updateMapping(width, height)

    private fun updateMapping(width: Float, height: Float) {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            return
        }

        if (mapping.width != width || mapping.height != height) {
            mapping = PlayfieldMapping(width, height)
        }

        // The factor of this container is the height over 480, the playfield's own scale makes up the rest.
        val factor = height / referenceSize.y
        content.scaleX = mapping.scaleX / factor
        content.scaleY = mapping.scaleY / factor
    }

    override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
        updateMapping(constraints.availableWidth, constraints.availableHeight)
        return super.adjustParentConstraints(constraints)
    }
}
