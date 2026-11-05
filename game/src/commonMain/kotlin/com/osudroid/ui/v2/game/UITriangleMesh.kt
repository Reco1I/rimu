package com.osudroid.ui.v2.game

import com.osudroid.utils.FloatArraySlice
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UICanvas
import kotlin.jvm.JvmField

/**
 * Draws a list of triangles in a single color, which is how slider bodies are drawn.
 */
class UITriangleMesh : UICanvas() {

    /**
     * The position of the vertices, relative to this mesh.
     */
    val vertices = FloatArraySlice().apply { ary = FloatArray(0) }


    override fun intersectsSceneRect(x: Float, y: Float, width: Float, height: Float) = true

    override fun onDraw(renderer: GraphicsRenderer) {
        if (vertices.length > 0) {
            renderer.drawTriangles(vertices.ary, vertices.length / 2, contentColor)
        }
    }


    companion object {

        @JvmField
        val DEPTH_LESS = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)

        @JvmField
        val DEPTH_DEFAULT = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)
    }
}
