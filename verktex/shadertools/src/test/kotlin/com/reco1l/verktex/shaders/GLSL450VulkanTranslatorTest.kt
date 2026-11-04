package com.reco1l.verktex.shaders

import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Sampler
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Stage
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.TranslationException
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Uniform
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.UniformType
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Variable
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.translate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GLSL450VulkanTranslatorTest {

    private val vertex = """
        #version 300 es

        layout(location = 0) in vec2 aPosition;
        layout(location = 2) in vec4 aColor;

        uniform vec2 uViewportSize;

        out vec2 vUV;
        flat out vec4 vColor;

        void main() {
            gl_Position = vec4(aPosition / uViewportSize, 0.0, 1.0);
            vUV = aPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragment = """
        #version 300 es
        precision mediump float;

        in vec2 vUV;
        flat in vec4 vColor;

        uniform sampler2D uTexture;
        uniform float uRadius;
        uniform vec2 uSize;
        uniform vec4 uColor;
        uniform mat4 uTransform;

        out vec4 fragColor;

        void main() {
            fragColor = texture(uTexture, vUV) * vColor * uColor;
        }
    """.trimIndent()

    private fun assertRejected(source: String, stage: Stage) {
        try {
            translate(source, stage)
            fail("Expected the source to be rejected")
        } catch (_: TranslationException) {
        }
    }

    @Test
    fun versionBecomes450() {
        val source = translate(vertex, Stage.Vertex).source
        assertTrue(source.startsWith("#version 450\n"))
        assertFalse(source.contains("300 es"))
    }

    @Test
    fun vertexInputsKeepTheirLocations() {
        val result = translate(vertex, Stage.Vertex)

        assertEquals(listOf(Variable("aPosition", "vec2", 0), Variable("aColor", "vec4", 2)), result.inputs)
        assertTrue(result.source.contains("layout(location = 2) in vec4 aColor;"))
    }

    @Test
    fun varyingsAreLocatedInDeclarationOrder() {
        val vertexResult = translate(vertex, Stage.Vertex)
        val fragmentResult = translate(fragment, Stage.Fragment)

        assertEquals(listOf(Variable("vUV", "vec2", 0), Variable("vColor", "vec4", 1, "flat")), vertexResult.outputs)
        assertEquals(vertexResult.outputs, fragmentResult.inputs)
        assertTrue(vertexResult.source.contains("layout(location = 1) flat out vec4 vColor;"))
        assertTrue(fragmentResult.source.contains("layout(location = 1) flat in vec4 vColor;"))

        GLSL450VulkanTranslator.checkInterface(vertexResult, fragmentResult)
    }

    @Test
    fun fragmentOutputIsAtLocation0() {
        val result = translate(fragment, Stage.Fragment)

        assertEquals(listOf(Variable("fragColor", "vec4", 0)), result.outputs)
        assertTrue(result.source.contains("layout(location = 0) out vec4 fragColor;"))
    }

    @Test
    fun uniformsFormAStd140BlockPerStage() {
        val result = translate(fragment, Stage.Fragment)

        // float at 0, vec2 aligned to 8, vec4 and mat4 aligned to 16.
        assertEquals(
            listOf(
                Uniform("uRadius", UniformType.Float, 0),
                Uniform("uSize", UniformType.Vec2, 8),
                Uniform("uColor", UniformType.Vec4, 16),
                Uniform("uTransform", UniformType.Mat4, 32),
            ),
            result.uniforms
        )
        assertEquals(96, result.uniformBlockSize)

        assertTrue(result.source.contains("layout(std140, set = 0, binding = 1) uniform FragmentUniforms {"))
        assertFalse(result.source.contains("uniform float uRadius;"))

        val vertexResult = translate(vertex, Stage.Vertex)
        assertEquals(listOf(Uniform("uViewportSize", UniformType.Vec2, 0)), vertexResult.uniforms)
        assertEquals(16, vertexResult.uniformBlockSize)
        assertTrue(vertexResult.source.contains("layout(std140, set = 0, binding = 0) uniform VertexUniforms {"))
    }

    @Test
    fun blockIsDeclaredBeforeItsFirstUse() {
        val source = translate(fragment, Stage.Fragment).source
        assertTrue(source.indexOf("uniform FragmentUniforms") < source.indexOf("void main"))
    }

    @Test
    fun vec3IsAlignedTo16() {
        val result = translate(
            "#version 300 es\nprecision mediump float;\nuniform float a;\nuniform vec3 b;\nuniform float c;\nout vec4 o;\nvoid main() { o = vec4(b, a + c); }",
            Stage.Fragment
        )

        // c fills the end of b's 16 bytes.
        assertEquals(listOf(0, 16, 28), result.uniforms.map { it.offset })
        assertEquals(32, result.uniformBlockSize)
    }

    @Test
    fun stagesWithoutUniformsDeclareNoBlock() {
        val result = translate("#version 300 es\nprecision mediump float;\nout vec4 o;\nvoid main() { o = vec4(1.0); }", Stage.Fragment)

        assertEquals(0, result.uniformBlockSize)
        assertFalse(result.source.contains("uniform"))
    }

    @Test
    fun samplersAreInASetOfTheirOwn() {
        val result = translate(fragment, Stage.Fragment)

        assertEquals(listOf(Sampler("uTexture", 0, isExternal = false)), result.samplers)
        assertTrue(result.source.contains("layout(set = 1, binding = 0) uniform sampler2D uTexture;"))
    }

    @Test
    fun externalSamplersBecomeSampler2D() {
        val result = translate(
            """
                #version 300 es
                #extension GL_OES_EGL_image_external_essl3 : require
                precision mediump float;
                in vec2 vUV;
                uniform samplerExternalOES uTexture;
                out vec4 fragColor;
                void main() { fragColor = texture(uTexture, vUV); }
            """.trimIndent(),
            Stage.Fragment
        )

        assertTrue(result.samplers.single().isExternal)
        assertTrue(result.source.contains("uniform sampler2D uTexture;"))
        assertFalse(result.source.contains("samplerExternalOES"))
        assertFalse(result.source.contains("#extension"))
    }

    @Test
    fun mismatchedVaryingsAreReported() {
        // The fragment stage declares the varyings in another order.
        val reordered = fragment.replace("in vec2 vUV;\nflat in vec4 vColor;", "flat in vec4 vColor;\nin vec2 vUV;")

        try {
            GLSL450VulkanTranslator.checkInterface(translate(vertex, Stage.Vertex), translate(reordered, Stage.Fragment))
            fail("Expected a mismatch")
        } catch (e: TranslationException) {
            assertTrue(e.message!!.contains("vColor"))
        }
    }

    @Test
    fun missingVaryingsAreReported() {
        val extra = fragment.replace("flat in vec4 vColor;", "flat in vec4 vColor;\nin float vExtra;")

        try {
            GLSL450VulkanTranslator.checkInterface(translate(vertex, Stage.Vertex), translate(extra, Stage.Fragment))
            fail("Expected a missing varying")
        } catch (e: TranslationException) {
            assertTrue(e.message!!.contains("vExtra"))
        }
    }

    @Test
    fun unsupportedSourcesAreRejected() {
        // Not GLSL ES 3.00.
        assertRejected("#version 100\nvoid main() {}", Stage.Vertex)
        // A vertex input without a location.
        assertRejected("#version 300 es\nin vec2 aPosition;\nvoid main() {}", Stage.Vertex)
        // A sampler in the vertex stage.
        assertRejected("#version 300 es\nuniform sampler2D uTexture;\nvoid main() {}", Stage.Vertex)
        // Arrays, several declarators, unknown types.
        assertRejected("#version 300 es\nuniform vec4 uColors[2];\nvoid main() {}", Stage.Vertex)
        assertRejected("#version 300 es\nuniform float a, b;\nvoid main() {}", Stage.Vertex)
        assertRejected("#version 300 es\nuniform bool uEnabled;\nvoid main() {}", Stage.Vertex)
        assertRejected("#version 300 es\nprecision mediump float;\nuniform samplerCube uSky;\nout vec4 o;\nvoid main() {}", Stage.Fragment)
    }

    @Test
    fun stageIsFoundByFileExtension() {
        assertEquals(Stage.Vertex, Stage.ofFile("main.vert"))
        assertEquals(Stage.Fragment, Stage.ofFile("ui.frag"))
        assertEquals(null, Stage.ofFile("README.md"))
    }
}
