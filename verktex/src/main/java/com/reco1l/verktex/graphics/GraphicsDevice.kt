package com.reco1l.verktex.graphics

/**
 * An interface that represents a graphics device, which is responsible for managing the rendering
 * context and providing access to graphics resources such as buffers, shaders, and textures.
 */
interface GraphicsDevice {

    /**
     * The capabilities of the device, which describe the features and limitations of the graphics hardware.
     */
    val capabilities: Capabilities

    /**
     * Creates a new instance of a [GraphicsBuffer] object, which is used to store vertex data for rendering.
     */
    fun createBuffer(): GraphicsBuffer

    /**
     * Creates a new instance of a [GraphicsShader] object using the provided source, which contains
     * the source code for the shader program.
     */
    fun createShader(source: GraphicsShader.Source): GraphicsShader

    /**
     * Creates a new instance of a [GraphicsTexture] object with the specified width and height, which can be used
     * to store image data for rendering.
     */
    fun createTexture(width: Float, height: Float): GraphicsTexture


    /**
     * Represents the capabilities of the device's graphics hardware.
     */
    data class Capabilities(
        /**
         * The maximum texture size supported by the device's graphics hardware.
         */
        val maxTextureSize: Float
    )

}