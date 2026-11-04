package com.reco1l.verktex.shaders

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Compiles every `.vert` and `.frag` of a directory to SPIR-V for Vulkan: each is translated with
 * [GLSL450VulkanTranslator] and compiled with glslc, written as `<name>.spv` (e.g. `main.vert.spv`).
 *
 * Arguments: the shader directory, the output directory and the path to glslc. When glslc doesn't
 * exist (no NDK installed) nothing is compiled and a warning is printed instead of failing, so the
 * OpenGL ES backends still build. A shader that fails to translate or compile fails the build.
 */
fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: CompileVulkanShaders <shader dir> <output dir> <glslc>")
        exitProcess(2)
    }

    val sourceDir = File(args[0])
    val outputDir = File(args[1])
    val glslc = File(args[2])

    outputDir.deleteRecursively()
    outputDir.mkdirs()

    if (!glslc.isFile) {
        println("warning: glslc wasn't found at $glslc, the Vulkan shaders aren't compiled. Install the NDK to build them.")
        return
    }

    val errors = compileShaders(sourceDir, outputDir, glslc)
    if (errors.isNotEmpty()) {
        System.err.println(errors.joinToString("\n\n"))
        exitProcess(1)
    }
}

/**
 * Compiles the shaders of [sourceDir] into [outputDir], returning the errors of those that failed.
 */
fun compileShaders(sourceDir: File, outputDir: File, glslc: File): List<String> {
    val workDir = File(outputDir.parentFile, "${outputDir.name}-glsl450").apply {
        deleteRecursively()
        mkdirs()
    }

    val errors = mutableListOf<String>()
    val shaders = sourceDir.listFiles().orEmpty().filter { GLSL450VulkanTranslator.Stage.ofFile(it.name) != null }.sortedBy { it.name }

    for (shader in shaders) {
        val stage = GLSL450VulkanTranslator.Stage.ofFile(shader.name)!!

        val translated = try {
            GLSL450VulkanTranslator.translate(shader.readText(), stage)
        } catch (e: GLSL450VulkanTranslator.TranslationException) {
            errors += "${shader.name}: ${e.message}"
            continue
        }

        // Kept next to the output, glslc reports errors with lines of the translated source.
        val translatedFile = File(workDir, shader.name).apply { writeText(translated.source) }
        val spirv = File(outputDir, "${shader.name}.spv")

        val process = ProcessBuilder(
            glslc.path,
            "--target-env=vulkan1.0",
            "-O",
            "-fshader-stage=${stage.fileExtension}",
            "-o", spirv.path,
            translatedFile.path
        ).redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().readText()

        if (!process.waitFor(1, TimeUnit.MINUTES) || process.exitValue() != 0) {
            errors += "${shader.name}: glslc failed on the translated ${translatedFile.path}\n$output"
        }
    }

    return errors
}
