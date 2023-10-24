package game.rimu.ui.views

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.data.adapter.Adapter
import game.rimu.data.adapter.IHeldView
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.views.addons.setTouchHandler
import androidx.recyclerview.widget.RecyclerView as AndroidRecyclerView



// ReyclerView

fun IWithContext.RecyclerView(
    parent: ViewGroup? = this as? ViewGroup,
    init: RecyclerView.() -> Unit
) = RecyclerView(ctx).apply {
    parent?.addView(this)
    init()
}

open class RecyclerView(final override val ctx: MainContext) :
    AndroidRecyclerView(ctx),
    IWithContext,
    ISkinnableWithRules<RecyclerView>,
    IScalableWithDimensions<RecyclerView>
{

    override val dimensions by lazy { ViewDimensions<RecyclerView>() }

    override val rules by lazy { ViewSkinningRules<RecyclerView>() }

    init
    {
        layoutManager = LinearLayoutManager(ctx)
    }
}



// Dropdown

open class DropdownMenu(ctx: MainContext) : RecyclerView(ctx)
{

    private val items = mutableListOf<Item>()


    override val dimensions = super.dimensions.apply {

        padding(2)
    }

    override val rules = super.rules.apply {

        backgroundColor = "accentColor"
        backgroundColorFactor = 0.2f
    }


    init {

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
        override val dimensions = super.dimensions.apply {

            cornerRadius = 8f
            padding(12, 8)
        }

        override fun onAssignData(data: Item, position: Int)
        {
            text = data.name
            setTouchHandler { onActionUp = data.onTouch }
        }
    }


    data class Item(

        val name: String,

        val onTouch: () -> Unit
    )
}