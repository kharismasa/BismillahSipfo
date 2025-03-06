package com.example.bismillahsipfo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val idPengguna: Int,
    val email: String,
    val nama: String,
    val nomorInduk: String,
    val status: String,
    val noTelp: String,
    val idGamifikasi: Int,
    val kartuIdentitas: String,
    val fotoProfil: String
)