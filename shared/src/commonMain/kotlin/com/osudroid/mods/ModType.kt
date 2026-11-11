package com.osudroid.mods

import com.osudroid.resources.Res
import com.reco1l.verktex.localization.LocalizedString

/**
 * Available types of [Mod]s.
 */
enum class ModType(
    /**
     * The localized mod type's name.
     */
    val stringId: LocalizedString
) {
    DifficultyReduction(Res.string.mod_section_difficulty_reduction),
    DifficultyIncrease(Res.string.mod_section_difficulty_increase),
    Automation(Res.string.mod_section_difficulty_automation),
    Conversion(Res.string.mod_section_difficulty_conversion),
    Fun(Res.string.mod_section_fun),
    System(Res.string.mod_section_system),
}