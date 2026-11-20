package com.osudroid.game

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.mods.ModApproachDifferent
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModAutoplay
import com.osudroid.mods.ModEasy
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModMuted
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModOldNightCore
import com.osudroid.mods.ModPerfect
import com.osudroid.mods.ModRelax
import com.osudroid.mods.ModScoreV2
import com.osudroid.mods.ModSuddenDeath
import com.osudroid.mods.ModSynesthesia
import com.osudroid.mods.ModTraceable
import com.osudroid.utils.ModHashMap

/**
 * What the gameplay of one play depends on: its mods and the parts of its beatmap the objects read, fixed once it's
 * loaded, and its [timing], which follows the music.
 *
 * Each play has its own, which the game objects get from their listener as they're initialized.
 */
class GameplayContext(
    /**
     * The mods of the play.
     */
    val mods: ModHashMap = ModHashMap(),

    /**
     * The HP of the beatmap, its health drain.
     */
    val healthDrain: Float = 0f,

    /**
     * The time the objects are shown before their start with the beatmap's AR alone, in milliseconds, which sizes the
     * approach circles of the mods changing it.
     */
    val originalTimePreempt: Float = HitObject.PREEMPT_MID.toFloat(),

    /**
     * Whether the hit samples play at the rate of the music.
     */
    val samplesMatchPlaybackRate: Boolean = false,

    /**
     * The version of the replay being watched, which changed how some objects are judged. 0 when not watching one.
     */
    val replayVersion: Int = 0
) {

    val hidden = mods.ofType(ModHidden::class)
    val traceable = mods.ofType(ModTraceable::class)
    val flashlight = mods.ofType(ModFlashlight::class)
    val muted = mods.ofType(ModMuted::class)
    val approachDifferent = mods.ofType(ModApproachDifferent::class)

    val isHidden = hidden != null
    val isTraceable = traceable != null
    val isFlashlight = flashlight != null
    val isMuted = muted != null
    val isHardRock = ModHardRock::class in mods
    val isRelax = ModRelax::class in mods
    val isAutopilot = ModAutopilot::class in mods
    val isAutoplay = ModAutoplay::class in mods
    val isSuddenDeath = ModSuddenDeath::class in mods
    val isPerfect = ModPerfect::class in mods
    val isSynesthesia = ModSynesthesia::class in mods
    val isScoreV2 = ModScoreV2::class in mods
    val isEasy = ModEasy::class in mods
    val isNightCore = ModNightCore::class in mods || ModOldNightCore::class in mods

    /**
     * Whether the objects fade out before they're hit, as Hidden does unless it only fades the approach circles.
     */
    val fadesOutObjects = hidden != null && !hidden.onlyFadeApproachCircles

    /**
     * Where the play is in its beatmap, updated every frame.
     */
    val timing = GameplayTiming()
}

/**
 * Where a play is in its beatmap: its rate and its beat.
 */
class GameplayTiming {

    /**
     * The rate gameplay progresses at, from the mods and the playback rate of a replay.
     */
    var speedMultiplier = 1f

    /**
     * The length of a beat of the current timing point, in seconds.
     */
    var beatLength = 0.0

    /**
     * The time since the last beat, in seconds.
     */
    var currentBeatTime = 0.0

    /**
     * Whether the current effect point is a kiai.
     */
    var isKiai = false
}
