package game.rimu.management.skin

import com.reco1l.skindecoder.SkinMapper
import com.reco1l.skindecoder.data.SkinData
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.data.asset.AssetBundle
import game.rimu.data.Skin
import game.rimu.data.Skin.Companion.BASE

/**
 * A working skin refers to a current loaded skin. Should be one per game instance but due to context
 * structure we can't make this a singleton.
 */
class WorkingSkin(

    override val ctx: MainContext,

    /**
     * The skin entity.
     */
    val source: Skin,

    /**
     * The shared skin decoder.
     */
    val decoder: SkinMapper

) : IWithContext
{

    /**
     * The skin asset bundle.
     */
    val assets = when (source.key)
    {
        BASE -> ctx.resources.defaultAssets
        else -> AssetBundle.from(ctx, source)
    }

    /**
     * The skin data.
     */
    val data = onDecodeData()

    /**
     * The skin declared colors in a map.
     */
    val colors
        get() = data.colours.map


    private fun onDecodeData(): SkinData
    {
        // Finding 'skin.ini' file in the skin assets bundle, in case it doesn't exist we return a
        // skin data with default set.
        return assets.getInputStream("skin")?.let {

            // Decoding 'skin.ini' file if it exists for the source skin.
            decoder.decode(it)

        } ?: SkinData()
    }


    fun onRelease()
    {
        if (source.key != BASE)
            assets.onRelease()
    }
}

