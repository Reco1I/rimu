package com.osudroid.mods

import com.osudroid.utils.ModHashMap
import com.osudroid.utils.createModInstance
import com.osudroid.utils.toFloatWithCommaSeparator
import kotlin.reflect.KClass

/**
 * Converts the mods of replays older than version 7, which stored them as a set of the constants of
 * upstream's `GameMod` enum and an "extra mod string" with the mods' settings.
 */
object LegacyModConverter {

    /**
     * The [Mod] of each constant of upstream's `GameMod` enum, by name.
     */
    val gameModNames: Map<String, KClass<out Mod>> = mapOf(
        "MOD_AUTO" to ModAutoplay::class,
        "MOD_AUTOPILOT" to ModAutopilot::class,
        "MOD_DOUBLETIME" to ModDoubleTime::class,
        "MOD_EASY" to ModEasy::class,
        "MOD_FLASHLIGHT" to ModFlashlight::class,
        "MOD_HALFTIME" to ModHalfTime::class,
        "MOD_HARDROCK" to ModHardRock::class,
        "MOD_HIDDEN" to ModHidden::class,
        "MOD_TRACEABLE" to ModTraceable::class,
        "MOD_NIGHTCORE" to ModNightCore::class,
        "MOD_NOFAIL" to ModNoFail::class,
        "MOD_PERFECT" to ModPerfect::class,
        "MOD_PRECISE" to ModPrecise::class,
        "MOD_REALLYEASY" to ModReallyEasy::class,
        "MOD_RELAX" to ModRelax::class,
        "MOD_SCOREV2" to ModScoreV2::class,
        "MOD_SMALLCIRCLE" to ModSmallCircle::class,
        "MOD_SUDDENDEATH" to ModSuddenDeath::class,
    )

    /**
     * Converts legacy mods to [Mod]s.
     *
     * @param modNames The names of the `GameMod` constants.
     * @param extraModString The extra mod string, with the custom speed (`x1.25`), the custom
     * difficulty (`CS4.0`, `AR9.0`, `OD8.0`, `HP5.0`) and the flashlight's follow delay (`FLD0.12`),
     * separated by `|`.
     * @throws IllegalArgumentException If a name isn't a `GameMod` constant.
     */
    fun convert(modNames: Iterable<String>, extraModString: String) = ModHashMap().apply {
        for (name in modNames) {
            val mod = requireNotNull(gameModNames[name]) { "Unknown legacy mod $name" }
            put(mod.createModInstance())
        }

        parseExtraModString(this, extraModString)
    }

    private fun parseExtraModString(mods: ModHashMap, str: String) {
        var customCS: Float? = null
        var customAR: Float? = null
        var customOD: Float? = null
        var customHP: Float? = null

        for (s in str.split('|')) {
            when {
                s.startsWith('x') && s.length == 5 -> mods.put(ModCustomSpeed(s.substring(1).toFloatWithCommaSeparator()))

                s.startsWith("CS") -> customCS = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("AR") -> customAR = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("OD") -> customOD = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("HP") -> customHP = s.substring(2).toFloatWithCommaSeparator()

                s.startsWith("FLD") -> {
                    val flashlight = mods.ofType<ModFlashlight>() ?: ModFlashlight().also { mods.put(it) }
                    flashlight.followDelay = s.substring(3).toFloatWithCommaSeparator()
                }
            }
        }

        if (customCS != null || customAR != null || customOD != null || customHP != null) {
            // The difficulty isn't given to the mod's constructor so that its values stay null: the
            // beatmap's aren't known.
            mods.put(ModDifficultyAdjust().apply {
                cs = customCS
                ar = customAR
                od = customOD
                hp = customHP
            })
        }
    }
}
