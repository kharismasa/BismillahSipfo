package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalTime
import com.example.bismillahsipfo.data.serializer.LocalTimeSerializer

@Serializable
data class JadwalRutin(
    @SerialName("id_jadwal_rutin") val idJadwalRutin: Int,
    @SerialName("id_fasilitas") val idFasilitas: Int,
    @SerialName("waktu_mulai") @Serializable(with = LocalTimeSerializer::class) val waktuMulai: LocalTime,
    @SerialName("waktu_selesai") @Serializable(with = LocalTimeSerializer::class) val waktuSelesai: LocalTime,
    @SerialName("hari") val hari: String,
    @SerialName("id_organisasi") val idOrganisasi: Int,
    @SerialName("list_lapangan") val listLapangan: List<Int>,
    @SerialName("tipe_jadwal") val tipeJadwal: String,
    @SerialName("urutan_slot") val urutanSlot: Int?,
)