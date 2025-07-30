package com.example.bismillahsipfo.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import com.example.bismillahsipfo.data.model.JadwalRutinWithOrganisasi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

object SearchFilterHelper {

    /**
     * Filter untuk Jadwal Peminjaman
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun filterJadwalPeminjaman(
        jadwalList: List<JadwalPeminjamanItem>,
        searchQuery: String = "",
        selectedDay: String = "Semua",
        selectedMonth: String = "Semua",
        selectedOrganization: String = "Semua",
        selectedTimeSlot: String = "Semua"
    ): List<JadwalPeminjamanItem> {

        var filteredList = jadwalList

        // Filter berdasarkan search query (pencarian teks bebas)
        if (searchQuery.isNotBlank()) {
            filteredList = filteredList.filter { jadwal ->
                val dateString = jadwal.tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val dayString = jadwal.tanggal.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id"))
                val monthString = jadwal.tanggal.month.getDisplayName(TextStyle.FULL, Locale("id"))
                val timeString = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"

                jadwal.namaOrganisasi.contains(searchQuery, ignoreCase = true) ||
                        dateString.contains(searchQuery) ||
                        dayString.contains(searchQuery, ignoreCase = true) ||
                        monthString.contains(searchQuery, ignoreCase = true) ||
                        timeString.contains(searchQuery) ||
                        jadwal.namaLapangan.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }

        // Filter berdasarkan hari
        if (selectedDay != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                jadwal.tanggal.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id")) == selectedDay
            }
        }

        // Filter berdasarkan bulan
        if (selectedMonth != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                jadwal.tanggal.month.getDisplayName(TextStyle.FULL, Locale("id")) == selectedMonth
            }
        }

        // Filter berdasarkan organisasi
        if (selectedOrganization != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                jadwal.namaOrganisasi == selectedOrganization
            }
        }

        // Filter berdasarkan slot waktu
        if (selectedTimeSlot != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                when (selectedTimeSlot) {
                    "Pagi (06:00-12:00)" -> jadwal.jamMulai.hour in 6..11
                    "Siang (12:00-18:00)" -> jadwal.jamMulai.hour in 12..17
                    "Malam (18:00-24:00)" -> jadwal.jamMulai.hour in 18..23
                    else -> true
                }
            }
        }

        return filteredList.sortedBy { it.tanggal }
    }

    /**
     * Filter untuk Jadwal Rutin
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun filterJadwalRutin(
        jadwalList: List<JadwalRutinWithOrganisasi>,
        searchQuery: String = "",
        selectedDay: String = "Semua",
        selectedOrganization: String = "Semua",
        selectedTimeSlot: String = "Semua"
    ): List<JadwalRutinWithOrganisasi> {

        var filteredList = jadwalList

        // Filter berdasarkan search query
        if (searchQuery.isNotBlank()) {
            filteredList = filteredList.filter { jadwal ->
                val timeString = "${jadwal.jadwalRutin.waktuMulai} - ${jadwal.jadwalRutin.waktuSelesai}"

                jadwal.namaOrganisasi.contains(searchQuery, ignoreCase = true) ||
                        jadwal.jadwalRutin.hari.contains(searchQuery, ignoreCase = true) ||
                        timeString.contains(searchQuery) ||
                        jadwal.namaLapangan.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }

        // Filter berdasarkan hari
        if (selectedDay != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                jadwal.jadwalRutin.hari == selectedDay
            }
        }

        // Filter berdasarkan organisasi
        if (selectedOrganization != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                jadwal.namaOrganisasi == selectedOrganization
            }
        }

        // Filter berdasarkan slot waktu
        if (selectedTimeSlot != "Semua") {
            filteredList = filteredList.filter { jadwal ->
                when (selectedTimeSlot) {
                    "Pagi (06:00-12:00)" -> jadwal.jadwalRutin.waktuMulai.hour in 6..11
                    "Siang (12:00-18:00)" -> jadwal.jadwalRutin.waktuMulai.hour in 12..17
                    "Malam (18:00-24:00)" -> jadwal.jadwalRutin.waktuMulai.hour in 18..23
                    else -> true
                }
            }
        }

        return filteredList.sortedBy {
            // Sort by day order (Senin, Selasa, dst)
            val dayOrder = mapOf(
                "Senin" to 1, "Selasa" to 2, "Rabu" to 3, "Kamis" to 4,
                "Jumat" to 5, "Sabtu" to 6, "Minggu" to 7
            )
            dayOrder[it.jadwalRutin.hari] ?: 8
        }
    }

    /**
     * Get unique organizations from jadwal peminjaman
     */
    fun getUniqueOrganizations(jadwalList: List<JadwalPeminjamanItem>): List<String> {
        return jadwalList.map { it.namaOrganisasi }.distinct().sorted()
    }

    /**
     * Get unique organizations from jadwal rutin
     */
    fun getUniqueOrganizationsRutin(jadwalList: List<JadwalRutinWithOrganisasi>): List<String> {
        return jadwalList.map { it.namaOrganisasi }.distinct().sorted()
    }

    /**
     * Get unique months from jadwal peminjaman
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUniqueMonths(jadwalList: List<JadwalPeminjamanItem>): List<String> {
        return jadwalList.map {
            it.tanggal.month.getDisplayName(TextStyle.FULL, Locale("id"))
        }.distinct().sorted()
    }

    /**
     * Get default filter options
     */
    fun getDayOptions(): List<String> {
        return listOf("Semua", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    }

    fun getTimeSlotOptions(): List<String> {
        return listOf("Semua", "Pagi (06:00-12:00)", "Siang (12:00-18:00)", "Malam (18:00-24:00)")
    }
}