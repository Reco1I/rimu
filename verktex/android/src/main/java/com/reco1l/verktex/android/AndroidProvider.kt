package com.reco1l.verktex.android

import android.graphics.BitmapFactory
import android.graphics.Typeface
import com.reco1l.verktex.Provider
import com.reco1l.verktex.android.bass.BASSAudioSampleFileSource
import com.reco1l.verktex.android.bass.BASSAudioStreamFileSource
import com.reco1l.verktex.android.bass.BASSAudioStreamUrlSource
import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

class AndroidProvider(private val context: android.content.Context) : Provider {

    private fun openFile(path: String, isExternal: Boolean): java.io.InputStream? {
        return try {
            if (isExternal) {
                val file = java.io.File(path)
                if (!file.exists()) {
                    return null
                }
                file.inputStream()
            } else {
                context.assets.open(path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    //region Files

    override fun exists(path: String, isExternal: Boolean): Boolean {
        return try {
            if (isExternal) {
                val file = java.io.File(path)
                return file.exists()
            }
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    //endregion

    //region Source creation

    override fun createFontFamilySource(path: String, isExternal: Boolean): FontFamily.Source? {
        return try {
            val typeface = if (isExternal) {
                Typeface.createFromFile(path)
            } else {
                Typeface.createFromAsset(context.assets, path)
            }

            AndroidFontSource(typeface)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun createSystemFontFamilySource(family: String?): FontFamily.Source? {
        // Typeface.create returns the default font for a family it doesn't know.
        val typeface = if (family == null) Typeface.DEFAULT else Typeface.create(family, Typeface.NORMAL)
        return AndroidFontSource(typeface)
    }

    override fun createTextureSource(path: String, isExternal: Boolean): GraphicsTexture.Source? {
        return try {
            if (isExternal) {
                val file = java.io.File(path)
                if (!file.exists()) {
                    return null
                }
                return BitmapFactory.decodeFile(path)?.let { bitmap ->
                    AndroidTextureSource(bitmap)
                }
            }
            return context.assets.open(path).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                    AndroidTextureSource(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun createShaderSource(
        vertexShaderPath: String,
        fragmentShaderPath: String,
        isExternal: Boolean
    ): GraphicsShader.Source? {
        return try {
            val vertexShader = openFile(vertexShaderPath, isExternal)?.bufferedReader().use { it?.readText() }
            val fragmentShader = openFile(fragmentShaderPath, isExternal)?.bufferedReader().use { it?.readText() }

            if (vertexShader != null && fragmentShader != null) {
                GraphicsShader.Source(
                    vertexShader,
                    fragmentShader,
                    spirvVertex = readSpirv(vertexShaderPath, isExternal),
                    spirvFragment = readSpirv(fragmentShaderPath, isExternal)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // The SPIR-V the build compiled next to a shader, see GraphicsShader.Source.
    private fun readSpirv(shaderPath: String, isExternal: Boolean): ByteArray? {
        val path = "$shaderPath.spv"
        if (!exists(path, isExternal)) return null
        return openFile(path, isExternal)?.use { it.readBytes() }
    }

    override fun createAudioSampleSource(path: String, isExternal: Boolean): AudioSample.Source {
        return BASSAudioSampleFileSource(context, path, isExternal)
    }

    override fun createAudioStreamSource(path: String, isExternal: Boolean): AudioStream.Source {
        return BASSAudioStreamFileSource(context, path, isExternal)
    }

    override fun createAudioUrlStreamSource(url: String): AudioStream.Source {
        return BASSAudioStreamUrlSource(url)
    }

    //endregion

}