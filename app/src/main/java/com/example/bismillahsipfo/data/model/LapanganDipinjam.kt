package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LapanganDipinjam (
    @SerialName("id_lapangan_dipinjam") val idLapanganDipinjam: Int,
    @SerialName("id_peminjaman") val idPeminjaman: Int,
    @SerialName("id_lapangan") val idLapangan: Int,
)