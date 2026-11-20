package com.osudroid.game.session

import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.game.loading.LoadedPlay
import com.osudroid.ui.v2.game.FollowPointConnection
import com.osudroid.ui.v2.game.GameplayPlayfield
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.osudroid.utils.BitSet
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.container.Container
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameObjectPool
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Adds the objects of a play, and the follow points between them, as their lifetime starts.
 *
 * @param listener The listener of the objects, which judges them and has the context of the play.
 * @param spinnerLayer The layer spinners are added to, in the space gameplay is laid out in.
 * @param events Told of each object whose lifetime starts.
 * @param replayObjectData The results a replay recorded for each object, null unless one is watched.
 * @param comboColor The color of the combo of an object.
 * @param sliderBorderColor The color of the border of the sliders.
 */
class ObjectSpawner(
    private val listener: GameObjectListener,
    private val loaded: LoadedPlay,
    private val playfield: GameplayPlayfield,
    private val spinnerLayer: Container,
    private val stat: StatisticV2,
    private val events: IGameplayEvents,
    private val replayObjectData: Array<Replay.ReplayObjectData?>?,
    private val comboColor: (HitObject) -> Color4,
    private val sliderBorderColor: () -> Color4
) {
    private val objects = loaded.objects

    // The object whose follow points are added next, as they start to fade in, see FollowPointConnection.fadeInStartTime.
    private var followPointIndex = 0

    // The slider added next, of the paths of the play.
    private var sliderIndex = 0

    /**
     * The objects whose lifetime started, the first ones of the beatmap.
     */
    var objectIndex = 0
        private set

    /**
     * The id of the last object added.
     */
    var lastObjectId = -1
        private set

    /**
     * The time from the last object added to the next, in seconds, which health drains over. 0 after the last one.
     */
    var distanceToNextObject = 0.0
        private set

    /**
     * Whether the lifetime of every object started.
     */
    val isDone: Boolean
        get() = objectIndex >= objects.size


    /**
     * Adds the follow points that start to fade in by [time], in seconds: that can be before the object they start
     * from is added.
     */
    fun spawnFollowPoints(time: Float) {
        while (followPointIndex < objects.size - 1) {
            val start = objects[followPointIndex]

            if (time < FollowPointConnection.fadeInStartTime(start)) {
                break
            }

            val end = objects[++followPointIndex]

            // A seek went past them.
            if (time >= end.startTime.toFloat() / 1000) {
                continue
            }

            if (start !is Spinner && end !is Spinner && !start.isLastInCombo) {
                FollowPointConnection.addConnection(playfield.underlay, start, end)
            }
        }
    }

    /**
     * Adds the objects whose lifetime starts by [time], in seconds, each judged at once for the time since.
     *
     * @param timingPoint The timing point the play is in, which sets the least time health drains over.
     * @param musicLength The length of the music, in milliseconds.
     * @param onLifetimeStart Called as the lifetime of an object starts, before it's added.
     * @return Whether an object starts after the end of the music, which ends the play.
     */
    fun spawn(time: Float, timingPoint: TimingControlPoint, musicLength: Int, onLifetimeStart: () -> Unit): Boolean {
        val context = listener.context

        while (objectIndex < objects.size) {
            val obj = objects[objectIndex]

            // The casts can be simplified, but it is necessary to prevent floating point errors (see how
            // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
            // are cast and converted to seconds individually).
            val lifetimeStart = obj.startTime.toFloat() / 1000 - obj.timePreempt.toFloat() / 1000
            val lifetimeDt = time - lifetimeStart

            if (lifetimeDt < 0) {
                break
            }

            ++objectIndex
            onLifetimeStart()

            if (obj.startTime > musicLength) {
                return true
            }

            val nextObj = if (objectIndex < objects.size) objects[objectIndex] else null

            distanceToNextObject = if (nextObj != null)
                max(nextObj.startTime - obj.startTime, timingPoint.msPerBeat / 2) / 1000
            else
                0.0

            events.onHitObjectLifetimeStart(obj)

            var gameObject: GameObject? = null

            if (obj is HitCircle) {
                val gameplayCircle = GameObjectPool.getInstance().circle

                gameplayCircle.init(listener, playfield, obj, comboColor(obj))
                listener.addObject(gameplayCircle)

                if (context.isAutoplay) {
                    gameplayCircle.setAutoPlay()
                }

                gameplayCircle.setId(++lastObjectId)

                if (replayObjectData != null) {
                    gameplayCircle.setReplayData(replayObjectData[gameplayCircle.getId()])
                }

                gameObject = gameplayCircle
            } else if (obj is Spinner) {
                val rps = 2 + 2 * loaded.playableBeatmap.difficulty.od / 10f
                val gameplaySpinner = GameObjectPool.getInstance().spinner

                gameplaySpinner.init(listener, spinnerLayer, playfield, obj, rps, stat)
                listener.addObject(gameplaySpinner)

                if (context.isAutoplay || context.isAutopilot) {
                    gameplaySpinner.setAutoPlay()
                }

                gameplaySpinner.setId(++lastObjectId)
                if (replayObjectData != null) {
                    gameplaySpinner.setReplayData(replayObjectData[gameplaySpinner.getId()])
                }

                gameObject = gameplaySpinner
            } else if (obj is Slider) {
                val gameplaySlider = GameObjectPool.getInstance().slider

                gameplaySlider.init(listener, playfield, stat, obj, loaded.playableBeatmap.controlPoints,
                    comboColor(obj), sliderBorderColor(), loaded.sliderPaths[sliderIndex], loaded.sliderRenderPaths[sliderIndex])

                ++sliderIndex
                listener.addObject(gameplaySlider)

                if (context.isAutoplay) {
                    gameplaySlider.setAutoPlay()
                }

                gameplaySlider.setId(++lastObjectId)
                if (replayObjectData != null) {
                    gameplaySlider.setReplayData(replayObjectData[gameplaySlider.getId()])
                    if (gameplaySlider.getReplayData()!!.tickSet == null)
                        gameplaySlider.getReplayData()!!.tickSet = BitSet()
                }

                gameObject = gameplaySlider
            }

            gameObject?.updateJudgement(lifetimeDt)
        }

        return false
    }

    /**
     * Moves past the first [objectCount] objects, judged before a seek: the next ones are added as their lifetime
     * starts, and the follow points from the start, those a seek went past skipped.
     */
    fun seekTo(objectCount: Int) {
        objectIndex = objectCount
        lastObjectId = objectCount - 1
        sliderIndex = (0 until objectCount).count { objects[it] is Slider }
        followPointIndex = 0
    }

    /**
     * Goes back to the first object, as the attempt starts again.
     */
    fun reset() {
        objectIndex = 0
        followPointIndex = 0
        lastObjectId = -1
        sliderIndex = 0
    }

    /**
     * Moves past the objects left, as a failed play saved: [onSkip] is given the id of each.
     */
    fun skipRemaining(onSkip: (id: Int) -> Unit) {
        while (objectIndex < objects.size) {
            ++objectIndex
            onSkip(++lastObjectId)
        }
    }
}
