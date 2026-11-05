package com.reco1l.verktex.desktop

import com.reco1l.verktex.desktop.gl.GLSL330Translator
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GLSL330TranslatorTest {

    // Tests run from the module's directory.
    private val shaders = File("../core/assets/shaders").listFiles { file -> file.extension in setOf("vert", "frag") }!!.toList()

    @Test
    fun everyBuiltInShaderBecomesGLSL330() {
        assertTrue("No shaders found", shaders.size >= 10)

        for (shader in shaders) {
            val translated = GLSL330Translator.translate(shader.readText())

            assertTrue("${shader.name} doesn't start with the 3.30 version", translated.trimStart().startsWith("#version 330 core"))
            assertFalse("${shader.name} still targets GLSL ES", "300 es" in translated)
            assertFalse("${shader.name} still uses an OES extension", "GL_OES_" in translated)
            assertFalse("${shader.name} still samples an external texture", "samplerExternalOES" in translated)
        }
    }

    @Test
    fun onlyTheHeaderAndExternalSamplersChange() {
        val source = """
            #version 300 es
            #extension GL_OES_EGL_image_external_essl3 : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            out vec4 fragColor;
            void main() { fragColor = texture(uTexture, vec2(0.0)); }
        """.trimIndent()

        val expected = """
            #version 330 core
            precision mediump float;
            uniform sampler2D uTexture;
            out vec4 fragColor;
            void main() { fragColor = texture(uTexture, vec2(0.0)); }
        """.trimIndent()

        assertEquals(expected, GLSL330Translator.translate(source))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherVersions() {
        GLSL330Translator.translate("#version 100\nvoid main() {}")
    }
}
