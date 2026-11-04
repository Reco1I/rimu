package com.osudroid.verktex

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every built-in shader translates for Vulkan, and the stages of each program link: Vulkan matches
 * varyings by location, which the translator assigns in declaration order.
 */
class BuiltInShadersVulkanTest {

    private fun translate(path: String, stage: Stage) =
        GLSL450VulkanTranslator.translate(File("assets", path).readText(), stage)

    @Test
    fun everyProgramTranslatesAndLinks() {
        assertTrue(BuiltIn.Shaders.Programs.isNotEmpty())

        for (program in BuiltIn.Shaders.Programs) {
            val vertex = translate(program.vertexPath, Stage.Vertex)
            val fragment = translate(program.fragmentPath, Stage.Fragment)

            GLSL450VulkanTranslator.checkInterface(vertex, fragment)
        }
    }

    @Test
    fun onlyTheVideoShaderSamplesAnExternalTexture() {
        val external = BuiltIn.Shaders.Programs
            .filter { program -> translate(program.fragmentPath, Stage.Fragment).samplers.any { it.isExternal } }
            .map { it.fragmentPath }

        assertEquals(listOf("verktex/shaders/video.frag"), external)
    }
}
