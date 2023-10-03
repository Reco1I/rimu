package com.reco1l.skindecoder.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NumericBooleanSerializer : KSerializer<Boolean>
{

    override val descriptor = PrimitiveSerialDescriptor(
        serialName = javaClass.name,
        kind = PrimitiveKind.INT
    )

    override fun serialize(encoder: Encoder, value: Boolean)
    {
        encoder.encodeInt(if (value) 1 else 0)
    }

    override fun deserialize(decoder: Decoder): Boolean
    {
        val value = decoder.decodeInt()

        if (value < 0 || value > 1)
            throw SerializationException("Invalid boolean value, must be 0 or 1.")

        return value == 1
    }
}