package com.example.bismillahsipfo.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class JadwalPeminjamanItem(
    val tanggal: LocalDate,
    val jamMulai: LocalTime,
    val jamSelesai: LocalTime,
    val namaOrganisasi: String,
    val namaLapangan: List<String>
)

