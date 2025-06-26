package com.example.bismillahsipfo.ui.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import java.time.format.DateTimeFormatter
import java.util.Locale

class TabelJadwalPeminjamanAdapter(private var jadwalList: List<JadwalPeminjamanItem>) :
    RecyclerView.Adapter<TabelJadwalPeminjamanAdapter.JadwalViewHolder>() {

    inner class JadwalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvHari: TextView = view.findViewById(R.id.tvHari)
        val tvWaktu: TextView = view.findViewById(R.id.tvWaktu)
        val tvOrganisasi: TextView = view.findViewById(R.id.tvOrganisasi)
        val tvLapangan: TextView = view.findViewById(R.id.tvLapangan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tabel_jadwal_peminjaman, parent, false)
        return JadwalViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val item = jadwalList[position]
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id"))
        holder.tvTanggal.text = item.tanggal.format(dateFormatter)
        holder.tvHari.text = item.tanggal.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("id"))
        holder.tvWaktu.text = "${item.jamMulai} - ${item.jamSelesai}"
        holder.tvOrganisasi.text = item.namaOrganisasi
        holder.tvLapangan.text = item.namaLapangan.joinToString(", ")
    }

    override fun getItemCount(): Int = jadwalList.size

    fun updateData(newData: List<JadwalPeminjamanItem>) {
        jadwalList = newData
        notifyDataSetChanged()
    }
}

