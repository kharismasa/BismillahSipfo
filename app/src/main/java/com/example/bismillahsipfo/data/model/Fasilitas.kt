package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Fasilitas(
    @SerialName("id_fasilitas") val idFasilitas: Int,
    @SerialName("nama_fasilitas") val namaFasilitas: String,
    @SerialName("deskripsi") val deskripsi: String,
    @SerialName("fasilitas_plus") val fasilitasPlus: String,
    @SerialName("photo") val photo: String?,
    @SerialName("alamat") val alamat: String,
    @SerialName("prosedur_peminjaman") val prosedurPeminjaman: String,
    @SerialName("tatatertib") val tatatertib: String,
    @SerialName("ketentuan_tarif") val ketentuanTarif: String,
    @SerialName("kontak") val kontak: String
)