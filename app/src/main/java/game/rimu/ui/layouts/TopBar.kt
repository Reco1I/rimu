package game.rimu.ui.layouts

import android.view.Gravity.CENTER_VERTICAL
import android.view.Gravity.RIGHT
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import androidx.annotation.DrawableRes
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.android.views.setPaddings
import com.reco1l.framework.annotation.Anchor
import game.rimu.R
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.scenes.MenuScene
import game.rimu.ui.scenes.ResultsScene
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.scenes.SceneIntro
import game.rimu.ui.scenes.SelectorScene
import game.rimu.ui.views.ConstraintLayout
import game.rimu.ui.views.ImageView
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.IScalable
import game.rimu.ui.views.addons.dimensions
import kotlin.reflect.KClass

class TopBarLayout(ctx: RimuContext) : AttachableLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class

    override var parents: Array<KClass<out RimuScene>>? = arrayOf(
        SceneIntro::class,
        MenuScene::class,
        SelectorScene::class,
        ResultsScene::class
    )


    val navigationTree = mutableListOf<() -> Boolean>()


    val leftLayout = LinearLayout {

        dimensions.height = MATCH_PARENT

        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL
    }

    val rightLayout = LinearLayout {

        dimensions.height = MATCH_PARENT

        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL or RIGHT

        setConstraints(right = Anchor.RIGHT)

        UserBoxView(ctx) attachTo this

        Button(R.drawable.v_settings).apply {

            setImageResource(R.drawable.v_settings)


        } attachTo this
    }

    lateinit var backButton: View


    init
    {
        dimensions.height = 50
        dimensions.width = MATCH_PARENT

        Button(R.drawable.v_back).apply {


        } attachTo this


    }

    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        setBackgroundColor(skin.data.colours.accentColor.bright(0.15f).argbPackedInt)

        super.onApplySkin(skin)
    }


    inner class Button(@DrawableRes icon: Int) : ImageView(ctx), IScalable
    {

        init
        {
            setImageResource(icon)
        }

        override fun onApplyScale(scale: Float)
        {
            setPaddings(
                left = (18).toScale(),
                right = (18).toScale()
            )
        }
    }
}


class UserBoxView(ctx: RimuContext) : ConstraintLayout(ctx)
{

    init
    {
        setBackgroundColor(0x26000000)

        dimensions.height = MATCH_PARENT
    }

    private val avatar = ImageView {

        dimensions {
            size = 32
            marginLeft = 16
            radius = 7f
        }

        setImageResource(R.drawable.default_avatar)

        setConstraints(
            top = Anchor.TOP,
            bottom = Anchor.BOTTOM
        )
    }

    private val rank = TextView {

        text = "Offline"
        alpha = 0.25f

        dimensions {
            fontSize = 20
            marginBottom = -5
        }

        setConstraints(
            right = Anchor.RIGHT,
            bottom = Anchor.BOTTOM
        )
    }

    private val username = TextView {

        text = "Reco1l"

        dimensions {
            marginLeft = 16
            marginRight = 16
        }

        setConstraints(
            target = avatar,
            left = Anchor.RIGHT,
        )

        setConstraints(
            top = Anchor.TOP,
            bottom = Anchor.BOTTOM
        )
    }
}