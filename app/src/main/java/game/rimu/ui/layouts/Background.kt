package game.rimu.ui.layouts

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.setSize
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


class Background(ctx: RimuContext) : AttachableLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerBackground::class

    override var parents: Array<KClass<out RimuScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        ResultsScene::class,
        SelectorScene::class
    )

    val image = FadeImageView(ctx).apply {

        dimensions.width = MATCH_PARENT
        dimensions.height = MATCH_PARENT

    } attachTo this


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        if (!image.hasImage())
            image.setImageBitmap(ctx.resources["menu-background", 0])
    }
}