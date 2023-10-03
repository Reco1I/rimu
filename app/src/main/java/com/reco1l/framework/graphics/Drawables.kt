package com.reco1l.framework.graphics

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape


fun Shape.toDrawable() = ShapeDrawable().apply {

    shape = this@toDrawable
}