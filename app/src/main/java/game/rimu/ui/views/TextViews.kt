package game.rimu.ui.views

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextUtils
import android.text.method.ArrowKeyMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.AppCompatTextView
import com.reco1l.framework.android.views.font
import com.reco1l.framework.android.views.fontColor
import com.reco1l.framework.android.views.fontSize
import com.reco1l.framework.android.views.radius
import com.reco1l.framework.android.views.setMargins
import com.reco1l.framework.annotation.Anchor
import com.reco1l.framework.annotation.BasicAnchor
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.views.addons.IScalableWithDimensions
import game.rimu.ui.views.addons.ISkinnable
import game.rimu.ui.views.addons.ViewDimensions
import kotlin.math.max


data class TextViewDimensions(

    var fontSize: Int = 18

) : ViewDimensions()
{

    override fun onApplyScale(view: View, scale: Float)
    {
        super.onApplyScale(view, scale)

        view as TextView
        view.fontSize = fontSize * scale
    }
}



fun <T> T.TextView(
    attach: Boolean = true,
    block: TextView.() -> Unit
) where T : ViewGroup, T : IWithContext = TextView(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

/**
 * Base class for every [TextView][AppCompatTextView] in rimu!, it has an special handling for icons.
 */

open class TextView(final override val ctx: RimuContext) :
    AppCompatTextView(ctx),
    IWithContext,
    ISkinnable,
    IScalableWithDimensions<TextViewDimensions>
{

    override val dimensions = TextViewDimensions()


    /**
     * If `true` the accent color will be applied automatically when changing skin.
     */
    var applyAccentColor = true


    private val icons = arrayOfNulls<Icon>(4)


    override fun onApplySkin(skin: WorkingSkin)
    {
        font = ctx.resources["font", 0]!!

        if (applyAccentColor)
            fontColor = skin.data.colours.accentColor.hexInt
    }


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
    block: TextField.() -> Unit
) where T : ViewGroup, T : IWithContext = TextField(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

/**
 * Base class for EditText in rimu!
 */

open class TextField(ctx: RimuContext) :

    TextView(ctx),
    IWithContext
{

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


    override fun onApplySkin(skin: WorkingSkin)
    {
        super.onApplySkin(skin)

        setBackgroundColor(0x4D000000)
    }

    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)

        radius = 8f

        setMargins(
            top = (14f).toScale(),
            bottom = (14f).toScale(),
            left = (16f).toScale(),
            right = (16f).toScale()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
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