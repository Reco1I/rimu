package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.ui.v2.skin.JudgementPiece
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class GameObjectPool private constructor() {
    @JvmField
    var circles = ArrayDeque<GameplayHitCircle>()
    private val judgements = HashMap<String, ArrayDeque<JudgementPiece>>()
    @JvmField
    var sliders = ArrayDeque<GameplaySlider>()
    @JvmField
    var spinners = ArrayDeque<GameplaySpinner>()
    var objectsCreated = 0
        private set

    val circle: GameplayHitCircle
        get() {
            if (!circles.isEmpty()) {
                return circles.removeFirst()
            }

            objectsCreated++
            return GameplayHitCircle()
        }

    fun putCircle(circle: GameplayHitCircle) {
        circles.add(circle)
    }

    val spinner: GameplaySpinner
        get() {
            if (!spinners.isEmpty()) {
                return spinners.removeFirst()
            }

            objectsCreated++
            return GameplaySpinner()
        }

    fun putSpinner(spinner: GameplaySpinner) {
        spinners.add(spinner)
    }

    /**
     * A judgement piece of the skin for the texture [textureName], see [JudgementPiece].
     */
    fun getJudgement(textureName: String): JudgementPiece {
        judgements[textureName]?.removeFirstOrNull()?.let { return it }

        objectsCreated++
        return SkinPieces.current.create(SkinComponent.Judgement(textureName))
    }

    fun putJudgement(piece: JudgementPiece) {
        judgements.getOrPut(piece.textureName) { ArrayDeque() }.add(piece)
    }

    val slider: GameplaySlider
        get() {
            if (!sliders.isEmpty()) {
                return sliders.removeFirst()
            }

            objectsCreated++
            return GameplaySlider()
        }

    fun putSlider(slider: GameplaySlider) {
        sliders.add(slider)
    }

    fun purge() {
        judgements.clear()
        circles.clear()
        sliders.clear()
        spinners.clear()

        objectsCreated = 0
    }

    fun preload() {
        for (i in 0 until 10) {
            putCircle(GameplayHitCircle())
        }
        for (i in 0 until 5) {
            putSlider(GameplaySlider())
        }
        for (i in 0 until 3) {
            putSpinner(GameplaySpinner())
        }

        objectsCreated = circles.size + sliders.size + spinners.size
    }

    companion object {
        @JvmField
        var instance = GameObjectPool()

        @JvmStatic
        fun getInstance(): GameObjectPool {
            return instance
        }
    }
}
