package com.osudroid.replay

import com.osudroid.difficulty.attributes.HighStrainSection
import kotlin.jvm.JvmField

/**
 * An extension of [HighStrainSection] for assigning dragged sections in three-finger detection.
 */
class ThreeFingerBeatmapSection(
    section: HighStrainSection,

    /**
     * The [ThreeFingerObject]s in this section.
     */
    @JvmField
    val objects: List<ThreeFingerObject>,
) : HighStrainSection(section.firstObjectIndex, section.lastObjectIndex, section.sumStrain)