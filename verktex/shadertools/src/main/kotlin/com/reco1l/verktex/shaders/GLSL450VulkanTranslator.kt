package com.reco1l.verktex.shaders

/**
 * Translates GLSL ES 3.00 shaders, the language the engine's shaders are written in, to GLSL 4.50
 * for Vulkan (`GL_KHR_vulkan_glsl`), to be compiled to SPIR-V with glslc. Every stage translates on
 * its own, so a vertex shader shared by several programs compiles once:
 *
 * - `#version 300 es` becomes `#version 450`.
 * - Vertex inputs keep their `layout(location = N)`, which they must have.
 * - Vertex outputs and fragment inputs get a location in declaration order, so the stages of a
 *   program must declare them in the same order, see [checkInterface]. Fragment outputs too.
 * - The uniforms that aren't samplers become the members of one std140 block per stage in set
 *   [UNIFORMS_SET], the vertex one at [VERTEX_UNIFORMS_BINDING] and the fragment one at
 *   [FRAGMENT_UNIFORMS_BINDING], in declaration order. Their offsets are in [Result.uniforms], so
 *   values can be packed by name.
 * - Samplers become combined image samplers in set [SAMPLERS_SET] from binding 0 on, fragment
 *   stage only. The sets are apart so textures can have descriptor sets of their own.
 *   A `samplerExternalOES` becomes a `sampler2D` (a Vulkan backend samples video frames through a
 *   YCbCr conversion instead) and is reported as [Sampler.isExternal].
 *
 * Anything outside this subset (arrays, several uniforms in one declaration,
 * other sampler types, ...) is rejected rather than translated wrongly.
 */
object GLSL450VulkanTranslator {

    const val UNIFORMS_SET = 0
    const val VERTEX_UNIFORMS_BINDING = 0
    const val FRAGMENT_UNIFORMS_BINDING = 1

    const val SAMPLERS_SET = 1

    enum class Stage(val fileExtension: String, val blockName: String, val uniformsBinding: Int) {
        Vertex("vert", "VertexUniforms", VERTEX_UNIFORMS_BINDING),
        Fragment("frag", "FragmentUniforms", FRAGMENT_UNIFORMS_BINDING);

        companion object {
            /**
             * The stage of a shader file by its extension, null if it's not a shader.
             */
            fun ofFile(name: String) = entries.find { name.endsWith(".${it.fileExtension}") }
        }
    }

    /**
     * The types a uniform block member can have, with their std140 size and alignment in bytes.
     */
    enum class UniformType(val glsl: String, val size: Int, val alignment: Int) {
        Float("float", 4, 4),
        Int("int", 4, 4),
        Vec2("vec2", 8, 8),
        Vec3("vec3", 12, 16),
        Vec4("vec4", 16, 16),
        Mat4("mat4", 64, 16);

        companion object {
            fun of(glsl: String) = entries.find { it.glsl == glsl }
        }
    }

    /**
     * A member of a stage's uniform block, [offset] bytes from its start.
     */
    data class Uniform(val name: String, val type: UniformType, val offset: Int)

    data class Sampler(val name: String, val binding: Int, val isExternal: Boolean)

    /**
     * A stage input or output. [interpolation] is `flat`, `smooth`, `noperspective` or empty.
     */
    data class Variable(val name: String, val type: String, val location: Int, val interpolation: String = "")

    data class Result(
        val stage: Stage,

        /**
         * The GLSL 4.50 source.
         */
        val source: String,

        /**
         * The members of the stage's uniform block, at binding [Stage.uniformsBinding].
         */
        val uniforms: List<Uniform>,

        /**
         * The size in bytes of the uniform block, a multiple of 16. 0 when the stage has no uniforms,
         * in which case it declares no block.
         */
        val uniformBlockSize: Int,

        val samplers: List<Sampler>,

        /**
         * The vertex attributes, or the fragment stage's varyings.
         */
        val inputs: List<Variable>,

        /**
         * The vertex stage's varyings, or the fragment outputs.
         */
        val outputs: List<Variable>,
    )

    class TranslationException(message: String) : IllegalArgumentException(message)


    private val VERSION = Regex("""^[ \t]*#version\s+300\s+es[ \t]*\r?$""", RegexOption.MULTILINE)
    private val EXTERNAL_IMAGE_EXTENSION = Regex("""^[ \t]*#extension\s+GL_OES_EGL_image_external(?:_essl3)?\s*:\s*\w+[ \t]*\r?\n?""", RegexOption.MULTILINE)

    private val IN_OUT = Regex(
        """^([ \t]*)(?:layout\s*\(\s*location\s*=\s*(\d+)\s*\)\s*)?(?:(flat|smooth|noperspective)\s+)?(in|out)\s+(?:(lowp|mediump|highp)\s+)?(\w+)\s+(\w+)\s*(\[[^\]]*])?\s*;""",
        RegexOption.MULTILINE
    )

    private val UNIFORM = Regex(
        """^([ \t]*)uniform\s+(?:(lowp|mediump|highp)\s+)?(\w+)\s+(\w+)\s*(\[[^\]]*])?\s*;""",
        RegexOption.MULTILINE
    )
    private val ANY_UNIFORM = Regex("""^[ \t]*uniform\b[^\n]*""", RegexOption.MULTILINE)

    private val SAMPLER_TYPES = setOf("sampler2D", "samplerExternalOES")


