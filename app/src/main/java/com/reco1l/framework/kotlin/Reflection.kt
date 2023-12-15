package com.reco1l.framework.kotlin

import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.full.createInstance

/**
 * Create a new instance with the given parameters.
 */
fun <T : Any> KClass<T>.createInstance(vararg parameters: Any?): T
{
    if (parameters.isEmpty())
        return createInstance()

    val constructor = constructors.find { it.parameters.size == parameters.size }
        ?:
        throw IllegalArgumentException("No constructor matches the given parameters for class: $this")

    return constructor.callBy(constructor.parameters.associateWithIndexed { _, i -> parameters[i] })
}

/**
 * Returns `true` if the property is lazy initialized.
 */
val KProperty0<*>.isLazyInit: Boolean
    get() = getDelegate() is Lazy<*>

/**
 * Returns `true` if the lazy property was initialized or if it's not a lazy property.
 */
val KProperty0<*>.isLazyInitialized: Boolean
    get() = (getDelegate() as? Lazy<*>)?.isInitialized() ?: true


// Properties

inline fun <T : Any, reified V : Any?> KClass<T>.setField(
    instance: Any,
    name: String,
    value: V
)
{
    java.getDeclaredField(name).apply {

        if (!isAccessible)
            isAccessible = true

        set(instance, value)
    }
}

inline fun <T : Any, reified V : Any?> KClass<T>.getField(instance: Any, name: String): V
{
    return java.getDeclaredField(name).let {

        if (!it.isAccessible)
            it.isAccessible = true

        it.get(instance)
    } as V
}

fun <T : Any> KClass<T>.invokeMethod(
    instance: Any,
    name: String,
    vararg parameters: Any
)
{
    java.getDeclaredMethod(name, *parameters.map { it.javaClass }.toTypedArray()).apply {

        if (!isAccessible)
            isAccessible = true

        invoke(instance, *parameters)
    }
}



