package com.example.bismillahsipfo.data.model

import java.time.LocalDate
import java.time.LocalTime

data class JadwalTersedia(
    val hari: String,
    val tanggal: LocalDate,
    val waktuMulai: LocalTime,
    val waktuSelesai: LocalTime,
    val listLapangan: List<Int>,
    val tipeJadwal: String,
    val urutanSlot: Int?
)