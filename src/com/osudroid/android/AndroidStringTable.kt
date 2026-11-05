package com.osudroid.android

import android.content.Context
import com.osudroid.resources.StringArrayResource
import com.osudroid.resources.StringResource
import ru.nsu.ccfit.zuev.osu.helper.StringTable

/**
 * The game's [StringTable] on Android, which reads the language pack's resources so Android picks the
 * language.
 */
class AndroidStringTable(private val context: Context) : StringTable.Implementation {

    private val identifiers = HashMap<String, Int>()


    override fun get(resource: StringResource): String = context.getString(identifier("string", resource.name))

    override fun getArray(resource: StringArrayResource): Array<String> =
        context.resources.getStringArray(identifier("array", resource.name))

    override fun get(resid: Int): String = context.getString(resid)

    override fun format(format: String, vararg args: Any?): String = String.format(format, *args)


    // The language pack's resources, then the app's own, which has strings not in the language pack yet.
    private fun identifier(type: String, name: String) = synchronized(identifiers) {
        identifiers.getOrPut("$type/$name") {
            try {
                Class.forName("com.osudroid.resources.R$$type").getField(name).getInt(null)
            } catch (_: NoSuchFieldException) {
                Class.forName("ru.nsu.ccfit.zuev.osuplus.R$$type").getField(name).getInt(null)
            }
        }
    }
}
