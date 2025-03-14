package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lapangan (
    @SerialName("id_lapangan") val idLapangan: Int,
    @SerialName("id_fasilitas") val idFasilitas: Int,
    @SerialName("nama_lapangan") val namaLapangan: String
)