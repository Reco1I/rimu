package game.rimu.ui.views

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextUtils
import android.text.method.ArrowKeyMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.AppCompatTextView
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.font
import com.reco1l.framework.android.views.fontColor
import com.reco1l.framework.android.views.fontSize
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.graphics.BasicAnchor
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ViewDimensions
import game.rimu.ui.ViewSkinningRules
import kotlin.math.max


data class TextViewDimensions<T : TextView>(

    var fontSize: Int = 14

) : ViewDimensions<T>()
{
    override fun onApplyScale(target: T, scale: Float)
    {
        super.onApplyScale(target, scale)

        target.fontSize = fontSize * scale
    }
}

data class TextViewSkinningRules<T : TextView>(

    /**
     * The font asset name and variant.
     */
    var font: String? = "font",

    var fontVariant: Int = 0,

    /**
     * The font color and factor applied to it, by default is the accent color with factor 1.0.
     */
    var fontColor: String? = "accentColor",

    var fontColorFactor: Float = 1f

) : ViewSkinningRules<T>()
{

    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        super.onApplySkin(target, skin)

        font?.also {

            target.font = skin.ctx.resources[it, fontVariant] ?: Typeface.DEFAULT
        }

        fontColor?.also {

            target.fontColor = skin.colors[it]?.factorInt(fontColorFactor) ?: Color.TRANSPARENT
        }
    }

}


// Base

fun <T> T.TextView(
    attach: Boolean = true,
    init: TextView.() -> Unit
) where T : IWithContext,
        T : ViewGroup = TextView(ctx) child@{

    if (attach)
        this@TextView.addView(this@child)

    init()
}

/**
 * Base class for every [TextView][AppCompatTextView] in rimu!, it has an special handling for icons.
 */
open class TextView(final override val ctx: RimuContext, init: TextView.() -> Unit) :
    AppCompatTextView(ctx),
    IWithContext,
    ISkinnableWithRules<TextView>,
    IScalableWithDimensions<TextView>
{

    override val dimensions = TextViewDimensions<TextView>()

    override val skinningRules = TextViewSkinningRules<TextView>()


    private val icons = arrayOfNulls<Icon>(4)


    init { init() }


    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)
        invalidateIcons()
    }


    /**
     * Set an icon for the specified [anchor].
     */
    fun setIcon(@BasicAnchor anchor: Int, drawable: Drawable, scale: Float = 1f)
    {
        icons[anchor - 1] = Icon(drawable, anchor, scale)
        invalidateIcons()
    }

    /**
     * Change the scale of the icon at the specified [anchor].
     */
    fun setIconScale(@BasicAnchor anchor: Int, scale: Float)
    {
        icons[anchor - 1]?.scale = scale
        invalidateIcons()
    }

    /**
     * Invalidate icons to redraw them.
     */
    fun invalidateIcons()
    {
        for (icon in icons)
        {
            if (icon == null)
                continue

            var w = icon.drawable.intrinsicWidth
            var h = icon.drawable.intrinsicHeight

            // Matching icon size with font size
            val factor = fontSize / max(w, h)

            w = (w * factor * icon.scale).toInt()
            h = (h * factor * icon.scale).toInt()

            icon.drawable.setTint(fontColor)
            icon.drawable.setBounds(0, 0, w, h)
        }

        // Invalidating icons
        setCompoundDrawables(
            icons[Anchor.LEFT - 1]?.drawable,
            icons[Anchor.TOP - 1]?.drawable,
            icons[Anchor.RIGHT - 1]?.drawable,
            icons[Anchor.BOTTOM - 1]?.drawable
        )
    }


    /**
     * A text icon, this is equivalent to a compound drawable.
     */
    data class Icon(

        /**
         * The icon drawable.
         */
        val drawable: Drawable,

        /**
         * The icon anchor position.
         */
        @BasicAnchor // TODO: Should support complex anchors in the future
        val anchor: Int,

        /**
         * The scale applied to the icon.
         */
        var scale: Float = 1f

    )
}



fun <T> T.TextField(
    attach: Boolean = true,
    init: TextField.() -> Unit
) where T : IWithContext,
        T : ViewGroup = TextField(ctx) child@{

    if (attach)
        this@TextField.addView(this@child)

    init()
}

/**
 * Base class for EditText in rimu!
 */
open class TextField(ctx: RimuContext, init: TextField.() -> Unit) :

    TextView(ctx, {}),
    IWithContext
{

    override val dimensions = super.dimensions.apply {

        cornerRadius = 8f
        padding(16, 14)
    }

    /**
     * Allow the input in the field, if this is set to `false` the keyboard will automatically hide
     * in case it was showing.
     *
     * @see [hideKeyboard]
     */
    var allowInput = true
        set(value)
        {
            field = value

            // De-focusing the view in case it is.
            isEnabled = value
            isFocusable = value
            isFocusableInTouchMode = value

            // Hiding the keyboard in case it was set to false
            if (!value)
                hideKeyboard()
        }


    // Saving input manager to avoid calling Context when using hide/show keyboard functions.
    private val inputManager = ctx.getSystemService(InputMethodManager::class.java)


    init
    {
        allowInput = true

        backgroundColor = 0x4D000000
        gravity = Gravity.CENTER_VERTICAL
        imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
    }


    /**
     * Show the keyboard forcefully if [allowInput] is enabled.
     */
    fun showKeyboard()
    {
        if (!allowInput)
            return

        // Requesting view focus
        requestFocus()

        // Showing soft keyboard
        @Suppress("DEPRECATION")
        inputManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)
    }

    /**
     * Hide the keyboard and clear view focus in case it is showing.
     */
    fun hideKeyboard()
    {
        // Clearing view focus
        clearFocus()

        // Hiding soft keyboard
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        // Ignoring input in case it's not allowed
        if (!allowInput)
            return false

        return super.onTouchEvent(event)
    }


    // Functions extracted from original EditText class:

    override fun getText(): Editable?
    {
        val text = super.getText() ?: return null

        if (text !is Editable)
            setText(text, BufferType.EDITABLE)

        return super.getText() as Editable
    }

    override fun setText(text: CharSequence, type: BufferType) = super.setText(text,
        BufferType.EDITABLE
    )

    override fun setEllipsize(ellipsis: TextUtils.TruncateAt)
    {
        if (ellipsis == TextUtils.TruncateAt.MARQUEE)
            return

        super.setEllipsize(ellipsis)
    }


    override fun getFreezesText() = true

    override fun getDefaultEditable() = true

    override fun getDefaultMovementMethod() = ArrowKeyMovementMethod.getInstance()!!

    override fun getAccessibilityClassName(): String = EditText::class.java.name
}