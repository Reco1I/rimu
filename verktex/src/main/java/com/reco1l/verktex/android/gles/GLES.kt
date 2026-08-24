package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.CompareFunction

fun CompareFunction.toGLES(): Int = when (this) {
    CompareFunction.Never -> GLES32.GL_NEVER
    CompareFunction.Less -> GLES32.GL_LESS
    CompareFunction.LessEqual -> GLES32.GL_LEQUAL
    CompareFunction.Equal -> GLES32.GL_EQUAL
    CompareFunction.Greater -> GLES32.GL_GREATER
    CompareFunction.GreaterEqual -> GLES32.GL_GEQUAL
    CompareFunction.NotEqual -> GLES32.GL_NOTEQUAL
    CompareFunction.Always -> GLES32.GL_ALWAYS
}

fun BlendMode.toGLESSource(): Int = when (this) {
    BlendMode.Opaque -> GLES32.GL_ONE
    BlendMode.Alpha -> GLES32.GL_SRC_ALPHA
    BlendMode.Additive -> GLES32.GL_SRC_ALPHA
    BlendMode.Multiply -> GLES32.GL_DST_COLOR
}

fun BlendMode.toGLESDestination(): Int = when (this) {
    BlendMode.Opaque -> GLES32.GL_ZERO
    BlendMode.Alpha -> GLES32.GL_ONE_MINUS_SRC_ALPHA
    BlendMode.Additive -> GLES32.GL_ONE
    BlendMode.Multiply -> GLES32.GL_ZERO
}
