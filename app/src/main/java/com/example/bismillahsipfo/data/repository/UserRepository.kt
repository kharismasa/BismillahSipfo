package com.example.bismillahsipfo.data.repository

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive


class UserRepository(private val context: Context) {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }

    suspend fun loginUser(email: String, password: String): User? {
        // Validasi email sebelum melakukan query
        if (!email.endsWith("@uii.ac.id") && !email.endsWith("@students.uii.ac.id") && !email.endsWith("@alumni.uii.ac.id")) {
            return null // Bisa juga lempar exception
        }

        return try {
            val response = supabase.from("pengguna")
                .select {
                    filter {
                        eq("email", email)
                        eq("password", password)
                    }
                    limit(1)
                }
                .decodeList<JsonObject>()

            if (response.isNotEmpty()) {
                val userData = response[0]
                User(
                    idPengguna = userData["id_pengguna"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    email = userData["email"]?.jsonPrimitive?.content ?: "",
                    password = userData["password"]?.jsonPrimitive?.content?: "",
                    nama = userData["nama"]?.jsonPrimitive?.content ?: "",
                    nomorInduk = userData["nomor_induk"]?.jsonPrimitive?.content ?: "",
                    status = userData["status"]?.jsonPrimitive?.content ?: "",
                    noTelp = userData["no_telp"]?.jsonPrimitive?.content ?: "",
                    kartuIdentitas = userData["kartu_identitas"]?.jsonPrimitive?.content ?: "",
                    fotoProfil = userData["foto_profil"]?.jsonPrimitive?.content ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateNoTelp(newNoTelp: String) {
        try {
            val response = supabase.from("pengguna")
                .update(mapOf("no_telp" to newNoTelp)) {
                    filter {
                        eq("id_pengguna", getCurrentUserId()) // Menggunakan ID pengguna yang benar
                    }
                }

            Log.d("UserRepository", "Update response: $response") // Log untuk melihat hasil response

            // Jika update berhasil, simpan perubahan di SharedPreferences
            val sharedPreferences = context.getSharedPreferences("UserPrefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("no_telp", newNoTelp) // Update nomor telepon di SharedPreferences
                apply()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UserRepository", "Error updating phone number: ${e.message}")
        }
    }

    suspend fun updateKartuIdentitas(newUrl: String) {
        try {
            val response = supabase.from("pengguna")
                .update(mapOf("kartu_identitas" to newUrl)) {
                    filter {
                        eq("id_pengguna", getCurrentUserId()) // Menggunakan ID pengguna yang benar
                    }
                }

            Log.d("UserRepository", "Kartu Identitas Update response: $response")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UserRepository", "Error updating kartu identitas: ${e.message}")
        }
    }

    fun getSupabaseClient(): SupabaseClient {
        return supabase
    }

    // Ubah visibilitas getCurrentUserId menjadi public atau buat getter
    fun getCurrentUserId(): Int {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("id_pengguna", -1)  // Ambil ID pengguna
    }

    // Fungsi untuk mendapatkan objek User berdasarkan ID pengguna yang sedang login
    suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId()
        if (userId == -1) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = supabase.from("pengguna")
                    .select{
                        filter {
                            eq("id_pengguna", userId)
                        }
                        limit(1)
                    }
                    .decodeList<User>()

                response.firstOrNull() // Mengambil user pertama atau null jika tidak ditemukan
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}
