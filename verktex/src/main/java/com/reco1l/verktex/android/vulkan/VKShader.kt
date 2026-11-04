package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Stage

/**
 * A shader made of the SPIR-V the build compiled from its GLSL ES sources. Their uniform layouts come
 * from translating the GLSL again with [GLSL450VulkanTranslator], which is what the SPIR-V was
 * compiled from, so they always match.
 *
 * Uniforms are packed per draw by the renderer, see [VKUniformPacker].
 */
class VKShader internal constructor(
    override val source: Source,
    private val context: VKContext,
) : GraphicsShader() {

    private val spirvVertex = source.spirvVertex ?: throw IllegalArgumentException(MISSING_SPIRV)

    private val spirvFragment = source.spirvFragment ?: throw IllegalArgumentException(MISSING_SPIRV)

    val vertexStage = GLSL450VulkanTranslator.translate(source.vertex, Stage.Vertex)
    val fragmentStage = GLSL450VulkanTranslator.translate(source.fragment, Stage.Fragment)

    init {
        GLSL450VulkanTranslator.checkInterface(vertexStage, fragmentStage)

        val range = context.uniformRange
        require(vertexStage.uniformBlockSize <= range && fragmentStage.uniformBlockSize <= range) {
            "A uniform block is larger than the $range bytes a draw can use."
        }
    }

    /**
     * Whether the fragment stage samples a texture, which needs a texture descriptor bound.
     */
    val usesSamplers: Boolean
        get() = fragmentStage.samplers.isNotEmpty()

    var vertexModule = 0L
        private set

    var fragmentModule = 0L
        private set


    // Uniforms are packed into the frame's ring for each draw instead.
    override fun applyUniforms(uniformSet: UniformSet) = Unit

    override fun onBind() = Unit

    override fun onLoad() {
        vertexModule = VKNative.createShaderModule(context.handle, spirvVertex)
        fragmentModule = VKNative.createShaderModule(context.handle, spirvFragment)

        check(vertexModule != 0L && fragmentModule != 0L) { "Couldn't create the shader modules." }
    }

    override fun onUnload() {
        if (vertexModule != 0L) VKNative.destroyShaderModule(context.handle, vertexModule)
        if (fragmentModule != 0L) VKNative.destroyShaderModule(context.handle, fragmentModule)
        vertexModule = 0L
        fragmentModule = 0L
    }

    private companion object {
        const val MISSING_SPIRV = "The shader has no SPIR-V, Vulkan can't compile GLSL at runtime. Was it built without the NDK?"
    }
}
