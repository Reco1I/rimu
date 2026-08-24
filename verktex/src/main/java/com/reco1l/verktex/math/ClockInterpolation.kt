package com.reco1l.verktex.math

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.time.TickingClock


//region Float

fun TickingClock.interpolateFloat(duration: Float, start: Float, end: Float, easing: Easing = Easing.None): Float {
    return FloatInterpolation.floatAt(elapsedTickTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
}

//endregion

//region Color

fun TickingClock.interpolateColor(duration: Float, start: Color4, end: Color4, easing: Easing = Easing.None): Color4 {
    return ColorInterpolation.colorAt(elapsedTickTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
}

//endregion




