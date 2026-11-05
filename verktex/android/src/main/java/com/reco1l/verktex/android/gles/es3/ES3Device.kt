package com.reco1l.verktex.android.gles.es3

import com.reco1l.verktex.android.gles.ESDevice
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * The [ESDevice] of OpenGL ES 3.0 and up.
 */
class ES3Device : ESDevice() {

    override fun createShader(source: GraphicsShader.Source): ES3Shader {
        return ES3Shader(source)
    }
}
