package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.ColorInterpolation
import com.reco1l.verktex.math.Easing

/**
 * Called when a transform or a step of a choreography has finished.
 */
fun interface OnTransformFinished {
    operator fun invoke(component: UIComponent)
}

/**
 * A single timed change of a property of a [UIComponent]. Created by the transform functions such as
 * [scaleTo] and advanced by [UIComponent.update].
 */
class Transform internal constructor(
    private val target: UIComponent,
    private val kind: Kind,
    internal val startTime: Float,
    private val duration: Float,
    private var easing: Easing,
    private val endX: Float = 0f,
    private val endY: Float = 0f,
    private val endColor: Color4 = Color4.White
) {

    private var hasStarted = false
    private var startX = 0f
    private var startY = 0f
    private var startColor = Color4.White

    internal var onFinished: OnTransformFinished? = null
        private set

    internal val endTime
        get() = startTime + duration


    /**
     * Sets the callback called when this transform has finished.
     */
    fun after(onFinished: OnTransformFinished): Transform {
        this.onFinished = onFinished
        return this
    }

    /**
     * Sets the easing of this transform.
     */
    fun eased(easing: Easing): Transform {
        this.easing = easing
        return this
    }

    /**
     * Returns a sequence whose next transforms start when this one ends.
     */
    fun then() = TransformSequence(target, endTime)


    /**
     * Applies this transform at the given time.
     *
     * @return Whether it has finished.
     */
    internal fun apply(time: Float): Boolean {
        if (time < startTime) {
            return false
        }

        if (!hasStarted) {
            captureStartValues()
            hasStarted = true
        }

        val hasEnded = duration <= 0f || time >= endTime
        val progress = if (hasEnded) 1f else easing.interpolate((time - startTime) / duration).coerceIn(0f, 1f)

        when (kind) {
            Kind.Delay -> Unit
            Kind.Scale -> {
                target.scaleX = lerp(startX, endX, progress)
                target.scaleY = lerp(startY, endY, progress)
            }
            Kind.Alpha -> target.alpha = lerp(startX, endX, progress)
            Kind.Rotation -> target.rotationZ = lerp(startX, endX, progress)
            Kind.TranslationX -> target.translationX = lerp(startX, endX, progress).px
            Kind.Translation -> {
                target.translationX = lerp(startX, endX, progress).px
                target.translationY = lerp(startY, endY, progress).px
            }
            // Only the RGB channels are animated so it can run alongside an alpha transform.
            Kind.Color -> target.color = ColorInterpolation.colorAt(progress, startColor, endColor, 0f, 1f).copy(alpha = target.alpha)
        }

        return hasEnded
    }

    private fun captureStartValues() {
        when (kind) {
            Kind.Delay -> Unit
            Kind.Scale -> {
                startX = target.scaleX
                startY = target.scaleY
            }
            Kind.Alpha -> startX = target.alpha
            Kind.Rotation -> startX = target.rotationZ
            Kind.TranslationX -> startX = target.translationX.resolve(target.bounds.width)
            Kind.Translation -> {
                startX = target.translationX.resolve(target.bounds.width)
                startY = target.translationY.resolve(target.bounds.height)
            }
            Kind.Color -> startColor = target.color
        }
    }

    private fun lerp(start: Float, end: Float, progress: Float) = start + (end - start) * progress


    internal enum class Kind {
        Delay,
        Scale,
        Alpha,
        Rotation,
        TranslationX,
        Translation,
        Color
    }
}


/**
 * The transforms running on a [UIComponent].
 */
internal class TransformTracker {

    // The time of the engine clock, which restarts when no transforms are left. A custom clock is used as it is.
    private var accumulatedTime = 0f

    private val transforms = ArrayList<Transform>()


    /**
     * The current time for the transforms of [target], in seconds.
     */
    fun currentTime(target: UIComponent) = target.activeClock?.time ?: accumulatedTime

    fun add(transform: Transform): Transform {
        transforms.add(transform)
        return transform
    }

    fun clear() {
        transforms.clear()
    }

    fun update(target: UIComponent, elapsedTime: Float) {
        if (transforms.isEmpty()) {
            accumulatedTime = 0f
            return
        }

        accumulatedTime += elapsedTime

        val time = currentTime(target)

        // Finished transforms call back into user code which can add transforms or clear them, so
        // the list is read by index on every step.
        var i = 0
        while (i < transforms.size) {
            val transform = transforms[i]

            if (transform.apply(time)) {
                transforms.removeAt(i)
                transform.onFinished?.invoke(target)
            } else {
                i++
            }
        }
    }
}


