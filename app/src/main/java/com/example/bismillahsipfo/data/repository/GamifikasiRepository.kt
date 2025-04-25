package com.example.bismillahsipfo.data.repository

import android.content.Context
import android.util.Log
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Gamifikasi
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.Pembayaran
import com.example.bismillahsipfo.data.model.StatusPembayaran
import com.example.bismillahsipfo.data.model.User
import com.example.bismillahsipfo.data.model.Voucher
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.System.`in`

class GamifikasiRepository(private val context: Context) {

    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
    }

    suspend fun getCurrentUser(): User? {
        val userRepository = UserRepository(context)
        return userRepository.getCurrentUser()
    }

    suspend fun getGamifikasiForUser(user: User): Gamifikasi? {
        return withContext(Dispatchers.IO) {
            try {
                val response: PostgrestResult = supabase.from("gamifikasi")
                    .select(Columns.ALL) {
                        filter {
                            eq("id_gamifikasi", user.idGamifikasi)
                        }
                    }
                val gamifikasi = response.decodeSingle<Gamifikasi>()
                Log.d("GamifikasiRepository", "Gamifikasi: $gamifikasi")
                gamifikasi
            } catch (e: Exception) {
                Log.e("GamifikasiRepository", "Error getting gamifikasi: ${e.message}")
                null
            }
        }
    }

    suspend fun getTotalPembayaranForUser(user: User): Double {
        return withContext(Dispatchers.IO) {
            try {
                // Mendapatkan list peminjaman untuk pengguna
                val peminjamanList: List<PeminjamanFasilitas> = supabase.from("peminjaman_fasilitas")
                    .select(Columns.ALL) {
                        filter {
                            eq("id_pengguna", user.idPengguna)
                        }
                    }
                    .decodeList()

                // Mendapatkan list idPembayaran dari peminjamanList
                val pembayaranIds = peminjamanList.map { it.idPembayaran }

                // Mendapatkan seluruh pembayaran dengan status SUCCESS
                val pembayaranList: List<Pembayaran> = supabase.from("pembayaran")
                    .select(Columns.ALL) {
                        filter {
                            eq("status_pembayaran", "success")
                        }
                    }
                    .decodeList()

                // Filter pembayaran yang id_pembayaran-nya ada di pembayaranIds
                val filteredPembayaranList = pembayaranList.filter { pembayaran ->
                    pembayaranIds.contains(pembayaran.idPembayaran)
                }

                // Menghitung total biaya dari pembayaran yang difilter
                filteredPembayaranList.sumOf { it.totalBiaya }

                val total = filteredPembayaranList.sumOf { it.totalBiaya }
                Log.d("GamifikasiRepository", "Total pembayaran: $total")
                total
            } catch (e: Exception) {
                Log.e("GamifikasiRepository", "Error calculating total pembayaran: ${e.message}")
                0.0
            }
        }
    }

    suspend fun getNextLevelGamifikasi(currentGamifikasi: Gamifikasi): Gamifikasi? {
        return withContext(Dispatchers.IO) {
            try {
                val response: PostgrestResult = supabase.from("gamifikasi")
                    .select(Columns.ALL) {
                        filter {
                            gt("level", currentGamifikasi.level)
                        }
                        order("level", io.github.jan.supabase.postgrest.query.Order.ASCENDING) // Gunakan Order.ASC untuk ascending
                        limit(1)
                    }
                response.decodeSingle<Gamifikasi>()
            } catch (e: Exception) {
                Log.e("GamifikasiRepository", "Error getting next level gamifikasi: ${e.message}")
                null
            }
        }
    }

    suspend fun getAllVouchers(): List<Voucher> {
        return withContext(Dispatchers.IO) {
            try {
                val response: PostgrestResult = supabase.from("voucher")
                    .select(Columns.ALL)
                response.decodeList()
            } catch (e: Exception) {
                Log.e("GamifikasiRepository", "Error getting vouchers: ${e.message}")
                emptyList()
            }
        }
    }
}