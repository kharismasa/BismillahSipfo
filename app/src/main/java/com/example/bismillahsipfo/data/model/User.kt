package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id_pengguna") val idPengguna: Int,
    @SerialName("email") val email: String,
    @SerialName("password") val nama: String,
    @SerialName("nama") val nomorInduk: String,
    @SerialName("nomor_induk") val status: String,
    @SerialName("no_telp") val noTelp: String,
    @SerialName("id_gamifikasi") val idGamifikasi: Int,
    @SerialName("kartu_identitas") val kartuIdentitas: String,
    @SerialName("foto_profil") val fotoProfil: String
)