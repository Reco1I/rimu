package com.osudroid.ui.v2.hud

import com.osudroid.math.Vector2
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

interface IGameplayEvents {

    fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float)

    /**
     * Called when a pointer down event occurs in gameplay.
     *
     * @param time The time in seconds when the event occurred with respect to gameplay time.
     */
    fun onGameplayTouchDown(time: Float)

    fun onHitObjectLifetimeStart(obj: HitObject)

    fun onNoteHit(statistics: StatisticV2)

    fun onBreakStateChange(isBreak: Boolean)

    fun onAccuracyRegister(type: HitObjectType, accuracy: Float)

    /**
     * Called for non-miss judgements on circles and slider heads, with the object's and cursor's screen-space
     * positions at the time of the hit.
     *
     * @param objectPosition The screen-space position of the hit circle that was judged.
     * @param cursorPosition The screen-space position of the cursor that produced the judgement.
     * @param objectRadius The screen-space radius of the hit circle that was judged.
     */
    fun onAimJudgement(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float)

    /**
     * Called when the gameplay is seeked to a new position.
     */
    fun onSeek()

}