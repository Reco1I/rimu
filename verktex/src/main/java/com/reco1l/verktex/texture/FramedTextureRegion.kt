package com.reco1l.verktex.texture

import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * The [FramedTextureRegion] class represents a texture region that consists of multiple frames, allowing
 * for animated textures.
 *
 * It extends [GraphicsTexture.Region] and provides functionality to manage the current frame, navigate
 * between frames, and update the region's position and size based on the selected frame.
 */
open class FramedTextureRegion(
    private val frames: List<GraphicsTexture.Region>,

    mainTexture: GraphicsTexture,
    mainSource: Source
) : GraphicsTexture.Region(mainTexture, mainSource) {

    /**
     * The index of the current frame in the [frames] list. Changing this value will update the region's
     * position and size to match the selected frame.
     */
    var frameIndex = 0
        set(value) {
            if (field == value) return

            if (value < 0 || value >= frames.size) {
                throw IndexOutOfBoundsException("Frame index $value is out of bounds for frames list of size ${frames.size}.")
            }

            field = value
            onFrameChange()
        }


    /**
     * Called whenever the [frameIndex] changes. This method updates the region's position and size to match
     * the selected frame in the [frames] list.
     */
    protected fun onFrameChange() {
        x = frames[frameIndex].x
        y = frames[frameIndex].y
        width = frames[frameIndex].width
        height = frames[frameIndex].height
    }


    /**
     * Advances to the next frame in the [frames] list. If the current frame is the last one, it wraps
     * around to the first frame.
     */
    fun nextFrame() {
        frameIndex = (frameIndex + 1) % frames.size
    }

    /**
     * Moves to the previous frame in the [frames] list. If the current frame is the first one, it wraps
     * around to the last frame.
     */
    fun previousFrame() {
        frameIndex = (frameIndex - 1 + frames.size) % frames.size
    }


    /**
     * Represents a source for a framed texture region, which consists of multiple frames. Each frame
     * is represented by a [GraphicsTexture.Region], and the source has a specified width and height.
     *
     * Width and height must be calculated based on the atlas packing algorithm, as the frames may
     * not be arranged in a simple grid. The [uploadToHardware] method uploads all frames to the hardware,
     * taking into account their positions within the source.
     *
     * @param frames The list of frames that make up the source.
     */
    class Source(
        private val frames: List<GraphicsTexture.Region>,
    ) : GraphicsTexture.Source {

        override val width = frames.maxOfOrNull { it.x + it.width } ?: 0f
        override val height = frames.maxOfOrNull { it.y + it.height } ?: 0f

        override fun uploadToHardware(
            texturePositionX: Float,
            texturePositionY: Float
        ) {
            frames.forEach { frame ->
                frame.source.uploadToHardware(texturePositionX + frame.x, texturePositionY + frame.y)
            }
        }

    }
}