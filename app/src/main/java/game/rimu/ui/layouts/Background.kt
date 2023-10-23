package game.rimu.ui.layouts

import android.graphics.Bitmap
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView.ScaleType
import game.rimu.android.RimuContext
import game.rimu.constants.BuildSettings
import game.rimu.management.beatmap.IBeatmapObserver
import game.rimu.management.beatmap.WorkingBeatmap
import game.rimu.ui.LayerBackground
import game.rimu.ui.BaseLayer
import game.rimu.ui.scenes.MenuScene
import game.rimu.ui.scenes.ResultsScene
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.scenes.SceneIntro
import game.rimu.ui.scenes.SelectorScene
import game.rimu.ui.views.FadeImageView
import kotlin.reflect.KClass


class Background(ctx: RimuContext) :
    ModelLayout(ctx),
    IBeatmapObserver
{

    override var layer: KClass<out BaseLayer> = LayerBackground::class

    override var parents: Array<KClass<out RimuScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        ResultsScene::class,
        SelectorScene::class
    )

    val image = FadeImageView {

        dimensions.width = MATCH_PARENT
        dimensions.height = MATCH_PARENT

        scaleType = ScaleType.CENTER_CROP
    }


    init
    {
        ctx.initializationTree!!.add {

            beatmaps.bindObserver(observer = this@Background)
        }
    }


    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        var bitmap: Bitmap? = ctx.resources["menu-background", 0]

        beatmap?.data?.events?.apply {

            if (!BuildSettings.SFW_MODE)
                bitmap = backgroundFilename?.let { beatmap.assets[it.substringBeforeLast('.'), 0] } ?: bitmap
        }

        mainThread { image.setImageBitmap(bitmap) }
    }

    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        if (!image.hasImage())
            image.setImageBitmap(ctx.resources["menu-background", 0])
    }
}