package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id_pengguna") val idPengguna: Int,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("nama") val nama: String,
    @SerialName("nomor_induk") val nomorInduk: String,
    @SerialName("status") val status: String,
    @SerialName("no_telp") val noTelp: String,
    @SerialName("kartu_identitas") val kartuIdentitas: String,
    @SerialName("foto_profil") val fotoProfil: String
)