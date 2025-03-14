package com.example.bismillahsipfo.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.bismillahsipfo.data.model.JenisNotifikasi

object JenisNotifikasiSerializer : KSerializer<JenisNotifikasi> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JenisNotifikasi", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JenisNotifikasi) {
        encoder.encodeString(value.name.replace("_", " ").lowercase().capitalize())
    }

    override fun deserialize(decoder: Decoder): JenisNotifikasi {
        val value = decoder.decodeString()
        return JenisNotifikasi.values().find { it.name.replace("_", " ").equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Invalid JenisNotifikasi: $value")
    }
}