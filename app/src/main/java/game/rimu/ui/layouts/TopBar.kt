package game.rimu.ui.layouts

import android.view.Gravity
import android.view.Gravity.CENTER_VERTICAL
import android.view.Gravity.RIGHT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.graphics.Anchor
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.LayerOverlay
import game.rimu.ui.BaseLayer
import game.rimu.ui.scenes.MenuScene
import game.rimu.ui.scenes.ResultsScene
import game.rimu.ui.scenes.BaseScene
import game.rimu.ui.scenes.SceneIntro
import game.rimu.ui.scenes.SelectorScene
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.ImageView
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import kotlin.reflect.KClass

class TopBarLayout(ctx: MainContext) : ModelLayout(ctx)
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    override var parents: Array<KClass<out BaseScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        SelectorScene::class,
        ResultsScene::class
    )


    val navigationTree = mutableListOf<() -> Boolean>()


    val backButton = IconButton {

        rules.image = "icon-back"

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

        IconButton {

            rules.image = "icon-music"

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

        IconButton {
            rules.image = "icon-notification"
            setTouchHandler { onActionUp = { ctx.layouts[NotificationCenter::class].alternate() } }
        }

        UserBoxView(ctx) attachTo this

        IconButton {
            rules.image = "icon-settings"
            setTouchHandler { onActionUp = { ctx.layouts[SettingsMenu::class].alternate() } }
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
        setBackgroundColor(skin.data.colours.accentColor.toInt(factor = 0.15f))
        super.onApplySkin(skin)
    }


}


class UserBoxView(ctx: MainContext) : LinearLayout(ctx)
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

        rules.image = "avatar-default"
    }

    private val username = TextView {

        text = "Reco1l"

        dimensions.apply {
            marginLeft = 12
            marginRight = 4
        }
    }
}