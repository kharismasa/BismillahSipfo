package com.example.bismillahsipfo.data.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class RiwayatPending(
    val tanggalMulai: LocalDate,
    val tanggalSelesai: LocalDate,
    val namaFasilitas: String,
    val totalBiaya: Double,
    val waktuKadaluwarsa: Instant
)
