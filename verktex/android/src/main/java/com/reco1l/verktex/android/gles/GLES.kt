package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.CompareFunction

fun CompareFunction.toGLES(): Int = when (this) {
    CompareFunction.Never -> GLES20.GL_NEVER
    CompareFunction.Less -> GLES20.GL_LESS
    CompareFunction.LessEqual -> GLES20.GL_LEQUAL
    CompareFunction.Equal -> GLES20.GL_EQUAL
    CompareFunction.Greater -> GLES20.GL_GREATER
    CompareFunction.GreaterEqual -> GLES20.GL_GEQUAL
    CompareFunction.NotEqual -> GLES20.GL_NOTEQUAL
    CompareFunction.Always -> GLES20.GL_ALWAYS
}

fun BlendFactor.toGLES(): Int = when (this) {
    BlendFactor.Zero -> GLES20.GL_ZERO
    BlendFactor.One -> GLES20.GL_ONE
    BlendFactor.SrcAlpha -> GLES20.GL_SRC_ALPHA
    BlendFactor.OneMinusSrcAlpha -> GLES20.GL_ONE_MINUS_SRC_ALPHA
    BlendFactor.DstAlpha -> GLES20.GL_DST_ALPHA
    BlendFactor.OneMinusDstAlpha -> GLES20.GL_ONE_MINUS_DST_ALPHA
    BlendFactor.DstColor -> GLES20.GL_DST_COLOR
    BlendFactor.SrcColor -> GLES20.GL_SRC_COLOR
    BlendFactor.OneMinusSrcColor -> GLES20.GL_ONE_MINUS_SRC_COLOR
    BlendFactor.OneMinusDstColor -> GLES20.GL_ONE_MINUS_DST_COLOR
}
