package com.reco1l.verktex

import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * An abstract class that provides a way to create resources from a given path. The implementation
 * of this class is platform-specific.
 */
interface Provider {

    /**
     * Checks if a file exists at the given path.
     *
     * @param path The path to the file.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     * how the path is resolved. As an example, in Android, internal paths are resolved relative to
     * the assets folder, while external paths are resolved relative to the file system.
     */
    fun exists(path: String, isExternal: Boolean): Boolean

    /**
     * Creates a FontFamilySource from the given path.
     *
     * @param path The path to the font family source.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     * how the path is resolved. As specified in the [exists] method.
     */
    fun createFontFamilySource(path: String, isExternal: Boolean): FontFamily.Source?

    /**
     * Creates a TextureSource from the given path.
     *
     * @param path The path to the texture source.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     * how the path is resolved. As specified in the [exists] method.
     */
    fun createTextureSource(path: String, isExternal: Boolean): GraphicsTexture.Source?

    /**
     * Creates a ShaderSource from the given fragment and vertex shader paths.
     *
     * @param vertexShaderPath The path to the vertex shader source.
     * @param fragmentShaderPath The path to the fragment shader source.
     * @param isExternal Whether the paths are external or internal, in some platforms this may affect
     * how the paths are resolved. As specified in the [exists] method.
     */
    fun createShaderSource(vertexShaderPath: String, fragmentShaderPath: String, isExternal: Boolean): GraphicsShader.Source?

    /**
     * Creates an AudioSource from the given path.
     *
     * @param path The path to the audio source.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     */
    fun createAudioSampleSource(path: String, isExternal: Boolean): AudioSample.Source?

    /**
     * Creates an AudioSource from the given path.
     *
     * @param path The path to the audio source.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     */
    fun createAudioStreamSource(path: String, isExternal: Boolean): AudioStream.Source?
}