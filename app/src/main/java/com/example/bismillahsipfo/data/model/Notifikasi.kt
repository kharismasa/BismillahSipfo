package com.example.bismillahsipfo.data.model

import com.example.bismillahsipfo.data.serializer.InstantSerializer
import com.example.bismillahsipfo.data.serializer.JenisNotifikasiSerializer
import com.example.bismillahsipfo.data.serializer.StatusNotifikasiSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
enum class JenisNotifikasi {
    @SerialName("Penyelesaian prosedur")
    PENYELESAIAN_PROSEDUR,

    @SerialName("Pengingat jadwal")
    PENGINGAT_JADWAL
}

@Serializable
enum class StatusNotifikasi {
    @SerialName("belum dikirim")
    BELUM_DIKIRIM,

    @SerialName("sudah dikirim")
    SUDAH_DIKIRIM
}

@Serializable
data class Notifikasi (
    @SerialName("id_notifikasi") val idNotifikasi: Int,
    @SerialName("id_peminjaman") val idPeminjaman: Int,
    @SerialName("jenis_notifikasi") @Serializable(with = JenisNotifikasiSerializer::class) val jenisNotifikasi: JenisNotifikasi,
    @SerialName("pesan") val pesan: String,
    @SerialName("tanggal_dibuat") @Serializable(with = InstantSerializer::class) val tanggalDibuat: Instant,
    @SerialName("jadwal_pengiriman") @Serializable(with = InstantSerializer::class) val jadwalPengiriman: Instant,
    @SerialName("status") @Serializable(with = StatusNotifikasiSerializer::class) val statusNotifikasi: StatusNotifikasi,
    @SerialName("pengiriman_ke") val pengirimanKe: Int,
    @SerialName("id_pengguna") val idPengguna: Int
)