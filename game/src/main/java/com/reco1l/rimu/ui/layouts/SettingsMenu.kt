package com.reco1l.rimu.ui.layouts

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import com.reco1l.toolkt.android.backgroundColor
import com.reco1l.toolkt.android.doPost
import com.reco1l.toolkt.android.orientation
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.toolkt.animation.Ease
import com.reco1l.toolkt.animation.animate
import com.reco1l.toolkt.animation.cancelAnimators
import com.reco1l.toolkt.animation.toAlpha
import com.reco1l.toolkt.animation.toTranslationX
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.rimu.R
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting
import com.reco1l.rimu.data.adapter.Adapter
import com.reco1l.rimu.data.adapter.IHeldView
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.LayerBackground
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.layouts.SettingTab.SKINS
import com.reco1l.rimu.ui.layouts.SettingTab.entries
import com.reco1l.rimu.ui.views.DummyView
import com.reco1l.rimu.ui.views.IconButton
import com.reco1l.rimu.ui.views.LinearLayout
import com.reco1l.rimu.ui.views.RecyclerView
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import com.reco1l.rimu.ui.views.preference.SeekBarSettingView
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

        setDimensions {
            height = MATCH_PARENT
        }

        setSkinning {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.15f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val tabContents = RecyclerView(body) {

        setDimensions {
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

        setDimensions {
            height = MATCH_PARENT
        }

        setSkinning {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }
    }

    private val bodyShadow = DummyView {

        setDimensions {
            width = tabContents.dimensions.width + 70 // Tab selection bar width.
            height = MATCH_PARENT
        }

        setSkinning {
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

            setDimensions {
                width = MATCH_PARENT
                fontSize = 20
                padding(0, 18)
            }

            setSkinning {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.15f
            }

            gravity = Gravity.CENTER
        }

        init
        {
            orientation = VERTICAL

            setDimensions {
                width = MATCH_PARENT
            }

            setSkinning {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.175f
            }

            block()
        }

        override fun onAssignData(data: SettingTab, position: Int) = Unit
    }
}