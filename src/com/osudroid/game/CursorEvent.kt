package com.osudroid.game

import com.reco1l.verktex.Platform
import com.osudroid.math.Vector2
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.verktex.input.PointerEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.game.GameHelper

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
     * The time at which this [CursorEvent] occurred, accounting for [Config.isFixFrameOffset], in the gameplay time
     * base (milliseconds since the start of the track).
     */
    val hitTime
        get() = trackTime + if (Config.isFixFrameOffset()) offset else 0.0

    /**
     * The position of the cursor in screen space.
     */
    @JvmField
    var position = Vector2(0f)

    /**
     * The position of the cursor in playfield space.
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
        copy.trackPosition.set(trackPosition)
    }

    private fun apply(event: PointerEvent, x: Float, y: Float) {
        systemTime = event.time
        trackTime = 0f
        action = event.action
        offset = 0.0

        val width = Config.getRES_WIDTH().toFloat()
        val height = Config.getRES_HEIGHT().toFloat()

        position.x = x.coerceIn(0f, width)
        position.y = y.coerceIn(0f, height)

        trackPosition.x = x
        trackPosition.y = y

        if (GameHelper.isHardRock()) {
            trackPosition.y -= height / 2f
            trackPosition.y *= -1
            trackPosition.y += height / 2f
        }

        trackPosition.x -= (width - Constants.MAP_ACTUAL_WIDTH) / 2f
        trackPosition.y -= (height - Constants.MAP_ACTUAL_HEIGHT) / 2f

        trackPosition.x *= Constants.MAP_WIDTH / Constants.MAP_ACTUAL_WIDTH.toFloat()
        trackPosition.y *= Constants.MAP_HEIGHT / Constants.MAP_ACTUAL_HEIGHT.toFloat()
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
         * @param x The horizontal position of the event in the playfield.
         * @param y The vertical position of the event in the playfield.
         * @return An instance of [CursorEvent].
         */
        @JvmStatic
        fun obtain(event: PointerEvent, x: Float, y: Float) = obtain().also { it.apply(event, x, y) }
    }
}