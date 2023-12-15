package com.reco1l.framework

/**
 * Intended to be used in manager classes where many events happens at once that must be listened.
 */
interface IObservable<T>
{

    /**
     * The list of observers
     */
    val observers: MutableList<T>

    /**
     * Bind a new observer.
     * @param index The index where the observer should be bind, by default last index.
     * @param observer The observer to be bind.
     */
    fun bindObserver(index: Int = observers.size, observer: T): Boolean
    {
        observers.add(index, observer)
        return index == observers.indexOf(observer)
    }

    /**
     * Unbind and existent observer.
     */
    fun unbindObserver(observer: T) = observers.remove(observer)
}

/**
 * Iterate over all observers.
 */
inline fun <T> IObservable<T>.forEachObserver(action: (T) -> Unit)
{
    for (i in 0..<observers.size)
        action(observers[i])
}


/**
 * Intended to be used in manager classes where many events happens at once that must be listened.
 */
interface IMapObservable<K, T>
{

    /**
     * The list of observers
     */
    val observers: MutableMap<K, MutableList<T>>

    /**
     * Bind a new observer.
     * @param index The index where the observer should be bind, by default last index.
     * @param observer The observer to be bind.
     */
    fun bindObserver(key: K, index: Int = observers[key]?.size ?: 0, observer: T): Boolean
    {
        val list = observers.getOrPut(key) { mutableListOf() }
        list.add(index, observer)
        return index == list.indexOf(observer)
    }

    /**
     * Unbind all observers from a key.
     */
    fun unbindObservers(key: K) = observers.remove(key)

    /**
     * Unbind and existent observer.
     */
    fun unbindObserver(key: K, observer: T): Boolean
    {
        val list = observers[key] ?: return false
        val result = list.remove(observer)

        if (list.isEmpty())
            observers.remove(key)

        return result
    }
}

/**
 * Iterate over all observers for a key.
 */
fun <K, T> IMapObservable<K, T>.forEachObserver(key: K, action: (T) -> Unit)
{
    val list = observers[key] ?: return

    for (i in 0..<list.size)
        action(list[i])
}
