package com.reco1l.framework.management.bindables

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