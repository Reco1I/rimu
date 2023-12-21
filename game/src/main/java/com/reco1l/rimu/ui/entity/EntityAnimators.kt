package com.reco1l.rimu.ui.entity

import org.andengine.entity.Entity
import org.andengine.entity.IEntity
import org.andengine.entity.modifier.EntityModifier
import org.andengine.entity.modifier.IEntityModifier
import org.andengine.entity.modifier.IEntityModifier.IEntityModifierListener
import org.andengine.entity.modifier.ParallelEntityModifier
import org.andengine.entity.modifier.SequenceEntityModifier
import org.andengine.entity.modifier.SingleValueSpanEntityModifier
import org.andengine.util.modifier.IModifier
import org.andengine.util.modifier.ease.EaseLinear
import org.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.max


// Listener

/**
 * Custom implementation of [IEntityModifierListener] with properties instead of functions.
 */
class ModifierListenerImpl(init: (ModifierListenerImpl.() -> Unit)? = null): IEntityModifierListener
{

    /**
     * The function to be called when the modifier starts.
     */
    var onStart: (() -> Unit)? = null

    /**
     * The function to be called when the modifier ends.
     */
    var onEnd: (() -> Unit)? = null


    init
    {
        init?.invoke(this)
    }


    override fun onModifierStarted(m: IModifier<IEntity>, e: IEntity) = onStart?.invoke() ?: Unit

    override fun onModifierFinished(m: IModifier<IEntity>, e: IEntity) = onEnd?.invoke() ?: Unit
}


// Base function

private fun IEntity.animateTo(

    setProperty: IEntity.(Float) -> Unit,

    initialValue: Float,

    finalValue: Float,

    end: Long,

    ease: IEaseFunction?,

    listener: (ModifierListenerImpl.() -> Unit)?

): IEntityModifier?
{
    if (end == 0L)
    {
        setProperty(finalValue)
        return null
    }

    val modifier = object : SingleValueSpanEntityModifier(
        end / 1000f,
        initialValue,
        finalValue,
        listener?.let { ModifierListenerImpl(it) },
        ease ?: EaseLinear.getInstance()
    )
    {
        override fun onSetInitialValue(e: IEntity, v: Float) = e.setProperty(v)
        override fun onSetValue(e: IEntity, p: Float, v: Float) = e.setProperty(v)

        override fun deepCopy() = throw NotImplementedError()
    }

    registerEntityModifier(modifier)
    return modifier
}


// Choreograph


/**
 * Creates a sequential animation.
 *
 * @param unregisterNested Whether nested modifiers should be unregistered once finish.
 */
fun IEntity.animateSequential(vararg modifiers: IEntityModifier?, unregisterNested: Boolean = false): SequenceEntityModifier
{
    // Unregistering in case they're already registered.
    modifiers.forEach {

        if (!unregisterNested)
            it.noUnregister()

        unregisterEntityModifier(it)
    }

    val modifier = SequenceEntityModifier(*modifiers)
    registerEntityModifier(modifier)
    return modifier
}

/**
 * Creates a parallel animation.
 *
 * @param unregisterNested Whether nested modifiers should be unregistered once finish.
 */
fun IEntity.animateParallel(vararg modifiers: IEntityModifier?, unregisterNested: Boolean = false): ParallelEntityModifier
{
    // Unregistering in case they're already registered.
    modifiers.forEach {

        if (!unregisterNested)
            it.noUnregister()

        unregisterEntityModifier(it)
    }

    val modifier = ParallelEntityModifier(*modifiers)
    registerEntityModifier(modifier)
    return modifier
}


// Modifiers

fun IEntity.toAlpha(

    value: Float,

    end: Long = 0L,

    ease: IEaseFunction? = null,

    listener: (ModifierListenerImpl.() -> Unit)? = null,

    ) = animateTo(IEntity::setAlpha, alpha, value, end, ease, listener)


fun Entity.toScale(

    value: Float,

    end: Long = 0L,

    ease: IEaseFunction? = null,

    listener: (ModifierListenerImpl.() -> Unit)? = null

) = animateTo(IEntity::setScale, max(scaleX, scaleY), value, end, ease, listener)

fun Entity.toDelay(

    duration: Long,

    listener: (ModifierListenerImpl.() -> Unit)? = null

) = animateTo({}, 0f, 1f, duration, null, listener)


// Options

/**
 * Disables the auto-unregister feature of the modifier.
 *
 * @see EntityModifier.isAutoUnregisterWhenFinished
 */
fun IEntityModifier?.noUnregister() = this?.apply { isAutoUnregisterWhenFinished = false }


