package com.example.bismillahsipfo.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.bismillahsipfo.data.model.PenggunaKhusus

object PenggunaKhususSerializer : KSerializer<PenggunaKhusus?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PenggunaKhusus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PenggunaKhusus?) {
        encoder.encodeString(value?.description ?: "")
    }

    override fun deserialize(decoder: Decoder): PenggunaKhusus? {
        val description = decoder.decodeString()
        return PenggunaKhusus.values().find { it.description == description }
            ?: throw IllegalArgumentException("Invalid PenggunaKhusus description: $description")
    }
}