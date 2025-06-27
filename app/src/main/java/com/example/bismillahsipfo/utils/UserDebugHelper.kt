package com.example.bismillahsipfo.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UserDebugHelper {

    private const val TAG = "UserDebugHelper"

    fun debugUserSession(context: Context) {
        Log.d(TAG, "=== DEBUGGING USER SESSION ===")

        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Check all stored user data
        val allPrefs = sharedPreferences.all
        Log.d(TAG, "📱 SharedPreferences data:")
        allPrefs.forEach { (key, value) ->
            when (key) {
                "password" -> Log.d(TAG, "  $key: [HIDDEN]")
                "api_key" -> Log.d(TAG, "  $key: [HIDDEN]")
                else -> Log.d(TAG, "  $key: $value")
            }
        }

        // Specific checks
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
        val userId = sharedPreferences.getInt("id_pengguna", -1)
        val userName = sharedPreferences.getString("nama", null)
        val userEmail = sharedPreferences.getString("email", null)

        Log.d(TAG, "🔐 Login Status: $isLoggedIn")
        Log.d(TAG, "👤 User ID: $userId")
        Log.d(TAG, "📧 User Email: $userEmail")
        Log.d(TAG, "👤 User Name: $userName")

        // Check UserRepository
        val userRepository = UserRepository(context)
        val currentUserId = userRepository.getCurrentUserId()
        Log.d(TAG, "🏛️ UserRepository.getCurrentUserId(): $currentUserId")

        // Test database queries that depend on user ID
        CoroutineScope(Dispatchers.Main).launch {
            testUserRelatedQueries(context, currentUserId)
        }

        if (BuildConfig.DEBUG) {
            val message = if (userId == -1) {
                "⚠️ User ID is -1! Check login."
            } else {
                "✅ User ID: $userId ($userName)"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun testUserRelatedQueries(context: Context, userId: Int) {
        if (userId == -1) {
            Log.e(TAG, "❌ Cannot test queries - User ID is -1")
            return
        }

        val repository = FasilitasRepository()

        try {
            // Test 1: Pending Pembayaran
            Log.d(TAG, "🧪 Testing getPendingAndFailedPembayaran for user $userId...")
            val pendingPembayaran = repository.getPendingAndFailedPembayaran(userId)
            Log.d(TAG, "📊 Pending/Failed Pembayaran count: ${pendingPembayaran.size}")
            pendingPembayaran.forEachIndexed { index, pembayaran ->
                Log.d(TAG, "  [$index] ID: ${pembayaran.idPembayaran}, Status: ${pembayaran.statusPembayaran}, Amount: ${pembayaran.totalBiaya}")
            }

            // Test 2: Riwayat Selesai
            Log.d(TAG, "🧪 Testing getRiwayatPeminjamanSelesai for user $userId...")
            val riwayatSelesai = repository.getRiwayatPeminjamanSelesai(userId)
            Log.d(TAG, "📊 Riwayat Selesai count: ${riwayatSelesai.size}")
            riwayatSelesai.forEachIndexed { index, riwayat ->
                Log.d(TAG, "  [$index] Fasilitas: ${riwayat.namaFasilitas}, Acara: ${riwayat.namaAcara}, Tanggal: ${riwayat.tanggalMulai}")
            }

            // Test 3: All Peminjaman for user
            Log.d(TAG, "🧪 Testing raw peminjaman_fasilitas query for user $userId...")
            val allPeminjaman = repository.getAllPeminjamanForUser(userId) // We'll create this method
            Log.d(TAG, "📊 All Peminjaman count: ${allPeminjaman.size}")

            // Test 4: All Pembayaran
            Log.d(TAG, "🧪 Testing all pembayaran...")
            val allPembayaran = repository.getAllPembayaran() // We'll create this method
            Log.d(TAG, "📊 All Pembayaran count: ${allPembayaran.size}")

            if (BuildConfig.DEBUG) {
                val summary = "Pending: ${pendingPembayaran.size}, Selesai: ${riwayatSelesai.size}"
                Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error testing user queries: ${e.message}", e)
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, "Query Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun simulateLogin(context: Context, testUserId: Int = 1) {
        Log.d(TAG, "🧪 Simulating login with User ID: $testUserId")

        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("id_pengguna", testUserId)
            putString("nama", "Test User")
            putString("email", "test@uii.ac.id")
            putBoolean("is_logged_in", true)
            apply()
        }

        if (BuildConfig.DEBUG) {
            Toast.makeText(context, "✅ Simulated login as User $testUserId", Toast.LENGTH_SHORT).show()
        }

        // Re-test after simulation
        debugUserSession(context)
    }

    fun checkDatabaseTables(context: Context) {
        Log.d(TAG, "🗃️ Checking database tables...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val repository = FasilitasRepository()

                // Check if tables have any data at all
                val allFasilitas = repository.getFasilitas()
                Log.d(TAG, "📊 Total Fasilitas in DB: ${allFasilitas.size}")

                // You'll need to add these methods to FasilitasRepository
                val totalPeminjaman = repository.getTotalPeminjamanCount()
                val totalPembayaran = repository.getTotalPembayaranCount()

                Log.d(TAG, "📊 Total Peminjaman in DB: $totalPeminjaman")
                Log.d(TAG, "📊 Total Pembayaran in DB: $totalPembayaran")

                if (BuildConfig.DEBUG) {
                    Toast.makeText(context,
                        "DB: ${allFasilitas.size} fasilitas, $totalPeminjaman peminjaman, $totalPembayaran pembayaran",
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking database: ${e.message}", e)
            }
        }
    }
}