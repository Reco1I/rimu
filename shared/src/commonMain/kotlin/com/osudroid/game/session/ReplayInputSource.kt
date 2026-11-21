package com.osudroid.game.session

import com.osudroid.game.Cursor
import com.osudroid.game.CursorEvent
import com.reco1l.verktex.input.PointerEvent
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * Plays the cursor movements of a [replay] back into the [cursors] of a play, as the player made them.
 *
 * @param isFlipped Whether the playfield is flipped vertically, as Hard Rock flips it.
 * @param onTouchDown Called as a cursor is pressed, with the time it's pressed at in seconds.
 */
class ReplayInputSource(
    private val replay: Replay,
    private val cursors: Array<Cursor?>,
    private val isFlipped: Boolean,
    private val onTouchDown: (time: Float) -> Unit
) {

    /**
     * Adds the movements reached by [time], in seconds, to the cursors, and moves them in between.
     *
     * @param deltaTime The length of the frame, in seconds: the movements a quarter of it ahead are added too.
     */
    fun update(time: Float, deltaTime: Float) {
        val mSecPassed = time * 1000
        val replayTimeMs = time * 1000
        val replayCatchUpTimeMs = (time + deltaTime / 4) * 1000
        var cIndex: Int

        for (i in 0 until replay.cursorIndex.size) {
            if (replay.cursorMoves.size <= i) {
                break
            }

            val moveArray = replay.cursorMoves[i]

            cIndex = replay.cursorIndex[i]
            var movement: Replay.ReplayMovement? = null

            // Emulating moves
            while (cIndex < moveArray.size) {
                // As in the condition it was: the movement is kept even when it's not reached yet.
                movement = moveArray.movements[cIndex]

                if (movement.time > replayCatchUpTimeMs) {
                    break
                }

                val event = CursorEvent.obtain()

                event.systemTime = movement.time.toLong()
                event.trackTime = movement.time.toFloat()
                event.offset = 0.0
                event.setTrackPosition(movement.x, movement.y, isFlipped)

                if (movement.touchType == TouchType.DOWN) {
                    event.action = PointerEvent.Action.Down
                    replay.lastMoveIndex[i] = -1
                    onTouchDown(movement.time / 1000f)
                } else if (movement.touchType == TouchType.MOVE) {
                    event.action = PointerEvent.Action.Move
                    replay.lastMoveIndex[i] = cIndex
                } else {
                    event.action = PointerEvent.Action.Up
                }
                cursors[i]!!.addEvent(event)
                replay.cursorIndex[i]++
                cIndex++
            }

            // Interpolating cursor movements
            if (movement != null && movement.touchType == TouchType.MOVE && replay.lastMoveIndex[i] >= 0) {
                val lIndex = replay.lastMoveIndex[i]
                val lastMovement = moveArray.movements[lIndex]
                val movementTime = movement.time
                val lastMovementTime = lastMovement.time
                val duration = lastMovementTime - movementTime

                if (duration == 0) {
                    continue
                }

                val t = (replayTimeMs - movementTime) / duration

                val event = CursorEvent.obtain()

                // We don't exactly need systemTime to be accurate here since it's not used for anything important
                // in replays.
                event.systemTime = mSecPassed.toLong()
                event.trackTime = mSecPassed.toLong().toFloat()
                event.offset = 0.0
                event.action = PointerEvent.Action.Move
                event.setTrackPosition(
                    lastMovement.x * t + movement.x * (1 - t),
                    lastMovement.y * t + movement.y * (1 - t),
                    isFlipped
                )

                cursors[i]!!.addEvent(event)
            }
        }
    }

    /**
     * Moves to [time], in seconds, as a seek does: the movements are played back from the start again, and the
     * presses before [time] are given to [onTouchDown] for what keeps their history.
     */
    fun seekTo(time: Float) {
        replay.cursorIndex.fill(0)
        replay.lastMoveIndex.fill(-1)

        for (i in 0 until replay.cursorMoves.size) {
            val moveArray = replay.cursorMoves[i]

            for (j in 0 until moveArray.size) {
                val movement = moveArray.movements[j]
                val tapTime = movement.time / 1000f

                if (tapTime > time) {
                    break
                }

                if (movement.touchType == TouchType.DOWN) {
                    onTouchDown(tapTime)
                }
            }
        }
    }
}
