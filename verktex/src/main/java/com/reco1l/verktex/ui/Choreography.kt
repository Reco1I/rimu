package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.ColorInterpolation
import com.reco1l.verktex.math.Easing

/**
 * A timeline of changes to the properties of a component with a defined lifetime, such as a hit object.
 *
 * Unlike transforms, which are removed once they finish, the steps of a choreography are kept until it's cleared:
 * the state of the component is computed from the time alone, so the result doesn't depend on what was
 * played before, and going back in time just works.
 *
 * Times are in seconds on the clock of the component, see [UIComponent.clock]. Steps must be added in order. Each one
 * starts from where the previous one on the same property ended, or from the current value if it's the first.
 */
class Choreography internal constructor(private val target: UIComponent) {

    /**
     * The time the last step or callback ends at.
     */
    var endTime = 0f
        private set

    /**
     * The time this choreography ends at, called with [onLifetimeEnd] once reached. Null means it ends with [endTime].
     */
    var lifetimeEnd: Float? = null

    /**
     * Called once when the lifetime of this choreography ends.
     */
    var onLifetimeEnd: OnTransformFinished? = null

    /**
     * Whether the lifetime of this choreography already ended.
     */
    val isFinished
        get() = lifetimeEndReached


    private val steps = ArrayList<Step>()
    private val freeSteps = ArrayList<Step>()
    private val callbacks = ArrayList<Callback>()

    private var cursor = 0f
    private var lifetimeEndReached = false

    // The value each property ends at after the steps added so far, for the next step to start from.
    private val hasLastValue = BooleanArray(Property.entries.size)
    private val lastValues = FloatArray(Property.entries.size)
    private var lastColor = Color4.White


    //region Building

    /**
     * Makes the next steps start at the given time.
     */
    fun at(time: Float): Choreography {
        cursor = time
        return this
    }

    /**
     * Makes the next steps start later.
     */
    fun delay(duration: Float): Choreography {
        cursor += duration
        endTime = maxOf(endTime, cursor)
        return this
    }

    /**
     * Makes the next steps start once all the previous ones have ended.
     */
    fun then(): Choreography {
        cursor = endTime
        return this
    }

    private fun currentValue(property: Property) = when (property) {
        Property.ScaleX -> target.scaleX
        Property.ScaleY -> target.scaleY
        Property.Alpha -> target.alpha
        Property.Rotation -> target.rotationZ
        Property.TranslationX -> target.translationX.resolve(target.bounds.width)
        Property.TranslationY -> target.translationY.resolve(target.bounds.height)
        Property.Color -> 0f
    }

    private fun obtainStep(property: Property, duration: Float, easing: Easing): Step {
        val step = freeSteps.removeLastOrNull() ?: Step()

        step.property = property
        step.startTime = cursor
        step.duration = duration
        step.easing = easing

        steps.add(step)
        endTime = maxOf(endTime, cursor + duration)
        return step
    }

    private fun addStep(property: Property, value: Float, duration: Float, easing: Easing) {
        val step = obtainStep(property, duration, easing)
        val index = property.ordinal

        step.from = if (hasLastValue[index]) lastValues[index] else currentValue(property)
        step.to = value

        hasLastValue[index] = true
        lastValues[index] = value
    }

