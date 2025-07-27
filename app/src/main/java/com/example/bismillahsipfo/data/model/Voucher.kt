package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Voucher(
    @SerialName("id_voucher") val idVoucher: Int,
    @SerialName("kode_voucher") val kodeVoucher: String,
    @SerialName("diskon") val diskon: Double,
    @SerialName("gambar_voucher") val gambarVoucher: String?
) {
//    init {
//        require(diskon >= BigDecimal.ZERO && diskon <= BigDecimal("100")) {
//            "Diskon harus berada antara 0 dan 100"
//        }
//    }
}