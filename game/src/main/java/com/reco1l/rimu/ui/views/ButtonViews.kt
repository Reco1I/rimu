package com.reco1l.rimu.ui.views

import android.view.Gravity
import com.reco1l.rimu.MainContext


/**
 * Button with icon in center.
 */
open class IconButton(ctx: MainContext) : ImageView(ctx)
{

    override val dimensions = super.dimensions.apply {

        height = 50
        width = 70
        padding(16)
    }

    override val skinningRules = super.skinningRules.apply {

        imageTint = "accentColor"
    }

    init
    {
        scaleType = ScaleType.FIT_CENTER
    }
}


/**
 * Button with text in center.
 */
open class TextButton(ctx: MainContext) : TextView(ctx)
{

    override val dimensions = super.dimensions.apply {

        fontSize = 12
        cornerRadius = 12f
        padding(12, 8)
    }

    override val skinningRules = super.skinningRules.apply {

        backgroundColor = "accentColor"
        fontColor = "accentColor"
        fontColorFactor = 0.1f
    }

    init
    {
        gravity = Gravity.CENTER
    }
}