package com.osudroid.ui.v2.hud

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Vec2
import kotlin.jvm.JvmField
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The layout of the HUD, a skin.json's `HUD`: an array of its elements, each with its `type`, its
 * position (`x`, `y`), `anchor`, `origin` and `scale`. Elements of types the HUD doesn't know are
 * kept, the HUD skips them.
 */
@Serializable(with = HUDSkinDataSerializer::class)
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


object HUDSkinDataSerializer : KSerializer<HUDSkinData> {

    @Serializable
    private class Element(
        val type: String,
        val x: Float = 0f,
        val y: Float = 0f,
        val anchor: String = "Center",
        val origin: String = "Center",
        val scale: Float = 1f,
    )

    private val elements = ListSerializer(Element.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor("com.osudroid.ui.v2.hud.HUDSkinData", elements.descriptor)

    override fun deserialize(decoder: Decoder) = HUDSkinData(
        decoder.decodeSerializableValue(elements).map {
            HUDElementSkinData(
                type = it.type,
                position = Vec2(it.x, it.y),
                anchor = Anchor.getFromName(it.anchor),
                origin = Anchor.getFromName(it.origin),
                scale = it.scale,
            )
        }
    )

    override fun serialize(encoder: Encoder, value: HUDSkinData) = encoder.encodeSerializableValue(
        elements,
        value.elements.map {
            Element(it.type, it.position.x, it.position.y, Anchor.getName(it.anchor), Anchor.getName(it.origin), it.scale)
        }
    )
}
