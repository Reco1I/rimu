package game.rimu.management.skin

import com.reco1l.framework.management.IObservable
import com.reco1l.skindecoder.SkinDecoder
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting.UI_SKIN
import game.rimu.data.Skin
import game.rimu.data.Skin.Companion.DEFAULT
import game.rimu.management.Setting
import game.rimu.ui.ISkinnable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SkinManager(override val ctx: RimuContext) :
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


    // Using a different coroutine context.
    private val changeScope = CoroutineScope(Dispatchers.IO)


    /**
     * The current skin key.
     */
    var currentSkinKey by Setting<String>(UI_SKIN)

    /**
     * The default skin which is always loaded just in case.
     */
    val default = WorkingSkin(ctx, DEFAULT, decoder)

    /**
     * The current skin identified by [currentSkinKey]
     */
    var current = onLoadSkin(currentSkinKey)


    init
    {
        ctx.settings.bindObserver(UI_SKIN) {

            // Executing in change scope
            changeScope.launch { onLoadSkin(it as String) }
        }
    }


    private fun onLoadSkin(key: String): WorkingSkin
    {
        if (key == default.source.key)
            return default

        // Determining if the skin is internal, for this we use a pattern where the hash equals
        // to the subdirectory in the assets folder and it ends with '/' to distinguish from external
        val isInternal = key.endsWith('/')

        // As stated above if it's not an internal skin we get it from the database, otherwise
        // we create a new one based on the internal directory.
        val skin = if (isInternal) Skin(key, "rimu! team") else ctx.database.skinTable.getSkin(key)
            // Using default skin if for whatever reason it fails to create the skin.
            ?: return default

        return WorkingSkin(ctx, skin, decoder)
    }
}