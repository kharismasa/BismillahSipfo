package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organisasi (
    @SerialName("id_organisasi") val idOrganisasi: Int,
    @SerialName("nama_organisasi") val namaOrganisasi: String,
)