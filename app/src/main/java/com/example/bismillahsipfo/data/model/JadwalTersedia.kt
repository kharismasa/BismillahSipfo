package com.example.bismillahsipfo.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
data class JadwalTersedia(
    val hari: String,
    val tanggal: LocalDate?,
    val waktuMulai: LocalTime?,
    val waktuSelesai: LocalTime?,
    val listLapangan: List<Int>,
    val tipeJadwal: String?,
    val urutanSlot: Int?,
    val isHoliday: Boolean
) : Serializable {
    // Implementasi untuk menyimpan data LocalDate dan LocalTime dalam format yang Serializable
    private fun writeObject(out: java.io.ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeObject(tanggal?.toString())
        out.writeObject(waktuMulai?.toString())
        out.writeObject(waktuSelesai?.toString())
    }

    private fun readObject(input: java.io.ObjectInputStream) {
        input.defaultReadObject()
        val tanggalStr = input.readObject() as String?
        val waktuMulaiStr = input.readObject() as String?
        val waktuSelesaiStr = input.readObject() as String?

        if (tanggalStr != null) {
            // tanggal = LocalDate.parse(tanggalStr)
            // Karena tanggal adalah val, kita tidak bisa mengubahnya di sini
            // Dalam kasus ini, sebaiknya gunakan var untuk property atau buat constructor tambahan
        }
        // Sama untuk waktuMulai dan waktuSelesai
    }

    // Kita bisa menambahkan method untuk mendapatkan string yang sesuai format
    fun getTanggalFormatted(): String {
        return tanggal?.toString() ?: ""
    }

    fun getWaktuMulaiFormatted(): String {
        return waktuMulai?.toString() ?: ""
    }

    fun getWaktuSelesaiFormatted(): String {
        return waktuSelesai?.toString() ?: ""
    }
}