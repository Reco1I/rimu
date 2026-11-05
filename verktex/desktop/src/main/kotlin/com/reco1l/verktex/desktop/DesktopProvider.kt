package com.reco1l.verktex.desktop

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Provider
import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.desktop.bass.BASSAudioSampleSource
import com.reco1l.verktex.desktop.bass.BASSAudioStreamSource
import com.reco1l.verktex.desktop.bass.BASSAudioStreamUrlSource
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import java.io.File
import java.io.InputStream

/**
 * Loads internal resources from the classpath (the engine's assets and the application's resources)
 * and external ones from the file system.
 */
class DesktopProvider : Provider {

    private fun open(path: String, isExternal: Boolean): InputStream? {
        if (isExternal) {
            val file = File(path)
            return if (file.isFile) file.inputStream() else null
        }
        return DesktopProvider::class.java.getResourceAsStream("/" + path.removePrefix("/"))
    }

    /**
     * The bytes of the resource at [path], or null when there's none.
     */
    fun read(path: String, isExternal: Boolean): ByteArray? = try {
        open(path, isExternal)?.use { it.readBytes() }
    } catch (e: Exception) {
        Logger.e("DesktopProvider", "Couldn't read $path", e)
        null
    }


    override fun exists(path: String, isExternal: Boolean): Boolean {
        if (isExternal) {
            return File(path).exists()
        }
        return DesktopProvider::class.java.getResource("/" + path.removePrefix("/")) != null
    }

    override fun createFontFamilySource(path: String, isExternal: Boolean): FontFamily.Source? {
        val data = read(path, isExternal) ?: return null
        return try {
            DesktopFontSource(data)
        } catch (e: Exception) {
            Logger.e("DesktopProvider", "Couldn't load the font $path", e)
            null
        }
    }

    override fun createSystemFontFamilySource(family: String?): FontFamily.Source? {
        return try {
            DesktopSystemFontSource.create(family)
        } catch (e: Exception) {
            Logger.e("DesktopProvider", "Couldn't load the system font ${family ?: "default"}", e)
            null
        }
    }

    override fun createTextureSource(path: String, isExternal: Boolean): GraphicsTexture.Source? {
        val data = read(path, isExternal) ?: return null
        return PixelsTextureSource.decode(data).also {
            if (it == null) Logger.w("DesktopProvider", "Couldn't decode the image $path")
        }
    }

    override fun createShaderSource(vertexShaderPath: String, fragmentShaderPath: String, isExternal: Boolean): GraphicsShader.Source? {
        val vertex = read(vertexShaderPath, isExternal)?.decodeToString() ?: return null
        val fragment = read(fragmentShaderPath, isExternal)?.decodeToString() ?: return null

        // No SPIR-V, desktop has no Vulkan backend.
        return GraphicsShader.Source(vertex, fragment)
    }

    override fun createAudioSampleSource(path: String, isExternal: Boolean): AudioSample.Source {
        return BASSAudioSampleSource(this, path, isExternal)
    }

    override fun createAudioStreamSource(path: String, isExternal: Boolean): AudioStream.Source {
        return BASSAudioStreamSource(this, path, isExternal)
    }

    override fun createAudioUrlStreamSource(url: String): AudioStream.Source {
        return BASSAudioStreamUrlSource(url)
    }
}
