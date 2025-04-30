package com.example.bismillahsipfo.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Pembayaran
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDateTime

class DipinjamFasilitasRepository {
    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getPeminjamanByUserAndDate(userId: Int): List<PeminjamanFasilitas> {
        val now = LocalDateTime.now()
        
        // Ambil semua peminjaman untuk user yang sedang login
        val peminjamanList = supabase.from("peminjaman_fasilitas")
            .select(Columns.ALL) {
                filter {
                    eq("id_pengguna", userId)
                    gte("tanggal_selesai", now.toLocalDate())
                }
            }
            .decodeList<PeminjamanFasilitas>()

        // Ambil semua id pembayaran dari peminjaman tersebut
        val idPembayaranList = peminjamanList.map { it.idPembayaran }

        // Ambil pembayaran dengan status "success" untuk id pembayaran tersebut
        val successfulPayments = supabase.from("pembayaran")
            .select(Columns.ALL) {
                filter {
                    eq("status_pembayaran", "success")
                }
            }
            .decodeList<Pembayaran>()

        val result = successfulPayments.filter { pembayaran ->
            idPembayaranList.contains(pembayaran.idPembayaran)
        }

        Log.d("DipinjamFasilitasRepository", "getSuccessPembayaran berhasil. Jumlah data: ${result.size}")

        // Filter peminjaman berdasarkan pembayaran yang sukses
        val successfulPeminjamanIds = result.map { it.idPembayaran }

        return peminjamanList.filter { peminjaman ->
            successfulPeminjamanIds.contains(peminjaman.idPembayaran)
        }

    }
}