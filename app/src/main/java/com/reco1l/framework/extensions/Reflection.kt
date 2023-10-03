package com.reco1l.framework.extensions

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties


/**
 * [Class.getSimpleName]
 */
val Any?.className: String
    get() = this?.javaClass?.simpleName ?: "Null"


/**
 * Create a new instance with the given parameters.
 */
fun <T : Any> KClass<T>.createInstance(vararg parameters: Any?): T
{
    if (parameters.isEmpty())
    {
        return createInstance()
    }

    val constructor = constructors.first { it.parameters.size == parameters.size }
    val params = constructor.parameters

    val arguments = params.associateWith { parameters[params.indexOf(it)] }

    return constructor.callBy(arguments)
}


/**
 * Iterate over all class fields of a specific type.
 *
 * @param type The type of the fields to filter.
 * @param action The action to execute.
 */
inline fun <reified T : Any> Any.forEachFieldOf(type: KClass<T>, action: (T) -> Unit)
{
    this::class.memberProperties
            .filter { it.returnType.classifier == type }
            .forEach { action(it.getter.call(this) as T) }
}

/**
 * Iterate over all class fields.
 *
 * @param action The action to execute.
 */
inline fun Any.forEachField(action: (Any?) -> Unit)
{
    this::class.memberProperties
        .forEach { action(it.getter.call(this)) }
}

/**
 * Advanced equals intended for data classes that supports [Array.contentEquals].
 */
inline fun <reified T : Any> equalsWithArraySupport(o1: T, o2: T): Boolean
{
    return T::class.memberProperties.all { property ->

        val getter = property.getter

        val v1 = getter.call(o1) ?: return false
        val v2 = getter.call(o2) ?: return false

        when(v1)
        {
            is Array<*> -> v1.contentEquals(v2 as Array<*>)
            is ByteArray -> v1.contentEquals(v2 as ByteArray)
            is CharArray -> v1.contentEquals(v2 as CharArray)
            is ShortArray -> v1.contentEquals(v2 as ShortArray)
            is IntArray -> v1.contentEquals(v2 as IntArray)
            is LongArray -> v1.contentEquals(v2 as LongArray)
            is FloatArray -> v1.contentEquals(v2 as FloatArray)
            is DoubleArray -> v1.contentEquals(v2 as DoubleArray)
            is BooleanArray -> v1.contentEquals(v2 as BooleanArray)
            else -> v1 == v2
        }
    }
}