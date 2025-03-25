package com.example.bismillahsipfo.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.databinding.RowJadwalDipinjamBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

class RowJadwalDipinjamAdapter : ListAdapter<Pair<PeminjamanFasilitas, Fasilitas>, RowJadwalDipinjamAdapter.JadwalDipinjamViewHolder>(JadwalDipinjamDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalDipinjamViewHolder {
        val binding = RowJadwalDipinjamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JadwalDipinjamViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: JadwalDipinjamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JadwalDipinjamViewHolder(private val binding: RowJadwalDipinjamBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(pair: Pair<PeminjamanFasilitas, Fasilitas>) {
            val (peminjaman, fasilitas) = pair
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
//            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))  // Menggunakan Locale Indonesia

            binding.tvTime.text = "${peminjaman.jamMulai.format(timeFormatter)} - ${peminjaman.jamSelesai.format(timeFormatter)}"
//            binding.tvTime.text = "${peminjaman.jamMulai.format(timeFormatter)}"
            binding.tvDate.text = peminjaman.tanggalMulai.format(dateFormatter)
            binding.tvEvent.text = peminjaman.namaAcara
            binding.tvFasilitas.text = fasilitas.namaFasilitas
            binding.tvAlamat.text = fasilitas.alamat
        }
    }

    class JadwalDipinjamDiffCallback : DiffUtil.ItemCallback<Pair<PeminjamanFasilitas, Fasilitas>>() {
        override fun areItemsTheSame(oldItem: Pair<PeminjamanFasilitas, Fasilitas>, newItem: Pair<PeminjamanFasilitas, Fasilitas>): Boolean {
            return oldItem.first.idPeminjaman == newItem.first.idPeminjaman && oldItem.first.tanggalMulai == newItem.first.tanggalMulai
        }

        override fun areContentsTheSame(oldItem: Pair<PeminjamanFasilitas, Fasilitas>, newItem: Pair<PeminjamanFasilitas, Fasilitas>): Boolean {
            return oldItem == newItem
        }
    }
}