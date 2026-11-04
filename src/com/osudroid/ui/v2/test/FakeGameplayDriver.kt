package com.osudroid.ui.v2.test

import android.graphics.PointF
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.hud.IGameplayEvents
import com.reco1l.verktex.testing.UITestStage
import kotlin.random.Random

/**
 * Makes up a play on the [FakeGameScene], hitting notes with some error now and then, and sends
 * the gameplay events to [listeners] as the game scene sends them to the HUD.
 *
 * The play lasts as long as the fake scene's objects do and starts over after.
 */
class FakeGameplayDriver(stage: UITestStage, private val listeners: List<IGameplayEvents>) {

    private val gameScene = TestGameContext.gameScene

    private val random = Random(727)

    private val hitObject = HitCircle(0.0, Vector2(256f, 192f), true, 0)

    private var nextNoteTime = 0f


    init {
        gameScene.resetPlay()

        stage.onEachFrame { frame ->
            val deltaTime = frame.elapsedFrameTime
            gameScene.time += deltaTime

            if (gameScene.time >= gameScene.lastObjectEndTime) {
                restart()
            }

            while (gameScene.time >= nextNoteTime) {
                hitNote()
                nextNoteTime += NOTE_INTERVAL
            }

            listeners.forEach { it.onGameplayUpdate(gameScene, deltaTime) }
        }

        stage.onDispose { gameScene.resetPlay() }
    }


    private fun restart() {
        gameScene.resetPlay()
        nextNoteTime = 0f
        listeners.forEach { it.onSeek() }
    }

    private fun hitNote() {
        val stat = gameScene.stat
        val time = gameScene.time
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

            val objectPosition = PointF(256f, 192f)
            val cursorPosition = PointF(256f + random.nextFloat() * 40f - 20f, 192f + random.nextFloat() * 40f - 20f)
            listeners.forEach { it.onAimJudgement(objectPosition, cursorPosition, OBJECT_RADIUS) }
        }

        listeners.forEach { it.onNoteHit(stat) }
    }


    private companion object {
        const val NOTE_INTERVAL = 0.2f
        const val OBJECT_RADIUS = 50f
    }
}

