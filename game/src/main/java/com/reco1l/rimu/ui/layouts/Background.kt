package com.reco1l.rimu.ui.layouts

import android.graphics.Bitmap
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView.ScaleType
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.BuildSettings
import com.reco1l.rimu.management.beatmap.IBeatmapObserver
import com.reco1l.rimu.management.beatmap.WorkingBeatmap
import com.reco1l.rimu.ui.LayerBackground
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.scenes.MenuScene
import com.reco1l.rimu.ui.scenes.ResultsScene
import com.reco1l.rimu.ui.scenes.BaseScene
import com.reco1l.rimu.ui.scenes.SceneIntro
import com.reco1l.rimu.ui.scenes.SelectorScene
import com.reco1l.rimu.ui.views.FadeImageView
import kotlin.reflect.KClass


class Background(ctx: MainContext) :
    ModelLayout(ctx),
    IBeatmapObserver
{

    override var layer: KClass<out BaseLayer> = LayerBackground::class

    override var parents: Array<KClass<out BaseScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        ResultsScene::class,
        SelectorScene::class
    )

    val image = FadeImageView {

        setDimensions {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

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