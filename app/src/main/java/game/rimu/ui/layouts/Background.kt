package game.rimu.ui.layouts

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView.ScaleType
import game.rimu.android.RimuContext
import game.rimu.ui.LayerBackground
import game.rimu.ui.LayoutLayer
import game.rimu.ui.scenes.MenuScene
import game.rimu.ui.scenes.ResultsScene
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.scenes.SceneIntro
import game.rimu.ui.scenes.SelectorScene
import game.rimu.ui.views.FadeImageView
import kotlin.reflect.KClass


class Background(ctx: RimuContext) : RimuLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerBackground::class

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


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        if (!image.hasImage())
            image.setImageBitmap(ctx.resources["menu-background", 0])
    }
}