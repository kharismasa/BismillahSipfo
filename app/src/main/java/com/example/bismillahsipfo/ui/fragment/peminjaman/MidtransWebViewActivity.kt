package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MidtransWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var paymentId: String? = null
    private var isCheckingStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_midtrans_webview)

        webView = findViewById(R.id.webView)

        // Get URL and payment ID from intent
        val url = intent.getStringExtra("MIDTRANS_URL")
        paymentId = intent.getStringExtra("PAYMENT_ID")

        if (url.isNullOrEmpty()) {
            Toast.makeText(this, "URL tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MidtransWebView", "Page loaded: $url")

                // Deteksi URL success/settlement
                if (url?.contains("status_code=200") == true ||
                    url?.contains("transaction_status=settlement") == true ||
                    url?.contains("transaction_status=capture") == true ||
                    url?.contains("/finish") == true) {

                    Log.d("MidtransWebView", "Payment success detected at URL: $url")
                    if (!isCheckingStatus) {
                        checkPaymentStatus()
                    }
                }
                // Deteksi URL failed
                else if (url?.contains("transaction_status=deny") == true ||
                    url?.contains("transaction_status=cancel") == true ||
                    url?.contains("transaction_status=expire") == true) {

                    Log.d("MidtransWebView", "Payment failed detected at URL: $url")
                    finishWithResult(false)
                }
            }
        }

        // Load the URL
        webView.loadUrl(url)
    }

    private fun checkPaymentStatus() {
        if (isCheckingStatus || paymentId.isNullOrEmpty()) return

        isCheckingStatus = true

        lifecycleScope.launch {
            try {
                // Tunggu sebentar agar Midtrans update status
                delay(3000)

                // Request check transaction status
                val requestMap = HashMap<String, Any>()
                requestMap["check_transaction_status"] = true
                requestMap["payment_id"] = paymentId!!

                val gson = Gson()
                val jsonString = gson.toJson(requestMap)
                Log.d("MidtransWebView", "Check status request: $jsonString")

                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    apiService.createTransaction(
                        url = "midtrans-sipfo", // PERBAIKAN: Gunakan endpoint yang benar
                        authHeader = "Bearer ${BuildConfig.API_KEY}",
                        requestBody = requestBody
                    )
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d("MidtransWebView", "Check status response: $responseBody")

                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.optString("status", "pending")

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
                                // Retry sekali lagi jika masih pending
                                delay(3000)
                                retryCheckStatus()
                            }
                        }
                    }
                } else {
                    Log.e("MidtransWebView", "Error response: ${response.code()}")
                    finishWithResult(true) // Tetap anggap success, biar user cek di riwayat
                }

            } catch (e: Exception) {
                Log.e("MidtransWebView", "Error checking status: ${e.message}")
                finishWithResult(true) // Tetap anggap success, biar user cek di riwayat
            } finally {
                isCheckingStatus = false
            }
        }
    }

    private suspend fun retryCheckStatus() {
        try {
            val requestMap = HashMap<String, Any>()
            requestMap["check_transaction_status"] = true
            requestMap["payment_id"] = paymentId!!

            val gson = Gson()
            val jsonString = gson.toJson(requestMap)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val apiService = RetrofitClient.createService(ApiService::class.java)
            val response = withContext(Dispatchers.IO) {
                apiService.createTransaction(
                    url = "midtrans-sipfo",
                    authHeader = "Bearer ${BuildConfig.API_KEY}",
                    requestBody = requestBody
                )
            }

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val status = jsonResponse.optString("status", "pending")

                    when (status) {
                        "success" -> finishWithResult(true)
                        "failed" -> finishWithResult(false)
                        else -> finishWithResult(true) // Anggap success, user cek di riwayat
                    }
                }
            } else {
                finishWithResult(true)
            }
        } catch (e: Exception) {
            Log.e("MidtransWebView", "Retry error: ${e.message}")
            finishWithResult(true)
        }
    }

    private fun finishWithResult(success: Boolean) {
        val intent = Intent(this, HasilPembayaranActivity::class.java).apply {
            putExtra("IS_SUCCESS", success)
            putExtra("PAYMENT_ID", paymentId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(intent)
        finish()
    }


    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finishWithResult(true)
            super.onBackPressed()
        }
    }
}