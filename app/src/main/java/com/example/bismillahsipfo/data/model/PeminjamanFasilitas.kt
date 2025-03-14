package com.example.bismillahsipfo.data.model

import com.example.bismillahsipfo.data.serializer.InstantSerializer
import com.example.bismillahsipfo.data.serializer.LocalDateSerializer
import com.example.bismillahsipfo.data.serializer.LocalTimeSerializer
import com.example.bismillahsipfo.data.serializer.PenggunaKhususSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Serializable
enum class PenggunaKhusus(val description: String) {
    @SerialName("Internal UII (1jt)")
    INTERNAL_UII("Internal UII (1jt)"),

    @SerialName("Internal UII vs team eksternal (1.5jt)")
    INTERNAL_VS_EKSTERNAL("Internal UII vs team eksternal (1.5jt)"),

    @SerialName("Eksternal UII (2.5jt)")
    EKSTERNAL_UII("Eksternal UII (2.5jt)")
}

@Serializable
data class PeminjamanFasilitas(
    @SerialName("id_peminjaman") val idPeminjaman: Int,
    @SerialName("id_fasilitas") val idFasilitas: Int,
    @SerialName("tanggal_mulai") @Serializable(with = LocalDateSerializer::class) val tanggalMulai: LocalDate,
    @SerialName("tanggal_selesai") @Serializable(with = LocalDateSerializer::class) val tanggalSelesai: LocalDate,
    @SerialName("jam_mulai") @Serializable(with = LocalTimeSerializer::class) val jamMulai: LocalTime,
    @SerialName("jam_selesai") @Serializable(with = LocalTimeSerializer::class) val jamSelesai: LocalTime,
    @SerialName("nama_organisasi") val namaOrganisasi: String,
    @SerialName("nama_acara") val namaAcara: String,
    @SerialName("id_pembayaran") val idPembayaran: String,
    @SerialName("pengguna_khusus") @Serializable(with = PenggunaKhususSerializer::class) val penggunaKhusus: PenggunaKhusus,
    @SerialName("id_pengguna") val idPengguna: Int,
    @SerialName("created_at") @Serializable(with = InstantSerializer::class) val createdAtPeminjaman: Instant
)