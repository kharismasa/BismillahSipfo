package com.example.bismillahsipfo.adapter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RowRiwayatSelesaiAdapter(
    private val peminjamanList: List<PeminjamanFasilitas>,
    private val fasilitasList: List<Fasilitas> // Facility data
) : RecyclerView.Adapter<RowRiwayatSelesaiAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_riwayat_selesai, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peminjaman = peminjamanList[position]

        // Find facility name by idFasilitas
        val fasilitas = fasilitasList.find { it.idFasilitas == peminjaman.idFasilitas }
        val namaFasilitas = fasilitas?.namaFasilitas ?: "Fasilitas tidak ditemukan"


        val jumlahHari = ChronoUnit.DAYS.between(peminjaman.tanggalMulai, peminjaman.tanggalSelesai).let {
            if (it == 0L) 1 else it + 1
        }
        holder.tvJumlahHari.text = "$jumlahHari Hari"
        holder.tvDateStartEnd.text = "${peminjaman.tanggalMulai} s/d ${peminjaman.tanggalSelesai}"
        holder.tvFasilitas.text = namaFasilitas
        holder.tvAcara.text = peminjaman.namaAcara
        holder.tvTimeStartEnd.text = "${peminjaman.jamMulai} - ${peminjaman.jamSelesai}"

        if (!peminjaman.suratPeminjamanUrl.isNullOrEmpty()) {
            holder.btnLihatSurat.visibility = View.VISIBLE
            holder.btnLihatSurat.setOnClickListener {
                // Open surat in browser or PDF viewer
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(peminjaman.suratPeminjamanUrl))
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.btnLihatSurat.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = peminjamanList.size

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDays(startDate: LocalDate, endDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(startDate, endDate).let {
            if (it == 0L) 1 else it // Jika tanggal mulai dan selesai sama, hitung 1 hari
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJumlahHari: TextView = itemView.findViewById(R.id.tvJumlahHari)
        val tvDateStartEnd: TextView = itemView.findViewById(R.id.tvDateStartEnd)
        val tvFasilitas: TextView = itemView.findViewById(R.id.tvFasilitas)
        val tvAcara: TextView = itemView.findViewById(R.id.tvEvent)
        val tvTimeStartEnd: TextView = itemView.findViewById(R.id.tvTimeStartEnd)
        val btnLihatSurat: Button = itemView.findViewById(R.id.btnLihatSurat)
    }
}
