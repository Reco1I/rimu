package com.reco1l.rimu.management

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.reco1l.framework.IMapObservable
import com.reco1l.framework.forEachObserver
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * Game settings manager.
 */
class SettingManager(override val ctx: MainContext) :
    SharedPreferences.OnSharedPreferenceChangeListener,
    IMapObservable<RimuSetting, (Any?) -> Unit>,
    IWithContext
{

    override val observers = mutableMapOf<RimuSetting, MutableList<(Any?) -> Unit>>()


    private val preferences = ctx.getSharedPreferences(ctx.applicationInfo.name, MODE_PRIVATE).apply {

        registerOnSharedPreferenceChangeListener(this@SettingManager)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?)
    {
        val setting = RimuSetting.valueOf(key ?: return)
        val newValue = preferences.all[key] ?: setting.default

        forEachObserver(setting) { it(newValue) }
    }

    /**
     * Get an specific settings from the preference map.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(setting: RimuSetting) = preferences.all[setting.name] as? T ?: setting.default as T

    /**
     * Set an specific settings from the preference map.
     *
     * Only types supported by the preference map are allowed: [Int], [Long], [Float], [Boolean],
     * [String] and `Set<String>`.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> set(setting: RimuSetting, value: T)
    {
        val key = setting.name

        preferences.edit().apply {

            if (value == null)
            {
                remove(key)
                apply()
                return
            }

            if (value is Set<*>)
            {
                value as? Set<String>
                    ?:
                    throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")

                putStringSet(key, value)
            }

            when (value)
            {
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)

                else -> throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")
            }
            apply()
        }
    }
}



fun <V : Any?> IWithContext.Setting(setting: RimuSetting) = Setting<V>(ctx, setting)

/**
 * Bind a property to a game option specified by the [SettingManager].
 */
class Setting<V : Any?>(

    /**
     * The attached context
     */
    private val ctx: MainContext,

    /**
     * The setting key
     */
    private val setting: RimuSetting,

    ) :
    ReadWriteProperty<Any?, V>
{
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>) = ctx.settings[setting] ?: setting.default as V

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) = ctx.settings.set(setting, value)
}
