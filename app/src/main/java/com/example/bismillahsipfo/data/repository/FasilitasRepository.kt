package com.example.bismillahsipfo.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import com.example.bismillahsipfo.data.model.JadwalRutin
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.LapanganDipinjam
import com.example.bismillahsipfo.data.model.Pembayaran
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.StatusPembayaran
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getJadwalRutinByFasilitasId(fasilitasId: Int): List<JadwalRutin> {
        return try {
            val response = supabaseClient.from("jadwal_rutin")
                .select() {
                    filter {
                         eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<JadwalRutin>()
            response ?: emptyList()
        } catch (e: Exception) {
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
    
            val resultList = mutableListOf<JadwalPeminjamanItem>()
    
            for (peminjaman in peminjamanList) {
                val pembayaran = pembayaranList.find { it.idPembayaran == peminjaman.idPembayaran }
                println("DEBUG: Pembayaran untuk peminjaman ${peminjaman.idPeminjaman}: ${pembayaran?.statusPembayaran}")
                if (pembayaran?.statusPembayaran == StatusPembayaran.SUCCESS) {
                    val tanggalMulai = peminjaman.tanggalMulai
                    val tanggalSelesai = if (peminjaman.tanggalSelesai.isBefore(tanggalMulai)) tanggalMulai else peminjaman.tanggalSelesai
    
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
    
            println("DEBUG: Total jadwal peminjaman yang ditemukan: ${resultList.size}")
            return resultList.sortedBy { it.tanggal }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

}
