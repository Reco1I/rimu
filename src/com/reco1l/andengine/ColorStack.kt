package com.reco1l.andengine

import com.reco1l.framework.Color4
import java.util.Stack
import javax.microedition.khronos.opengles.GL10

object ColorStack : Stack<Color4>() {

    fun pushColor(color: Color4, inheritAncestors: Boolean = true) {

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

        super.push(Color4(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f), alpha.coerceIn(0f, 1f)))
    }

    override fun push(item: Color4?): Color4? {
        throw UnsupportedOperationException("Use pushColor(GL10, Color4) instead.")
    }


    private fun readResolve(): Any = ColorStack

}
