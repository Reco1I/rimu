package com.osudroid.ui.v2.test

import com.osudroid.beatmaps.DroidHitWindow
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.reco1l.verktex.testing.UITestStage
import kotlin.random.Random
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Makes up a play on [gameScene], which isn't playing: hits notes with some error now and then, and
 * sends the gameplay events to [listeners] as the game scene sends them to the HUD.
 *
 * Only the statistics of [gameScene] follow the play: its time is the one of its gameplay clock,
 * which doesn't run. The play lasts [PLAY_LENGTH] and starts over after.
 */
class FakeGameplayDriver(
    stage: UITestStage,
    private val gameScene: GameScene,
    private val listeners: List<IGameplayEvents>
) {

    private val random = Random(727)

    private val hitObject = HitCircle(0.0, Vector2(256f, 192f), true, 0)

    private var time = 0f

    private var nextNoteTime = 0f


    init {
        resetPlay()

        stage.onEachFrame { frame ->
            val deltaTime = frame.elapsedFrameTime
            time += deltaTime

            if (time >= PLAY_LENGTH) {
                restart()
            }

            while (time >= nextNoteTime) {
                hitNote()
                nextNoteTime += NOTE_INTERVAL
            }

            listeners.forEach { it.onGameplayUpdate(gameScene, deltaTime) }
        }

    }


    private fun resetPlay() {
        time = 0f
        gameScene.stat = StatisticV2()
        gameScene.hitWindow = DroidHitWindow(8f)
        gameScene.firstObjectStartTime = 0f
        gameScene.lastObjectEndTime = PLAY_LENGTH
    }

    private fun restart() {
        resetPlay()
        nextNoteTime = 0f
        listeners.forEach { it.onSeek() }
    }

    private fun hitNote() {
        val stat = gameScene.stat
        val roll = random.nextFloat()

        listeners.forEach {
            it.onHitObjectLifetimeStart(hitObject)
            it.onGameplayTouchDown(time)
        }

        val (score, maxError) = when {
            roll < 0.75f -> 300 to 0.025f
            roll < 0.92f -> 100 to 0.07f
            roll < 0.97f -> 50 to 0.1f
            else -> 0 to 0f
        }

        if (score == 0) {
            stat.registerHit(0, true, false)
        } else {
            val offset = (random.nextFloat() * 2f - 1f) * maxError
            stat.registerHit(score, false, false)
            stat.addHitOffset(offset.toDouble())
            listeners.forEach { it.onAccuracyRegister(HitObjectType.Normal, offset) }

            val objectPosition = Vector2(256f, 192f)
            val cursorPosition = Vector2(256f + random.nextFloat() * 40f - 20f, 192f + random.nextFloat() * 40f - 20f)
            listeners.forEach { it.onAimJudgement(objectPosition, cursorPosition, OBJECT_RADIUS) }
        }

        listeners.forEach { it.onNoteHit(stat) }
    }


    private companion object {
        const val PLAY_LENGTH = 60f
        const val NOTE_INTERVAL = 0.2f
        const val OBJECT_RADIUS = 50f
    }
}

