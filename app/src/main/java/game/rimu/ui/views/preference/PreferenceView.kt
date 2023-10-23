package game.rimu.ui.views.preference

import android.os.Build
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.android.material.slider.Slider
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.graphics.Anchor
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting
import game.rimu.management.Setting
import game.rimu.ui.views.ConstraintLayout
import game.rimu.ui.views.SeekBar
import game.rimu.ui.views.TextButton
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import kotlin.math.pow
import android.widget.SeekBar as AndroidSeekBar


sealed class SettingView(ctx: RimuContext) : ConstraintLayout(ctx)
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

            dimensions.fontSize = 8
            rules.fontColorFactor = 0.8f

            setConstraints(target = titleView, topToTarget = Anchor.BOTTOM)
        }
    }


    var title
        get() = titleView.text?.toString()
        set(value) { titleView.text = value }

    var summary: String?
        get() = summaryView.text?.toString()
        set(value) { summaryView.text = value }

}

sealed class SettingViewWithBinding<T : Any>(ctx: RimuContext, key: RimuSetting) : SettingView(ctx)
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

class ButtonSettingView(ctx: RimuContext) : SettingView(ctx)
{

    override val titleView = TextButton {

        dimensions.width = MATCH_PARENT
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
    ctx: RimuContext,
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
    ctx: RimuContext,
    key: RimuSetting
) :
    SettingViewWithBinding<Float>(ctx, key),
    Slider.OnSliderTouchListener,
    Slider.OnChangeListener
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
        get() = seekBar.valueTo
        set(value) { seekBar.valueTo = value }

    /**
     * Set the max allowed value, by default `0`.
     */
    var min: Float
        get() = seekBar.valueFrom
        set(value) { seekBar.valueFrom = value }


    private val seekBar = SeekBar {

        dimensions.apply {
            width = MATCH_PARENT
            marginTop = 10
        }

        addOnSliderTouchListener(this@SeekBarSettingView)
        addOnChangeListener(this@SeekBarSettingView)
    }


    private fun updateBinding()
    {
        binding = seekBar.value
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        seekBar.setConstraints(
            target = if (hasSummary) summaryView else titleView,
            topToTarget = Anchor.BOTTOM
        )

        seekBar.value = binding
    }


    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean)
    {
        if (immediateChange && fromUser)
            updateBinding()
    }

    override fun onStopTrackingTouch(slider: Slider)
    {
        if (!immediateChange)
            updateBinding()
    }

    override fun onStartTrackingTouch(slider: Slider) = Unit

}