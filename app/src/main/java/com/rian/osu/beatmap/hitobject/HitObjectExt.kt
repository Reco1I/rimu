@file:JvmName("HitObjectUtils")

package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * The end time of this hit object.
 */
val HitObject.endTime: Double
    get() = if (this is HitObjectWithLength) this.endTime else this.startTime

/**
 * The end position of this hit object.
 */
val HitObject.endPosition: Vector2
    get() = if (this is HitObjectWithLength) this.endPosition else this.position

/**
 * The stacked end position of this hit object.
 */
val HitObject.stackedEndPosition: Vector2
    get() = if (this is HitObjectWithLength) this.stackedEndPosition else this.stackedPosition