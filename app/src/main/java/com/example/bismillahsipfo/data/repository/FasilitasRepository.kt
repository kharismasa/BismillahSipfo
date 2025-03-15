package com.example.bismillahsipfo.data.repository

import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.Fasilitas
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

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
}
