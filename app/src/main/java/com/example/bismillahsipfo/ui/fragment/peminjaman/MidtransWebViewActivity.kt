package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MidtransWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var paymentId: String? = null
    private var namaFasilitas: String? = null
    private var namaAcara: String? = null
    private var tanggal: String? = null

    // Enhanced monitoring variables
    private var isCheckingStatus = false
    private var statusCheckRetryCount = 0
    private val maxRetryCount = 10
    private var backgroundMonitoringJob: Job? = null
    private var userExitedWebView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_midtrans_webview)

        webView = findViewById(R.id.webView)

        // Get data from intent
        val url = intent.getStringExtra("MIDTRANS_URL")
        paymentId = intent.getStringExtra("PAYMENT_ID")
        namaFasilitas = intent.getStringExtra("NAMA_FASILITAS")
        namaAcara = intent.getStringExtra("NAMA_ACARA")
        tanggal = intent.getStringExtra("TANGGAL")

        if (url.isNullOrEmpty()) {
            Toast.makeText(this, "URL tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configure enhanced WebView
        setupEnhancedWebView()

        // Load the URL
        webView.loadUrl(url)

        // Start background status monitoring
        startBackgroundStatusMonitoring()
    }

    private fun setupEnhancedWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MidtransWebView", "Page loaded: $url")

                // Enhanced URL detection untuk berbagai payment methods
                when {
                    // Success indicators - Universal
                    url?.contains("status_code=200") == true ||
                            url?.contains("transaction_status=settlement") == true ||
                            url?.contains("transaction_status=capture") == true ||
                            url?.contains("/finish") == true ||
                            url?.contains("success") == true -> {

                        Log.d("MidtransWebView", "SUCCESS detected at URL: $url")
                        handlePaymentSuccess()
                    }

                    // Virtual Account specific success
                    url?.contains("va_number") == true && url.contains("pending") -> {
                        Log.d("MidtransWebView", "Virtual Account created, monitoring payment...")
                        // Untuk VA, mulai aggressive monitoring
                        startAggressiveStatusMonitoring()
                    }

                    // E-wallet specific patterns
                    url?.contains("gopay") == true && url.contains("success") -> {
                        Log.d("MidtransWebView", "GoPay success detected")
                        handlePaymentSuccess()
                    }

                    url?.contains("shopeepay") == true && url.contains("success") -> {
                        Log.d("MidtransWebView", "ShopeePay success detected")
                        handlePaymentSuccess()
                    }

                    url?.contains("qris") == true && url.contains("success") -> {
                        Log.d("MidtransWebView", "QRIS success detected")
                        handlePaymentSuccess()
                    }

                    // Failed indicators
                    url?.contains("transaction_status=deny") == true ||
                            url?.contains("transaction_status=cancel") == true ||
                            url?.contains("transaction_status=expire") == true ||
                            url?.contains("failed") == true -> {

                        Log.d("MidtransWebView", "Payment failed detected at URL: $url")
                        finishWithResult(false)
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("MidtransWebView", "Navigating to: $url")

                // Handle deep links untuk e-wallet
                if (url?.startsWith("gojek://") == true ||
                    url?.startsWith("shopeepay://") == true ||
                    url?.startsWith("id.dana://") == true) {

                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)

                        // User keluar ke e-wallet app, mulai background monitoring
                        userExitedWebView = true
                        startAggressiveStatusMonitoring()

                        return true
                    } catch (e: Exception) {
                        Log.e("MidtransWebView", "Error opening e-wallet app: ${e.message}")
                    }
                }

                return false
            }
        }
    }

    private fun handlePaymentSuccess() {
        if (!isCheckingStatus) {
            Log.d("MidtransWebView", "Handling payment success...")
            checkPaymentStatusWithRetry()
        }
    }

    // SOLUSI UTAMA: Background Status Monitoring
    private fun startBackgroundStatusMonitoring() {
        backgroundMonitoringJob = lifecycleScope.launch {
            var monitoringCount = 0
            val maxMonitoringTime = 900 // 15 menit

            while (monitoringCount < maxMonitoringTime && !isFinishing) {
                delay(10000) // Check setiap 10 detik
                monitoringCount += 10

                Log.d("MidtransWebView", "Background monitoring check #${monitoringCount/10}")

                try {
                    val currentStatus = checkCurrentPaymentStatus()

                    when (currentStatus) {
                        "success" -> {
                            Log.d("MidtransWebView", "Payment success detected in background monitoring")
                            finishWithResult(true)
                            return@launch
                        }
                        "failed" -> {
                            Log.d("MidtransWebView", "Payment failed detected in background monitoring")
                            finishWithResult(false)
                            return@launch
                        }
                        "pending" -> {
                            // Continue monitoring
                            Log.d("MidtransWebView", "Payment still pending, continuing monitoring...")
                        }
                    }

                    // Jika user sudah exit WebView dan sudah monitoring 5 menit,
                    // berikan option untuk manual check
                    if (userExitedWebView && monitoringCount >= 300) {
                        withContext(Dispatchers.Main) {
                            showContinueMonitoringDialog()
                        }
                        return@launch
                    }

                } catch (e: Exception) {
                    Log.e("MidtransWebView", "Background monitoring error: ${e.message}")
                }
            }

            // Timeout monitoring, let user decide
            withContext(Dispatchers.Main) {
                showTimeoutDialog()
            }
        }
    }

    private fun startAggressiveStatusMonitoring() {
        // Untuk VA dan e-wallet, check lebih agresif
        lifecycleScope.launch {
            repeat(20) { attempt ->
                delay(15000) // Check setiap 15 detik

                Log.d("MidtransWebView", "Aggressive monitoring attempt #${attempt + 1}")

                try {
                    val status = checkCurrentPaymentStatus()

                    if (status == "success") {
                        Log.d("MidtransWebView", "Success detected in aggressive monitoring")
                        finishWithResult(true)
                        return@launch
                    } else if (status == "failed") {
                        Log.d("MidtransWebView", "Failed detected in aggressive monitoring")
                        finishWithResult(false)
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e("MidtransWebView", "Aggressive monitoring error: ${e.message}")
                }
            }
        }
    }

    private suspend fun checkCurrentPaymentStatus(): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestMap = HashMap<String, Any>()
                requestMap["check_transaction_status"] = true
                requestMap["payment_id"] = paymentId!!

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
                        return@withContext jsonResponse.optString("status", "pending")
                    }
                }

                return@withContext "pending"
            } catch (e: Exception) {
                Log.e("MidtransWebView", "Status check error: ${e.message}")
                return@withContext "pending"
            }
        }
    }

    private fun showContinueMonitoringDialog() {
        AlertDialog.Builder(this)
            .setTitle("Monitoring Pembayaran")
            .setMessage("Pembayaran masih dalam proses. Lanjutkan monitoring otomatis atau cek manual di riwayat?")
            .setPositiveButton("Lanjut Monitor") { _, _ ->
                // Continue background monitoring
                startBackgroundStatusMonitoring()
            }
            .setNegativeButton("Cek Manual") { _, _ ->
                finishWithResult(true) // Let user check in history
            }
            .setCancelable(false)
            .show()
    }

    private fun showTimeoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Timeout Monitoring")
            .setMessage("Monitoring pembayaran timeout. Silakan cek status di halaman riwayat.")
            .setPositiveButton("OK") { _, _ ->
                finishWithResult(true)
            }
            .setCancelable(false)
            .show()
    }

    private fun checkPaymentStatusWithRetry() {
        if (isCheckingStatus || paymentId.isNullOrEmpty()) return

        isCheckingStatus = true
        statusCheckRetryCount++

        lifecycleScope.launch {
            try {
                Log.d("MidtransWebView", "Status check attempt #$statusCheckRetryCount")

                delay(3000) // Wait for Midtrans to update

                val status = checkCurrentPaymentStatus()

                when (status) {
                    "success" -> {
                        Log.d("MidtransWebView", "Payment confirmed as success")
                        finishWithResult(true)
                    }
                    "failed" -> {
                        Log.d("MidtransWebView", "Payment confirmed as failed")
                        finishWithResult(false)
                    }
                    else -> {
                        if (statusCheckRetryCount < maxRetryCount) {
                            delay(5000)
                            isCheckingStatus = false
                            checkPaymentStatusWithRetry()
                        } else {
                            // Max retry reached, continue with background monitoring
                            Log.w("MidtransWebView", "Max retry reached, continuing background monitoring")
                            isCheckingStatus = false
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("MidtransWebView", "Error in status check retry: ${e.message}")

                if (statusCheckRetryCount < maxRetryCount) {
                    delay(5000)
                    isCheckingStatus = false
                    checkPaymentStatusWithRetry()
                } else {
                    isCheckingStatus = false
                }
            }
        }
    }

    private fun finishWithResult(success: Boolean) {
        // Cancel background monitoring
        backgroundMonitoringJob?.cancel()

        val intent = Intent(this, HasilPembayaranActivity::class.java).apply {
            putExtra("IS_SUCCESS", success)
            putExtra("PAYMENT_ID", paymentId)
            putExtra("NAMA_FASILITAS", namaFasilitas)
            putExtra("NAMA_ACARA", namaAcara)
            putExtra("TANGGAL", tanggal)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        userExitedWebView = true

        AlertDialog.Builder(this)
            .setTitle("Keluar dari Pembayaran")
            .setMessage("Pembayaran masih berjalan. Keluar dan monitor di background atau batalkan pembayaran?")
            .setPositiveButton("Monitor Background") { _, _ ->
                // Continue background monitoring dan navigasi ke hasil
                finishWithResult(true)
            }
            .setNegativeButton("Batalkan") { _, _ ->
                finishWithResult(false)
            }
            .setNeutralButton("Tetap di Sini") { dialog, _ ->
                dialog.dismiss()
                userExitedWebView = false
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMonitoringJob?.cancel()
    }

    override fun onPause() {
        super.onPause()
        userExitedWebView = true
    }

    override fun onResume() {
        super.onResume()
        userExitedWebView = false
    }
}