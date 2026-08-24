package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Vec4
import kotlin.math.min

/**
 * An interface for packing algorithms used in texture atlas management.
 */
abstract class TexturePackingAlgorithm {

    /**
     * Allocates a rectangle of the specified width and height from the list of free rectangles.
     *
     * @return A Vec4 representing the allocated rectangle, or null if no suitable rectangle was found.
     */
    abstract fun allocate(width: Float, height: Float, freeRegions: MutableList<Vec4>): Vec4?

    /**
     * Checks if a rectangle of the specified width and height can fit within any of the free rectangles.
     *
     * @return True if the rectangle can fit, false otherwise.
     */
    abstract fun fits(width: Float, height: Float, freeRegions: List<Vec4>): Boolean


    /**
     * Packs a list of texture regions into a texture atlas of the specified width and height.
     */
    fun pack(regions: List<GraphicsTexture.Region>, freeRegions: MutableList<Vec4>, rootWidth: Float, rootHeight: Float) {

        freeRegions.clear()
        freeRegions.add(Vec4(0f, 0f, rootWidth, rootHeight))

        for (region in regions) {

            val allocatedNode = allocate(region.source.width, region.source.height, freeRegions)
                ?: throw IllegalStateException("Failed to allocate region: $region")

            region.x = allocatedNode.x
            region.y = allocatedNode.y
            region.width = region.source.width
            region.height = region.source.height
        }
    }


    companion object {

        /**
         * An implementation of the MaxRects packing algorithm for texture atlas management.
         */
        val MaxRects = object : TexturePackingAlgorithm() {

            override fun allocate(
                width: Float,
                height: Float,
                freeRegions: MutableList<Vec4>
            ): Vec4? {
                val bestNode = findBestRegion(width, height, freeRegions) ?: return null
                splitFreeRegions(bestNode, freeRegions)
                pruneFreeList(freeRegions)
                return bestNode
            }

            override fun fits(
                width: Float,
                height: Float,
                freeRegions: List<Vec4>
            ): Boolean {
                return findBestRegion(width, height, freeRegions) != null
            }


            private fun findBestRegion(width: Float, height: Float, freeRegions: List<Vec4>): Vec4? {
                var bestNode: Vec4? = null
                var bestAreaFit = Int.MAX_VALUE.toFloat()
                var bestShortSideFit = Int.MAX_VALUE.toFloat()

                for (free in freeRegions) {

                    if (width <= free.width && height <= free.height) {

                        val areaFit = free.width * free.height - width * height

                        val leftOverHoriz = free.width - width
                        val leftOverVert = free.height - height

                        val shortSideFit = min(leftOverHoriz, leftOverVert)

                        if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
                            bestNode = Vec4(free.x, free.y, free.x + width, free.y + height)
                            bestAreaFit = areaFit
                            bestShortSideFit = shortSideFit
                        }

                    }
                }

                return bestNode
            }

            private fun pruneFreeList(freeRegions: MutableList<Vec4>) {

                val it = freeRegions.iterator()

                while (it.hasNext()) {

                    val a = it.next()

                    var contained = false

                    for (b in freeRegions) {

                        if (a === b) continue

                        if (isContainedIn(a, b)) {
                            contained = true
                            break
                        }
                    }

                    if (contained) {
                        it.remove()
                    }
                }
            }

            private fun intersects(a: Vec4, b: Vec4): Boolean {
                return !(b.x >= a.x + a.width ||
                        b.x + b.width <= a.x ||
                        b.y >= a.y + a.height ||
                        b.y + b.height <= a.y)
            }

            private fun isContainedIn(a: Vec4, b: Vec4): Boolean {
                return a.x >= b.x &&
                        a.y >= b.y &&
                        a.x + a.width <= b.x + b.width &&
                        a.y + a.height <= b.y + b.height
            }

            private fun splitFreeRegions(used: Vec4, freeRegions: MutableList<Vec4>) {

                val newRegions = mutableListOf<Vec4>()
                val iterator = freeRegions.iterator()

                while (iterator.hasNext()) {

                    val free = iterator.next()

                    if (!intersects(free, used)) {
                        continue
                    }

                    iterator.remove()

                    // Top
                    if (used.y > free.y) {
                        newRegions.add(Vec4(
                            free.x,
                            free.y,
                            free.width,
                            used.y - free.y
                        )
                        )
                    }

                    // Bottom
                    val freeBottom = free.y + free.height
                    val usedBottom = used.y + used.height

                    if (usedBottom < freeBottom) {
                        newRegions.add(Vec4(
                            free.x,
                            usedBottom,
                            free.width,
                            freeBottom - usedBottom
                        )
                        )
                    }

                    // Left
                    if (used.x > free.x) {
                        newRegions.add(Vec4(
                            free.x,
                            free.y,
                            used.x - free.x,
                            free.height
                        )
                        )
                    }

                    // Right
                    val freeRight = free.x + free.width
                    val usedRight = used.x + used.width

                    if (usedRight < freeRight) {
                        newRegions.add(Vec4(
                            usedRight,
                            free.y,
                            freeRight - usedRight,
                            free.height
                        )
                        )
                    }
                }

                freeRegions.addAll(newRegions)
            }

        }
    }
}