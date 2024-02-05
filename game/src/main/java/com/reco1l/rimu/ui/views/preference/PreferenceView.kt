package com.reco1l.rimu.ui.views.preference

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting
import com.reco1l.rimu.management.Setting
import com.reco1l.rimu.ui.views.ConstraintLayout
import com.reco1l.rimu.ui.views.SeekBar
import com.reco1l.rimu.ui.views.TextButton
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import com.reco1l.rimu.ui.views.view


sealed class SettingView(ctx: MainContext) : ConstraintLayout(ctx)
{

    override val dimensions = super.dimensions.apply {

        width = MATCH_PARENT
        cornerRadius = 12f
        padding(12)
        margin(8)
    }

    protected var hasSummary = false

    protected open val titleView = TextView {}

    protected open val summaryView by lazy {

        // Summary view will not be created unless the summary has been set.
        hasSummary = true

        TextView {

            setDimensions {
                fontSize = 8
            }

            setSkinning {
                fontColorFactor = 0.8f
            }

            setConstraints(target = titleView, topToTarget = Anchor.BOTTOM)
        }
    }


    var title
        get() = titleView.text?.toString()
        set(value)
        {
            titleView.text = value
        }

    var summary: String?
        get() = summaryView.text?.toString()
        set(value)
        {
            summaryView.text = value
        }

}

sealed class SettingViewWithBinding<T : Any>(ctx: MainContext, key: RimuSetting) : SettingView(ctx)
{

    protected var binding by Setting<T>(key)

}


// Button


fun IWithContext.ButtonSettingView(
    parent: ViewGroup? = this as? ViewGroup,
    init: ButtonSettingView.() -> Unit
) = ButtonSettingView(ctx).apply {
    parent?.addView(this)
    init()
}

class ButtonSettingView(ctx: MainContext) : SettingView(ctx)
{

    override val titleView = TextButton {

        setDimensions {
            width = MATCH_PARENT
        }
    }

    var onButtonPress: (() -> Unit)? = null
        set(value)
        {
            titleView.setTouchHandler { onActionUp = value }
            field = value
        }
}


// Dropdown

fun <T : Any> IWithContext.DropdownSettingView(
    key: RimuSetting,
    parent: ViewGroup? = this as? ViewGroup,
    init: DropdownSettingView<T>.() -> Unit
) = DropdownSettingView<T>(ctx, key).apply {
    parent?.addView(this)
    init()
}

class DropdownSettingView<T : Any>(
    ctx: MainContext,
    key: RimuSetting,
) : SettingViewWithBinding<T>(ctx, key)


// SeekBar

fun IWithContext.SeekBarSettingView(
    key: RimuSetting,
    parent: ViewGroup? = this as? ViewGroup,
    init: SeekBarSettingView.() -> Unit
) = SeekBarSettingView(ctx, key).apply {
    parent?.addView(this)
    init()
}

class SeekBarSettingView(
    ctx: MainContext,
    key: RimuSetting
) : SettingViewWithBinding<Float>(ctx, key)
{

    /**
     * If `true` the bound option will be updated as while user is seeking, otherwise it'll be
     * updated after it stops seeking.
     */
    var immediateChange = true

    /**
     * Set the max allowed value, by default `100`.
     */
    var max: Float
        get() = seekBar.max
        set(value)
        {
            seekBar.max = value
        }

    /**
     * Set the max allowed value, by default `0`.
     */
    var min: Float
        get() = seekBar.min
        set(value)
        {
            seekBar.min = value
        }


    private val seekBar = SeekBar {

        setDimensions {
            width = MATCH_PARENT
            marginTop = 10
        }

        onSeek = {
            if (immediateChange)
                updateBinding()
        }

        onEndSeek = {
            if (!immediateChange)
                updateBinding()
        }
    }


    private fun updateBinding()
    {
        binding = seekBar.progress
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        seekBar.setConstraints(
            target = if (hasSummary) summaryView else titleView,
            topToTarget = Anchor.BOTTOM
        )

        seekBar.progress = binding
    }

}