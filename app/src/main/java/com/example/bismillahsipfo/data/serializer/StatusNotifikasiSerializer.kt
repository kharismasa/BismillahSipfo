package com.example.bismillahsipfo.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.bismillahsipfo.data.model.StatusNotifikasi

object StatusNotifikasiSerializer : KSerializer<StatusNotifikasi> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatusNotifikasi", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StatusNotifikasi) {
        encoder.encodeString(value.name.replace("_", " ").lowercase())
    }

    override fun deserialize(decoder: Decoder): StatusNotifikasi {
        val value = decoder.decodeString()
        return StatusNotifikasi.values().find { it.name.replace("_", " ").equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Invalid StatusNotifikasi: $value")
    }
}