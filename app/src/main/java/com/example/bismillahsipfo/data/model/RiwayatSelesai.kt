package com.example.bismillahsipfo.data.model

import java.time.LocalDate
import java.time.LocalTime

data class RiwayatSelesai(
    val tanggalMulai: LocalDate,
    val tanggalSelesai: LocalDate,
    val namaFasilitas: String,
    val namaAcara: String,
    val jamMulai: LocalTime,
    val jamSelesai: LocalTime
)