package com.example.bismillahsipfo.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import com.example.bismillahsipfo.data.model.JadwalRutin
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.LapanganDipinjam
import com.example.bismillahsipfo.data.model.Organisasi
import com.example.bismillahsipfo.data.model.Pembayaran
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.RiwayatPending
import com.example.bismillahsipfo.data.model.RiwayatSelesai
import com.example.bismillahsipfo.data.model.StatusPembayaran
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.System.`in`
import java.time.DayOfWeek
import java.time.LocalDate

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
                Log.d("FasilitasRepository", "Jadwal Rutin ${jadwalRutin.idJadwalRutin}: Lapangan IDs = ${jadwalRutin.listLapangan}, Names = $lapanganNames")
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
        }
    }

    suspend fun insertLapanganDipinjam(idPeminjaman: Int, idLapangan: Int) {
        try {
            supabaseClient.from("lapangan_dipinjam")
                .insert(LapanganDipinjam(0, idPeminjaman, idLapangan))
                
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun getOrganisasiList(): List<String> {
        return try {
            val response = supabaseClient.from("organisasi")
                .select()
                .decodeList<Organisasi>()
            response.map { it.namaOrganisasi }
        } catch (e: Exception) {
            Log.e("FasilitasRepository", "Error fetching organisasi list: ${e.message}")
            emptyList()
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

        val jadwalRutin = supabaseClient.from("jadwal_rutin")
            .select(){
                filter {
                    eq("id_fasilitas", idFasilitas)
                    eq("id_organisasi", idOrganisasi)
                }
            }
            .decodeList<JadwalRutin>()
        Log.d("FasilitasRepository", "Jadwal rutin fetched. Size: ${jadwalRutin.size}")

        val peminjaman = supabaseClient.from("peminjaman_fasilitas")
            .select(){
                filter {
                    eq("id_fasilitas", idFasilitas)
                }
            }
            .decodeList<PeminjamanFasilitas>()
        Log.d("FasilitasRepository", "Peminjaman fetched. Size: ${peminjaman.size}")
        peminjaman.forEach { Log.d("FasilitasRepository", "Peminjaman: $it") }

        val today = LocalDate.now()
        val startDate = today.plusDays(7)
        var endDate = startDate.plusWeeks(4) // 4 minggu dari tanggal mulai

        val result = mutableListOf<JadwalTersedia>()

        var currentDate = startDate
        while (currentDate <= endDate && result.size < 9) {
            for (jadwal in jadwalRutin) {
                if (result.size >= 9) break
                val dayOfWeekIndonesia = when (currentDate.dayOfWeek) {
                    DayOfWeek.MONDAY -> "Senin"
                    DayOfWeek.TUESDAY -> "Selasa"
                    DayOfWeek.WEDNESDAY -> "Rabu"
                    DayOfWeek.THURSDAY -> "Kamis"
                    DayOfWeek.FRIDAY -> "Jumat"
                    DayOfWeek.SATURDAY -> "Sabtu"
                    DayOfWeek.SUNDAY -> "Minggu"
                }
                if (dayOfWeekIndonesia == jadwal.hari) {
                    val conflictingPeminjaman = peminjaman.any { p ->
                        p.tanggalMulai <= currentDate && currentDate <= p.tanggalSelesai &&
                                ((p.jamMulai <= jadwal.waktuMulai && jadwal.waktuMulai < p.jamSelesai) ||
                                        (p.jamMulai < jadwal.waktuSelesai && jadwal.waktuSelesai <= p.jamSelesai))
                    }
                    if (!conflictingPeminjaman) {
                        Log.d("FasilitasRepository", "Adding jadwal tersedia: ${jadwal.hari}, $currentDate, ${jadwal.waktuMulai}-${jadwal.waktuSelesai}")
                        result.add(JadwalTersedia(
                            hari = jadwal.hari,
                            tanggal = currentDate,
                            waktuMulai = jadwal.waktuMulai,
                            waktuSelesai = jadwal.waktuSelesai,
                            listLapangan = jadwal.listLapangan
                        ))
                    } else {
                        Log.d("FasilitasRepository", "Conflicting peminjaman found for: ${jadwal.hari}, $currentDate, ${jadwal.waktuMulai}-${jadwal.waktuSelesai}")
                    }
                }
            }
            currentDate = currentDate.plusDays(1)
            // Jika sudah melewati endDate tapi belum dapat 9 jadwal, reset ke startDate
            if (currentDate > endDate) {
                currentDate = startDate
                endDate = endDate.plusWeeks(4) // Tambah 4 minggu lagi
            }
        }
        Log.d("FasilitasRepository", "Final jadwal tersedia list: $result")
        return result.take(9) // Pastikan hanya mengembalikan maksimal 9 jadwal
    }
}

data class JadwalRutinWithOrganisasi(
    val jadwalRutin: JadwalRutin,
    val namaOrganisasi: String,
    val namaLapangan: List<String>
)