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

    override fun read(path: String, isExternal: Boolean): ByteArray? = try {
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

    override fun list(path: String, isExternal: Boolean): List<String>? {
        if (isExternal) {
            return File(path).list()?.toList()
        }

        val folder = path.trim('/')
        val url = DesktopProvider::class.java.getResource("/$folder") ?: return null

        return try {
            when (url.protocol) {
                "file" -> File(url.toURI()).list()?.toList()

                "jar" -> {
                    val connection = url.openConnection() as java.net.JarURLConnection
                    // A cached jar is shared with the class loader, closing it would break it.
                    connection.useCaches = false
                    val prefix = "$folder/"

                    connection.jarFile.use { jar ->
                        jar.entries().asSequence()
                            .map { it.name }
                            .filter { it.startsWith(prefix) && it.length > prefix.length }
                            .map { it.substring(prefix.length).trimEnd('/') }
                            .filter { '/' !in it }
                            .distinct()
                            .toList()
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            Logger.e("DesktopProvider", "Couldn't list $path", e)
            null
        }
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
