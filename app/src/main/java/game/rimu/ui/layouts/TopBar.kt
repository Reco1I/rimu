package game.rimu.ui.layouts

import android.view.Gravity
import android.view.Gravity.CENTER_VERTICAL
import android.view.Gravity.RIGHT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.graphics.Anchor
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.scenes.MenuScene
import game.rimu.ui.scenes.ResultsScene
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.scenes.SceneIntro
import game.rimu.ui.scenes.SelectorScene
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.ImageView
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.TouchHandler
import game.rimu.ui.views.addons.setTouchHandler
import kotlin.reflect.KClass

class TopBarLayout(ctx: RimuContext) : RimuLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class

    override var parents: Array<KClass<out RimuScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        SelectorScene::class,
        ResultsScene::class
    )


    val navigationTree = mutableListOf<() -> Boolean>()


    val backButton = IconButton(texture = "icon-back") {

        setTouchHandler {  }

    }


    val leftLayout = LinearLayout {

        setBackgroundColor(0x26000000)

        dimensions.height = MATCH_PARENT
        gravity = CENTER_VERTICAL
        orientation = HORIZONTAL

        setConstraints(
            target = backButton,
            leftToTarget = Anchor.RIGHT
        )

        IconButton(texture = "icon-music") {

            setTouchHandler {

                onActionUp = { ctx.layouts[MusicPlayerBox::class].alternate() }
            }

        }
    }

    val rightLayout = LinearLayout {

        dimensions.height = MATCH_PARENT

        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL or RIGHT

        setConstraints(rightToTarget = Anchor.RIGHT)

        UserBoxView(ctx) attachTo this

        IconButton(texture = "icon-settings") {

            setOnTouchListener(TouchHandler {})

        }
    }


    init
    {
        dimensions.apply {
            height = 50
            width = MATCH_PARENT
        }
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        setBackgroundColor(skin.data.colours.accentColor.factorInt(0.15f))
        super.onApplySkin(skin)
    }


}


class UserBoxView(ctx: RimuContext) : LinearLayout(ctx)
{

    init
    {
        setBackgroundColor(0x26000000)
        setTouchHandler {  }

        gravity = Gravity.CENTER
        orientation = HORIZONTAL
        dimensions.height = MATCH_PARENT

        dimensions.apply {
            height = MATCH_PARENT
            paddingLeft = 12
            paddingRight = 12
            cornerRadius = 6f

            marginTop = 4
            marginBottom = 4
        }
    }

    private val avatar = ImageView {

        dimensions.apply {
            width = 26
            height = 26
            cornerRadius = 7f
        }

        skinningRules.texture = "avatar-default" to 0
    }

    private val username = TextView {

        text = "Reco1l"

        dimensions.apply {
            marginLeft = 12
            marginRight = 4
        }
    }
}