    fun translate(source: String, stage: Stage): Result {
        if (!VERSION.containsMatchIn(source)) {
            throw TranslationException("Only GLSL ES 3.00 sources (#version 300 es) can be translated.")
        }

        var body = VERSION.replaceFirst(source, "#version 450")
        body = EXTERNAL_IMAGE_EXTENSION.replace(body, "")

        val inputs = mutableListOf<Variable>()
        val outputs = mutableListOf<Variable>()

        body = IN_OUT.replace(body) { match ->
            val (indent, explicitLocation, interpolation, direction, precision, type, name, array) = match.destructured

            if (array.isNotEmpty()) {
                throw TranslationException("Array inputs and outputs aren't supported: $name")
            }

            val isInput = direction == "in"
            val list = if (isInput) inputs else outputs

            val location = when {
                explicitLocation.isNotEmpty() -> explicitLocation.toInt()
                stage == Stage.Vertex && isInput -> throw TranslationException("The vertex input $name needs a layout(location = N).")
                else -> (list.maxOfOrNull { it.location } ?: -1) + 1
            }

            if (list.any { it.location == location }) {
                throw TranslationException("Two ${if (isInput) "inputs" else "outputs"} share location $location: $name")
            }
            list += Variable(name, type, location, interpolation)

            buildString {
                append(indent).append("layout(location = ").append(location).append(") ")
                if (interpolation.isNotEmpty()) append(interpolation).append(' ')
                append(direction).append(' ')
                if (precision.isNotEmpty()) append(precision).append(' ')
                append(type).append(' ').append(name).append(';')
            }
        }

        val uniforms = mutableListOf<Uniform>()
        val samplers = mutableListOf<Sampler>()
        var blockOffset = 0
        var blockLine = -1

        body = UNIFORM.replace(body) { match ->
            val (indent, precision, type, name, array) = match.destructured

            if (array.isNotEmpty()) {
                throw TranslationException("Uniform arrays aren't supported: $name")
            }

            if (type in SAMPLER_TYPES) {
                if (stage != Stage.Fragment) {
                    throw TranslationException("Samplers are only supported in fragment shaders: $name")
                }

                val binding = samplers.size
                samplers += Sampler(name, binding, isExternal = type == "samplerExternalOES")

                return@replace buildString {
                    append(indent).append("layout(set = ").append(SAMPLERS_SET).append(", binding = ").append(binding).append(") uniform ")
                    if (precision.isNotEmpty()) append(precision).append(' ')
                    append("sampler2D ").append(name).append(';')
                }
            }

            val uniformType = UniformType.of(type) ?: throw TranslationException(
                if (type.startsWith("sampler")) "Unsupported sampler type $type: $name"
                else "Unsupported uniform type $type: $name"
            )

            val offset = alignUp(blockOffset, uniformType.alignment)
            uniforms += Uniform(name, uniformType, offset)
            blockOffset = offset + uniformType.size

            // The block replaces the first declaration, the others are left as empty lines. Moving a
            // declaration earlier is always valid, they are all at global scope.
            if (blockLine == -1) {
                blockLine = match.range.first
                BLOCK_PLACEHOLDER
            } else {
                ""
            }
        }

        // Anything still declaring a uniform wasn't understood, e.g. "uniform float a, b;" or a block.
        ANY_UNIFORM.find(body)?.let {
            throw TranslationException("Unsupported uniform declaration: ${it.value.trim()}")
        }

        val blockSize = if (uniforms.isEmpty()) 0 else alignUp(blockOffset, 16)

        if (uniforms.isNotEmpty()) {
            val block = buildString {
                append("layout(std140, set = ").append(UNIFORMS_SET).append(", binding = ").append(stage.uniformsBinding).append(") uniform ")
                append(stage.blockName).append(" {\n")
                for (uniform in uniforms) {
                    append("    ").append(uniform.type.glsl).append(' ').append(uniform.name).append(";\n")
                }
                append("};")
            }
            body = body.replace(BLOCK_PLACEHOLDER, block)
        }

        return Result(stage, body, uniforms, blockSize, samplers, inputs, outputs)
    }

    /**
     * Checks that the varyings of [fragment] are outputs of [vertex] with the same type, location and
     * interpolation, as the stages are linked by location only.
     *
     * @throws TranslationException describing the first mismatch.
     */
    fun checkInterface(vertex: Result, fragment: Result) {
        require(vertex.stage == Stage.Vertex && fragment.stage == Stage.Fragment)

        for (input in fragment.inputs) {
            val output = vertex.outputs.find { it.name == input.name }
                ?: throw TranslationException("The fragment input ${input.name} isn't a vertex output.")

            if (output.type != input.type || output.location != input.location || output.interpolation != input.interpolation) {
                throw TranslationException(
                    "The fragment input ${input.name} (${describe(input)}) doesn't match the vertex output (${describe(output)}). " +
                        "Declare the varyings in the same order and with the same qualifiers in both stages."
                )
            }
        }
    }

    private fun describe(variable: Variable) =
        "${variable.interpolation.ifEmpty { "smooth" }} ${variable.type} at location ${variable.location}"

    private fun alignUp(value: Int, alignment: Int) = (value + alignment - 1) / alignment * alignment

    // A marker without GLSL meaning, replaced by the uniform block once all the members are known.
    private const val BLOCK_PLACEHOLDER = "\u0000UNIFORM_BLOCK\u0000"
}
