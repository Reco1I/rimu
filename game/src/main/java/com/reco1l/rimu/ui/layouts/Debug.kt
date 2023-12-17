package com.reco1l.rimu.ui.layouts

import android.annotation.SuppressLint
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.view.MotionEvent
import android.widget.TextView.BufferType.SPANNABLE
import androidx.core.text.color
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.scenes.BaseScene
import com.reco1l.rimu.ui.scenes.SceneIntro
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.toolkt.android.fontColor
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.toolkt.graphics.Anchor
import org.andengine.engine.handler.IUpdateHandler
import kotlin.reflect.KClass


class DebugOverlay(ctx: MainContext) :
    ModelLayout(ctx),
    IUpdateHandler
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    override var parents: Array<KClass<out BaseScene>>? = arrayOf(SceneIntro::class)


    @SuppressLint("ClickableViewAccessibility")
    private val text = TextView {

        setDimensions {
            fontSize = 8
            cornerRadius = 4f
            marginLeft = 2
            marginBottom = 2
            padding(3)
        }

        setSkinning {
            fontColor = null
        }

        fontColor = WHITE
        background = ColorDrawable(BLACK).apply { alpha = 160 }

        setConstraints(
            leftToTarget = Anchor.LEFT,
            bottomToTarget = Anchor.BOTTOM
        )

        var iX = 0f
        var iY = 0f

        setOnTouchListener { view, event ->

            when (event.action)
            {
                MotionEvent.ACTION_DOWN -> {
                    iX = event.x
                    iY = event.y
                }

                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX - iX
                    view.y = event.rawY - iY
                }
            }
            true
        }
    }

    private val sections = mutableMapOf<String, String>()

    private val sectionsToRemove = mutableListOf<String>()


    private var needsUpdate = true


    init
    {
        ctx.engine.registerUpdateHandler(this)
        z = 5f
    }


    fun setSection(name: String, value: String)
    {
        sections[name] = value
        needsUpdate = true
    }

    fun removeSection(name: String)
    {
        if (sectionsToRemove.add(name))
        {
            needsUpdate = true
        }

    }

    override fun onUpdate(sElapsed: Float)
    {
        if (!needsUpdate)
            return

        needsUpdate = false

        if (sections.isEmpty())
            return

        SpannableStringBuilder().apply {

            sections.forEach { (key, section) ->

                if (isNotEmpty())
                    appendLine()

                append('[').append(key).append(']')
                appendLine()
                color(0xFFC7DCFF.toInt()) { append(section) }
            }

            mainThread {

                text.setText(this, SPANNABLE)
            }
        }


        while (sectionsToRemove.isNotEmpty())
        {
            sections.remove(sectionsToRemove.removeAt(0))
        }

    }

}