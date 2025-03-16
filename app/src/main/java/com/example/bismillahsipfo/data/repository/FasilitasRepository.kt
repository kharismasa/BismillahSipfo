package com.example.bismillahsipfo.data.repository

import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.JadwalRutin
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.LapanganDipinjam
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
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

    suspend fun getPeminjamanFasilitasByFasilitasId(fasilitasId: Int): List<PeminjamanFasilitas> {
        return try {
            val response = supabaseClient.from("peminjaman_fasilitas")
                .select() {
                    filter {
                        eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<PeminjamanFasilitas>()
            response ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLapanganDipinjamByFasilitasId(fasilitasId: Int): List<LapanganDipinjam> {
        return try {
            val response = supabaseClient.from("lapangan_dipinjam")
                .select() {
                    filter {
                         eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<LapanganDipinjam>()
            response ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLapanganByFasilitasId(fasilitasId: Int): List<Lapangan> {
        return try {
            val response = supabaseClient.from("lapangan")
                .select() {
                    filter {
                         eq("id_fasilitas", fasilitasId)
                    }
                }
                .decodeList<Lapangan>()
            response ?: emptyList()
        } catch (e: Exception) {
            emptyList()
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

}
