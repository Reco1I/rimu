package com.reco1l.verktex.math

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.time.FrameClock
import kotlin.math.abs
import kotlin.math.exp


// These are called every frame with the current value as start, so they smooth it towards the end
// independently of the frame rate, reaching it after about the given duration. Being stateless they
// cannot follow an easing curve, which would need the start value and time of the transition.

private fun FrameClock.smoothingFactor(duration: Float): Float {
    if (duration <= 0f) return 1f
    return 1f - exp(-5f * elapsedFrameTime / duration)
}

//region Float

fun FrameClock.interpolateFloat(duration: Float, start: Float, end: Float): Float {
    val value = start + (end - start) * smoothingFactor(duration)
    // Snaps once close enough, as the smoothing never reaches the end exactly.
    return if (abs(end - value) < 1e-3f) end else value
}

//endregion

//region Color

fun FrameClock.interpolateColor(duration: Float, start: Color4, end: Color4): Color4 {
    return ColorInterpolation.colorAt(smoothingFactor(duration), start, end, 0f, 1f)
}

//endregion
