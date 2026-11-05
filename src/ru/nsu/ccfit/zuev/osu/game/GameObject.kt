package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.game.CursorEvent
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.mods.ModRelax
import com.reco1l.verktex.ui.Choreography
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.Replay

abstract class GameObject {
    @JvmField
    protected var endsCombo = false
    @JvmField
    protected var autoPlay = false
    @JvmField
    protected var hitTime = 0f
    @JvmField
    protected var id = -1
    @JvmField
    protected var replayObjectData: Replay.ReplayObjectData? = null
    @JvmField
    protected var startHit = false
    @JvmField
    protected var position = Vector2(0f)

    fun getReplayData(): Replay.ReplayObjectData? {
        return replayObjectData
    }

    fun setReplayData(replayObjectData: Replay.ReplayObjectData?) {
        this.replayObjectData = replayObjectData
    }

    fun setAutoPlay() {
        autoPlay = true
    }

    abstract fun update(dt: Float)

    fun getHitTime(): Float {
        return hitTime
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getPosition(): Vector2 {
        return position
    }

    fun isStartHit(): Boolean {
        return startHit
    }

    open fun playLoopingSamples() {}

    open fun stopLoopingSamples() {}

    /**
     * Obtains the [CursorEvent] that hits this [GameObject], if any.
     *
     * @param listener The [GameObjectListener] to get cursors from.
     * @param hitObject The [HitObject] associated with this [GameObject].
     * @param objectElapsedTime The elapsed time since the start of this [GameObject], in seconds.
     * @return The [CursorEvent] that hits this [GameObject], or `null` if none.
     */
    protected fun getHittingCursor(listener: GameObjectListener, hitObject: HitObject,
                                   objectElapsedTime: Double): CursorEvent? {
        val cursorCount = listener.getCursorsCount()

        // For Autopilot, we only need to check if a cursor is pressed.
        if (GameHelper.isAutopilot()) {
            for (i in 0 until cursorCount) {
                val cursor = listener.getCursor(i)
                val events = cursor.downEvents
                val size = events.size

                while (cursor.latestProcessedDownEventIndex < size) {
                    val event = events[cursor.latestProcessedDownEventIndex++]

                    if (canHit(event, hitObject.hitWindow!!)) {
                        return event
                    }
                }
            }

            return null
        }

        // For Relax, we need to check two cases:
        // 1. If a cursor flows over the object when it is hittable.
        // 2. If a cursor is pressed while it is over the object when it is hittable.
        if (GameHelper.isRelax()) {
            for (i in 0 until cursorCount) {
                val cursor = listener.getCursor(i)
                val events = cursor.events
                val size = events.size

                if (size > 0) {
                    while (cursor.latestProcessedEventIndex < size) {
                        val event = events[cursor.latestProcessedEventIndex++]

                        if (event.isActionUp) {
                            continue
                        }

                        val isHit = isHit(hitObject, event)

                        // Case 1
                        if (event.isActionDown && isHit && canHit(event, hitObject.hitWindow!!)) {
                            return event
                        }

                        // Case 2
                        if (objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit) {
                            return event
                        }
                    }
                } else {
                    // If there are no new events, check the latest event.
                    val event = cursor.latestEvent

                    // Only consider case 2 in this scenario, as the event should logically be marked as a move event
                    // even if it's a down event (no new events mean the user keeps pressing on the same spot).
                    if (event != null && !event.isActionUp && objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit(hitObject, event)) {
                        return event
                    }
                }
            }

            return null
        }

        // In regular gameplay, we need to iteratively check whether one or more cursors is on the object while a cursor
        // is being pressed.
        for (i in 0 until cursorCount) {
            val cursor = listener.getCursor(i)
            val downEvents = cursor.downEvents
            val downEventsSize = downEvents.size

            // Consume down events that have not been processed yet, even those that are already past the hit threshold.
            // This ensures that start time ordered hit policy (aka "notelock") is enforced correctly.
            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                val downEvent = downEvents[cursor.latestProcessedDownEventIndex++]

                if (!canHit(downEvent, hitObject.hitWindow!!)) {
                    continue
                }

                for (j in 0 until cursorCount) {
                    if (i == j) {
                        // For the cursor that generated the down event, check if the event position is on the object.
                        if (isHit(hitObject, downEvent)) {
                            return downEvent
                        }
                    } else {
                        // For the other case, we need to check the event closest to the down event and check if it is
                        // on the object.
                        val otherCursor = listener.getCursor(j)
                        val closestEvent = otherCursor.getClosestEventBefore(downEvent.systemTime)

                        if (closestEvent == null || closestEvent.isActionUp) {
                            continue
                        }

                        if (isHit(hitObject, closestEvent)) {
                            return downEvent
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Determines whether a [CursorEvent] can hit this [GameObject].
     *
     * @param cursorEvent The [CursorEvent] to check.
     * @param hitWindow The [HitWindow] of this [GameObject].
     * @return `true` if the [CursorEvent] can hit this [GameObject], `false` otherwise.
     */
    private fun canHit(cursorEvent: CursorEvent, hitWindow: HitWindow): Boolean {
        if (cursorEvent.isActionUp) {
            return false
        }

        val hittableRange = if (GameHelper.isAutopilot())
            MathUtils.clamp(hitWindow.mehWindow + 100, 200.0, HitWindow.MISS_WINDOW).toFloat() / 1000
        else
            HitWindow.MISS_WINDOW.toFloat() / 1000

        return cursorEvent.hitTime / 1000 >= (hitTime - hittableRange).toDouble()
    }

    /**
     * Determines whether a [CursorEvent] hits this [GameObject]. The determination is solely based on
     * distance, not considering timing.
     *
     * @param hitObject The [HitObject] associated with this [GameObject].
     * @param cursorEvent The [CursorEvent] to check.
     * @return `true` if the [CursorEvent] hits this [GameObject], `false` otherwise.
     */
    private fun isHit(hitObject: HitObject, cursorEvent: CursorEvent): Boolean {
        if (cursorEvent.isActionUp) {
            return false
        }

        return Utils.squaredDistance(position, cursorEvent.position) <= Utils.sqr(hitObject.screenSpaceGameplayRadius.toFloat())
    }

    //region Lifetime management

    /**
     * Whether the underlying [HitObject] of this [GameObject] has been judged in its entirety, including
     * nested [HitObject]s.
     */
    open fun isJudged(): Boolean {
        return false
    }

    /**
     * Called when this [GameObject]'s lifetime expires.
     */
    open fun onExpire() {}

    /**
     * The time in seconds since the beatmap started at which this [GameObject]'s lifetime ends.
     */
    var lifetimeEnd = 0f
        protected set

    /**
     * Extends the lifetime of this [GameObject] to allow a [Choreography] to finish.
     *
     * @param choreography The [Choreography] to extend this [GameObject]'s lifetime with.
     */
    protected fun extendLifetime(choreography: Choreography) {
        lifetimeEnd = max(lifetimeEnd, choreography.endTime)
    }

    //endregion
}