    @JvmOverloads
    fun scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.ScaleX, value, duration, easing)
        addStep(Property.ScaleY, value, duration, easing)
        return this
    }

    @JvmOverloads
    fun fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.Alpha, value, duration, easing)
        return this
    }

    @JvmOverloads
    fun fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

    @JvmOverloads
    fun fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

    @JvmOverloads
    fun rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.Rotation, value, duration, easing)
        return this
    }

    @JvmOverloads
    fun moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.TranslationX, x, duration, easing)
        addStep(Property.TranslationY, y, duration, easing)
        return this
    }

    @JvmOverloads
    fun moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.TranslationX, value, duration, easing)
        return this
    }

    @JvmOverloads
    fun moveToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        addStep(Property.TranslationY, value, duration, easing)
        return this
    }

    /**
     * Changes the RGB channels of the color, leaving the alpha to [fadeTo].
     */
    @JvmOverloads
    fun colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None): Choreography {
        val step = obtainStep(Property.Color, duration, easing)
        val index = Property.Color.ordinal

        step.fromColor = if (hasLastValue[index]) lastColor else target.color
        step.toColor = color

        hasLastValue[index] = true
        lastColor = color
        return this
    }

    /**
     * Calls [callback] once when time reaches the end of all the steps added so far. It isn't called again if time
     * goes back and passes it again.
     */
    fun after(callback: OnTransformFinished): Choreography {
        callbacks.add(Callback(endTime, callback))
        return this
    }

    //endregion

    /**
     * Removes all the steps and callbacks, so this choreography can be used again with a pooled component.
     */
    fun clear() {
        freeSteps.addAll(steps)
        steps.clear()
        callbacks.clear()

        hasLastValue.fill(false)

        cursor = 0f
        endTime = 0f
        lifetimeEnd = null
        onLifetimeEnd = null
        lifetimeEndReached = false
    }

    /**
     * Jumps to the end, applying the final values and calling the callbacks left.
     */
    fun finish() = update(maxOf(endTime, lifetimeEnd ?: endTime))


    /**
     * Applies the state of the given time.
     */
    internal fun update(time: Float) {
        applySteps(time)

        for (i in callbacks.indices) {
            val callback = callbacks.getOrNull(i) ?: break

            if (!callback.isCalled && time >= callback.time) {
                callback.isCalled = true
                callback.action(target)
            }
        }

        if (!lifetimeEndReached && time >= (lifetimeEnd ?: endTime) && (steps.isNotEmpty() || lifetimeEnd != null)) {
            lifetimeEndReached = true
            onLifetimeEnd?.invoke(target)
        }
    }

    private fun applySteps(time: Float) {
        var appliedProperties = 0

        // Only the latest step that already started on each property matters.
        for (i in steps.indices.reversed()) {
            val step = steps[i]
            val bit = 1 shl step.property.ordinal

            if (appliedProperties and bit == 0 && step.startTime <= time) {
                appliedProperties = appliedProperties or bit
                step.apply(target, time)
            }
        }

        // Properties whose first step didn't start yet have to be at the value it starts from.
        for (i in steps.indices) {
            val step = steps[i]
            val bit = 1 shl step.property.ordinal

            if (appliedProperties and bit == 0) {
                appliedProperties = appliedProperties or bit
                step.apply(target, step.startTime)
            }
        }
    }


    private enum class Property {
        ScaleX,
        ScaleY,
        Alpha,
        Rotation,
        TranslationX,
        TranslationY,
        Color
    }

    private class Step {
        var property = Property.Alpha
        var startTime = 0f
        var duration = 0f
        var easing = Easing.None
        var from = 0f
        var to = 0f
        var fromColor = Color4.White
        var toColor = Color4.White

        fun apply(target: UIComponent, time: Float) {
            val progress = when {
                duration <= 0f -> 1f
                time >= startTime + duration -> 1f
                else -> easing.interpolate((time - startTime) / duration).coerceIn(0f, 1f)
            }

            val value = from + (to - from) * progress

            when (property) {
                Property.ScaleX -> target.scaleX = value
                Property.ScaleY -> target.scaleY = value
                Property.Alpha -> target.alpha = value
                Property.Rotation -> target.rotationZ = value
                Property.TranslationX -> target.translationX = value.px
                Property.TranslationY -> target.translationY = value.px
                Property.Color -> target.color = ColorInterpolation.colorAt(progress, fromColor, toColor, 0f, 1f).copy(alpha = target.alpha)
            }
        }
    }

    private class Callback(val time: Float, val action: OnTransformFinished) {
        var isCalled = false
    }
}


/**
 * Gets the choreography of this component, creating it if needed.
 */
fun UIComponent.choreography(): Choreography = choreography ?: Choreography(this).also { choreography = it }

/**
 * Adds steps to the choreography of this component.
 */
inline fun UIComponent.choreograph(block: Choreography.() -> Unit): Choreography = choreography().apply(block)
