package com.reco1l.framework.management

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Bindable<K, V : Any?>(
    /**
     * The key to identify the bindable.
     */
    val key: K,

    /**
     * Value used for when the provider returns `null`.
     */
    private val fallback: V,

    /**
     * The delegator where the bindable will listen when the value is changed or to get the initial
     * value.
     */
    private val provider: IBindableValueProvider<K, V, out Bindable<K, V>>
) :
    ReadWriteProperty<Any?, V>
{

    override fun getValue(thisRef: Any?, property: KProperty<*>) = provider.getValueForBindable(key) ?: fallback

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) = provider.setValueFromBindable(key, value)
}

interface IBindableValueProvider<K, V : Any?, B: Bindable<K, V>>
{

    /**
     * Called by the bindable to get the value from the key.
     */
    fun getValueForBindable(key: K) : V

    /**
     * Called by the bindable to set the value from the key.
     */
    fun setValueFromBindable(key: K, value: V)

}