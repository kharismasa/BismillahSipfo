package com.example.bismillahsipfo.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.RiwayatPending
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class RowRiwayatPendingAdapter(
    private val riwayatPendingList: List<RiwayatPending>
) : RecyclerView.Adapter<RowRiwayatPendingAdapter.ViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_riwayat_pending, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val riwayat = riwayatPendingList[position]

        val jumlahHari = ChronoUnit.DAYS.between(riwayat.tanggalMulai, riwayat.tanggalSelesai).let {
            if (it == 0L) 1 else it + 1
        }
        holder.tvJumlahHari.text = "$jumlahHari Hari"
        holder.tvDateStartEnd.text = "${riwayat.tanggalMulai.format(DateTimeFormatter.ISO_LOCAL_DATE)} s/d ${riwayat.tanggalSelesai.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        holder.tvFasilitas.text = riwayat.namaFasilitas
        holder.tvPrice.text = if (riwayat.totalBiaya > 0) {
            numberFormat.format(riwayat.totalBiaya)
        } else {
            "Tidak tersedia"
        }
        holder.tvExp.text = riwayat.waktuKadaluwarsa.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
    }

    override fun getItemCount(): Int = riwayatPendingList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJumlahHari: TextView = itemView.findViewById(R.id.tvJumlahHari)
        val tvDateStartEnd: TextView = itemView.findViewById(R.id.tvDateStartEnd)
        val tvFasilitas: TextView = itemView.findViewById(R.id.tvFasilitas)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvExp: TextView = itemView.findViewById(R.id.tvExp)
    }
}
