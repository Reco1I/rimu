package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.ui.v2.SkinTextures
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class GameObjectPool private constructor() {
    @JvmField
    var circles = ArrayDeque<GameplayHitCircle>()
    @JvmField
    var effects: MutableMap<String, ArrayDeque<GameEffect>> = HashMap()
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

    // The style of spinners the skin has, see SkinTextures.isNewStyleSpinner. Found once the game loads.
    private var isNewStyleSpinner = true

    val spinner: GameplaySpinner
        get() {
            while (!spinners.isEmpty()) {
                val spinner = spinners.removeFirst()

                // Pooled spinners can be of the style of a previous skin.
                if ((spinner is GameplayModernSpinner) == isNewStyleSpinner) {
                    return spinner
                }
            }

            objectsCreated++

            return createSpinner()
        }

    private fun createSpinner() = if (isNewStyleSpinner) GameplayModernSpinner() else GameplaySpinner()

    fun putSpinner(spinner: GameplaySpinner) {
        spinners.add(spinner)
    }

    fun getEffect(texname: String): GameEffect {
        if (effects.containsKey(texname) && !effects[texname]!!.isEmpty()) {
            return effects[texname]!!.removeFirst()
        }

        objectsCreated++
        return GameEffect(texname)
    }

    fun putEffect(effect: GameEffect) {
        if (!effects.containsKey(effect.texname)) {
            effects[effect.texname] = ArrayDeque()
        }

        effects[effect.texname]!!.add(effect)
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
        effects.clear()
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
        isNewStyleSpinner = SkinTextures.isNewStyleSpinner()
        spinners.removeAll { (it is GameplayModernSpinner) != isNewStyleSpinner }

        for (i in 0 until 3) {
            putSpinner(createSpinner())
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
