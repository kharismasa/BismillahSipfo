package com.example.bismillahsipfo.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.bismillahsipfo.data.model.JenisHariLibur

object JenisHariLiburSerializer : KSerializer<JenisHariLibur> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JenisHariLibur", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JenisHariLibur) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): JenisHariLibur {
        val value = decoder.decodeString()
        return JenisHariLibur.values().find { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Invalid JenisHariLibur: $value")
    }
}