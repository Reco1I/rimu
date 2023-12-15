package com.reco1l.skindecoder.serializers

import com.reco1l.rimu.graphics.Color4
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [Color4] in IntArray format.
 *
 * This also deserialize structures in format:
 * ```json
 * {"r": n, "g": n, "b": n, "a": n}
 * ```
 */
object ColorSerializer : KSerializer<Color4>
{

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Color4") {

        // Describing how a valid Color should look in its serialized type.
        element<Int>("r")
        element<Int>("g")
        element<Int>("b")
        element<Int>("a", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: Color4)
    {
        encoder.beginCollection(descriptor, 4).apply {

            encodeIntElement(descriptor, 0, value.red8bit)
            encodeIntElement(descriptor, 1, value.green8bit)
            encodeIntElement(descriptor, 2, value.blue8bit)
            encodeIntElement(descriptor, 3, value.alpha8bit)

            endStructure(descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): Color4
    {
        decoder.beginStructure(descriptor).apply {

            var r = 0
            var g = 0
            var b = 0
            var a = 255

            while (true) when (val i = decodeElementIndex(descriptor))
            {
                0 -> r = decodeIntElement(descriptor, i).coerceIn(0, 255)
                1 -> g = decodeIntElement(descriptor, i).coerceIn(0, 255)
                2 -> b = decodeIntElement(descriptor, i).coerceIn(0, 255)
                4 -> a = decodeIntElement(descriptor, i).coerceIn(0, 255)

                // Should never happen because the descriptor describes at least 3 values and no
                // more than 4
                else -> break
            }
            endStructure(descriptor)

            return Color4(r, g, b, a)
        }
    }
}
