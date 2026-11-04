package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.Logger
import com.reco1l.verktex.android.AndroidWindow
import com.reco1l.verktex.graphics.DefaultUniforms
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator

/**
 * The Vulkan implementation of [GraphicsRenderer]. Each command's vertices and uniform blocks are
 * written into the frame's ring and a draw is encoded for them, the native side records and submits
 * the whole frame at once when it's presented.
 *
 * Phase 4 of the roadmap (bring-up) draws plain triangles only: commands whose shader samples a
 * texture, or needing quad indices, instancing, depth, a depth clear or a capture are skipped, see [skippedCommands].
 */
class VKRenderer(
    override val context: VKContext,
    override val device: VKDevice,
    override val window: AndroidWindow,
) : GraphicsRenderer() {

    override var version: String = context.version

    /**
     * The commands of the last frame that were skipped as their features aren't supported yet.
     */
    var skippedCommands = 0
        private set

    private val defaultUniforms = DefaultUniforms()
    private val pipelines = HashMap<VKPipelineKey, Long>()
    private var skipsLogged = false


    init {
        context.onRenderPassRecreated = ::destroyPipelines
        createBuiltInShaders()
    }


    override fun prepareFrame(): Boolean = context.acquireFrame()

    override fun begin() {
        super.begin()
        skippedCommands = 0
    }

    override fun drawCommand(command: DrawCommand) {
        val buffer = command.buffer
        val shader = command.shader as? VKShader

        if (command.clearDepth || command.captureTexture != null) {
            skippedCommands++
            return
        }

        if (buffer == null || shader == null || buffer.isEmpty()) {
            return
        }

        // A command can carry the texture of an earlier draw while its shader doesn't sample it (setState
        // keeps the active one), only shaders that sample need it.
        if (command.isQuads || command.instanceBuffer != null || shader.usesSamplers || command.depthState.enabled) {
            skippedCommands++
            return
        }

        if (shader.state == GraphicsResource.State.Unloaded) {
            shader.load()
        }

        val ring = context.ring
        val vertexOffset = ring.writeWords(buffer.words, buffer.wordCount)
        val vertexUniforms = writeUniforms(shader.vertexStage, command)
        val fragmentUniforms = writeUniforms(shader.fragmentStage, command)

        if (vertexOffset < 0 || vertexUniforms < 0 || fragmentUniforms < 0) {
            Logger.w("VKRenderer", "The frame ring is full, the rest of the frame isn't drawn.")
            skippedCommands++
            return
        }

        val pipeline = pipeline(VKPipelineKey(shader, command.vertexLayout, null, command.blendMode))
        if (pipeline == 0L) {
            skippedCommands++
            return
        }

        val scissor = command.scissor
        val width = window.windowWidth
        val height = window.windowHeight

        context.encoder.draw(
            pipeline = pipeline,
            scissorX = scissor?.x?.toInt() ?: 0,
            scissorY = scissor?.y?.toInt() ?: 0,
            scissorWidth = scissor?.z?.toInt() ?: width,
            scissorHeight = scissor?.w?.toInt() ?: height,
            vertexOffset = vertexOffset,
            vertexCount = buffer.position / command.vertexLayout.stride,
            vertexUniformsOffset = vertexUniforms,
            fragmentUniformsOffset = fragmentUniforms,
        )
    }

    // Returns the offset of the stage's block in the ring, 0 for a stage without one (the descriptor
    // still needs a valid offset), or -1 when the ring is full.
    private fun writeUniforms(stage: GLSL450VulkanTranslator.Result, command: DrawCommand): Int {
        if (stage.uniformBlockSize == 0) {
            return 0
        }

        val ring = context.ring
        val offset = ring.allocate(context.uniformRange, context.uniformAlignment)
        if (offset < 0) {
            return -1
        }

        VKUniformPacker.pack(
            target = ring.buffer,
            offset = offset,
            blockSize = stage.uniformBlockSize,
            uniforms = stage.uniforms,
            values = command.uniformSet.entries,
            defaults = defaultUniforms.forViewport(window.windowWidth, window.windowHeight).entries,
        )
        return offset
    }

    private fun pipeline(key: VKPipelineKey): Long {
        return pipelines.getOrPut(key) {
            VKNative.createPipeline(
                context.handle,
                key.shader.vertexModule,
                key.shader.fragmentModule,
                VKPipelineState.vertexInput(key.vertexLayout, key.instanceLayout),
                VKPipelineState.blendFactors(key.blendMode)
            )
        }
    }

    private fun destroyPipelines() {
        for (pipeline in pipelines.values) {
            if (pipeline != 0L) VKNative.destroyPipeline(context.handle, pipeline)
        }
        pipelines.clear()
    }

    override fun end() {
        super.end()

        if (skippedCommands > 0 && !skipsLogged) {
            skipsLogged = true
            Logger.i("VKRenderer", "$skippedCommands draw commands were skipped, their features aren't supported by the Vulkan backend yet.")
        }

        context.present()
    }
}
