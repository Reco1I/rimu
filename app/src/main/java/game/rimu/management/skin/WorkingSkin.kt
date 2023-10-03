package game.rimu.management.skin

import com.reco1l.skindecoder.SkinDecoder
import com.reco1l.skindecoder.data.SkinData
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.data.asset.AssetBundle
import game.rimu.data.Skin

/**
 * A working skin refers to a current loaded skin. Should be one per game instance but due to context
 * structure we can't make this a singleton.
 */
class WorkingSkin(

    override val ctx: RimuContext,

    /**
     * The skin entity.
     */
    val source: Skin,

    /**
     * The shared skin decoder.
     */
    val decoder: SkinDecoder

) : IWithContext
{

    /**
     * The skin asset bundle.
     */
    val assets = AssetBundle.from(ctx, source)

    /**
     * The skin data.
     */
    val data = onDecodeData()


    private fun onDecodeData(): SkinData
    {
        // Finding 'skin.ini' file in the skin assets bundle, in case it doesn't exist we return a
        // skin data with default set.
        return assets.getInputStream("skin")?.let {

            // Decoding 'skin.ini' file if it exists for the source skin.
            decoder.decode(it)

        } ?: SkinData()
    }
}

