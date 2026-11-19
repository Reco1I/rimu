package com.osudroid.game

/**
 * The result of a judgement as its effects show it, from the name of the texture the game gives it.
 */
enum class HitResult {
    Great,
    Ok,
    Meh,
    Miss,

    /**
     * The head or a repeat of a slider.
     */
    SliderPoint,

    /**
     * A tick of a slider.
     */
    Tick;

    companion object {

        /**
         * The result shown with the judgement texture [name], e.g. `hit300g` or `sliderpoint30`.
         */
        fun fromTexture(name: String) = when {
            name.startsWith("hit300") -> Great
            name.startsWith("hit100") -> Ok
            name.startsWith("hit50") -> Meh
            name == "sliderpoint30" -> SliderPoint
            name == "sliderpoint10" -> Tick
            else -> Miss
        }
    }
}
