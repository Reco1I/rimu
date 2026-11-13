package com.osudroid.ui.skinning

import com.osudroid.utils.putIfNotNull
import kotlin.math.min
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Recursively fills [target] with keys from [source] it's missing, without overwriting existing ones.
 *
 * @return The filled copy of [target].
 */
fun mergeMissing(target: JsonObject, source: JsonObject): JsonObject {
    val merged = LinkedHashMap(target)

    for ((key, sourceValue) in source) {
        if (!target.containsKey(key)) {
            merged[key] = sourceValue
            continue
        }

        val targetValue = target[key]
        if (sourceValue is JsonObject && targetValue is JsonObject) {
            merged[key] = mergeMissing(targetValue, sourceValue)
        }
    }

    return JsonObject(merged)
}

fun convertToJson(ini: IniReader) = buildJsonObject {

    fun convertToHex(ints: IntArray?): String? {

        if (ints == null || ints.isEmpty()) {
            return null
        }

        return buildString {

            append('#')
            append(ints[0].toHex())
            append(ints[1].toHex())
            append(ints[2].toHex())
        }
    }

    fun parseComboColors(ini: IniReader) = mutableListOf<String>().also {

        // osu! skins supports up to 7 combo colors
        for (i in 0 until 8) {
            val ints: IntArray = ini["Colours", "Combo${i + 1}"] ?: continue

            // At this point convertToHex() shouldn't return null, but we handle it anyway.
            convertToHex(ints)?.also { rgb -> it.add(rgb) }
        }

    }.takeUnless { it.isEmpty() }?.toTypedArray()

    putIfNotNull("Cursor", buildJsonObject {
        putIfNotNull("rotateCursor", ini.get<Boolean>("General", "CursorRotate") != false)
        putIfNotNull("rotateCursorTrail", ini.get<Boolean>("General", "CursorTrailRotate") != false)
        putIfNotNull("expandCursor", ini.get<Boolean>("General", "CursorExpand") != false)
        putIfNotNull("centreCursor", ini.get<Boolean>("General", "CursorCentre") != false)
    })

    putIfNotNull("ComboColor", buildJsonObject {

        parseComboColors(ini)?.also {

            val array = JsonArray(it.map { color -> JsonPrimitive(color) })

            putIfNotNull("colors", array)
            putIfNotNull("forceOverride", true)
        }
    })

    putIfNotNull("Slider", buildJsonObject {

        ini.get<IntArray?>("Colours", "SliderTrackOverride")?.also { trackColor ->

            val hintColor = IntArray(trackColor.size) { min(255, trackColor[it] + 15) }

            convertToHex(hintColor)?.also {

                putIfNotNull("sliderHintColor", it)
                putIfNotNull("sliderHintAlpha", 1f)
                putIfNotNull("sliderHintWidth", 25f)
                putIfNotNull("sliderHintEnable", true)
                putIfNotNull("sliderHintShowMinLength", 1f)
            }

            convertToHex(trackColor)?.also {

                putIfNotNull("sliderBodyColor", it)
                putIfNotNull("sliderBodyBaseAlpha", 1f)
                putIfNotNull("sliderFollowComboColor", false)
            }
        }

        putIfNotNull("sliderBorderColor", convertToHex(ini["Colours", "SliderBorder"]))
    })

    putIfNotNull("Color", buildJsonObject {

        putIfNotNull("MenuItemSelectedTextColor", convertToHex(ini["Colours", "SongSelectActiveText"]) ?: "#FFFFFF")
        putIfNotNull("MenuItemDefaultTextColor", convertToHex(ini["Colours", "SongSelectInactiveText"]) ?: "#000000")
        putIfNotNull("MenuItemDefaultColor", "#EB4999") // Matching osu! stable inactive color.
    })

    putIfNotNull("Fonts", buildJsonObject {

        putIfNotNull("hitCirclePrefix", ini["Fonts", "HitCirclePrefix"] ?: "default")
        putIfNotNull("hitCircleOverlap", ini["Fonts", "HitCircleOverlap"] ?: -2)
        putIfNotNull("scorePrefix", ini["Fonts", "ScorePrefix"] ?: "score")
        putIfNotNull("scoreOverlap", ini["Fonts", "ScoreOverlap"] ?: 0)
        putIfNotNull("comboPrefix", ini["Fonts", "ComboPrefix"] ?: "score")
        putIfNotNull("comboOverlap", ini["Fonts", "ComboOverlap"] ?: 0)
    })

    putIfNotNull("Utils", buildJsonObject {
        putIfNotNull("comboTextScale", 0.8f)
        putIfNotNull("animationFramerate", ini["General", "AnimationFramerate"] ?: -1f)
        putIfNotNull("layeredHitSounds", ini.get<Boolean>("General", "LayeredHitSounds") != false)
        putIfNotNull("sliderBallFlip", ini.get<Boolean>("General", "SliderBallFlip") != false)
        putIfNotNull("spinnerFrequencyModulate", ini.get<Boolean>("General", "SpinnerFrequencyModulate") != false)
    })

    putIfNotNull("Layout", buildJsonObject {

        putIfNotNull("useNewLayout", true)

        putIfNotNull("BackButton", buildJsonObject {
            putIfNotNull("scaleWhenHold", false)
        })

        // In osu!droid's default skin, these buttons are cut in the bottom, which makes them smaller
        // (see https://github.com/osudroid/osu-droid/commit/7bc5040ce426760c2f3ea04b7209e4ded8e78524).
        // To account for this behavior in osu! skins, we need to offset by them 16 pixels downwards.
        // A negative value is used as the origin and anchor of these buttons are in the bottom-left
        // corner (see SkinLayout).
        putIfNotNull("ModsButton", buildJsonObject {
            putIfNotNull("y", -16)
        })

        putIfNotNull("OptionsButton", buildJsonObject {
            putIfNotNull("y", -16)
        })

        putIfNotNull("RandomButton", buildJsonObject {
            putIfNotNull("y", -16)
        })
    })
}

/**
 * Formats this value as two or more uppercase hexadecimal digits, as `"%02X"` does.
 */
private fun Int.toHex() = toString(16).uppercase().padStart(2, '0')
