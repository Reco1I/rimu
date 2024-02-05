package com.reco1l.rimu.ui.layouts

import android.view.Gravity
import android.view.Gravity.CENTER_VERTICAL
import android.view.Gravity.RIGHT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import com.reco1l.toolkt.android.attachTo
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.toolkt.graphics.Anchor

import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.scenes.MenuScene
import com.reco1l.rimu.ui.scenes.ResultsScene
import com.reco1l.rimu.ui.scenes.BaseScene
import com.reco1l.rimu.ui.scenes.SceneIntro
import com.reco1l.rimu.ui.scenes.SelectorScene
import com.reco1l.rimu.ui.views.IconButton
import com.reco1l.rimu.ui.views.ImageView
import com.reco1l.rimu.ui.views.LinearLayout
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import com.reco1l.rimu.ui.views.view
import kotlin.reflect.KClass

class TopBarLayout(ctx: MainContext) :
    ModelLayout(
        ctx = ctx,
        layer = LayerOverlay::class,
        parents = arrayOf(
            SceneIntro::class,
            MenuScene::class,
            SelectorScene::class,
            ResultsScene::class
        )
    )
{

    val navigationTree = mutableListOf<() -> Boolean>()


    val backButton = IconButton {

        setSkinning { image = "icon-back" }

        setTouchHandler { onActionUp = ctx.skins::next }
    }


    val leftLayout = LinearLayout {

        setBackgroundColor(0x26000000)

        gravity = CENTER_VERTICAL
        orientation = HORIZONTAL

        setDimensions {
            height = MATCH_PARENT
        }

        setConstraints(
            target = backButton,
            leftToTarget = Anchor.RIGHT
        )

        IconButton {

            setSkinning {
                image = "icon-music"
            }

            setTouchHandler {

                onActionUp = { ctx.layouts[MusicPlayerBox::class].alternate() }
            }

        }
    }

    val rightLayout = LinearLayout {

        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL or RIGHT

        setDimensions {
            height = MATCH_PARENT
        }

        setConstraints(rightToTarget = Anchor.RIGHT)

        IconButton {

            setSkinning {
                image = "icon-notification"
            }

            setTouchHandler { onActionUp = { ctx.layouts[NotificationCenter::class].alternate() } }
        }

        UserBoxView(ctx) attachTo this

        IconButton {

            setSkinning {
                image = "icon-settings"
            }
            setTouchHandler { onActionUp = { ctx.layouts[SettingsMenu::class].alternate() } }
        }
    }


    init
    {
        setDimensions {
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

        setDimensions {
            height = MATCH_PARENT
            paddingLeft = 12
            paddingRight = 12
            cornerRadius = 6f

            marginTop = 4
            marginBottom = 4
        }
    }

    private val avatar = ImageView {

        setDimensions {
            width = 26
            height = 26
            cornerRadius = 7f
        }

        skinningRules.image = "avatar-default"
    }

    private val username = TextView {

        text = "Reco1l"

        setDimensions {
            marginLeft = 12
            marginRight = 4
        }
    }
}