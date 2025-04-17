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



//import android.os.Build
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.annotation.RequiresApi
//import androidx.recyclerview.widget.RecyclerView
//import com.example.bismillahsipfo.R
//import com.example.bismillahsipfo.data.model.Lapangan
//import com.example.bismillahsipfo.data.model.LapanganDipinjam
//import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
//import java.time.format.TextStyle
//import java.util.Locale
//
//@RequiresApi(Build.VERSION_CODES.O)
//class TabelJadwalPeminjamanAdapter(
//    private val peminjamanList: List<PeminjamanFasilitas>,
//    private val lapanganDipinjamList: List<LapanganDipinjam>,
//    private val lapanganList: List<Lapangan>
//) : RecyclerView.Adapter<TabelJadwalPeminjamanAdapter.ViewHolder>() {
//
//    private val jadwalList = mutableListOf<JadwalPeminjaman>()
//
//    init {
//        generateJadwalList()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun generateJadwalList() {
//        for (peminjaman in peminjamanList) {
//            var currentDate = peminjaman.tanggalMulai
//            while (!currentDate.isAfter(peminjaman.tanggalSelesai)) {
//                val hari = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
//                val waktu = "${peminjaman.jamMulai} - ${peminjaman.jamSelesai}"
//                val organisasi = peminjaman.namaOrganisasi
//
//                // Cari lapangan yang dipinjam berdasarkan id_peminjaman
//                val idLapanganList = lapanganDipinjamList
//                    .filter { it.idPeminjaman == peminjaman.idPeminjaman }
//                    .map { it.idLapangan }
//
//                // Cari nama lapangan dari daftar lapangan
//                val namaLapanganList = lapanganList
//                    .filter { idLapanganList.contains(it.idLapangan) }
//                    .map { it.namaLapangan }
//
//                val namaLapangan = namaLapanganList.joinToString(", ")
//
//                jadwalList.add(
//                    JadwalPeminjaman(
//                        tanggal = currentDate.toString(),
//                        hari = hari,
//                        waktu = waktu,
//                        organisasi = organisasi,
//                        lapangan = namaLapangan
//                    )
//                )
//
//                currentDate = currentDate.plusDays(1) // Tambah 1 hari
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.tabel_jadwal_peminjaman, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val jadwal = jadwalList[position]
//        holder.tvTanggal.text = jadwal.tanggal
//        holder.tvHari.text = jadwal.hari
//        holder.tvWaktu.text = jadwal.waktu
//        holder.tvOrganisasi.text = jadwal.organisasi
//        holder.tvLapangan.text = jadwal.lapangan
//    }
//
//    override fun getItemCount(): Int = jadwalList.size
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
//        val tvHari: TextView = itemView.findViewById(R.id.tvHari)
//        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
//        val tvOrganisasi: TextView = itemView.findViewById(R.id.tvOrganisasi)
//        val tvLapangan: TextView = itemView.findViewById(R.id.tvLapangan)
//    }
//
//    data class JadwalPeminjaman(
//        val tanggal: String,
//        val hari: String,
//        val waktu: String,
//        val organisasi: String,
//        val lapangan: String
//    )
//}