/**
 * A group of transforms on the same [UIComponent]. Transforms added one after another run at the same
 * time, use [then] or [delay] to make the next ones wait.
 */
class TransformSequence internal constructor(private val target: UIComponent, private var cursor: Float) {

    private var last: Transform? = null


    private fun add(kind: Transform.Kind, duration: Float, easing: Easing, x: Float = 0f, y: Float = 0f, color: Color4 = Color4.White): TransformSequence {
        val transform = Transform(target, kind, cursor, duration, easing, x, y, color)
        target.transformTracker().add(transform)

        val currentLast = last
        if (currentLast == null || transform.endTime >= currentLast.endTime) {
            last = transform
        }
        return this
    }


    fun scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.Scale, duration, easing, value, value)

    fun fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.Alpha, duration, easing, value)

    fun fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

    fun fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

    fun moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.Translation, duration, easing, x, y)

    fun moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.TranslationX, duration, easing, value)

    fun rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.Rotation, duration, easing, value)

    fun colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None) =
        add(Transform.Kind.Color, duration, easing, color = color)

    /**
     * Makes the next transforms start [duration] seconds after the current position of the sequence.
     */
    fun delay(duration: Float): TransformSequence {
        add(Transform.Kind.Delay, duration, Easing.None)
        cursor += duration
        return this
    }

    /**
     * Makes the next transforms start once all the previous ones have ended.
     */
    fun then(): TransformSequence {
        last?.let { cursor = it.endTime }
        return this
    }

    /**
     * Sets the callback called when the last transform of this sequence to end has finished.
     */
    fun after(onFinished: OnTransformFinished): TransformSequence {
        if (last == null) {
            add(Transform.Kind.Delay, 0f, Easing.None)
        }
        last!!.after(onFinished)
        return this
    }
}


private fun UIComponent.addTransform(kind: Transform.Kind, duration: Float, easing: Easing, x: Float = 0f, y: Float = 0f, color: Color4 = Color4.White): Transform {
    val tracker = transformTracker()
    return tracker.add(Transform(this, kind, tracker.currentTime(this), duration, easing, x, y, color))
}

/**
 * Creates a group of transforms starting after [delay] seconds, see [TransformSequence]. A negative delay
 * makes them start in the past, as if they were already running.
 */
inline fun UIComponent.transformSequence(delay: Float = 0f, block: TransformSequence.() -> Unit): TransformSequence {
    return newTransformSequence(delay).apply(block)
}

@PublishedApi
internal fun UIComponent.newTransformSequence(delay: Float): TransformSequence {
    return TransformSequence(this, transformTracker().currentTime(this) + delay)
}

/**
 * Animates [UIComponent.scale] to [value], in seconds.
 */
@JvmOverloads
fun UIComponent.scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.Scale, duration, easing, value, value)

/**
 * Animates [UIComponent.alpha] to [value], in seconds.
 */
@JvmOverloads
fun UIComponent.fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.Alpha, duration, easing, value)

/**
 * Animates [UIComponent.alpha] to 1, in seconds.
 */
@JvmOverloads
fun UIComponent.fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

/**
 * Animates [UIComponent.alpha] to 0, in seconds.
 */
@JvmOverloads
fun UIComponent.fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

/**
 * Animates [UIComponent.translationX] and [UIComponent.translationY] to the given pixel values, in seconds.
 */
@JvmOverloads
fun UIComponent.moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.Translation, duration, easing, x, y)

/**
 * Animates [UIComponent.translationX] to the given pixel value, in seconds.
 */
@JvmOverloads
fun UIComponent.moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.TranslationX, duration, easing, value)

/**
 * Animates [UIComponent.rotationZ] to [value] degrees, in seconds.
 */
@JvmOverloads
fun UIComponent.rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.Rotation, duration, easing, value)

/**
 * Animates the RGB channels of [UIComponent.color] to [color], in seconds.
 */
@JvmOverloads
fun UIComponent.colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None) =
    addTransform(Transform.Kind.Color, duration, easing, color = color)

/**
 * Does nothing for [duration] seconds. Useful to run code later through [Transform.after].
 */
fun UIComponent.delay(duration: Float) = addTransform(Transform.Kind.Delay, duration, Easing.None)

/**
 * Stops all transforms of this component, leaving its properties at their current values. Their
 * [Transform.after] callbacks are not called.
 */
fun UIComponent.clearTransforms() {
    transforms?.clear()
}
