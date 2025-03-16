package com.example.bismillahsipfo.data.model

import java.time.LocalDate
import java.time.LocalTime

data class JadwalPeminjamanItem(
    val tanggal: LocalDate,
    val jamMulai: LocalTime,
    val jamSelesai: LocalTime,
    val namaOrganisasi: String,
    val namaLapangan: List<String>
)