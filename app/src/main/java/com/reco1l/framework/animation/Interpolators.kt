package com.reco1l.framework.animation

import org.andengine.util.modifier.ease.EaseExponentialIn
import org.andengine.util.modifier.ease.EaseExponentialOut
import android.animation.TimeInterpolator
import org.andengine.util.modifier.ease.EaseBounceIn
import org.andengine.util.modifier.ease.EaseBounceOut

object Ease
{

    // TODO Make functions inline or either convert IEaseFunction inheritors to TimeInterpolator
    //  (would require Engine changes).

    val EXPO_OUT = TimeInterpolator { EaseExponentialOut.getValue(it) }

    val EXPO_IN = TimeInterpolator { EaseExponentialIn.getValue(it) }

    val BOUNCE_OUT = TimeInterpolator { EaseBounceOut.getValue(it) }

    val BOUNCE_IN = TimeInterpolator { EaseBounceIn.getValue(it) }

    val DECELERATE = TimeInterpolator { 1.0f - (1.0f - it) * (1.0f - it) }

}