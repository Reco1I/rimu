package game.rimu.ui.layouts

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.doPost
import com.reco1l.framework.android.views.orientation
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.animation.Ease
import com.reco1l.framework.animation.animate
import com.reco1l.framework.animation.cancelAnimators
import com.reco1l.framework.animation.toAlpha
import com.reco1l.framework.animation.toTranslationX
import com.reco1l.framework.graphics.Anchor
import game.rimu.R
import game.rimu.MainContext
import game.rimu.constants.RimuSetting
import game.rimu.data.adapter.Adapter
import game.rimu.data.adapter.IHeldView
import game.rimu.ui.BaseLayer
import game.rimu.ui.LayerBackground
import game.rimu.ui.LayerOverlay
import game.rimu.ui.layouts.SettingTab.SKINS
import game.rimu.ui.layouts.SettingTab.entries
import game.rimu.ui.views.DummyView
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.RecyclerView
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import game.rimu.ui.views.preference.SeekBarSettingView
import kotlin.reflect.KClass


enum class SettingTab
{
    SKINS
    // TODO
}

class SettingsMenu(ctx: MainContext) : ModelLayout(ctx)
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class


    private val body = LinearLayout {

        z = 1f
        orientation = HORIZONTAL
        dimensions.height = MATCH_PARENT

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.15f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val tabContents = RecyclerView(body) {

        dimensions.apply {
            height = MATCH_PARENT
            width = 300
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
    }

    private val tabSelector = RecyclerView(body) {

        orientation = VERTICAL
        adapter = Adapter(entries, { TabIconButton() })

        dimensions.height = MATCH_PARENT

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }
    }

    private val bodyShadow = DummyView {

        dimensions.apply {
            width = tabContents.dimensions.width + 70 // Tab selection bar width.
            height = MATCH_PARENT
        }

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.1f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }


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


    private var currentTab: SettingTab

    private var backgroundAnimator: ValueAnimator? = null


    init
    {
        setTouchHandler {
            noEffect()
            onActionUp = { hide() }
        }

        backgroundColor = Color.BLACK
        background.alpha = 0
        currentTab = SKINS
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        backgroundAnimator?.cancel()
        backgroundAnimator = background::setAlpha.animate(background.alpha, 65, 200)

        ctx.layouts[LayerBackground::class].apply {

            cancelAnimators()
            toTranslationX(-50f, 400, ease = Ease.EXPO_OUT)
        }

        body.apply {
            cancelAnimators()
            toAlpha(0f)

            doPost {
                toTranslationX(width.toFloat())
                toAlpha(1f)
                toTranslationX(0f, 400, 50, Ease.EXPO_OUT)
            }
        }

        bodyShadow.apply {
            cancelAnimators()
            toAlpha(0f)

            doPost {
                toTranslationX(width.toFloat())
                toAlpha(1f)
                toTranslationX(0f, 400, ease = Ease.EXPO_OUT)
            }
        }

    }


    override fun hide()
    {
        backgroundAnimator?.cancel()
        backgroundAnimator = background::setAlpha.animate(background.alpha, 0, 200)

        ctx.layouts[LayerBackground::class].apply {

            cancelAnimators()
            toTranslationX(0f, 350, ease = Ease.EXPO_IN)
        }

        body.apply {
            cancelAnimators()
            toTranslationX(width.toFloat(), 350, ease = Ease.EXPO_IN)
        }

        bodyShadow.apply {
            cancelAnimators()
            toTranslationX(width.toFloat(), 350, 50, Ease.EXPO_IN, listener = {
                onEnd = { super.hide() }
            })
        }
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