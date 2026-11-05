package com.reco1l.verktex.desktop.gl

/**
 * Rewrites the built-in GLSL ES 3.00 shaders as GLSL 3.30 (OpenGL 3.3 core). The two languages are
 * almost the same: only the version changes, precision qualifiers are accepted and ignored. External
 * textures (OES_EGL_image_external) don't exist on desktop, so they're sampled as plain 2D textures.
 */
object GLSL330Translator {

    private val version = Regex("""^\s*#version\s+300\s+es\s*$""", RegexOption.MULTILINE)
    private val externalImageExtension = Regex("""^\s*#extension\s+GL_OES_EGL_image_external\w*\s*:\s*\w+\s*$\n?""", RegexOption.MULTILINE)
    private val externalSampler = Regex("""\bsamplerExternalOES\b""")

    fun translate(source: String): String {
        require(version.containsMatchIn(source)) { "Only GLSL ES 3.00 shaders (#version 300 es) can be translated." }

        return source
            .replace(version, "#version 330 core")
            .replace(externalImageExtension, "")
            .replace(externalSampler, "sampler2D")
    }
}
