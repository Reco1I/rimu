package com.reco1l.verktex.android.gles.es2

import android.opengl.GLES20
import com.reco1l.verktex.android.gles.ESShader

/**
 * The [ESShader] of OpenGL ES 2.0, which translates the GLSL ES 3.00 sources to GLSL ES 1.00 with
 * [GLSLES100Translator] and binds the attribute locations the sources declared.
 */
class ES2Shader(source: Source) : ESShader(source) {

    private val vertex by lazy { GLSLES100Translator.translate(source.vertex, GLSLES100Translator.Stage.Vertex) }
    private val fragment by lazy { GLSLES100Translator.translate(source.fragment, GLSLES100Translator.Stage.Fragment) }


    override fun vertexSource() = vertex.source

    override fun fragmentSource() = fragment.source

    override fun beforeLink(program: Int) {
        for ((name, location) in vertex.attributeLocations) {
            GLES20.glBindAttribLocation(program, location, name)
        }
    }
}
