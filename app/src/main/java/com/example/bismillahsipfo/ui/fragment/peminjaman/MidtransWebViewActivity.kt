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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MidtransWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var paymentId: String? = null

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

                // Tambahkan pattern URL lain yang mungkin digunakan Midtrans
                if (url?.contains("payment/success") == true ||
                    url?.contains("payment/finish") == true ||
                    url?.contains("transaction_status=settlement") == true ||
                    url?.contains("transaction_status=capture") == true) {
                    Log.d("MidtransWebView", "Payment success detected at URL: $url")
                    finishWithResult(true)
                } else if (url?.contains("payment/error") == true ||
                    url?.contains("payment/failed") == true ||
                    url?.contains("transaction_status=deny") == true ||
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

    private fun finishWithResult(success: Boolean) {
        // Jika pembayaran berhasil, kirim request untuk update status
        if (success && paymentId != null) {
            updatePaymentStatus(paymentId!!)
        }

        val intent = Intent()
        intent.putExtra("PAYMENT_SUCCESS", success)
        intent.putExtra("PAYMENT_ID", paymentId)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun updatePaymentStatus(paymentId: String) {
        Log.d("MidtransWebView", "Mencoba update status untuk payment_id: $paymentId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Buat JSON untuk request update status
                val requestMap = HashMap<String, Any>()
                requestMap["payment_id"] = paymentId
                requestMap["update_status"] = true
                requestMap["new_status"] = "success"

                val gson = Gson()
                val jsonString = gson.toJson(requestMap)
                Log.d("MidtransWebView", "Request body: $jsonString")
                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                // Kirim request ke Supabase Function
                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = apiService.createTransaction(
                    url = "update-payment-status", // Endpoint baru di backend
                    authHeader = "Bearer ${BuildConfig.API_KEY}",
                    requestBody = requestBody
                )

                Log.d("MidtransWebView", "Response: ${response.code()} - ${response.body()?.string()}")
            } catch (e: Exception) {
                Log.e("MidtransWebView", "Error updating payment status: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}