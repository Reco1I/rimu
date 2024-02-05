package com.reco1l.rimu.ui.entity

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.FloatAction
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import kotlin.math.max


// Base function

private fun Actor.animateTo(

    initialValue: Float,

    finalValue: Float,

    duration: Float,

    interpolator: Interpolation?,

    setProperty: Actor.(Float) -> Unit

): Action?
{
    if (duration == 0f)
    {
        setProperty(finalValue)
        return null
    }

    val action = object : FloatAction(initialValue, finalValue, duration, interpolator)
    {

        override fun update(percent: Float)
        {
            super.update(percent)

            setProperty(value)
        }
    }

    addAction(action)
    return action
}


// Choreograph


/**
 * Creates a sequential animation.
 */
fun Actor.animateSequential(vararg actions: Action?): SequenceAction?
{
    if (actions.isEmpty() || actions.all { it == null })
        return null

    val sequence = SequenceAction()

    actions.forEach {

        // Removing from the actor in case they're already added so we can add to the sequence
        // action instead.
        removeAction(it)

        sequence.addAction(it)
    }

    addAction(sequence)
    return sequence
}

/**
 * Creates a parallel animation.
 */
fun Actor.animateParallel(vararg actions: Action?): ParallelAction?
{
    if (actions.isEmpty() || actions.all { it == null })
        return null

    val parallel = ParallelAction()

    actions.forEach {

        // Removing from the actor in case they're already added so we can add to the sequence
        // action instead.
        removeAction(it)

        parallel.addAction(it)
    }

    addAction(parallel)
    return parallel
}


// Modifiers

fun Actor.toAlpha(

    value: Float,

    duration: Float = 0f,

    interpolator: Interpolation? = null

) = animateTo(color.a, value, duration, interpolator) { color.a = it }


fun Actor.toScale(

    value: Float,

    duration: Float = 0f,

    interpolator: Interpolation? = null

) = animateTo(max(scaleX, scaleY), value, duration, interpolator) { setScale(it) }


fun Actor.toDelay(duration: Float) = animateTo(0f, 1f, duration, null) {}
