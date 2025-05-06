package com.example.bismillahsipfo.data.model

data class JadwalRutinWithOrganisasi(
    val jadwalRutin: JadwalRutin,
    val namaOrganisasi: String,
    val namaLapangan: List<String>
)
