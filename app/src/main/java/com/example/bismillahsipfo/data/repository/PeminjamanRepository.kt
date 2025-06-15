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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    suspend fun uploadPdfToStorage(fileUri: Uri, originalFileName: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                // PERBAIKAN: Selalu generate nama file yang unik
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val uuid = UUID.randomUUID().toString().substring(0, 8) // Ambil 8 karakter pertama dari UUID

                // Ekstrak ekstensi dari nama file asli
                val extension = if (!originalFileName.isNullOrEmpty()) {
                    val lastDot = originalFileName.lastIndexOf('.')
                    if (lastDot != -1) originalFileName.substring(lastDot) else ".pdf"
                } else {
                    ".pdf"
                }

                // Format: surat_YYYYMMDD_HHMMSS_UUID8CHAR.extension
                val uniqueFileName = "surat_${timestamp}_${uuid}${extension}"

                Log.d("PeminjamanRepository", "Original filename: $originalFileName")
                Log.d("PeminjamanRepository", "Generated unique filename: $uniqueFileName")

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

                // Upload file dengan nama yang unik (tidak akan pernah conflict)
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