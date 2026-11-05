package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.CompareFunction
import org.lwjgl.opengl.GL33.*

fun CompareFunction.toGL(): Int = when (this) {
    CompareFunction.Never -> GL_NEVER
    CompareFunction.Less -> GL_LESS
    CompareFunction.LessEqual -> GL_LEQUAL
    CompareFunction.Equal -> GL_EQUAL
    CompareFunction.Greater -> GL_GREATER
    CompareFunction.GreaterEqual -> GL_GEQUAL
    CompareFunction.NotEqual -> GL_NOTEQUAL
    CompareFunction.Always -> GL_ALWAYS
}

fun BlendFactor.toGL(): Int = when (this) {
    BlendFactor.Zero -> GL_ZERO
    BlendFactor.One -> GL_ONE
    BlendFactor.SrcAlpha -> GL_SRC_ALPHA
    BlendFactor.OneMinusSrcAlpha -> GL_ONE_MINUS_SRC_ALPHA
    BlendFactor.DstAlpha -> GL_DST_ALPHA
    BlendFactor.OneMinusDstAlpha -> GL_ONE_MINUS_DST_ALPHA
    BlendFactor.DstColor -> GL_DST_COLOR
    BlendFactor.SrcColor -> GL_SRC_COLOR
    BlendFactor.OneMinusSrcColor -> GL_ONE_MINUS_SRC_COLOR
    BlendFactor.OneMinusDstColor -> GL_ONE_MINUS_DST_COLOR
}
