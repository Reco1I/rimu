package com.reco1l.rimu.data.adapter

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.reco1l.rimu.ui.IScalable
import com.reco1l.rimu.ui.ISkinnable
import androidx.recyclerview.widget.RecyclerView.Adapter as AndroidAdapter

/**
 * Basic implementation of data adapter for RecyclerViews.
 */
open class Adapter<T>(

    /**
     * The data list.
     */
    var data: List<T>,

    /**
     * Callback for views creation.
     */
    var onCreateView: (Int) -> IHeldView<T>,

    /**
     * Callback used to distinct view types.
     */
    var onDistinctViews: (Int) -> Int = { it }

) : AndroidAdapter<ViewHolder>()
{


    override fun getItemCount() = data.size


    override fun getItemId(position: Int) = if (hasStableIds()) position.toLong() else NO_ID

    override fun getItemViewType(position: Int) = onDistinctViews(position)


    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder
    {
        return object : ViewHolder(onCreateView(type) as View)
        {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        @Suppress("UNCHECKED_CAST")
        (holder.itemView as IHeldView<T>).onBindData(data[position], position)
    }


    override fun onViewAttachedToWindow(holder: ViewHolder)
    {
        super.onViewAttachedToWindow(holder)

        val view = holder.itemView

        if (view is IScalable)
            view.invalidateScale()

        if (view is ISkinnable)
            view.invalidateSkin()
    }

}

/**
 * Callback for data assignment by the adapter.
 */
interface IHeldView<T>
{

    /**
     * The currently bound data.
     */
    var boundData: T?

    /**
     * Called when the data in the adapter is bound to the view.
     */
    @CallSuper
    fun onBindData(data: T, position: Int)
    {
        boundData = data
    }
}

