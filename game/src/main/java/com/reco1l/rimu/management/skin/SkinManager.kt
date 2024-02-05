package com.reco1l.rimu.management.skin

import android.util.Log
import com.reco1l.toolkt.kotlin.nextOf
import com.reco1l.toolkt.kotlin.orCatch
import com.reco1l.toolkt.IObservable
import com.reco1l.toolkt.forEachObserver
import com.reco1l.skindecoder.SkinMapper
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting.UI_SKIN
import com.reco1l.rimu.data.Skin
import com.reco1l.rimu.data.Skin.Companion.BASE
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.rimu.ui.layouts.Notification
import com.reco1l.rimu.ui.layouts.ToastView
import com.reco1l.rimu.ui.views.view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class SkinManager(override val ctx: MainContext) :
    IObservable<ISkinnable>,
    IWithContext
{

    override val observers = mutableListOf<ISkinnable>()


    /**
     * The skin importer.
     */
    val importer = SkinImporter(ctx)

    /**
     * The skin decoder used along for skin changes.
     */
    val decoder = SkinMapper()


    /**
     * The list of skins.
     */
    var skins = getDefaults()
        private set


    /**
     * The current skin, can only be `null` if game is initializing.
     */
    var current: WorkingSkin? = null
        private set


    private val changeScope = CoroutineScope(Dispatchers.IO)


    init
    {
        ctx.settings.bindObserver(UI_SKIN) {

            changeScope.launch {

                // Finding the source key in the list of skin sources.
                val source = get(it as String)

                onCreateWorkingSkin(source ?: get(BASE)!!)
            }
        }

        ctx.onPostInitialization {

            // Initializing assets with default skin until we get the corresponding skin.
            setCurrent(get(BASE)!!)

            GlobalScope.launch {

                // The flow will update the list everytime the table is changed.
                ctx.database.skinTable.getFlow().collect(::onTableChange)

                // Now loading skin
                setCurrent(get(ctx.settings[UI_SKIN]) ?: get(BASE)!!)
            }
        }
    }


    fun next()
    {
        changeScope.launch {

            setCurrent(skins.nextOf(current?.source) ?: get(BASE)!!)
        }
    }


    /**
     * Find the stored [Skin] entry from the given key.
     */
    operator fun get(key: String) = skins.find {

        // Checking with endsWith because default skins key starts with 'skins/' prefix so we must
        // check what's after the slash, in external skins this will never happen.
        key.endsWith(it.key)
    }

    /**
     * Get list of defaults skins.
     */
    private fun getDefaults() = ctx.assets.list("skins")!!.map {

        Skin("skins/$it", "rimu! team", true)
    }


    private fun setCurrent(source: Skin)
    {
        if (source == current?.source)
            return

        changeScope.launch {

            // We're gonna to release the last skin once we get the new one so we ensure assets
            // aren't released before finish loading.
            val last = current

            current = onCreateWorkingSkin(source).also { skin ->

                forEachObserver { it.onApplySkin(skin) }
            }

            if (last != null)
            {
                mainThread {

                    ToastView {

                        header = "Skin changed"
                        message = current!!.data.general.name + (current!!.data.general.author?.let { " by $it" } ?: "")

                    }.show()

                }

                last.onRelease()
            }
        }
    }


    private fun onTableChange(value: List<Skin>)
    {
        // Default skins will always be on top
        skins = getDefaults() + value
    }

    private fun onCreateWorkingSkin(source: Skin) = { WorkingSkin(ctx, source, decoder) }.orCatch {

        Notification(
            header = "Error",
            message = """
                    Unable to load skin "${source.key}${source.author?.let { author -> " by $author" }}"
                    Cause: ${it.javaClass} - ${it.message}
                """.trimMargin(),
            icon = ""
        ).show(ctx)

        Log.e(javaClass.simpleName, "Error while loading skin.", it)

        // Fallback to default skin because current skin can never be null.
        WorkingSkin(ctx, get(BASE)!!, decoder)
    }
}