package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.example.bismillahsipfo.ui.MainActivity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class HasilPembayaranActivity : AppCompatActivity() {

    // Smart monitoring variables
    private var smartMonitoringJob: Job? = null
    private var isMonitoring = false
    private var monitoringCount = 0
    private val maxMonitoringAttempts = 30

    // UI components (menggunakan layout yang sudah ada)
    private lateinit var imageSuccess: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvNamaFasilitas: TextView
    private lateinit var tvNamaAcara: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvPaymentId: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnCheckStatus: Button
    private lateinit var btnKembaliBeranda: Button

    // Data variables
    private var paymentId: String? = null
    private var isSuccessInitial: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pembayaran)

        // Initialize views menggunakan layout yang sudah ada
        initializeViews()

        // Ambil data dari intent
        isSuccessInitial = intent.getBooleanExtra("IS_SUCCESS", true)
        val namaFasilitas = intent.getStringExtra("NAMA_FASILITAS")
        val namaAcara = intent.getStringExtra("NAMA_ACARA")
        val tanggal = intent.getStringExtra("TANGGAL")
        paymentId = intent.getStringExtra("PAYMENT_ID")

        // Set data ke UI
        setupInitialUI(namaFasilitas, namaAcara, tanggal)

        // Setup button listeners
        setupButtonListeners()

        // Auto-start smart monitoring jika status belum success
        if (!isSuccessInitial && paymentId != null) {
            startSmartMonitoring(paymentId!!)
        }
    }

    private fun initializeViews() {
        imageSuccess = findViewById(R.id.imageSuccess)
        tvTitle = findViewById(R.id.tvTitle)
        tvNamaFasilitas = findViewById(R.id.tvNamaFasilitas)
        tvNamaAcara = findViewById(R.id.tvNamaAcara)
        tvTanggal = findViewById(R.id.tvTanggal)
        tvStatus = findViewById(R.id.tvStatus)
        tvPaymentId = findViewById(R.id.tvPaymentId)
        tvMessage = findViewById(R.id.tvMessage)
        btnCheckStatus = findViewById(R.id.btnCheckStatus)
        btnKembaliBeranda = findViewById(R.id.btnKembaliBeranda)
    }

    private fun setupInitialUI(namaFasilitas: String?, namaAcara: String?, tanggal: String?) {
        // Set data
        tvNamaFasilitas.text = namaFasilitas ?: "-"
        tvNamaAcara.text = namaAcara ?: "-"
        tvTanggal.text = tanggal ?: "-"
        tvPaymentId.text = paymentId ?: "-"

        // Update UI based on initial status
        if (isSuccessInitial) {
            updateUIForSuccess()
        } else {
            updateUIForPending()
        }
    }

    private fun setupButtonListeners() {
        btnCheckStatus.setOnClickListener {
            if (isMonitoring) {
                stopSmartMonitoring()
                btnCheckStatus.text = "Cek Status Pembayaran"
                updateMessage("Monitoring dihentikan. Anda bisa cek manual di riwayat.")
            } else if (paymentId != null) {
                startSmartMonitoring(paymentId!!)
            } else {
                Toast.makeText(this, "Payment ID tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        btnKembaliBeranda.setOnClickListener {
            navigateToHome()
        }

        // Debug button jika dalam mode debug
        if (BuildConfig.DEBUG) {
            btnCheckStatus.setOnLongClickListener {
                paymentId?.let { id ->
                    forceUpdateStatus(id, "success")
                }
                true
            }
        }
    }

    private fun startSmartMonitoring(paymentId: String) {
        if (isMonitoring) return

        isMonitoring = true
        monitoringCount = 0

        // Update button text dan UI untuk monitoring
        btnCheckStatus.text = "Stop Monitoring"
        updateUIForMonitoring()
        updateMessage("Memulai monitoring otomatis...")

        smartMonitoringJob = lifecycleScope.launch {
            try {
                // Phase 1: Quick checks (first 5 minutes) - setiap 30 detik
                for (i in 1..10) {
                    if (!isMonitoring) break

                    monitoringCount++
                    updateMessage("Monitoring pembayaran... (${monitoringCount}/${maxMonitoringAttempts})")

                    val result = performActiveStatusPolling(paymentId, i)

                    if (handleMonitoringResult(result, paymentId)) {
                        return@launch // Exit jika status final
                    }

                    delay(30000) // 30 seconds
                }

                // Phase 2: Medium checks (next 10 minutes) - setiap 1 menit
                for (i in 1..10) {
                    if (!isMonitoring) break

                    monitoringCount++
                    updateMessage("Monitoring lanjutan... (${monitoringCount}/${maxMonitoringAttempts})")

                    val result = performActiveStatusPolling(paymentId, i + 10)

                    if (handleMonitoringResult(result, paymentId)) {
                        return@launch
                    }

                    delay(60000) // 1 minute
                }

                // Phase 3: Slow checks (final 10 minutes) - setiap 1.5 menit
                for (i in 1..10) {
                    if (!isMonitoring) break

                    monitoringCount++
                    updateMessage("Monitoring akhir... (${monitoringCount}/${maxMonitoringAttempts})")

                    val result = performActiveStatusPolling(paymentId, i + 20)

                    if (handleMonitoringResult(result, paymentId)) {
                        return@launch
                    }

                    delay(90000) // 1.5 minutes
                }

                // Monitoring timeout
                handleMonitoringTimeout()

            } catch (e: Exception) {
                handleMonitoringError("Error: ${e.message}")
            }
        }
    }

    private fun stopSmartMonitoring() {
        isMonitoring = false
        smartMonitoringJob?.cancel()

        btnCheckStatus.text = "Cek Status Pembayaran"
    }

    private suspend fun performActiveStatusPolling(paymentId: String, pollCount: Int): MonitoringResult? {
        return withContext(Dispatchers.IO) {
            try {
                val requestMap = HashMap<String, Any>()
                requestMap["active_status_polling"] = true
                requestMap["payment_id"] = paymentId
                requestMap["poll_count"] = pollCount

                val gson = Gson()
                val jsonString = gson.toJson(requestMap)
                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = apiService.createTransaction(
                    url = "midtrans-sipfo",
                    authHeader = "Bearer ${BuildConfig.API_KEY}",
                    requestBody = requestBody
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val success = jsonResponse.optBoolean("success", false)

                        if (success) {
                            val status = jsonResponse.optString("status", "pending")
                            val isFinal = jsonResponse.optBoolean("final", false)
                            val message = jsonResponse.optString("message", "")

                            return@withContext MonitoringResult(
                                status = status,
                                isFinal = isFinal,
                                message = message
                            )
                        }
                    }
                }

                return@withContext null
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

    private suspend fun handleMonitoringResult(result: MonitoringResult?, paymentId: String): Boolean {
        result?.let { res ->
            when (res.status) {
                "success" -> {
                    withContext(Dispatchers.Main) {
                        handleMonitoringSuccess()
                    }
                    return true
                }
                "failed" -> {
                    withContext(Dispatchers.Main) {
                        handleMonitoringFailed()
                    }
                    return true
                }
                "pending" -> {
                    // Continue monitoring
                    if (res.isFinal) {
                        withContext(Dispatchers.Main) {
                            updateMessage("Pembayaran pending di Midtrans - tetap monitoring...")
                        }
                    }
                }
            }
        }
        return false
    }

    private fun handleMonitoringSuccess() {
        stopSmartMonitoring()
        updateUIForSuccess()
        updateMessage("✅ Pembayaran berhasil terdeteksi!")

        Toast.makeText(this,
            "Pembayaran berhasil! Kembali ke beranda dalam 3 detik...",
            Toast.LENGTH_LONG).show()

        // Auto-navigate setelah 3 detik
        lifecycleScope.launch {
            delay(3000)
            navigateToHome()
        }
    }

    private fun handleMonitoringFailed() {
        stopSmartMonitoring()
        updateUIForFailed()
        updateMessage("❌ Pembayaran gagal - silakan coba lagi atau hubungi admin")

        Toast.makeText(this,
            "Pembayaran gagal. Silakan coba lagi.",
            Toast.LENGTH_LONG).show()
    }

    private fun handleMonitoringTimeout() {
        lifecycleScope.launch(Dispatchers.Main) {
            stopSmartMonitoring()
            updateUIForTimeout()
            updateMessage("⏰ Monitoring timeout - pembayaran mungkin masih diproses. Cek riwayat untuk status terbaru.")

            Toast.makeText(this@HasilPembayaranActivity,
                "Monitoring timeout. Silakan cek di halaman riwayat.",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun handleMonitoringError(error: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            stopSmartMonitoring()
            updateMessage("⚠️ $error - silakan cek manual di riwayat")

            Toast.makeText(this@HasilPembayaranActivity,
                "Error monitoring. Coba manual atau cek riwayat.",
                Toast.LENGTH_SHORT).show()
        }
    }

    // UI Update methods menggunakan komponen yang sudah ada
    private fun updateUIForSuccess() {
        imageSuccess.setImageResource(R.drawable.ic_success)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, R.color.dark_blue))
        tvTitle.text = "Pembayaran Berhasil!"
        tvStatus.text = "SUKSES"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
    }

    private fun updateUIForPending() {
        imageSuccess.setImageResource(R.drawable.ic_warning)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        tvTitle.text = "Status Pembayaran"
        tvStatus.text = "PENDING"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
    }

    private fun updateUIForMonitoring() {
        imageSuccess.setImageResource(R.drawable.ic_warning)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, R.color.dark_blue))
        tvTitle.text = "Monitoring Pembayaran"
        tvStatus.text = "MONITORING..."
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.dark_blue))
    }

    private fun updateUIForFailed() {
        imageSuccess.setImageResource(R.drawable.ic_warning)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        tvTitle.text = "Pembayaran Gagal"
        tvStatus.text = "GAGAL"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
    }

    private fun updateUIForTimeout() {
        imageSuccess.setImageResource(R.drawable.ic_warning)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        tvTitle.text = "Monitoring Timeout"
        tvStatus.text = "TIMEOUT"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }

    private fun updateMessage(message: String) {
        tvMessage.text = message
    }

    private fun forceUpdateStatus(paymentId: String, status: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestMap = HashMap<String, Any>()
                requestMap["force_update_status"] = true
                requestMap["payment_id"] = paymentId
                requestMap["force_status"] = status

                val gson = Gson()
                val jsonString = gson.toJson(requestMap)
                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = apiService.createTransaction(
                    url = "midtrans-sipfo",
                    authHeader = "Bearer ${BuildConfig.API_KEY}",
                    requestBody = requestBody
                )

                lifecycleScope.launch(Dispatchers.Main) {
                    if (response.isSuccessful && status == "success") {
                        updateUIForSuccess()
                        updateMessage("✅ Status berhasil diupdate secara manual (DEBUG)")
                        Toast.makeText(this@HasilPembayaranActivity,
                            "DEBUG: Status diupdate ke success",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@HasilPembayaranActivity,
                            "DEBUG: Gagal update status",
                            Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@HasilPembayaranActivity,
                        "DEBUG Error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToHome() {
        // Stop monitoring when navigating away
        stopSmartMonitoring()

        val intent = Intent(this, com.example.bismillahsipfo.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_HOME", true)
        }

        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToHome()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSmartMonitoring()
    }

    // Data class for monitoring results
    data class MonitoringResult(
        val status: String,
        val isFinal: Boolean,
        val message: String
    )
}