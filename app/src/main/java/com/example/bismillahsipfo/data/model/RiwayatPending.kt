package com.example.bismillahsipfo.data.model

import java.time.Instant
import java.time.LocalDate

data class RiwayatPending(
    val tanggalMulai: LocalDate,
    val tanggalSelesai: LocalDate,
    val namaFasilitas: String,
    val totalBiaya: Double,
    val waktuKadaluwarsa: Instant,
    val statusPembayaran: StatusPembayaran, // Tambahkan status
    val idPembayaran: String, // Tambahkan payment ID untuk redirect
    val midtransToken: String? = null, // Tambahkan token untuk webview
    val midtransRedirectUrl: String? = null // Tambahkan redirect URL
)