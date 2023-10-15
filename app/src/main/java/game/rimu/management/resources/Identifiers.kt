package game.rimu.management.resources

import game.rimu.android.RimuContext


data class AssetID(

    val key: String,

    val variant: Int = 0
)
{
    inline operator fun <reified T : Any> get(context: RimuContext) = context.resources.get<T>(key, variant)
}

data class ColorID(

    val key: String,

    val factor: Float = 1f,

    val alpha: Float = 1f
)
{
    operator fun get(context: RimuContext) = context.skins.current.data.colours.map[key]?.toInt(
        alpha = alpha,
        factor = factor
    )
}

