package com.example.bismillahsipfo.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
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

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
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
                        "email" to email
                        "password" to password
                    }
                    limit(1)
                }
                .decodeList<JsonObject>()

            if (response.isNotEmpty()) {
                val userData = response[0]
                User(
                    idPengguna = userData["id_pengguna"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    email = userData["email"]?.jsonPrimitive?.content ?: "",
                    nama = userData["nama"]?.jsonPrimitive?.content ?: "",
                    nomorInduk = userData["nomor_induk"]?.jsonPrimitive?.content ?: "",
                    status = userData["status"]?.jsonPrimitive?.content ?: "",
                    noTelp = userData["no_telp"]?.jsonPrimitive?.content ?: "",
                    idGamifikasi = userData["id_gamifikasi"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
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

    suspend fun getUser(): User? {
        return try {
            val response = supabase.from("pengguna")
                .select() {
                    filter {
                        eq("id_pengguna", getCurrentUserId())
                    }
                }
                .decodeSingle<JsonObject>()

            User(
                idPengguna = response["id_pengguna"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                email = response["email"]?.jsonPrimitive?.content ?: "",
                nama = response["nama"]?.jsonPrimitive?.content ?: "",
                nomorInduk = response["nomor_induk"]?.jsonPrimitive?.content ?: "",
                status = response["status"]?.jsonPrimitive?.content ?: "",
                noTelp = response["no_telp"]?.jsonPrimitive?.content ?: "",
                idGamifikasi = response["id_gamifikasi"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                kartuIdentitas = response["kartu_identitas"]?.jsonPrimitive?.content ?: "",
                fotoProfil = response["foto_profil"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateNoTelp(newNoTelp: String) {
        try {
            supabase.from("pengguna")
                .update(
                    {
                        set("no_telp", newNoTelp)
                    }
                ) {
                    filter {
                        eq("id_pengguna", getCurrentUserId())
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error, mungkin throw custom exception
        }
    }

    suspend fun updateKartuIdentitas(newUrl: String) {
        try {
            supabase.from("pengguna")
                .update(
                    {
                        set("kartu_identitas", newUrl)
                    }
                ) {
                    filter {
                        eq("id_pengguna", getCurrentUserId())
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error, mungkin throw custom exception
        }
    }

    fun getSupabaseClient(): SupabaseClient {
        return supabase
    }

//    private fun getCurrentUserId(): Int {
//        // Implementasi untuk mendapatkan ID pengguna saat ini
//        // Misalnya, dari SharedPreferences
//        val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//        return sharedPref.getInt("USER_ID", -1)
//    }

    private fun getCurrentUserId(): Int {
        return sharedPreferences.getInt("USER_ID", -1)
    }

    // Fungsi untuk menyimpan ID pengguna saat login
    fun saveUserId(userId: Int) {
        sharedPreferences.edit().putInt("USER_ID", userId).apply()
    }

    // Fungsi untuk menghapus ID pengguna saat logout
    fun clearUserData() {
        sharedPreferences.edit().apply {
            clear() // Menghapus semua data
            apply()
        }
    }

}
