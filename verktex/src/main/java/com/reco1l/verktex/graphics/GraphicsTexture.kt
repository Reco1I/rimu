package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Vec4

/**
 * The [GraphicsTexture] class represents a texture resource that can be used for rendering in the application.
 * It provides functionality for managing texture regions, packing algorithms, and uploading texture
 * data to the GPU
 */
abstract class GraphicsTexture : GraphicsResource() {

    /**
     * The width of the texture in pixels.
     */
    abstract val width: Float

    /**
     * The height of the texture in pixels.
     */
    abstract val height: Float


    /**
     * The algorithm used for texture atlas packing.
     */
    open val packingAlgorithm: TexturePackingAlgorithm = TexturePackingAlgorithm.MaxRects

    /**
     * Whether the texture is an external image (such as video frames) that must be sampled with
     * [com.reco1l.verktex.BuiltIn.Shaders.Video] instead of the regular texture shader.
     */
    open val isExternal: Boolean get() = false

    /**
     * The column-major 4x4 matrix applied to texture coordinates when sampling an external texture,
     * null for regular textures.
     */
    open val transform: FloatArray? get() = null


    private val freeRegions = mutableListOf<Vec4>()

    private val regions = mutableListOf<Region>()
    private val pendingUploads = mutableListOf<Region>()

    private var freeRegionsInitialized = false

    // Textures are loaded and unloaded from any thread while the draw worker binds the texture.
    private val lock = Any()


    //region Source allocation

    private fun ensureFreeRegions() {
        if (!freeRegionsInitialized) {
            freeRegionsInitialized = true
            freeRegions.add(Vec4(0f, 0f, width, height))
        }
    }

    /**
     * Checks if the given [source] can fit within the texture using the current packing algorithm.
     *
     * @see TexturePackingAlgorithm
     */
    open fun fitsSource(source: Source): Boolean = synchronized(lock) {
        ensureFreeRegions()
        packingAlgorithm.fits(source.width, source.height, freeRegions)
    }

    /**
     * Attempts to add a new [source] to the texture. If the source fits within the texture, space is
     * reserved for it right away and its data is uploaded the next time the texture is bound.
     *
     * @see TexturePackingAlgorithm
     * @return The newly created [Region] if the source fits, or null if it does not fit.
     */
    open fun addSource(source: Source): Region? = synchronized(lock) {

        val existingRegion = regions.find { it.source == source }
        if (existingRegion != null) {
            return existingRegion
        }

        ensureFreeRegions()

        val node = packingAlgorithm.allocate(source.width, source.height, freeRegions) ?: return null
        val region = Region(this, source, node.x, node.y, source.width, source.height)

        regions += region
        pendingUploads += region
        region
    }

    //endregion


    /**
     * Removes the specified [region] from the texture.
     *
     * The remaining regions are repacked right away, so the freed space can be used immediately.
     * They are uploaded again at their new positions on the next bind. When they do not repack, they
     * keep their positions and the region's space is returned to the free space instead.
     */
    fun removeRegion(region: Region) = synchronized(lock) {
        if (regions.remove(region)) {
            freeRegionsInitialized = true

            if (packingAlgorithm.pack(regions, freeRegions, width, height)) {
                pendingUploads.clear()
                pendingUploads += regions
            } else {
                pendingUploads.remove(region)
                // The removed rectangle was a used one, and MaxRects keeps free rectangles disjoint from used ones, so giving it back cannot create overlaps.
                freeRegions += Vec4(region.x, region.y, region.x + region.width, region.y + region.height)
            }
        }
    }


    final override fun onLoad() {
        synchronized(lock) {
            onCreateTexture()

            // The texture storage was (re)created, so every region must be uploaded again.
            pendingUploads.clear()
            pendingUploads += regions
        }

        onTextureCreated()
    }

    final override fun onBind() = synchronized(lock) {
        onBindTexture()

        if (pendingUploads.isNotEmpty()) {
            for (region in pendingUploads) {
                region.source.uploadToHardware(region.x, region.y)
            }
            pendingUploads.clear()
        }
    }

    /**
     * Called when the texture should be created on the GPU with storage for [width] x [height] pixels.
     * The texture must be left bound after this call.
     */
    protected abstract fun onCreateTexture()

    /**
     * Called after [onCreateTexture] once the texture's lock is released, on the same thread.
     */
    protected open fun onTextureCreated() = Unit

    /**
     * Called when the texture should be bound so that sources can be uploaded to it or it can be drawn.
     */
    protected abstract fun onBindTexture()


    /**
     * Represents a rectangular region within the texture that is associated with a specific source.
     * This class holds information about the position and size of the region, as well as the
     * texture coordinates (u0, v0, u1, v1) for rendering.
     */
    open class Region(

        /**
         * The texture to which this region belongs.
         */
        val texture: GraphicsTexture,

        /**
         * The source associated with this region, which provides the texture data.
         */
        val source: Source,

        /**
         * The x-coordinate of the region's position within the texture.
         */
        var x: Float = 0f,

        /**
         * The y-coordinate of the region's position within the texture.
         */
        var y: Float = 0f,

        /**
         * The width of the region within the texture.
         */
        var width: Float = 0f,

        /**
         * The height of the region within the texture.
         */
        var height: Float = 0f
    ) {

        /**
         * The u-coordinate of the top-left corner of the region in texture space (normalized).
         */
        val u0: Float
            get() = x / texture.width

        /**
         * The v-coordinate of the top-left corner of the region in texture space (normalized).
         */
        val v0: Float
            get() = y / texture.height

        /**
         * The u-coordinate of the bottom-right corner of the region in texture space (normalized).
         */
        val u1: Float
            get() = (x + width) / texture.width

        /**
         * The v-coordinate of the bottom-right corner of the region in texture space (normalized).
         */
        val v1: Float
            get() = (y + height) / texture.height

    }

    /**
     * Represents a source of texture data that can be uploaded to the GPU.
     *
     * Implementations of this interface should provide the width and height of the source, as well
     * as a method to upload the source data to the GPU at a specified position (x, y) within the texture.
     */
    interface Source {

        /**
         * The width of the source in pixels.
         */
        val width: Float

        /**
         * The height of the source in pixels.
         */
        val height: Float

        /**
         * Uploads the source data to the GPU at the specified position (x, y) within the texture.
         */
        fun uploadToHardware(texturePositionX: Float, texturePositionY: Float)
    }
}

