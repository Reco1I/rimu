package com.reco1l.verktex.android

import android.view.Surface
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * What producers (e.g. a video player) see of a texture made with `GraphicsRenderer.createExternalTexture`
 * on Android: a [Surface] to draw frames into. How the frames reach the GPU is up to the backend.
 */
interface AndroidExternalTexture {

    /**
     * The region covering the whole texture.
     */
    val region: GraphicsTexture.Region

    /**
     * The [Surface] producers draw into, null until the texture is loaded.
     */
    val surface: Surface?

    /**
     * Called every time [surface] is (re)created, i.e. on the first load and after a context loss,
     * so the producer can attach to the new one. It runs on the draw thread in the middle of a draw,
     * so it must stay cheap. A handler that touches the component tree must hop to the update thread first.
     */
    var onSurfaceRecreated: (() -> Unit)?

    /**
     * Marks that a new frame is waiting, the texture shows it from its next draw on.
     */
    fun markFrameAvailable()
}
