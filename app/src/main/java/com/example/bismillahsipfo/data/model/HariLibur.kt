package com.example.bismillahsipfo.data.model

import com.example.bismillahsipfo.data.serializer.LocalDateSerializer
import com.example.bismillahsipfo.data.serializer.JenisHariLiburSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class JenisHariLibur {
    @SerialName("nasional")
    NASIONAL,

    @SerialName("institusi")
    INSTITUSI
}

@Serializable
data class HariLibur(
    @SerialName("id_hari_libur") val idHariLibur: Int,
    @SerialName("date") @Serializable(with = LocalDateSerializer::class) val dateHariLibur: LocalDate,
    @SerialName("nama_hari_libur") val namaHariLibur: String,
    @SerialName("jenis") @Serializable(with = JenisHariLiburSerializer::class) val jenisHariLibur: JenisHariLibur
)