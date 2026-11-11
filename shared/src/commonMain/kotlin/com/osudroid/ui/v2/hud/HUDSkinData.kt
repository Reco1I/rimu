package com.osudroid.ui.v2.hud

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Vec2
import com.osudroid.utils.optDouble
import com.osudroid.utils.optJsonObject
import com.osudroid.utils.optString
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class HUDSkinData(val elements: List<HUDElementSkinData>) {


    /**
     * Whether the layout has an element of the type named [type], see [HUDElementSkinData.type].
     */
    fun hasElement(type: String) = elements.any { it.type == type }


    companion object {

        /**
         * The default layout data for the HUD.
         *
         * Based on the default skin layout from osu!droid version 1.8.2.1.
         */
        @JvmField
        val Default = HUDSkinData(
            listOf(
                HUDElementSkinData(
                    type = "accuracy_counter",
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight,
                    scale = 0.6f * 0.96f,
                    position = Vec2(-17f, 9f)
                ),
                HUDElementSkinData(
                    type = "leaderboard",
                    anchor = Anchor.TopLeft,
                    origin = Anchor.TopLeft,
                    position = Vec2(0f, 83f)
                ),
                HUDElementSkinData(
                    type = "combo_counter",
                    anchor = Anchor.BottomLeft,
                    origin = Anchor.BottomLeft,
                    position = Vec2(10f, -10f),
                    scale = 1.28f
                ),
                HUDElementSkinData(
                    type = "pie_song_progress",
                    anchor = Anchor.TopRight,
                    origin = Anchor.CenterRight
                ),
                HUDElementSkinData(
                    type = "health_bar",
                    anchor = Anchor.TopLeft,
                    origin = Anchor.TopLeft
                ),
                HUDElementSkinData(
                    type = "score_counter",
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight,
                    scale = 0.96f,
                    position = Vec2(-10f, 0f)
                ),
                HUDElementSkinData(
                    type = "back_button",
                    anchor = Anchor.CenterRight,
                    origin = Anchor.CenterRight,
                    position = Vec2(-20f, 0f)
                ),
                HUDElementSkinData(
                    type = "ur_counter",
                    anchor = Anchor.BottomRight,
                    origin = Anchor.BottomRight,
                    position = Vec2(-10f, -32f)
                ),
                HUDElementSkinData(
                    type = "avg_offset_counter",
                    anchor = Anchor.BottomRight,
                    origin = Anchor.BottomRight,
                    position = Vec2(-10f, -59f)
                ),
                HUDElementSkinData(
                    type = "hit_error_meter",
                    anchor = Anchor.BottomCenter,
                    origin = Anchor.BottomCenter
                )
            )
        )


        @JvmStatic
        fun writeToJSON(data: HUDSkinData) = buildJsonArray {
            data.elements.forEach {
                add(buildJsonObject {
                    put("type", it.type)
                    put("x", it.position.x)
                    put("y", it.position.y)
                    put("anchor", Anchor.getName(it.anchor))
                    put("origin", Anchor.getName(it.origin))
                    put("scale", it.scale)
                })
            }
        }

        /**
         * Reads a layout. Elements of types the HUD doesn't know are kept, the HUD skips them.
         */
        @JvmStatic
        fun readFromJSON(json: JsonArray) = HUDSkinData(
            elements = List(json.size) { i ->

                val element = json[i] as? JsonObject ?: throw IllegalArgumentException("HUD element $i is not an object.")
                val type = element["type"]?.let { (it as? JsonPrimitive)?.content } ?: throw IllegalArgumentException("HUD element $i has no type.")

                HUDElementSkinData(
                    type = type,
                    position = Vec2(
                        element.optDouble("x", 0.0).toFloat(),
                        element.optDouble("y", 0.0).toFloat(),
                    ),
                    anchor = Anchor.getFromName(element.optString("anchor", "Center")),
                    origin = Anchor.getFromName(element.optString("origin", "Center")),
                    scale = element.optDouble("scale", 1.0).toFloat(),
                )
            }
        )


    }

}

data class HUDElementSkinData(

    /**
     * The type of the element, by the name it has in skins (see `HUDElements`).
     */
    val type: String,

    /**
     * The offset of the element from the constraint.
     */
    val position: Vec2 = Vec2.Zero,

    /**
     * The anchor of the element.
     */
    val anchor: Vec2 = Anchor.Center,

    /**
     * The origin of the element.
     */
    val origin: Vec2 = Anchor.Center,

    /**
     * The scale applied to the element.
     */
    val scale: Float = 1f
)