package com.reco1l.andengine

import androidx.core.util.Pools.SimplePool
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.util.GLHelper
import java.util.Stack
import javax.microedition.khronos.opengles.GL10

object ColorStack : Stack<Color4>() {

    fun pushColor(gl: GL10, color: Color4, inheritAncestors: Boolean = true) {

        var red = color.red
        var green = color.green
        var blue = color.blue
        var alpha = color.alpha

        if (!empty()) {
            val previous = peek()
            if (inheritAncestors) {
                red *= previous.red
                green *= previous.green
                blue *= previous.blue
            }
            alpha *= previous.alpha
        }

        GLHelper.setColor(gl, red, green, blue, alpha)

        super.push(Color4(red, green, blue, alpha))
    }


    fun popColor(gl: GL10) {
        if (empty()) return

        super.pop()

        if (!empty()) {
            val previous = peek()
            GLHelper.setColor(gl, previous.red, previous.green, previous.blue, previous.alpha)
        }
    }


    override fun pop(): Color4? {
        throw UnsupportedOperationException("Use popColor(GL10) instead.")
    }

    override fun push(item: Color4?): Color4? {
        throw UnsupportedOperationException("Use pushColor(GL10, Color4) instead.")
    }

    private fun readResolve(): Any = ColorStack

}
