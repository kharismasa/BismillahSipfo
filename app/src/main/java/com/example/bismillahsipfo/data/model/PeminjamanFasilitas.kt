package com.example.bismillahsipfo.data.model

import LocalDateSerializer
import com.example.bismillahsipfo.data.serializer.InstantSerializer
import com.example.bismillahsipfo.data.serializer.LocalTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Serializable
enum class PenggunaKhusus(val description: String) {
    @SerialName("Internal UII")
    INTERNAL_UII("Internal UII"),

    @SerialName("Internal UII vs Team Eksternal")
    INTERNAL_VS_EKSTERNAL("Internal UII vs Team Eksternal"),

    @SerialName("Team Eksternal")
    EKSTERNAL_UII("Team Eksternal")
}

@Serializable
// tabel "peminjaman_fasilitas"
data class PeminjamanFasilitas(
    @SerialName("id_peminjaman") val idPeminjaman: Int? = null,
    @SerialName("id_fasilitas") val idFasilitas: Int,
    @SerialName("tanggal_mulai") @Serializable(with = LocalDateSerializer::class) val tanggalMulai: LocalDate,
    @SerialName("tanggal_selesai") @Serializable(with = LocalDateSerializer::class) val tanggalSelesai: LocalDate,
    @SerialName("jam_mulai") @Serializable(with = LocalTimeSerializer::class) val jamMulai: LocalTime,
    @SerialName("jam_selesai") @Serializable(with = LocalTimeSerializer::class) val jamSelesai: LocalTime,
    @SerialName("nama_organisasi") val namaOrganisasi: String,
    @SerialName("nama_acara") val namaAcara: String,
    @SerialName("id_pembayaran") val idPembayaran: String,
    @SerialName("pengguna_khusus") val penggunaKhusus: PenggunaKhusus?, // Jadikan nullable
    @SerialName("id_pengguna") val idPengguna: Int,
    @SerialName("created_at") @Serializable(with = InstantSerializer::class) val createdAtPeminjaman: Instant,
    @SerialName("surat_peminjaman_url") val suratPeminjamanUrl: String? = null
)
