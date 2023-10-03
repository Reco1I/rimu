package game.rimu.ui.views

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toDrawable
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.views.addons.IScalableWithDimensions
import game.rimu.ui.views.addons.ViewDimensions


fun <T> T.ImageView(
    attach: Boolean = true,
    block: ImageView.() -> Unit
) where T : ViewGroup, T : IWithContext = ImageView(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

open class ImageView(override val ctx: RimuContext) :
    AppCompatImageView(ctx),
    IWithContext,
    IScalableWithDimensions<ViewDimensions>
{
    override val dimensions = ViewDimensions()
}

/**
 * ImageView that fades when the drawable is changed.
 */

class FadeImageView(override val ctx: RimuContext) :
    AppCompatImageView(ctx),
    IWithContext
{

    private val blankDrawable = ColorDrawable(Color.TRANSPARENT)

    /**
     * The transition animation duration.
     */
    var duration: Int = 300


    // The transition drawable that will be used for the animation, initially it will contain two
    // transparent ColorDrawables.
    private val transition = TransitionDrawable(
        arrayOf(
            ColorDrawable(Color.TRANSPARENT),
            blankDrawable
        )
    )


    init
    {
        // Calling super to set the TransitionDrawable properly.
        super.setImageDrawable(transition)
    }


    override fun setImageBitmap(bitmap: Bitmap?) = setImageDrawable(bitmap?.toDrawable(resources))

    /**
     * Sets a drawable as the content of this ImageView with a transition animation.
     *
     * @see duration
     */
    override fun setImageDrawable(drawable: Drawable?) = playTransition(drawable)


    private fun playTransition(newDrawable: Drawable?)
    {
        transition.setDrawable(0, transition.getDrawable(1))
        transition.setDrawable(1, newDrawable ?: blankDrawable)
        transition.startTransition(duration)

        requestLayout()
    }

    /**
     * Returns `true` if it has an image showing, use this instead of `drawable == null`.
     */
    fun hasImage() = transition.getDrawable(1) != blankDrawable
}