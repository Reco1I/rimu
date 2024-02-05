package com.reco1l.rimu.ui.views

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.adapter.Adapter
import com.reco1l.rimu.data.adapter.IHeldView
import com.reco1l.rimu.ui.IScalableWithDimensions
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import com.reco1l.toolkt.android.orientation
import kotlin.math.abs
import androidx.recyclerview.widget.RecyclerView as AndroidRecyclerView


fun ViewGroup.RecyclerView(block: RecyclerView.() -> Unit) = RecyclerView(context as MainContext).also {
    addView(it)
    it.block()
}

open class RecyclerView(final override val ctx: MainContext) :
    AndroidRecyclerView(ctx),
    IWithContext,
    ISkinnableWithRules<RecyclerView, ViewSkinningRules<RecyclerView>>,
    IScalableWithDimensions<RecyclerView, ViewDimensions<RecyclerView>>
{

    override val dimensions by lazy { ViewDimensions<RecyclerView>() }

    override val skinningRules by lazy { ViewSkinningRules<RecyclerView>() }

    init
    {
        layoutManager = LinearLayoutManager(ctx)
    }
}


// Carrousel

fun ViewGroup.CarrouselRecyclerView(block: CarrouselRecyclerView.() -> Unit) = CarrouselRecyclerView(context as MainContext).also {
    addView(it)
    it.block()
}

class CarrouselRecyclerView(ctx: MainContext) : RecyclerView(ctx)
{

    /**
     * Whether the translation should be inverted.
     *
     * By default the translation on both axis from zero to positive values, this means that if the
     * layout direction is vertical then the translation will be from left to right and in case of
     * horizontal layout direction the translation is from top to bottom.
     * That can be inverted by setting this to `true`.
     */
    var invertedTranslation = false
        set(value)
        {
            field = value
            invalidateTranslations()
        }


    override fun onScrolled(dx: Int, dy: Int) = invalidateTranslations()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateTranslations()
    }


    private fun invalidateTranslations()
    {
        val coordinates = IntArray(2)

        var i = childCount - 1

        while (i >= 0)
        {
            val view = getChildAt(i) ?: continue
            view.getLocationInWindow(coordinates)
            computeViewTranslation(view, coordinates[0], coordinates[1])
            --i
        }
    }

    private fun computeViewTranslation(view: View, viewX: Int, viewY: Int) = when (orientation)
    {
        HORIZONTAL -> {

            // Calculates the difference/offset from the center of the X axis.
            val offset = (width - view.width) / 2f
            val factor = 1 - abs(viewY - offset) / abs(offset + view.width / 0.01f)


            val translation = view.height * (1 - factor.coerceIn(0f, 1f))

            view.translationY = if (invertedTranslation) -translation else translation
        }

        VERTICAL -> {

            // Calculates the difference/offset from the center of the Y axis.
            val offset = (height - view.height) / 2f
            val factor = 1 - abs(viewX - offset) / abs(offset + view.height / 0.01f)


            val translation = view.width * (1 - factor.coerceIn(0f, 1f))

            view.translationX = if (invertedTranslation) -translation else translation
        }

        else -> Unit
    }

}


// Dropdown

open class DropdownMenu(ctx: MainContext) : RecyclerView(ctx)
{

    private val items = mutableListOf<Item>()


    override val dimensions = super.dimensions.apply {

        padding(2)
    }

    override val skinningRules = super.skinningRules.apply {

        backgroundColor = "accentColor"
        backgroundColorFactor = 0.2f
    }


    init
    {
        adapter = Adapter(
            data = items,
            onCreateView = { ItemView() }
        )
    }


    fun addItem(name: String, onTouch: () -> Unit)
    {
        val item = Item(name, onTouch)
        items.add(item)
        adapter!!.notifyItemInserted(items.indexOf(item))
    }


    inner class ItemView : TextView(ctx), IHeldView<Item>
    {

        override var boundData: Item? = null


        override val dimensions = super.dimensions.apply {

            cornerRadius = 8f
            padding(12, 8)
        }

        override fun onBindData(data: Item, position: Int)
        {
            super.onBindData(data, position)

            text = data.name
            setTouchHandler { onActionUp = data.onTouch }
        }
    }


    data class Item(

        val name: String,

        val onTouch: () -> Unit
    )
}