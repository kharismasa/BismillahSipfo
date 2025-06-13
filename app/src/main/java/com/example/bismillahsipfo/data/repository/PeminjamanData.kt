package com.example.bismillahsipfo.data.repository

import com.example.bismillahsipfo.data.model.JadwalTersedia

data class PeminjamanData(
    val idFasilitas: Int = -1,
    val namaFasilitas: String? = null,
    val opsiPeminjaman: String? = null,
    val namaAcara: String? = null,
    val namaOrganisasi: String? = null,
    val idOrganisasi: Int = -1,
    val jadwalTersedia: JadwalTersedia? = null,
    val listLapangan: List<Int>? = null,
    val lapanganDipinjam: List<Int>? = null,
    val penggunaKhusus: String? = null,
    val tanggalMulai: String? = null,
    val tanggalSelesai: String? = null,
    val jamMulai: String? = null,
    val jamSelesai: String? = null,
    val pdfUri: String? = null,
    val selectedFileName: String? = null,
    val uploadedFileUrl: String? = null
)