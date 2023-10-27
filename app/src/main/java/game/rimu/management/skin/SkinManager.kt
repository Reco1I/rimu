package game.rimu.management.skin

import com.reco1l.framework.android.Logger
import com.reco1l.framework.kotlin.klass
import com.reco1l.framework.kotlin.nextOf
import com.reco1l.framework.kotlin.orCatch
import com.reco1l.framework.IObservable
import com.reco1l.framework.forEachObserver
import com.reco1l.skindecoder.SkinDecoder
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.constants.RimuSetting.UI_SKIN
import game.rimu.data.Skin
import game.rimu.data.Skin.Companion.BASE
import game.rimu.ui.ISkinnable
import game.rimu.ui.layouts.Notification
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
    val decoder = SkinDecoder()


    /**
     * The list of skins.
     */
    var skins = getDefaults()
        private set

    /**
     * Determines if a skin was already initialized, since current skin can never be null and it's
     * late init we must check this first.
     */
    var isInitialized = false
        private set


    /**
     * The current skin.
     */
    lateinit var current: WorkingSkin
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

        ctx.initializationTree!!.add {

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

            setCurrent(skins.nextOf(current.source) ?: get(BASE)!!)
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
    fun getDefaults() = ctx.assets.list("skins")!!.map { Skin(it, "rimu! team", true) }


    private fun setCurrent(source: Skin)
    {
        if (isInitialized && source == current.source)
            return

        changeScope.launch {

            // We're gonna to release the last skin once we get the new one so we ensure assets
            // aren't released before finish loading.
            val last = if (isInitialized) current else null

            current = onCreateWorkingSkin(source)
            isInitialized = true

            forEachObserver { it.onApplySkin(current) }

            last?.onRelease()
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
                    Cause: ${it.klass} - ${it.message}
                """.trimMargin(),
            icon = ""
        ).show(ctx)

        Logger.e(klass, "Error while loading skin.", it)

        // Fallback to default skin because current skin can never be null.
        WorkingSkin(ctx, get(BASE)!!, decoder)
    }
}