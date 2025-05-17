package com.example.bismillahsipfo.data.repository

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
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun getFasilitas(): List<Fasilitas> {
        return try {
            val response = supabaseClient.from("fasilitas")
                .select()
                .decodeList<Fasilitas>()
            response ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFasilitasById(id: Int): Fasilitas? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient.from("fasilitas")
                    .select {
                        filter {
                            eq("id_fasilitas", id)
                        }
                        limit(1)
                    }
                    .decodeSingle<Fasilitas>()

                response
            } catch (e: Exception) {
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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
                        val tanggalRange = tanggalMulai.datesUntil(tanggalSelesai.plusDays(1)).toList()

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

        // Ambil semua pembayaran dengan status "success"
        val successfulPembayaran = supabaseClient.from("pembayaran")
            .select(){
                filter {
                    eq("status_pembayaran", "success")
                }
            }
            .decodeList<Pembayaran>()
        Log.d("FasilitasRepository", "Successful pembayaran fetched. Size: ${successfulPembayaran.size}")

        // Ambil peminjaman fasilitas dan filter berdasarkan pembayaran yang sukses
        val allPeminjaman = supabaseClient.from("peminjaman_fasilitas")
            .select(){
                filter {
                    eq("id_fasilitas", idFasilitas)
                }
            }
            .decodeList<PeminjamanFasilitas>()

        val peminjaman = allPeminjaman.filter { peminjaman ->
            successfulPembayaran.any { it.idPembayaran == peminjaman.idPembayaran }
        }
        Log.d("FasilitasRepository", "Filtered peminjaman fetched. Size: ${peminjaman.size}")

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
        if (hariLibur.any { it.dateHariLibur in startDate..endDate }) {
            return JadwalAvailabilityStatus.HOLIDAY
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

        // Check for jadwal rutin
        val jadwalRutin = supabaseClient.from("jadwal_rutin")
            .select() {
                filter {
                    eq("id_fasilitas", idFasilitas)
                }
            }
            .decodeList<JadwalRutin>()

        if (jadwalRutin.any { jadwal ->
                startDate.dayOfWeek.getIndonesianName() == jadwal.hari &&
                        ((startTime <= jadwal.waktuMulai && jadwal.waktuMulai < endTime) ||
                                (startTime < jadwal.waktuSelesai && jadwal.waktuSelesai <= endTime))
            }) {
            return JadwalAvailabilityStatus.CONFLICT_WITH_JADWAL_RUTIN
        }

        return JadwalAvailabilityStatus.AVAILABLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJadwalTersediaForFasilitas30(): List<JadwalTersedia> {
        val hariLibur = getHariLibur()
        val today = LocalDate.now()
        val startDate = today.plusDays(7) // Mulai dari 7 hari setelah hari ini
        val endDate = today.plusWeeks(12)

        // Ambil semua peminjaman yang sudah ada untuk fasilitas 30
        val existingPeminjaman = supabaseClient.from("peminjaman_fasilitas")
            .select() {
                filter {
                    eq("id_fasilitas", 30)
                    gte("tanggal_mulai", startDate)
                    lte("tanggal_mulai", endDate)
                }
            }
            .decodeList<PeminjamanFasilitas>()

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
        return existingPeminjaman.none { peminjaman ->
            peminjaman.tanggalMulai <= slot.tanggal && slot.tanggal!! <= peminjaman.tanggalSelesai &&
                    ((peminjaman.jamMulai <= slot.waktuMulai && slot.waktuMulai!! < peminjaman.jamSelesai) ||
                            (peminjaman.jamMulai < slot.waktuSelesai && slot.waktuSelesai!! <= peminjaman.jamSelesai) ||
                            (slot.waktuMulai!! <= peminjaman.jamMulai && peminjaman.jamSelesai <= slot.waktuSelesai))
        }
    }

}
