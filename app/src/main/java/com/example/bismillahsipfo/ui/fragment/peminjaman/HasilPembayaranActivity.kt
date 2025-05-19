package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bismillahsipfo.R

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
    }
}