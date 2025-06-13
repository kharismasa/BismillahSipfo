package com.example.bismillahsipfo.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.bismillahsipfo.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PeminjamanRepository(private val context: Context) {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }

    /**
     * Upload PDF file ke Supabase Storage bucket "surat"
     * User bisa ganti file kapan saja sebelum bayar, file akan di-overwrite
     * @param fileUri URI file yang akan diupload
     * @param fileName nama file (opsional, akan generate UUID jika null)
     * @return URL public file yang sudah diupload atau null jika gagal
     */
    suspend fun uploadPdfToStorage(fileUri: Uri, fileName: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Generate unique filename jika tidak disediakan
                val uniqueFileName = fileName ?: "${UUID.randomUUID()}.pdf"

                Log.d("PeminjamanRepository", "Starting upload for file: $uniqueFileName")

                // Baca file dari URI
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) {
                    Log.e("PeminjamanRepository", "Failed to read file bytes")
                    return@withContext null
                }

                Log.d("PeminjamanRepository", "File size: ${bytes.size} bytes")

                // Validasi ukuran file (max 1000KB)
                val maxSizeBytes = 1000 * 1024 // 1000KB
                if (bytes.size > maxSizeBytes) {
                    Log.e("PeminjamanRepository", "File size exceeds limit: ${bytes.size} > $maxSizeBytes")
                    return@withContext null
                }

                // Upload ke storage bucket "surat"
                val bucket = supabase.storage["surat"]

                // Upload file (akan overwrite jika file dengan nama sama sudah ada)
                bucket.upload(uniqueFileName, bytes)

                Log.d("PeminjamanRepository", "File uploaded successfully: $uniqueFileName")

                // Get public URL
                val publicUrl = bucket.publicUrl(uniqueFileName)
                Log.d("PeminjamanRepository", "Public URL: $publicUrl")

                return@withContext publicUrl

            } catch (e: Exception) {
                Log.e("PeminjamanRepository", "Error uploading file: ${e.message}", e)
                return@withContext null
            }
        }
    }
}