package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.BlendFactor
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

fun BlendFactor.toGLES(): Int = when (this) {
    BlendFactor.Zero -> GLES32.GL_ZERO
    BlendFactor.One -> GLES32.GL_ONE
    BlendFactor.SrcAlpha -> GLES32.GL_SRC_ALPHA
    BlendFactor.OneMinusSrcAlpha -> GLES32.GL_ONE_MINUS_SRC_ALPHA
    BlendFactor.DstAlpha -> GLES32.GL_DST_ALPHA
    BlendFactor.OneMinusDstAlpha -> GLES32.GL_ONE_MINUS_DST_ALPHA
    BlendFactor.DstColor -> GLES32.GL_DST_COLOR
    BlendFactor.SrcColor -> GLES32.GL_SRC_COLOR
    BlendFactor.OneMinusSrcColor -> GLES32.GL_ONE_MINUS_SRC_COLOR
    BlendFactor.OneMinusDstColor -> GLES32.GL_ONE_MINUS_DST_COLOR
}
