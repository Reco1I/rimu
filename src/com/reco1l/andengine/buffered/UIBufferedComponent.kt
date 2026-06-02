package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.component.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.GL10

abstract class UIBufferedComponent : UIComponent() {

    /**
     * The blend information of the entity.
     */
    var blendInfo = BlendInfo.Mixture

    /**
     * The depth information of the entity.
     */
    var depthInfo = DepthInfo.None

    /**
     * The clear information of the entity.
     */
    var clearInfo = ClearInfo.None


    override fun doDraw(gl: GL10, camera: Camera) {

        UIRenderer.setState(gl,
            blendFunctionSource = blendInfo.sourceFactor,
            blendFunctionDestination = blendInfo.destinationFactor,
            depthTestingEnabled = depthInfo.test,
            depthMask = depthInfo.mask,
            depthFunction = depthInfo.function
        )

        super.doDraw(gl, camera)
    }
}
