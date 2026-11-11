package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.Logger
import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.android.AndroidWindow
import com.reco1l.verktex.graphics.DefaultUniforms
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator

/**
 * The Vulkan implementation of [GraphicsRenderer]. Each command's vertices, instances and uniform
 * blocks are written into the frame's ring and a draw is encoded for them, the native side records
 * and submits the whole frame at once when it's presented.
 *
 * Captures have the top-left origin of the rest of the engine, so [framebufferOrigin] is the default.
 * External (video) textures are sampled through a YCbCr conversion, their pipelines are made for it.
 * Where they aren't supported their draws are skipped, see [skippedCommands].
 */
class VKRenderer(
    override val context: VKContext,
    override val device: VKDevice,
    override val window: AndroidWindow,
) : GraphicsRenderer() {

    override var version: String = context.version

    override val backend: GraphicsBackend
        get() = GraphicsBackend.Vulkan

    /**
     * The commands of the last frame that were skipped as their features aren't supported yet.
     */
    var skippedCommands = 0
        private set

    /**
     * Called once, on the draw thread, after the first frame was presented.
     */
    var onFirstFramePresented: (() -> Unit)? = null

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
        if (command.clearDepth) {
            context.encoder.clearDepth()
            return
        }

        val captureTexture = command.captureTexture
        if (captureTexture != null) {
            capture(captureTexture)
            return
        }

        val buffer = command.buffer ?: return
        val shader = command.shader as? VKShader ?: return

        if (buffer.isEmpty()) {
            return
        }

        val texture = command.texture

        if (shader.state == GraphicsResource.State.Unloaded) {
            shader.load()
        }

        // A command can carry the texture of an earlier draw while its shader doesn't sample it
        // (setState keeps the active one), it's only bound when sampled. Without a texture, a shader
        // that samples gets a white one, e.g. UI boxes, which don't read it.
        var ycbcrSampler = 0L
        val textureHandle = when {
            texture is VKExternalTexture -> {
                // A video frame, imported when a new one arrived. Nothing is drawn until the first.
                texture.load()
                texture.latch()
                if (texture.handle == 0L) {
                    if (!texture.hasSurface) skippedCommands++
                    return
                }
                ycbcrSampler = texture.ycbcrSampler
                texture.handle
            }
            shader.usesSamplers && texture is VKTexture -> {
                // Loads the texture and stages what was added to it since the last frame.
                texture.flushUploads()
                texture.handle
            }
            else -> 0L
        }

        val ring = context.ring
        // Only the drawn vertices are copied, a range draw then starts at the ring offset.
        val vertexCount = command.drawnVertexCount()
        val strideWords = command.vertexLayout.stride / 4
        val vertexOffset = if (command.vertexCount >= 0) {
            ring.writeWords(buffer.words, command.firstVertex * strideWords, vertexCount * strideWords)
        } else {
            ring.writeWords(buffer.words, buffer.wordCount)
        }

        val instanceBuffer = command.instanceBuffer
        val instanceOffset = if (instanceBuffer != null) ring.writeWords(instanceBuffer.words, instanceBuffer.wordCount) else 0

        val vertexUniforms = writeUniforms(shader.vertexStage, command)
        val fragmentUniforms = writeUniforms(shader.fragmentStage, command)

        if (vertexOffset < 0 || instanceOffset < 0 || vertexUniforms < 0 || fragmentUniforms < 0) {
            Logger.w("VKRenderer", "The frame ring is full, the rest of the frame isn't drawn.")
            skippedCommands++
            return
        }

        val pipeline = pipeline(VKPipelineKey(shader, command.vertexLayout, command.instanceLayout, command.blendMode, command.depthState, ycbcrSampler))
        if (pipeline == 0L) {
            skippedCommands++
            return
        }

        val scissor = command.scissor

        context.encoder.draw(
            pipeline = pipeline,
            scissorX = scissor?.x?.toInt() ?: 0,
            scissorY = scissor?.y?.toInt() ?: 0,
            scissorWidth = scissor?.z?.toInt() ?: window.windowWidth,
            scissorHeight = scissor?.w?.toInt() ?: window.windowHeight,
            vertexOffset = vertexOffset,
            count = if (command.isQuads) vertexCount / 4 else vertexCount,
            vertexUniformsOffset = vertexUniforms,
            fragmentUniformsOffset = fragmentUniforms,
            texture = textureHandle,
            instanceOffset = instanceOffset,
            instanceCount = if (instanceBuffer != null) command.instanceCount else 0,
            indexed = command.isQuads,
        )
    }

    private fun capture(texture: GraphicsTexture) {
        if (texture !is VKTexture) {
            skippedCommands++
            return
        }

        // Creates the texture if it's new, it's cleared before the frame draws.
        texture.flushUploads()
        context.encoder.capture(texture.handle)
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
                VKPipelineState.blendFactors(key.blendMode),
                VKPipelineState.depth(key.depthState),
                key.ycbcrSampler
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

        // Recording the command buffer happens here instead of while executing the commands as on
        // OpenGL ES, it's counted in the submit time so both backends measure the same work. Presenting
        // isn't, it waits for the display as eglSwapBuffers does.
        val submitStartNanos = System.nanoTime()
        context.submit()
        PerformanceStats.submitTime += (System.nanoTime() - submitStartNanos) / 1_000_000f
        PerformanceStats.onFrameDrawn()

        context.present()

        onFirstFramePresented?.let {
            onFirstFramePresented = null
            it()
        }

        PerformanceStats.gpuTime = VKNative.gpuTime(context.handle)
    }
}
