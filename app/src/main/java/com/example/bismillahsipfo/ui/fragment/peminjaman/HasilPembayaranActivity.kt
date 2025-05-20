package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class HasilPembayaranActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pembayaran)

        // Ambil data dari intent
        val isSuccess = intent.getBooleanExtra("IS_SUCCESS", true)
        val namaFasilitas = intent.getStringExtra("NAMA_FASILITAS")
        val namaAcara = intent.getStringExtra("NAMA_ACARA")
        val tanggal = intent.getStringExtra("TANGGAL")
        val paymentId = intent.getStringExtra("PAYMENT_ID")

        // Find views
        val imageSuccess = findViewById<ImageView>(R.id.imageSuccess)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvNamaFasilitas = findViewById<TextView>(R.id.tvNamaFasilitas)
        val tvNamaAcara = findViewById<TextView>(R.id.tvNamaAcara)
        val tvTanggal = findViewById<TextView>(R.id.tvTanggal)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPaymentId = findViewById<TextView>(R.id.tvPaymentId)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val btnKembaliBeranda = findViewById<Button>(R.id.btnKembaliBeranda)

        // Update UI based on status
        if (isSuccess) {
            imageSuccess.setImageResource(R.drawable.ic_success)
            imageSuccess.setColorFilter(ContextCompat.getColor(this, R.color.dark_blue))
            tvTitle.text = "Pembayaran Berhasil!"
            tvStatus.text = "SUKSES"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
            tvMessage.text = "Terima kasih telah menggunakan layanan kami. Detail peminjaman telah dicatat dan dapat dilihat pada halaman Riwayat."
        } else {
            imageSuccess.setImageResource(R.drawable.ic_warning)
            imageSuccess.setColorFilter(ContextCompat.getColor(this, R.color.red))
            tvTitle.text = "Pembayaran Dibatalkan"
            tvStatus.text = "DIBATALKAN"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
            tvMessage.text = "Pembayaran Anda belum berhasil. Silakan coba lagi atau hubungi admin untuk bantuan."
        }

        // Set data
        tvNamaFasilitas.text = namaFasilitas ?: "-"
        tvNamaAcara.text = namaAcara ?: "-"
        tvTanggal.text = tanggal ?: "-"
        tvPaymentId.text = paymentId ?: "-"

        // Handle button click
        btnKembaliBeranda.setOnClickListener {
            // Kembali ke beranda dan hapus seluruh history activity
            finish()
        }

        // Panggil di onCreate() setelah menginisialisasi view
        if (paymentId != null) {
            checkPaymentStatus(paymentId)
        }
    }

    private fun checkPaymentStatus(paymentId: String) {
        val btnCheckStatus = findViewById<Button>(R.id.btnCheckStatus)
        btnCheckStatus.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Buat JSON untuk request check status
                    val requestMap = HashMap<String, Any>()
                    requestMap["payment_id"] = paymentId
                    requestMap["check_transaction_status"] = true

                    val gson = Gson()
                    val jsonString = gson.toJson(requestMap)
                    val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                    // Kirim request ke Supabase Function
                    val apiService = RetrofitClient.createService(ApiService::class.java)
                    val response = apiService.createTransaction(
                        url = "midtrans-sipfo", // Menggunakan endpoint yang sudah ada
                        authHeader = "Bearer ${BuildConfig.API_KEY}",
                        requestBody = requestBody
                    )

                    val responseBody = response.body()?.string()

                    // Update UI di main thread
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = org.json.JSONObject(responseBody)
                            val success = jsonResponse.optBoolean("success", false)
                            val newStatus = jsonResponse.optString("status", "pending")
                            val message = jsonResponse.optString("message", "")

                            if (success) {
                                if (newStatus == "success") {
                                    // Update UI untuk sukses
                                    updateUIForSuccess()
                                    Toast.makeText(this@HasilPembayaranActivity,
                                        "Status pembayaran: SUKSES",
                                        Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@HasilPembayaranActivity,
                                        "Status pembayaran: $newStatus",
                                        Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@HasilPembayaranActivity,
                                    "Gagal mengecek status: $message",
                                    Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@HasilPembayaranActivity,
                                "Gagal terhubung ke server",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@HasilPembayaranActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateUIForSuccess() {
        val imageSuccess = findViewById<ImageView>(R.id.imageSuccess)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)

        imageSuccess.setImageResource(R.drawable.ic_success)
        imageSuccess.setColorFilter(ContextCompat.getColor(this, R.color.dark_blue))
        tvTitle.text = "Pembayaran Berhasil!"
        tvStatus.text = "SUKSES"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
        tvMessage.text = "Terima kasih telah menggunakan layanan kami. Detail peminjaman telah dicatat dan dapat dilihat pada halaman Riwayat."
    }

}