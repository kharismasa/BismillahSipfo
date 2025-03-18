package com.example.bismillahsipfo.data.model

import com.example.bismillahsipfo.data.serializer.InstantSerializer
import com.example.bismillahsipfo.data.serializer.BigDecimalSerializer
import com.example.bismillahsipfo.data.serializer.StatusPembayaranSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.math.BigDecimal

@Serializable
enum class StatusPembayaran {
    @SerialName("success")
    SUCCESS,

    @SerialName("pending")
    PENDING,

    @SerialName("failed")
    FAILED
}

@Serializable
data class Pembayaran (
    @SerialName("id_pembayaran") val idPembayaran: String,
    @SerialName("id_voucher") val idVoucher: Int?, // Jadikan nullable
    @SerialName("metode_pembayaran") val metodePembayaran: String,
    @SerialName("status_pembayaran") @Serializable(with = StatusPembayaranSerializer::class) val statusPembayaran: StatusPembayaran,
    @SerialName("total_biaya") @Serializable(with = BigDecimalSerializer::class) val totalBiaya: BigDecimal,
    @SerialName("created_at") @Serializable(with = InstantSerializer::class) val createdAtPembayaran: Instant,
    @SerialName("waktu_kadaluwarsa") @Serializable(with = InstantSerializer::class) val waktuKadaluwarsa: Instant
)
