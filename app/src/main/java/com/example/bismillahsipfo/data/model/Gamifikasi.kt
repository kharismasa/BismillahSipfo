package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Gamifikasi (
    @SerialName("id_gamifikasi") val idGamifikasi: Int,
    @SerialName("level") val level: Int,
    @SerialName("jumlah_peminjaman_minimal") val jumlahPeminjamanMinimal: Double,
    @SerialName("tropi") val tropi: String,
    @SerialName("id_voucher") val idVoucher: Int
)