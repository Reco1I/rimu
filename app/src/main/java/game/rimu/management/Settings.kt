package game.rimu.management

import android.content.Context
import android.content.SharedPreferences
import com.reco1l.framework.extensions.className
import com.reco1l.framework.management.IMapObservable
import com.reco1l.framework.management.bindables.Bindable
import com.reco1l.framework.management.bindables.IBindableValueProvider
import com.reco1l.framework.management.forEachObserver
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting



/**
 * Game settings manager.
 */
class SettingManager(override val ctx: RimuContext) :
    SharedPreferences.OnSharedPreferenceChangeListener,
    IBindableValueProvider<String, Any?, Setting<Any?>>,
    IMapObservable<RimuSetting, (Any?) -> Unit>,
    IWithContext
{

    override val observers = mutableMapOf<RimuSetting, MutableList<(Any?) -> Unit>>()


    private val preferences = ctx.getSharedPreferences(ctx.applicationInfo.name,
        Context.MODE_PRIVATE
    ).apply {

        registerOnSharedPreferenceChangeListener(this@SettingManager)
    }


    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String)
    {
        val setting = RimuSetting.valueOf(key)
        val newValue = preferences.all[key] ?: setting.default

        forEachObserver(setting) { it(newValue) }
    }

    /**
     * Get an specific settings from the preference map.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: RimuSetting) = get<T>(key.key) ?: key.default as T

    /**
     * Get an specific settings from the preference map.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = preferences.all[key] as? T

    /**
     * Set an specific settings from the preference map.
     *
     * Only types supported by the preference map are allowed: [Int], [Long], [Float], [Boolean],
     * [String] and `Set<String>`.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> set(key: String, value: T)
    {
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
                    ?: throw IllegalArgumentException("Unsupported value type: ${value.className}")

                putStringSet(key, value)
            }

            when (value)
            {
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)

                else -> throw IllegalArgumentException("Unsupported value type: ${value.className}")
            }
            apply()
        }
    }


    // Bindables

    override fun getValueForBindable(key: String) = get<Any?>(key)

    override fun setValueFromBindable(key: String, value: Any?) = set(key, value)
}



fun <V : Any?>IWithContext.Setting(setting: RimuSetting) = Setting<V>(ctx, setting)

/**
 * Bind a property to a game option specified by the [SettingManager].
 */
@Suppress("UNCHECKED_CAST")
class Setting<V : Any?>(

    /**
     * The attached context
     */
    ctx: RimuContext,

    /**
     * The setting key
     */
    setting: RimuSetting,

) :
    Bindable<String, V>(
        setting.key,
        setting.default as V,
        ctx.settings as IBindableValueProvider<String, V, Setting<V>>
    )