package com.reco1l.verktex.android.gles.es2

/**
 * Translates GLSL ES 3.00 shaders, the language the engine's shaders are written in, to GLSL ES 1.00
 * for OpenGL ES 2.0. It covers what a shader limited to ES 2.0 features would use differently:
 *
 * - `#version 300 es` becomes `#version 100`.
 * - Vertex inputs become `attribute`s. Their `layout(location = N)` is dropped and reported in
 *   [Result.attributeLocations], to be bound with `glBindAttribLocation` before linking.
 * - Vertex outputs and fragment inputs become `varying`s, and the `flat` and `smooth` qualifiers
 *   are dropped. A flat value is interpolated instead, which only gives the same result when it
 *   is equal on every vertex of the primitive.
 * - The fragment output is replaced by `gl_FragColor`.
 * - `texture()` becomes `texture2D()`, which also samples external textures.
 * - `GL_OES_EGL_image_external_essl3` becomes `GL_OES_EGL_image_external`, and
 *   `GL_OES_standard_derivatives` is enabled when `fwidth`, `dFdx` or `dFdy` are used.
 * - A high fragment precision falls back to medium where it isn't supported.
 *
 * Sources that aren't GLSL ES 3.00 are returned unchanged.
 */
object GLSLES100Translator {

    enum class Stage { Vertex, Fragment }

    data class Result(
        val source: String,

        /**
         * The location of each vertex attribute declared with `layout(location = N)`.
         */
        val attributeLocations: Map<String, Int> = emptyMap(),
    )


    private val VERSION = Regex("""^\s*#version\s+300\s+es\s*$""", RegexOption.MULTILINE)
    private val EXTENSION = Regex("""^\s*#extension\s+(\w+)\s*:\s*(\w+)\s*$""", RegexOption.MULTILINE)

    private val LOCATED_INPUT = Regex("""^(\s*)layout\s*\(\s*location\s*=\s*(\d+)\s*\)\s*in\s+(\w+)\s+(\w+)\s*;""", RegexOption.MULTILINE)
    private val INPUT = Regex("""^(\s*)(?:(?:flat|smooth)\s+)?in\s+""", RegexOption.MULTILINE)
    private val OUTPUT = Regex("""^(\s*)(?:(?:flat|smooth)\s+)?out\s+""", RegexOption.MULTILINE)
    private val FRAGMENT_OUTPUT = Regex("""^\s*(?:layout\s*\([^)]*\)\s*)?out\s+\w+\s+(\w+)\s*;[^\n]*\n?""", RegexOption.MULTILINE)

    private val TEXTURE_CALL = Regex("""\btexture\s*\(""")
    private val DERIVATIVES = Regex("""\b(?:fwidth|dFdx|dFdy)\s*\(""")
    private val HIGHP_FLOAT = Regex("""^\s*precision\s+highp\s+float\s*;""", RegexOption.MULTILINE)
    private val ANY_FLOAT_PRECISION = Regex("""^\s*precision\s+\w+\s+float\s*;""", RegexOption.MULTILINE)


    fun translate(source: String, stage: Stage): Result {
        if (!VERSION.containsMatchIn(source)) {
            return Result(source)
        }

        var body = VERSION.replace(source, "")

        // Extensions must come before any other token, they're gathered at the top.
        val extensions = linkedMapOf<String, String>()
        body = EXTENSION.replace(body) { match ->
            val name = match.groupValues[1]
            extensions[if (name == "GL_OES_EGL_image_external_essl3") "GL_OES_EGL_image_external" else name] = match.groupValues[2]
            ""
        }

        val attributeLocations = linkedMapOf<String, Int>()

        when (stage) {
            Stage.Vertex -> {
                body = LOCATED_INPUT.replace(body) { match ->
                    val (indent, location, type, name) = match.destructured
                    attributeLocations[name] = location.toInt()
                    "${indent}attribute $type $name;"
                }
                body = INPUT.replace(body) { "${it.groupValues[1]}attribute " }
                body = OUTPUT.replace(body) { "${it.groupValues[1]}varying " }
            }

            Stage.Fragment -> {
                val outputs = FRAGMENT_OUTPUT.findAll(body).map { it.groupValues[1] }.toList()
                require(outputs.size <= 1) { "GLSL ES 1.00 has a single fragment output, found: $outputs" }

                body = FRAGMENT_OUTPUT.replace(body, "")
                outputs.firstOrNull()?.let { name ->
                    body = body.replace(Regex("""\b${Regex.escape(name)}\b"""), "gl_FragColor")
                }

                body = INPUT.replace(body) { "${it.groupValues[1]}varying " }

                if (DERIVATIVES.containsMatchIn(body)) {
                    extensions.putIfAbsent("GL_OES_standard_derivatives", "enable")
                }

                body = HIGHP_FLOAT.replace(body,
                    "\n#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif"
                )

                // Fragment shaders have no default float precision in GLSL ES 1.00.
                if (!ANY_FLOAT_PRECISION.containsMatchIn(body)) {
                    body = "precision mediump float;\n$body"
                }
            }
        }

        body = TEXTURE_CALL.replace(body, "texture2D(")

        val header = StringBuilder("#version 100\n")
        for ((name, behaviour) in extensions) {
            header.append("#extension ").append(name).append(" : ").append(behaviour).append('\n')
        }

        return Result(header.append(body.trimStart('\n')).toString(), attributeLocations)
    }
}
