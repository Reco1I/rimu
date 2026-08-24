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


    private val freeRegions = mutableListOf<Vec4>()

    private val regionsAllocated = mutableListOf<Region>()
    private val regionsToAllocate = mutableListOf<Region>()
    private val regionsToDeallocate = mutableListOf<Region>()

    private var isDirty = true


    //region Source allocation

    /**
     * Checks if the given [source] can fit within the texture using the current packing algorithm.
     *
     * @see TexturePackingAlgorithm
     */
    fun fitsSource(source: Source): Boolean {
        return packingAlgorithm.fits(source.width, source.height, freeRegions)
    }

    /**
     * Attempts to add a new [source] to the texture. If the source fits within the texture, a new
     * [Region] is created and added to the list of regions to allocate.
     *
     * @see TexturePackingAlgorithm
     * @return The newly created [Region] if the source fits, or null if it does not fit.
     */
    fun addSource(source: Source): Region? {

        val existingRegion = regionsAllocated.find { it.source == source }
        if (existingRegion != null) {
            return existingRegion
        }

        if (fitsSource(source)) {
            val region = Region(this, source)
            regionsToAllocate += region
            isDirty = true
            return region
        }

        return null
    }

    //endregion


    /**
     * Removes the specified [region] from the texture. If the region is currently allocated, it
     * will be marked for deallocation and removed from the list of regions to allocate.
     *
     * The texture atlas will be repacked on the next load to free up space for other regions.
     */
    fun removeRegion(region: Region) {
        if (regionsToAllocate.remove(region)) {
            return
        }

        regionsToDeallocate += region
        isDirty = true
    }


    override fun onLoad() {
        if (isDirty) {
            isDirty = false

            regionsAllocated.removeAll(regionsToDeallocate)
            regionsAllocated += regionsToAllocate

            packingAlgorithm.pack(regionsAllocated, freeRegions, width, height)

            regionsAllocated.forEach { region -> region.source.uploadToHardware(region.x, region.y) }
        }
    }


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

