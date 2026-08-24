package com.reco1l.verktex.android.gles

import android.os.Build
import android.view.RoundedCorner
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsWindow
import kotlin.math.max
import android.content.Context as AndroidContext


class ESWindow(androidContext: AndroidContext) : SurfaceView(androidContext), SurfaceHolder.Callback, GraphicsWindow {

    override var width: Int = 0
    override var height: Int = 0
    override var insets = GraphicsWindow.Insets(
        safeArea = Vec4.Zero,
        offsets = Vec2.Zero
    )

    private var surfaceDestroyed = false
    private var surfaceCreated = false


    init {
        holder.addCallback(this)

        setOnApplyWindowInsetsListener { view, viewInsets ->

            val compatInsets = ViewCompat.getRootWindowInsets(view)

            var radiusTopLeft = 0
            var radiusTopRight = 0
            var radiusBottomLeft = 0
            var radiusBottomRight = 0

            // Unfortunately there's no backward compatible API to get corner radius.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val windowInsets = view.getRootWindowInsets()

                val topLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                val topRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                val bottomLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                val bottomRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

                if (topLeft != null) radiusTopLeft = topLeft.radius
                if (topRight != null) radiusTopRight = topRight.radius
                if (bottomLeft != null) radiusBottomLeft = bottomLeft.radius
                if (bottomRight != null) radiusBottomRight = bottomRight.radius
            }

            val leftRadius = max(radiusTopLeft, radiusBottomLeft)
            val rightRadius = max(radiusTopRight, radiusBottomRight)

            val bars = compatInsets?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

            val safeArea = Vec4(
                x = max(bars?.left ?: 0, leftRadius).toFloat(),
                y = 0f,
                z = max(bars?.right ?: 0, rightRadius).toFloat(),
                w = 0f
            )

            val virtualKeyboardHeight = compatInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0

            insets = GraphicsWindow.Insets(
                safeArea = safeArea,
                offsets = Vec2(0f, -virtualKeyboardHeight.toFloat())
            )

            viewInsets
        }
    }


    override fun shouldClose(): Boolean {
        return surfaceDestroyed
    }

    override fun pollEvents() {
        // no-op
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceDestroyed = false
        surfaceCreated = true
        Verktex.graphics.markContextLoss()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    ) {
        width = surfaceWidth
        height = surfaceHeight

        insets = insets.copy(
            safeArea = Vec4(
                x = 0f,
                y = 0f,
                z = width.toFloat(),
                w = height.toFloat()
            )
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceDestroyed = true
    }

}