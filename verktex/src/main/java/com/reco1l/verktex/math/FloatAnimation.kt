package com.reco1l.verktex.math

import com.reco1l.verktex.time.FrameClock

/**
 * Animates a value towards a [target] along an [easing] curve over [duration] seconds.
 *
 * Unlike [interpolateFloat] it remembers where and when the transition started, so the easing
 * curve is followed as a whole, e.g. [Easing.OutBounce] bounces against the target.
 */
class FloatAnimation(
    initialValue: Float,
    private var duration: Float,
    private var easing: Easing = Easing.OutExpo
) {

    /**
     * The current value of the animation.
     */
    var value = initialValue
        private set

    /**
     * The value to animate to. Changing it starts a new transition from the current [value].
     */
    var target = initialValue
        set(target) {
            if (field == target) return
            field = target
            startValue = value
            elapsed = 0f
        }

    private var startValue = initialValue
    private var elapsed = duration


    /**
     * Starts a transition to [target] with its own [duration] and [easing], if not already
     * animating to it.
     */
    fun animateTo(target: Float, duration: Float, easing: Easing) {
        if (this.target == target) return
        this.duration = duration
        this.easing = easing
        this.target = target
    }

    /**
     * Sets [value] and [target] right away, stopping any transition.
     */
    fun jumpTo(value: Float) {
        this.value = value
        target = value
        elapsed = duration
    }

    /**
     * Advances the animation by the clock's elapsed frame time and returns the current [value].
     */
    fun update(clock: FrameClock): Float {
        if (elapsed < duration) {
            elapsed = (elapsed + clock.elapsedFrameTime).coerceAtMost(duration)
            value = startValue + (target - startValue) * easing.interpolate(elapsed / duration)
        }
        return value
    }
}
