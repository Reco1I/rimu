package com.osudroid.game

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.Platform
import com.osudroid.math.Vector2
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.verktex.input.PointerEvent
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Represents a cursor event in gameplay.
 */
class CursorEvent : IPoolable {
    private constructor()

    override var isRecycled = false

    /**
     * The time at which the [CursorEvent] occurred, in the [Platform.uptimeMillis] time base.
     */
    @JvmField
    var systemTime = 0L

    /**
     * The time at which the [CursorEvent] occurred, in the gameplay time base (milliseconds since the start of the track).
     */
    @JvmField
    var trackTime = 0f

    /**
     * The offset of the [CursorEvent] from the last frame, in milliseconds.
     */
    @JvmField
    var offset = 0.0

    /**
     * The action of the [CursorEvent], which can be one of [PointerEvent.Action.Down], [PointerEvent.Action.Move], or [PointerEvent.Action.Up].
     */
    @JvmField
    var action = PointerEvent.Action.Down

    /**
     * Whether the action is [PointerEvent.Action.Down].
     */
    val isActionDown
        get() = action == PointerEvent.Action.Down

    /**
     * Whether the action is [PointerEvent.Action.Move].
     */
    val isActionMove
        get() = action == PointerEvent.Action.Move

    /**
     * Whether the action is [PointerEvent.Action.Up].
     */
    val isActionUp
        get() = action == PointerEvent.Action.Up

    /**
     * The time at which this [CursorEvent] occurred, accounting for [com.osudroid.settings.OsuSettings.FixFrameOffset], in the gameplay time
     * base (milliseconds since the start of the track).
     */
    val hitTime
        get() = trackTime + if (Settings[OsuSettings.FixFrameOffset]) offset else 0.0

    /**
     * The position of the cursor in the space gameplay is laid out in (see [PlayfieldMapping]), kept within it.
     */
    @JvmField
    var position = Vector2(0f)

    /**
     * The position of the cursor in the playfield, in osu!pixels, as hit objects are placed: flipped as they are by
     * Hard Rock. Hit objects are judged with it.
     */
    @JvmField
    var playfieldPosition = Vector2(0f)

    /**
     * The position of the cursor in the playfield, in osu!pixels, as replays store it: not flipped by Hard Rock.
     */
    @JvmField
    var trackPosition = Vector2(0f)

    /**
     * Releases this [CursorEvent] back to the pool for reuse.
     */
    fun release() {
        pool.release(this)
    }

    /**
     * Copies this [CursorEvent] to a new instance.
     */
    fun copy() = obtain().also { copy ->
        copy.systemTime = systemTime
        copy.trackTime = trackTime
        copy.action = action
        copy.offset = offset
        copy.position.set(position)
        copy.playfieldPosition.set(playfieldPosition)
        copy.trackPosition.set(trackPosition)
    }

    /**
     * Sets the positions of this [CursorEvent] from a position in the playfield as replays store it, in osu!pixels.
     *
     * @param isFlipped Whether the playfield is flipped vertically, as Hard Rock flips it: replays store positions as
     * they would be without it.
     */
    fun setTrackPosition(x: Float, y: Float, mapping: PlayfieldMapping, isFlipped: Boolean) {
        trackPosition.set(x, y)
        playfieldPosition.set(x, y)

        if (isFlipped) {
            PlayfieldMapping.flipVertically(playfieldPosition)
        }

        position.set(mapping.toResolution(playfieldPosition))
    }

    private fun apply(event: PointerEvent, x: Float, y: Float, mapping: PlayfieldMapping, isFlipped: Boolean) {
        systemTime = event.time
        trackTime = 0f
        action = event.action
        offset = 0.0

        position.x = x.coerceIn(0f, mapping.width)
        position.y = y.coerceIn(0f, mapping.height)

        // Judged where the cursor is shown, within the space.
        playfieldPosition.set(mapping.toPlayfield(position.x, position.y))

        // Stored where it was.
        trackPosition.set(mapping.toPlayfield(x, y))

        if (isFlipped) {
            PlayfieldMapping.flipVertically(trackPosition)
        }
    }

    companion object {
        private val pool = SynchronizedPool<CursorEvent>(100)

        /**
         * Obtains an instance of [CursorEvent] from the pool or creates a new one if the pool is empty.
         *
         * @return An instance of [CursorEvent].
         */
        @JvmStatic
        fun obtain() = pool.acquire() ?: CursorEvent()

        /**
         * Obtains an instance of [CursorEvent] from the pool or creates a new one if the pool is empty.
         *
         * @param event The [PointerEvent] to apply to the [CursorEvent].
         * @param x The horizontal position of the event in the space gameplay is laid out in.
         * @param y The vertical position of the event in the space gameplay is laid out in.
         * @param mapping The mapping of the playfield in that space.
         * @param isFlipped Whether the playfield is flipped vertically, as Hard Rock flips it.
         * @return An instance of [CursorEvent].
         */
        @JvmStatic
        fun obtain(event: PointerEvent, x: Float, y: Float, mapping: PlayfieldMapping, isFlipped: Boolean) =
            obtain().also { it.apply(event, x, y, mapping, isFlipped) }
    }
}