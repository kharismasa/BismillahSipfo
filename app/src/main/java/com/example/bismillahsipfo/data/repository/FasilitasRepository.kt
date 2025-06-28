package com.example.bismillahsipfo.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.HariLibur
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import com.example.bismillahsipfo.data.model.JadwalRutin
import com.example.bismillahsipfo.data.model.JadwalRutinWithOrganisasi
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.LapanganDipinjam
import com.example.bismillahsipfo.data.model.Organisasi
import com.example.bismillahsipfo.data.model.Pembayaran
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.RiwayatSelesai
import com.example.bismillahsipfo.data.model.StatusPembayaran
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.example.bismillahsipfo.utils.DebugHelper
import com.google.gson.Gson
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class FasilitasRepository {

    private val supabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }

    // Update method getFasilitas dengan logging
    suspend fun getFasilitas(): List<Fasilitas> {
        return try {
            DebugHelper.logDatabaseQuery("FasilitasRepository", "SELECT", "fasilitas", "Starting query...")
            DebugHelper.logNetworkRequest("FasilitasRepository", "${BuildConfig.BASE_URL}/rest/v1/fasilitas")

            val response = supabaseClient.from("fasilitas")
                .select()
                .decodeList<Fasilitas>()

            DebugHelper.logDatabaseQuery("FasilitasRepository", "SELECT", "fasilitas", "Found ${response?.size ?: 0} items")
            DebugHelper.logNetworkResponse("FasilitasRepository", "fasilitas", true, 200, "Success")

            response ?: emptyList()
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "❌ Error fetching fasilitas: ${e.message}", e)
            DebugHelper.logNetworkResponse("FasilitasRepository", "fasilitas", false, -1, e.message ?: "Unknown error")
            emptyList()
        }
    }

    // Update method getFasilitasById dengan logging
    suspend fun getFasilitasById(id: Int): Fasilitas? {
        return withContext(Dispatchers.IO) {
            try {
                DebugHelper.logDatabaseQuery("FasilitasRepository", "SELECT", "fasilitas", "Getting ID: $id")

                val response = supabaseClient.from("fasilitas")
                    .select {
                        filter {
                            eq("id_fasilitas", id)
                        }
                        limit(1)
                    }
                    .decodeSingle<Fasilitas>()

                DebugHelper.logDatabaseQuery("FasilitasRepository", "SELECT", "fasilitas", "Found: ${response?.namaFasilitas ?: "null"}")
                response
            } catch (e: Exception) {
                Log.e("FasilitasRepository", "❌ Error fetching fasilitas by ID $id: ${e.message}", e)
                null
            }
        }
    }

    suspend fun getJadwalRutinByFasilitasId(fasilitasId: Int): List<JadwalRutinWithOrganisasi> {
        return try {
            val jadwalRutinList = supabaseClient.from("jadwal_rutin")
                .select() {
                    filter {
                        eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<JadwalRutin>()

            Log.d("FasilitasRepository", "Jadwal Rutin fetched: ${jadwalRutinList.size}")

            val organisasiList = supabaseClient.from("organisasi")
                .select()
                .decodeList<Organisasi>()

            Log.d("FasilitasRepository", "Organisasi fetched: ${organisasiList.size}")

            val allLapanganIds = jadwalRutinList.flatMap { it.listLapangan }.distinct()
            Log.d("FasilitasRepository", "All Lapangan IDs: $allLapanganIds")
            val lapanganMap = getLapanganByIds(allLapanganIds).associateBy { it.idLapangan }
            Log.d("FasilitasRepository", "Lapangan Map: ${lapanganMap.keys}")

            val result = jadwalRutinList.map { jadwalRutin ->
                val organisasi = organisasiList.find { it.idOrganisasi == jadwalRutin.idOrganisasi }
                val lapanganNames = jadwalRutin.listLapangan.mapNotNull { lapanganMap[it]?.namaLapangan }
                Log.d("FasilitasRepository", "Jadwal Rutin ${jadwalRutin.idJadwalRutin}: Lapangan IDs = ${jadwalRutin.listLapangan}, Names = $lapanganNames, Tipe Jadwal = ${jadwalRutin.tipeJadwal}, Urutan Slot = ${jadwalRutin.urutanSlot}")
                JadwalRutinWithOrganisasi(jadwalRutin, organisasi?.namaOrganisasi ?: "", lapanganNames)
            }

            Log.d("FasilitasRepository", "JadwalRutinWithOrganisasi created: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error fetching jadwal rutin: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getLapanganByIds(ids: List<Int>): List<Lapangan> {
        Log.d("FasilitasRepository", "Fetching lapangan for ids: $ids")
        return try {
            val lapangan = supabaseClient.from("lapangan")
                .select()
                .decodeList<Lapangan>()
            Log.d("FasilitasRepository", "Fetched ${lapangan.size} lapangan")
            lapangan
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error fetching lapangan: ${e.message}")
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJadwalPeminjaman(fasilitasId: Int): List<JadwalPeminjamanItem> {
        try {
            val peminjamanList = supabaseClient.from("peminjaman_fasilitas")
                .select() {
                    filter {
                        eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<PeminjamanFasilitas>()

            val pembayaranList = supabaseClient.from("pembayaran")
                .select()
                .decodeList<Pembayaran>()

            val lapanganDipinjam = supabaseClient.from("lapangan_dipinjam").select().decodeList<LapanganDipinjam>()
            val lapanganList = supabaseClient.from("lapangan").select().decodeList<Lapangan>()

            val today = LocalDate.now()
            val resultList = mutableListOf<JadwalPeminjamanItem>()

            for (peminjaman in peminjamanList) {
                val pembayaran = pembayaranList.find { it.idPembayaran == peminjaman.idPembayaran }
                if (pembayaran?.statusPembayaran == StatusPembayaran.SUCCESS) {
                    val tanggalMulai = maxOf(peminjaman.tanggalMulai, today)
                    val tanggalSelesai = peminjaman.tanggalSelesai

                    if (tanggalSelesai >= today) {
                        // ✅ SOLUSI: Manual loop instead of datesUntil()
                        val tanggalRange = generateDateRange(tanggalMulai, tanggalSelesai)

                        val lapanganIds = lapanganDipinjam.filter { it.idPeminjaman == peminjaman.idPeminjaman }.map { it.idLapangan }
                        val namaLapangan = lapanganList.filter { it.idLapangan in lapanganIds }.map { it.namaLapangan }

                        tanggalRange.forEach { tanggal ->
                            resultList.add(
                                JadwalPeminjamanItem(
                                    tanggal = tanggal,
                                    jamMulai = peminjaman.jamMulai,
                                    jamSelesai = peminjaman.jamSelesai,
                                    namaOrganisasi = peminjaman.namaOrganisasi,
                                    namaLapangan = namaLapangan
                                )
                            )
                        }
                    }
                }
            }

            println("DEBUG: Total jadwal peminjaman yang ditemukan: ${resultList.size}")
            return resultList.sortedBy { it.tanggal }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // ✅ TAMBAHKAN helper function ini di FasilitasRepository
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dateList = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            dateList.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        return dateList
    }

    // Get Peminjaman data filtered by status 'SUCCESS' or 'FAILED'
    suspend fun getRiwayatPeminjamanSelesai(idPengguna: Int): List<RiwayatSelesai> {
        return try {
            val peminjamanList = supabaseClient.from("peminjaman_fasilitas")
                .select(){
                    filter {
                        eq("id_pengguna", idPengguna)
                    }
                }
                .decodeList<PeminjamanFasilitas>()

            val pembayaranList = supabaseClient.from("pembayaran")
                .select()
                .decodeList<Pembayaran>()

            val fasilitasList = supabaseClient.from("fasilitas")
                .select()
                .decodeList<Fasilitas>()

            val resultList = mutableListOf<RiwayatSelesai>()

            for (peminjaman in peminjamanList) {
                val pembayaran = pembayaranList.find { it.idPembayaran == peminjaman.idPembayaran }
                val fasilitas = fasilitasList.find { it.idFasilitas == peminjaman.idFasilitas }

                if (fasilitas != null && pembayaran != null && pembayaran.statusPembayaran == StatusPembayaran.SUCCESS) {
                    resultList.add(
                        RiwayatSelesai(
                            tanggalMulai = peminjaman.tanggalMulai,
                            tanggalSelesai = peminjaman.tanggalSelesai,
                            namaFasilitas = fasilitas.namaFasilitas,
                            namaAcara = peminjaman.namaAcara,
                            jamMulai = peminjaman.jamMulai,
                            jamSelesai = peminjaman.jamSelesai
                        )
                    )
                }
            }

            Log.d("FasilitasRepository", "Riwayat Selesai: ${resultList.size} item")
            resultList
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error in getRiwayatPeminjamanSelesai: ${e.message}")
            emptyList()
        }
    }

    // Get Fasilitas data filtered by 'SUCCESS' or 'FAILED' status
    suspend fun getFasilitasListForSelesai(): List<Fasilitas> {
        return try {
            val fasilitasList = supabaseClient.from("fasilitas")
                .select()
                .decodeList<Fasilitas>()
            fasilitasList
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get Pembayaran data for 'PENDING' status
    suspend fun getPembayaranListForPending(): List<Pembayaran> {
        return try {
            val pembayaranList = supabaseClient.from("pembayaran")
                .select()
                .decodeList<Pembayaran>()
            val pendingPembayaran = pembayaranList.filter { it.statusPembayaran == StatusPembayaran.PENDING }
            Log.d("FasilitasRepository", "Pembayaran Pending: ${pendingPembayaran.size} item")
            pendingPembayaran
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error in getPembayaranListForPending: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPendingPembayaran(idPengguna: Int): List<Pembayaran> {
        Log.d("FasilitasRepository", "Memulai getPendingPembayaran untuk idPengguna: $idPengguna")
        return try {
            val peminjamanList = supabaseClient.from("peminjaman_fasilitas")
                .select(){
                    filter {
                        eq("id_pengguna", idPengguna)
                    }
                }
                .decodeList<PeminjamanFasilitas>()

            val idPembayaranList = peminjamanList.map { it.idPembayaran }

            val allPendingPembayaran = supabaseClient.from("pembayaran")
                .select(){
                    filter {
                        eq("status_pembayaran", "pending")
                    }
                }
                .decodeList<Pembayaran>()

            // Filter pembayaran berdasarkan idPembayaran yang ada dalam idPembayaranList
            val result = allPendingPembayaran.filter { pembayaran ->
                idPembayaranList.contains(pembayaran.idPembayaran)
            }

            Log.d("FasilitasRepository", "getPendingPembayaran berhasil. Jumlah data: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error in getPendingPembayaran: ${e.message}")
            Log.e("FasilitasRepository", "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    suspend fun getPendingAndFailedPembayaran(idPengguna: Int): List<Pembayaran> {
        Log.d("FasilitasRepository", "Memulai getPendingAndFailedPembayaran untuk idPengguna: $idPengguna")
        return try {
            val peminjamanList = supabaseClient.from("peminjaman_fasilitas")
                .select(){
                    filter {
                        eq("id_pengguna", idPengguna)
                    }
                }
                .decodeList<PeminjamanFasilitas>()

            val idPembayaranList = peminjamanList.map { it.idPembayaran }

            val allPendingAndFailedPembayaran = supabaseClient.from("pembayaran")
                .select(){
                    filter {
                        or {
                            eq("status_pembayaran", "pending")
                            eq("status_pembayaran", "failed")
                        }
                    }
                }
                .decodeList<Pembayaran>()

            // Filter pembayaran berdasarkan idPembayaran yang ada dalam idPembayaranList
            val result = allPendingAndFailedPembayaran.filter { pembayaran ->
                idPembayaranList.contains(pembayaran.idPembayaran)
            }

            Log.d("FasilitasRepository", "getPendingAndFailedPembayaran berhasil. Jumlah data: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error in getPendingAndFailedPembayaran: ${e.message}")
            Log.e("FasilitasRepository", "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    // Method untuk regenerate Midtrans token untuk pembayaran ulang
    suspend fun regenerateMidtransToken(paymentId: String): Pair<Boolean, String?> {
        return try {
            val requestMap = HashMap<String, Any>()
            requestMap["generate_midtrans_token"] = true
            requestMap["payment_id"] = paymentId

            val gson = Gson()
            val jsonString = gson.toJson(requestMap)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val apiService = RetrofitClient.createService(ApiService::class.java)
            val response = apiService.createTransaction(
                url = "midtrans-sipfo",
                authHeader = "Bearer ${BuildConfig.API_KEY}",
                requestBody = requestBody
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)
                    if (success) {
                        val redirectUrl = jsonResponse.optString("redirect_url", null)
                        return Pair(true, redirectUrl)
                    }
                }
            }

            Pair(false, null)
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error regenerating midtrans token: ${e.message}")
            Pair(false, null)
        }
    }

    suspend fun getPeminjamanByIdPembayaran(idPembayaran: String): PeminjamanFasilitas? {
        Log.d("FasilitasRepository", "Memulai getPeminjamanByIdPembayaran untuk id: $idPembayaran")
        return try {
            val result = supabaseClient.from("peminjaman_fasilitas")
                .select(){
                    filter {
                        eq("id_pembayaran", idPembayaran)
                    }
                    limit(1)
                }
                .decodeSingle<PeminjamanFasilitas>()
            Log.d("FasilitasRepository", "getPeminjamanByIdPembayaran berhasil untuk id: $idPembayaran")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error in getPeminjamanByIdPembayaran: ${e.message}")
            null
        }
    }

    suspend fun getLapanganByFasilitasId(idFasilitas: Int): List<Lapangan> {
        return try {
            supabaseClient.from("lapangan")
                .select() {
                    filter {
                        eq("id_fasilitas", idFasilitas)
                    }
                }
                .decodeList<Lapangan>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertPeminjamanFasilitas(peminjamanFasilitas: PeminjamanFasilitas): Int {
        return try {
            val result = supabaseClient.from("peminjaman_fasilitas")
                .insert(peminjamanFasilitas) {
                    select()
                }
                .decodeSingle<PeminjamanFasilitas>()

            result.idPeminjaman
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error inserting PeminjamanFasilitas: ${e.message}")
            -1
        }!!
    }

    suspend fun insertLapanganDipinjam(idPeminjaman: Int, idLapangan: Int) {
        try {
            supabaseClient.from("lapangan_dipinjam")
                .insert(LapanganDipinjam(0, idPeminjaman, idLapangan))
                
        } catch (e: Exception) {
            // Handle error
        }
    }

    //Untuk Form Peminjaman
    suspend fun getOrganisasiListByFasilitasId(idFasilitas: Int): List<Organisasi> {
        return try {
            Log.d("FasilitasRepository", "Fetching organisasi list for fasilitas ID: $idFasilitas")
            val jadwalRutinList = supabaseClient.from("jadwal_rutin")
                .select() {
                    filter {
                        eq("id_fasilitas", idFasilitas)
                    }
                }
                .decodeList<JadwalRutin>()
            Log.d("FasilitasRepository", "Jadwal rutin fetched: ${jadwalRutinList.size}")

            val organisasiIds = jadwalRutinList.map { it.idOrganisasi }.distinct()
            Log.d("FasilitasRepository", "Unique organisasi IDs: $organisasiIds")

            if (organisasiIds.isEmpty()) {
                return emptyList()
            }

            val organisasiList = supabaseClient.from("organisasi")
                .select() {
                    filter {
                        or {
                            organisasiIds.forEach { id ->
                                eq("id_organisasi", id)
                            }
                        }
                    }
                }
                .decodeList<Organisasi>()
            Log.d("FasilitasRepository", "Organisasi fetched: ${organisasiList.size}")

            Log.d("FasilitasRepository", "Final organisasi list: $organisasiList")
            organisasiList
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error fetching organisasi list by fasilitas: ${e.message}")
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJadwalTersedia(idFasilitas: Int, idOrganisasi: Int): List<JadwalTersedia> {
        Log.d("FasilitasRepository", "Getting jadwal tersedia for idFasilitas: $idFasilitas, idOrganisasi: $idOrganisasi")

        // Mengambil jadwal rutin
        val jadwalRutin = supabaseClient.from("jadwal_rutin")
            .select(){
                filter {
                    eq("id_fasilitas", idFasilitas)
                    eq("id_organisasi", idOrganisasi)
                }
            }
            .decodeList<JadwalRutin>()
        Log.d("FasilitasRepository", "Jadwal rutin fetched. Size: ${jadwalRutin.size}")

        // PERBAIKAN: Ambil pembayaran dengan status "success" DAN "pending"
        val activePembayaran = supabaseClient.from("pembayaran")
            .select(){
                filter {
                    or {
                        eq("status_pembayaran", "success")
                        eq("status_pembayaran", "pending")
                    }
                }
            }
            .decodeList<Pembayaran>()
        Log.d("FasilitasRepository", "Active pembayaran (success + pending) fetched. Size: ${activePembayaran.size}")

        // PERBAIKAN: Filter peminjaman berdasarkan pembayaran yang success ATAU pending
        val allPeminjaman = supabaseClient.from("peminjaman_fasilitas")
            .select(){
                filter {
                    eq("id_fasilitas", idFasilitas)
                }
            }
            .decodeList<PeminjamanFasilitas>()

        val peminjaman = allPeminjaman.filter { peminjaman ->
            activePembayaran.any { it.idPembayaran == peminjaman.idPembayaran }
        }
        Log.d("FasilitasRepository", "Filtered peminjaman (success + pending) fetched. Size: ${peminjaman.size}")

        // Log pembayaran yang dikecualikan untuk debugging
        val excludedPayments = peminjaman.map { p ->
            val payment = activePembayaran.find { it.idPembayaran == p.idPembayaran }
            "${p.idPembayaran} (${payment?.statusPembayaran})"
        }
        Log.d("FasilitasRepository", "Excluded payments: $excludedPayments")

        // Mengambil data hari libur
        val hariLibur = getHariLibur()
        Log.d("FasilitasRepository", "Hari libur fetched. Size: ${hariLibur.size}")

        // Menentukan rentang tanggal
        val today = LocalDate.now()
        val startDate = today.plusDays(7)
        var endDate = startDate.plusWeeks(36)
        val minDate = today.plusDays(7)

        val result = mutableListOf<JadwalTersedia>()

        if (idFasilitas == 30) {
            // Logika khusus untuk fasilitas dengan ID 30
            val allJadwalRutin = supabaseClient.from("jadwal_rutin")
                .select(){
                    filter {
                        eq("id_fasilitas", idFasilitas)
                    }
                }
                .decodeList<JadwalRutin>()
            Log.d("FasilitasRepository", "All Jadwal rutin fetched for fasilitas 30. Size: ${allJadwalRutin.size}")

            val cycleStartDate = LocalDate.of(2025, 4, 1)
            var currentDate = cycleStartDate
            var currentSlot = 1

            while (result.size < 9) {
                val dayOfWeek = currentDate.dayOfWeek
                if (dayOfWeek in DayOfWeek.TUESDAY..DayOfWeek.FRIDAY) {
                    val matchingJadwal = allJadwalRutin.find { it.urutanSlot == currentSlot }
                    if (matchingJadwal != null && matchingJadwal.idOrganisasi == idOrganisasi) {
                        val isHoliday = hariLibur.any { it.dateHariLibur == currentDate }

                        // PERBAIKAN: Check conflict dengan peminjaman yang memiliki status success/pending
                        val conflictingPeminjaman = peminjaman.any { p ->
                            p.tanggalMulai <= currentDate && currentDate <= p.tanggalSelesai
                        }
                        if (!isHoliday && !conflictingPeminjaman && currentDate >= minDate) {
                            val newJadwal = JadwalTersedia(
                                hari = dayOfWeek.getIndonesianName(),
                                tanggal = currentDate,
                                waktuMulai = matchingJadwal.waktuMulai,
                                waktuSelesai = matchingJadwal.waktuSelesai,
                                listLapangan = matchingJadwal.listLapangan,
                                tipeJadwal = matchingJadwal.tipeJadwal,
                                urutanSlot = currentSlot,
                                isHoliday = false
                            )
                            result.add(newJadwal)

                            // Log untuk hari, tanggal, dan nama organisasi
                            val organisasi = supabaseClient.from("organisasi")
                                .select() {
                                    filter {
                                        eq("id_organisasi", matchingJadwal.idOrganisasi)
                                    }
                                }
                                .decodeSingle<Organisasi>()
                            Log.d("FasilitasRepository", "Added jadwal tersedia: Hari: ${newJadwal.hari}, Tanggal: ${newJadwal.tanggal}, Organisasi: ${organisasi.namaOrganisasi}")
                        } else {
                            Log.d("FasilitasRepository", "Skipped jadwal: Hari: ${dayOfWeek.getIndonesianName()}, Tanggal: $currentDate, " +
                                    "Alasan: ${when {
                                        isHoliday -> "Hari Libur"
                                        conflictingPeminjaman -> "Peminjaman yang Konflik"
                                        currentDate < minDate -> "Kurang dari 7 hari dari hari ini"
                                        else -> "Alasan lain"
                                    }}")
                        }
                    }
                    currentSlot = if (currentSlot == 16) 1 else currentSlot + 1
                }
                currentDate = currentDate.plusDays(1)
            }
        } else {
            // Logika untuk fasilitas lainnya (tetap sama seperti sebelumnya)
            var currentDate = startDate
            while (currentDate <= endDate && result.size < 9) {
                for (jadwal in jadwalRutin) {
                    if (result.size >= 9) break
                    val dayOfWeekIndonesia = currentDate.dayOfWeek.getIndonesianName()
                    if (dayOfWeekIndonesia == jadwal.hari) {
                        val isHoliday = hariLibur.any { it.dateHariLibur == currentDate }
                        if (!isHoliday) {
                            val conflictingPeminjaman = peminjaman.any { p ->
                                p.tanggalMulai <= currentDate && currentDate <= p.tanggalSelesai &&
                                        ((p.jamMulai <= jadwal.waktuMulai && jadwal.waktuMulai < p.jamSelesai) ||
                                                (p.jamMulai < jadwal.waktuSelesai && jadwal.waktuSelesai <= p.jamSelesai))
                            }
                            if (!conflictingPeminjaman) {
                                val newJadwal = JadwalTersedia(
                                    hari = jadwal.hari,
                                    tanggal = currentDate,
                                    waktuMulai = jadwal.waktuMulai,
                                    waktuSelesai = jadwal.waktuSelesai,
                                    listLapangan = jadwal.listLapangan,
                                    tipeJadwal = jadwal.tipeJadwal,
                                    urutanSlot = jadwal.urutanSlot,
                                    isHoliday = false
                                )
                                result.add(newJadwal)
                                Log.d("FasilitasRepository", "Adding jadwal tersedia: ${jadwal.hari}, $currentDate, ${jadwal.waktuMulai}-${jadwal.waktuSelesai}, Tipe: ${jadwal.tipeJadwal}, Urutan: ${jadwal.urutanSlot}")
                            } else {
                                Log.d("FasilitasRepository", "Conflicting peminjaman found for: ${jadwal.hari}, $currentDate, ${jadwal.waktuMulai}-${jadwal.waktuSelesai}, Tipe: ${jadwal.tipeJadwal}, Urutan: ${jadwal.urutanSlot}")
                            }
                        } else {
                            Log.d("FasilitasRepository", "Skipping holiday: $currentDate")
                        }
                    }
                }
                currentDate = currentDate.plusDays(1)
            }
        }

        Log.d("FasilitasRepository", "Final jadwal tersedia list: $result")
        return result
    }

    // PERBAIKAN: Update method getJadwalTersediaForFasilitas30 untuk mengecualikan status pending dan success
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJadwalTersediaForFasilitas30(): List<JadwalTersedia> {
        val hariLibur = getHariLibur()
        val today = LocalDate.now()
        val startDate = today.plusDays(7) // Mulai dari 7 hari setelah hari ini
        val endDate = today.plusWeeks(12)

        // PERBAIKAN: Ambil semua pembayaran dengan status success dan pending
        val activePembayaran = supabaseClient.from("pembayaran")
            .select(){
                filter {
                    or {
                        eq("status_pembayaran", "success")
                        eq("status_pembayaran", "pending")
                    }
                }
            }
            .decodeList<Pembayaran>()
        Log.d("FasilitasRepository", "Active pembayaran (success + pending) for fasilitas 30. Size: ${activePembayaran.size}")

        // PERBAIKAN: Ambil peminjaman yang sudah ada untuk fasilitas 30 dengan status success/pending
        val allPeminjamanFasilitas30 = supabaseClient.from("peminjaman_fasilitas")
            .select() {
                filter {
                    eq("id_fasilitas", 30)
                    gte("tanggal_mulai", startDate)
                    lte("tanggal_mulai", endDate)
                }
            }
            .decodeList<PeminjamanFasilitas>()

        // Filter hanya peminjaman dengan pembayaran success/pending
        val existingPeminjaman = allPeminjamanFasilitas30.filter { peminjaman ->
            activePembayaran.any { it.idPembayaran == peminjaman.idPembayaran }
        }
        Log.d("FasilitasRepository", "Existing peminjaman with active payments for fasilitas 30. Size: ${existingPeminjaman.size}")

        // Log pembayaran yang dikecualikan untuk debugging
        val excludedPayments = existingPeminjaman.map { p ->
            val payment = activePembayaran.find { it.idPembayaran == p.idPembayaran }
            "${p.idPembayaran} (${payment?.statusPembayaran}) - ${p.tanggalMulai} to ${p.tanggalSelesai}"
        }
        Log.d("FasilitasRepository", "Excluded payments for fasilitas 30: $excludedPayments")

        return (0..ChronoUnit.DAYS.between(startDate, endDate)).mapNotNull { dayOffset ->
            val currentDate = startDate.plusDays(dayOffset)
            if (currentDate.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) &&
                !hariLibur.any { it.dateHariLibur == currentDate }) {

                val morningSlot = createJadwalTersedia(currentDate, LocalTime.of(7, 0), LocalTime.of(9, 30), "Pagi", 1)
                val eveningSlot = createJadwalTersedia(currentDate, LocalTime.of(15, 0), LocalTime.of(17, 30), "Sore", 2)

                listOfNotNull(
                    if (isSlotAvailable(morningSlot, existingPeminjaman)) morningSlot else null,
                    if (isSlotAvailable(eveningSlot, existingPeminjaman)) eveningSlot else null
                )
            } else {
                null
            }
        }.flatten()
    }

    // Extension function untuk mendapatkan nama hari dalam bahasa Indonesia
    @RequiresApi(Build.VERSION_CODES.O)
    fun DayOfWeek.getIndonesianName(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "Senin"
            DayOfWeek.TUESDAY -> "Selasa"
            DayOfWeek.WEDNESDAY -> "Rabu"
            DayOfWeek.THURSDAY -> "Kamis"
            DayOfWeek.FRIDAY -> "Jumat"
            DayOfWeek.SATURDAY -> "Sabtu"
            DayOfWeek.SUNDAY -> "Minggu"
        }
    }

    suspend fun getHariLibur(): List<HariLibur> {
        return try {
            val result = supabaseClient.from("hari_libur")
                .select()
                .decodeList<HariLibur>()
            Log.d("FasilitasRepository", "Hari libur fetched successfully. Count: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error fetching hari libur: ${e.message}")
            Log.e("FasilitasRepository", "Stack trace: ${e.stackTraceToString()}")
            // Coba ambil raw data untuk debugging
            try {
                val rawData = supabaseClient.from("hari_libur").select().decodeList<String>()
                Log.d("FasilitasRepository", "Raw hari libur data: $rawData")
            } catch (e2: Exception) {
                Log.e("FasilitasRepository", "Error fetching raw hari libur data: ${e2.message}")
            }
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun checkJadwalAvailability(idFasilitas: Int, tanggalMulai: String, tanggalSelesai: String, jamMulai: String, jamSelesai: String): JadwalAvailabilityStatus {
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val startDate = LocalDate.parse(tanggalMulai, format)
        val endDate = LocalDate.parse(tanggalSelesai, format)
        val startTime = LocalTime.parse(jamMulai)
        val endTime = LocalTime.parse(jamSelesai)

        // Check for holidays
        val hariLibur = getHariLibur()
        val holidayInRange = hariLibur.find { it.dateHariLibur in startDate..endDate }
        if (holidayInRange != null) {
            val formattedDate = holidayInRange.dateHariLibur.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
            return JadwalAvailabilityStatus.HOLIDAY(
                namaHariLibur = holidayInRange.namaHariLibur,
                tanggal = formattedDate
            )
        }

        // Check for existing peminjaman
        val existingPeminjaman = supabaseClient.from("peminjaman_fasilitas")
            .select() {
                filter {
                    eq("id_fasilitas", idFasilitas)
                    or {
                        and {
                            lte("tanggal_mulai", endDate)
                            gte("tanggal_selesai", startDate)
                        }
                    }
                }
            }
            .decodeList<PeminjamanFasilitas>()

        if (existingPeminjaman.any { peminjaman ->
                (peminjaman.tanggalMulai <= endDate && startDate <= peminjaman.tanggalSelesai) &&
                        (peminjaman.jamMulai < endTime && startTime < peminjaman.jamSelesai)
            }) {
            return JadwalAvailabilityStatus.UNAVAILABLE
        }

        // Check for jadwal rutin - UBAH BAGIAN INI
        val jadwalRutin = supabaseClient.from("jadwal_rutin")
            .select() {
                filter {
                    eq("id_fasilitas", idFasilitas)
                }
            }
            .decodeList<JadwalRutin>()

        // LANGKAH 1: Cari jadwal rutin yang konflik (gunakan find() bukan any())
        val conflictingJadwal = jadwalRutin.find { jadwal ->
            startDate.dayOfWeek.getIndonesianName() == jadwal.hari &&
                    ((startTime <= jadwal.waktuMulai && jadwal.waktuMulai < endTime) ||
                            (startTime < jadwal.waktuSelesai && jadwal.waktuSelesai <= endTime))
        }

        // LANGKAH 2: Jika ada konflik, ambil data organisasi
        if (conflictingJadwal != null) {
            // LANGKAH 3: Ambil data organisasi berdasarkan idOrganisasi
            val organisasi = try {
                supabaseClient.from("organisasi")
                    .select() {
                        filter {
                            eq("id_organisasi", conflictingJadwal.idOrganisasi)
                        }
                    }
                    .decodeSingle<Organisasi>()
            } catch (e: Exception) {
                Log.e("FasilitasRepository", "Error fetching organisasi: ${e.message}")
                null
            }

            // LANGKAH 4: Format tanggal konflik
            val formattedDate = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))

            // LANGKAH 5: Return dengan data organisasi dan tanggal
            return JadwalAvailabilityStatus.CONFLICT_WITH_JADWAL_RUTIN(
                namaOrganisasi = organisasi?.namaOrganisasi ?: "Organisasi tidak diketahui",
                tanggal = formattedDate
            )
        }

        return JadwalAvailabilityStatus.AVAILABLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createJadwalTersedia(date: LocalDate, startTime: LocalTime, endTime: LocalTime, tipeJadwal: String, urutanSlot: Int): JadwalTersedia {
        return JadwalTersedia(
            hari = date.dayOfWeek.getIndonesianName(),
            tanggal = date,
            waktuMulai = startTime,
            waktuSelesai = endTime,
            listLapangan = emptyList(),
            tipeJadwal = tipeJadwal,
            urutanSlot = urutanSlot,
            isHoliday = false
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isSlotAvailable(slot: JadwalTersedia, existingPeminjaman: List<PeminjamanFasilitas>): Boolean {
        val isAvailable = existingPeminjaman.none { peminjaman ->
            peminjaman.tanggalMulai <= slot.tanggal && slot.tanggal!! <= peminjaman.tanggalSelesai &&
                    ((peminjaman.jamMulai <= slot.waktuMulai && slot.waktuMulai!! < peminjaman.jamSelesai) ||
                            (peminjaman.jamMulai < slot.waktuSelesai && slot.waktuSelesai!! <= peminjaman.jamSelesai) ||
                            (slot.waktuMulai!! <= peminjaman.jamMulai && peminjaman.jamSelesai <= slot.waktuSelesai))
        }

        // Log untuk debugging slot availability
        if (!isAvailable) {
            val conflictingPeminjaman = existingPeminjaman.filter { peminjaman ->
                peminjaman.tanggalMulai <= slot.tanggal && slot.tanggal!! <= peminjaman.tanggalSelesai &&
                        ((peminjaman.jamMulai <= slot.waktuMulai && slot.waktuMulai!! < peminjaman.jamSelesai) ||
                                (peminjaman.jamMulai < slot.waktuSelesai && slot.waktuSelesai!! <= peminjaman.jamSelesai) ||
                                (slot.waktuMulai!! <= peminjaman.jamMulai && peminjaman.jamSelesai <= slot.waktuSelesai))
            }
            Log.d("FasilitasRepository", "Slot ${slot.tanggal} ${slot.waktuMulai}-${slot.waktuSelesai} tidak tersedia karena konflik dengan: ${conflictingPeminjaman.map { "${it.idPembayaran} (${it.tanggalMulai}-${it.tanggalSelesai} ${it.jamMulai}-${it.jamSelesai})" }}")
        }

        return isAvailable
    }

    // TAMBAHAN: Method untuk get detail status pembayaran (untuk debugging)
    suspend fun getPembayaranStatus(idPembayaran: String): StatusPembayaran? {
        return try {
            val pembayaran = supabaseClient.from("pembayaran")
                .select() {
                    filter {
                        eq("id_pembayaran", idPembayaran)
                    }
                    limit(1)
                }
                .decodeSingle<Pembayaran>()
            pembayaran.statusPembayaran
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error getting payment status for $idPembayaran: ${e.message}")
            null
        }
    }

    // Method untuk debug - mendapatkan semua peminjaman untuk user tertentu
    suspend fun getAllPeminjamanForUser(userId: Int): List<PeminjamanFasilitas> {
        return try {
            Log.d("FasilitasRepository", "🔍 Getting all peminjaman for user $userId")

            val result = supabaseClient.from("peminjaman_fasilitas")
                .select() {
                    filter {
                        eq("id_pengguna", userId)
                    }
                }
                .decodeList<PeminjamanFasilitas>()

            Log.d("FasilitasRepository", "✅ Found ${result.size} peminjaman for user $userId")
            result.forEachIndexed { index, peminjaman ->
                Log.d("FasilitasRepository", "  [$index] ID: ${peminjaman.idPeminjaman}, Payment: ${peminjaman.idPembayaran}, Fasilitas: ${peminjaman.idFasilitas}")
            }

            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "❌ Error getting peminjaman for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    // Method untuk debug - mendapatkan total count peminjaman
    suspend fun getTotalPeminjamanCount(): Int {
        return try {
            Log.d("FasilitasRepository", "🔍 Getting total peminjaman count")

            val result = supabaseClient.from("peminjaman_fasilitas")
                .select(columns = Columns.raw("count()"))

            // Untuk count query, kita perlu parsing yang berbeda
            // Alternatif: ambil semua data dan hitung
            val allData = supabaseClient.from("peminjaman_fasilitas")
                .select()
                .decodeList<PeminjamanFasilitas>()

            Log.d("FasilitasRepository", "✅ Total peminjaman count: ${allData.size}")
            allData.size
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "❌ Error getting total peminjaman count: ${e.message}", e)
            0
        }
    }

    // Method untuk debug - mendapatkan total count pembayaran
    suspend fun getTotalPembayaranCount(): Int {
        return try {
            Log.d("FasilitasRepository", "🔍 Getting total pembayaran count")

            val allData = supabaseClient.from("pembayaran")
                .select()
                .decodeList<Pembayaran>()

            Log.d("FasilitasRepository", "✅ Total pembayaran count: ${allData.size}")
            allData.forEachIndexed { index, pembayaran ->
                Log.d("FasilitasRepository", "  [$index] ID: ${pembayaran.idPembayaran}, Status: ${pembayaran.statusPembayaran}, Amount: ${pembayaran.totalBiaya}")
            }

            allData.size
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "❌ Error getting total pembayaran count: ${e.message}", e)
            0
        }
    }

    // Method untuk debug - mendapatkan semua pembayaran
    suspend fun getAllPembayaran(): List<Pembayaran> {
        return try {
            Log.d("FasilitasRepository", "🔍 Getting all pembayaran")

            val result = supabaseClient.from("pembayaran")
                .select()
                .decodeList<Pembayaran>()

            Log.d("FasilitasRepository", "✅ Found ${result.size} pembayaran total")
            result
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "❌ Error getting all pembayaran: ${e.message}", e)
            emptyList()
        }
    }

    // Method untuk debug - test query dengan berbagai filter
    suspend fun debugUserQueries(userId: Int): String {
        val results = StringBuilder()

        try {
            // Test 1: Direct peminjaman query
            val peminjaman = supabaseClient.from("peminjaman_fasilitas")
                .select() {
                    filter {
                        eq("id_pengguna", userId)
                    }
                }
                .decodeList<PeminjamanFasilitas>()
            results.append("Peminjaman for user $userId: ${peminjaman.size}\n")

            // Test 2: Get payment IDs from peminjaman
            val paymentIds = peminjaman.map { it.idPembayaran }
            results.append("Payment IDs from peminjaman: $paymentIds\n")

            // Test 3: Get pembayaran by those IDs
            if (paymentIds.isNotEmpty()) {
                val pembayaran = supabaseClient.from("pembayaran")
                    .select() {
                        filter {
                            isIn("id_pembayaran", paymentIds)
                        }
                    }
                    .decodeList<Pembayaran>()
                results.append("Pembayaran by payment IDs: ${pembayaran.size}\n")

                pembayaran.forEach { p ->
                    results.append("  - ${p.idPembayaran}: ${p.statusPembayaran}\n")
                }
            }

            // Test 4: Check specific status filters
            val pendingPembayaran = supabaseClient.from("pembayaran")
                .select() {
                    filter {
                        or {
                            eq("status_pembayaran", "pending")
                            eq("status_pembayaran", "failed")
                        }
                    }
                }
                .decodeList<Pembayaran>()
            results.append("All pending/failed pembayaran: ${pendingPembayaran.size}\n")

            // Test 5: Filter intersection
            val userPendingPembayaran = pendingPembayaran.filter { pembayaran ->
                paymentIds.contains(pembayaran.idPembayaran)
            }
            results.append("User's pending/failed pembayaran: ${userPendingPembayaran.size}\n")

        } catch (e: Exception) {
            results.append("Error in debug queries: ${e.message}\n")
        }

        val resultString = results.toString()
        Log.d("FasilitasRepository", "🧪 Debug Query Results:\n$resultString")
        return resultString
    }

}
