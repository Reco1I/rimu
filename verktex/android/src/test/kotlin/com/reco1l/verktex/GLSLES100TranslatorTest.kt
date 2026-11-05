package com.reco1l.verktex

import com.reco1l.verktex.android.gles.es2.ES2CommandExecutor
import com.reco1l.verktex.android.gles.es2.GLSLES100Translator
import com.reco1l.verktex.android.gles.es2.GLSLES100Translator.Stage
import com.reco1l.verktex.graphics.VertexLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The OpenGL ES 2.0 backend translates the engine's GLSL ES 3.00 shaders to GLSL ES 1.00.
 */
class GLSLES100TranslatorTest {

    private val shaderDirectory = File("src/main/assets/shaders")

    private fun translate(name: String): GLSLES100Translator.Result {
        val stage = if (name.endsWith(".vert")) Stage.Vertex else Stage.Fragment
        return GLSLES100Translator.translate(File(shaderDirectory, name).readText(), stage)
    }

    private fun code(source: String) = source.lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")

    @Test
    fun everyBuiltInShaderIsTranslated() {
        val shaders = shaderDirectory.listFiles { file -> file.extension == "vert" || file.extension == "frag" }!!
        assertTrue("No shaders found in ${shaderDirectory.absolutePath}", shaders.isNotEmpty())

        for (file in shaders) {
            val source = code(translate(file.name).source)
            val name = file.name

            assertTrue(name, source.startsWith("#version 100\n"))
            assertFalse(name, source.contains("#version 300"))
            assertFalse(name, source.contains("layout("))
            assertFalse(name, Regex("""^\s*(flat\s+)?(in|out)\s""", RegexOption.MULTILINE).containsMatchIn(source))
            assertFalse(name, Regex("""\bflat\b""").containsMatchIn(source))
            assertFalse(name, Regex("""\btexture\s*\(""").containsMatchIn(source))
            assertFalse(name, source.contains("fragColor"))
            assertFalse(name, source.contains("_essl3"))
            assertTrue(name, source.contains("varying "))

            if (file.extension == "vert") {
                assertTrue(name, source.contains("attribute "))
            } else {
                assertTrue(name, source.contains("gl_FragColor"))
                assertTrue(name, Regex("""precision \w+ float;""").containsMatchIn(source))
            }
        }
    }

    @Test
    fun attributeLocationsAreReported() {
        assertEquals(mapOf("aPosition" to 0, "aUV" to 1, "aColor" to 2), translate("main.vert").attributeLocations)

        val particle = translate("particle.vert").attributeLocations
        assertEquals(9, particle.size)
        assertEquals(0, particle["aCorner"])
        assertEquals(8, particle["aColor"])

        assertEquals(7, translate("ui.vert").attributeLocations["aMode"])
    }

    @Test
    fun derivativesEnableTheirExtension() {
        for (name in listOf("ui.frag", "quad.frag")) {
            assertTrue(name, translate(name).source.contains("#extension GL_OES_standard_derivatives : enable"))
        }
        assertFalse(translate("texture.frag").source.contains("GL_OES_standard_derivatives"))
    }

    @Test
    fun externalTexturesUseTheEs2Extension() {
        val source = translate("video.frag").source

        assertTrue(source.contains("#extension GL_OES_EGL_image_external : require"))
        assertTrue(source.contains("uniform samplerExternalOES uTexture;"))
        assertTrue(source.contains("texture2D(uTexture"))
    }

    @Test
    fun highFragmentPrecisionFallsBackToMedium() {
        val source = translate("ui.frag").source

        assertTrue(source.contains("#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif"))
    }

    @Test
    fun extensionsComeRightAfterTheVersion() {
        val lines = translate("video.frag").source.lines()

        assertEquals("#version 100", lines[0])
        assertTrue(lines[1].startsWith("#extension"))
    }

    @Test
    fun nonEs3SourcesAreLeftUntouched() {
        val source = "attribute vec2 aPosition;\nvoid main() { gl_Position = vec4(aPosition, 0.0, 1.0); }"
        assertEquals(source, GLSLES100Translator.translate(source, Stage.Vertex).source)
    }

    @Test
    fun instanceAttributesFollowTheVertexOnes() {
        val vertex = VertexLayout(listOf(VertexLayout.Attribute(0, 2)))
        val instance = VertexLayout(listOf(
            VertexLayout.Attribute(1, 2, isPerInstance = true),
            VertexLayout.Attribute(2, 4, VertexLayout.Type.NormalizedByte, isPerInstance = true)
        ))

        val combined = ES2CommandExecutor.combineLayouts(vertex, instance)

        assertEquals(vertex.stride + instance.stride, combined.stride)
        assertEquals(listOf(0, 8, 16), combined.offsets.toList())
        assertTrue(combined.attributes.none { it.isPerInstance })
    }
}
