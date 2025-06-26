package com.example.bismillahsipfo.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.RiwayatPending
import com.example.bismillahsipfo.data.model.StatusPembayaran
import com.example.bismillahsipfo.ui.fragment.peminjaman.MidtransWebViewActivity
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class RowRiwayatPendingAdapter(
    private val riwayatList: List<RiwayatPending>,
    private val onRegenerateToken: ((String, (Boolean, String?) -> Unit) -> Unit)? = null
) : RecyclerView.Adapter<RowRiwayatPendingAdapter.RiwayatViewHolder>() {

    inner class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJumlahHari: TextView = itemView.findViewById(R.id.tvJumlahHari)
        val tvFasilitas: TextView = itemView.findViewById(R.id.tvFasilitas)
        val tvDateStartEnd: TextView = itemView.findViewById(R.id.tvDateStartEnd)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvExp: TextView = itemView.findViewById(R.id.tvExp)
        val btnBayarSekarang: Button = itemView.findViewById(R.id.btnBayarSekarang)

        // Status badge (jika ada di layout)
        val tvStatusBadge: TextView? = itemView.findViewById(R.id.tvStatusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_riwayat_pending, parent, false)
        return RiwayatViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        val riwayat = riwayatList[position]
        val context = holder.itemView.context

        // Calculate jumlah hari
        val jumlahHari = ChronoUnit.DAYS.between(riwayat.tanggalMulai, riwayat.tanggalSelesai) + 1
        holder.tvJumlahHari.text = if (jumlahHari == 1L) "1 Hari" else "$jumlahHari Hari"

        // Set facility name
        holder.tvFasilitas.text = riwayat.namaFasilitas

        // Format date range
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale("id", "ID"))
        val startDate = riwayat.tanggalMulai.format(dateFormatter)
        val endDate = riwayat.tanggalSelesai.format(dateFormatter)
        holder.tvDateStartEnd.text = if (startDate == endDate) startDate else "$startDate - $endDate"

        // Format price
        holder.tvPrice.text = formatRupiah(riwayat.totalBiaya)

        // Setup UI based on status
        when (riwayat.statusPembayaran) {
            StatusPembayaran.PENDING -> {
                setupPendingUI(holder, riwayat, context)
            }
            StatusPembayaran.FAILED -> {
                setupFailedUI(holder, riwayat, context)
            }
            else -> {
                // Handle other statuses if needed
                setupPendingUI(holder, riwayat, context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupPendingUI(holder: RiwayatViewHolder, riwayat: RiwayatPending, context: Context) {
        // Status badge
        holder.tvStatusBadge?.apply {
            text = "Menunggu Pembayaran"
            setTextColor(Color.parseColor("#D97706"))
            setBackgroundResource(R.drawable.status_pending_bg)
        }

        // Expiry time
        val expiryFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm", Locale("id", "ID"))
        holder.tvExp.text = riwayat.waktuKadaluwarsa.atZone(java.time.ZoneId.systemDefault())
            .format(expiryFormatter)
        holder.tvExp.setTextColor(Color.parseColor("#DC2626"))

        // Enable button for pending payments
        holder.btnBayarSekarang.apply {
            isEnabled = true
            text = "Bayar Sekarang"
//            setBackgroundColor(ContextCompat.getColor(context, R.color.dark_blue))
            setBackgroundResource(R.drawable.button_payment_selector)
            setTextColor(ContextCompat.getColor(context, R.color.white))

            setOnClickListener {
                handlePaymentButtonClick(riwayat, context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFailedUI(holder: RiwayatViewHolder, riwayat: RiwayatPending, context: Context) {
        // Status badge
        holder.tvStatusBadge?.apply {
            text = "Pembayaran Gagal"
            setTextColor(Color.parseColor("#DC2626"))
            setBackgroundResource(R.drawable.status_failed_bg)
        }

        // Failed message instead of expiry
        holder.tvExp.apply {
            text = "Pembayaran gagal - Tidak dapat diulang"
            setTextColor(Color.parseColor("#DC2626"))
        }

        // Disable button for failed payments
        holder.btnBayarSekarang.apply {
            isEnabled = false
            text = "Tidak Dapat Diulang"

            setBackgroundResource(R.drawable.button_payment_selector) // Will show disabled state
//            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))

            setOnClickListener {
                Toast.makeText(context,
                    "Pembayaran gagal tidak dapat diulang. Silakan buat peminjaman baru.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handlePaymentButtonClick(riwayat: RiwayatPending, context: Context) {
        if (riwayat.statusPembayaran != StatusPembayaran.PENDING) {
            Toast.makeText(context, "Pembayaran tidak dalam status pending", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if token already exists
        if (!riwayat.midtransRedirectUrl.isNullOrEmpty()) {
            // Direct redirect to existing URL
            navigateToMidtransWebView(context, riwayat.midtransRedirectUrl, riwayat)
        } else {
            // Regenerate token
            Toast.makeText(context, "Memproses pembayaran...", Toast.LENGTH_SHORT).show()

            onRegenerateToken?.invoke(riwayat.idPembayaran) { success, redirectUrl ->
                if (success && !redirectUrl.isNullOrEmpty()) {
                    navigateToMidtransWebView(context, redirectUrl, riwayat)
                } else {
                    Toast.makeText(context,
                        "Gagal memproses pembayaran. Silakan coba lagi.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToMidtransWebView(context: Context, url: String, riwayat: RiwayatPending) {
        val intent = Intent(context, MidtransWebViewActivity::class.java).apply {
            putExtra("MIDTRANS_URL", url)
            putExtra("PAYMENT_ID", riwayat.idPembayaran)
            putExtra("NAMA_FASILITAS", riwayat.namaFasilitas)
            putExtra("NAMA_ACARA", "Peminjaman Fasilitas") // Default jika tidak ada nama acara
            putExtra("TANGGAL", "${riwayat.tanggalMulai} - ${riwayat.tanggalSelesai}")
        }
        context.startActivity(intent)
    }

    private fun formatRupiah(amount: Double): String {
        return if (amount == 0.0) {
            "Gratis"
        } else {
            "Rp ${String.format(Locale("id", "ID"), "%,.0f", amount)}"
        }
    }

    override fun getItemCount(): Int = riwayatList.size
}


//class RowRiwayatPendingAdapter(
//    private val riwayatPendingList: List<RiwayatPending>
//) : RecyclerView.Adapter<RowRiwayatPendingAdapter.ViewHolder>() {
//
//    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_riwayat_pending, parent, false)
//        return ViewHolder(view)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val riwayat = riwayatPendingList[position]
//
//        val jumlahHari = ChronoUnit.DAYS.between(riwayat.tanggalMulai, riwayat.tanggalSelesai).let {
//            if (it == 0L) 1 else it + 1
//        }
//        holder.tvJumlahHari.text = "$jumlahHari Hari"
//        holder.tvDateStartEnd.text = "${riwayat.tanggalMulai.format(DateTimeFormatter.ISO_LOCAL_DATE)} s/d ${riwayat.tanggalSelesai.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
//        holder.tvFasilitas.text = riwayat.namaFasilitas
//        holder.tvPrice.text = if (riwayat.totalBiaya > 0) {
//            numberFormat.format(riwayat.totalBiaya)
//        } else {
//            "Tidak tersedia"
//        }
//        holder.tvExp.text = riwayat.waktuKadaluwarsa.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
//    }
//
//    override fun getItemCount(): Int = riwayatPendingList.size
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvJumlahHari: TextView = itemView.findViewById(R.id.tvJumlahHari)
//        val tvDateStartEnd: TextView = itemView.findViewById(R.id.tvDateStartEnd)
//        val tvFasilitas: TextView = itemView.findViewById(R.id.tvFasilitas)
//        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
//        val tvExp: TextView = itemView.findViewById(R.id.tvExp)
//    }
//}
