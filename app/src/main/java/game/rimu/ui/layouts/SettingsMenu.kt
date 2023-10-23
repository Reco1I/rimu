package game.rimu.ui.layouts

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.VERTICAL
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.orientation
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.graphics.Anchor
import game.rimu.R
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting
import game.rimu.data.adapter.Adapter
import game.rimu.data.adapter.IHeldView
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.layouts.SettingTab.*
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.RecyclerView
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import game.rimu.ui.views.preference.ButtonSettingView
import game.rimu.ui.views.preference.SeekBarSettingView
import kotlin.reflect.KClass


enum class SettingTab
{
    SKINS
    // TODO
}

class SettingsMenu(ctx: RimuContext) : ModelLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class

    private val tabSelector = RecyclerView {

        orientation = VERTICAL
        adapter = Adapter(entries, { TabIconButton() })

        dimensions.height = MATCH_PARENT

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val tabContents = RecyclerView {

        dimensions.apply {
            height = MATCH_PARENT
            width = 300
        }

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.15f
        }

        orientation = VERTICAL
        adapter = Adapter(
            data = entries,
            onCreateView = {
                when (entries[it])
                {
                    SKINS -> skinsLayout
                }
            }
        )

        setConstraints(target = tabSelector, rightToTarget = Anchor.LEFT)
    }


    private var currentTab: SettingTab


    // Layouts

    private val skinsLayout = TabSectionLayout {

        title.text = "UI"

        SeekBarSettingView(RimuSetting.UI_SCALE)
        {
            max = 1.5f
            min = 0.5f
            immediateChange = false
            title = ctx.getString(R.string.setting_ui_scale)
        }

        SeekBarSettingView(RimuSetting.MUSIC_VOLUME)
        {
            title = ctx.getString(R.string.setting_bgm_volume)
        }
    }


    init
    {
        setTouchHandler {
            noEffect()
            onActionUp = { hide() }
        }

        backgroundColor = 0x41000000
        currentTab = SKINS
    }


    inner class TabIconButton : IconButton(ctx), IHeldView<SettingTab>
    {
        override fun onAssignData(data: SettingTab, position: Int)
        {
            rules.image = when (data)
            {
                SKINS -> "icon-skin"
            }
            invalidateSkin()

            setTouchHandler { onActionUp = { currentTab = data } }
        }
    }

    inner class TabSectionLayout(block: TabSectionLayout.() -> Unit) :
        LinearLayout(ctx),
        IHeldView<SettingTab>
    {

        val title = TextView {

            dimensions.apply {
                width = MATCH_PARENT
                fontSize = 20
                padding(0, 18)
            }

            rules.apply {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.15f
            }

            gravity = Gravity.CENTER
        }

        init
        {
            orientation = VERTICAL
            dimensions.width = MATCH_PARENT

            rules.apply {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.175f
            }

            block()
        }

        override fun onAssignData(data: SettingTab, position: Int) = Unit
    }
}