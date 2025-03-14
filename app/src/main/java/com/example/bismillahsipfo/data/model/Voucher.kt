package com.example.bismillahsipfo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import com.example.bismillahsipfo.data.serializer.BigDecimalSerializer

@Serializable
data class Voucher(
    @SerialName("id_voucher") val idVoucher: Int,
    @SerialName("kode_voucher") val kodeVoucher: String,
    @SerialName("diskon") @Serializable(with = BigDecimalSerializer::class) val diskon: BigDecimal,
    @SerialName("gambar_voucher") val gambarVoucher: String
) {
    init {
        require(diskon >= BigDecimal.ZERO && diskon <= BigDecimal("100")) {
            "Diskon harus berada antara 0 dan 100"
        }
    }
}