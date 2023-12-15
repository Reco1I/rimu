package com.reco1l.framework.android.views

import android.widget.LinearLayout.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import com.reco1l.rimu.ui.views.RecyclerView


var RecyclerView.orientation
    get() = (layoutManager as? LinearLayoutManager)?.orientation
    set(value)
    {
        if (layoutManager !is LinearLayoutManager)
            layoutManager = LinearLayoutManager(context)

        (layoutManager as? LinearLayoutManager)?.orientation = value ?: VERTICAL
    }