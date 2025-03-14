package com.example.bismillahsipfo.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.bismillahsipfo.data.model.StatusPembayaran

object StatusPembayaranSerializer : KSerializer<StatusPembayaran> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatusPembayaran", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StatusPembayaran) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): StatusPembayaran {
        val value = decoder.decodeString()
        return StatusPembayaran.values().find { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Invalid StatusPembayaran: $value")
    }
}