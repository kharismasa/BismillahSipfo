package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.databinding.RowJadwalDipinjamBinding

class JadwalDipinjamAdapter : ListAdapter<PeminjamanFasilitas, JadwalDipinjamAdapter.JadwalDipinjamViewHolder>(JadwalDipinjamDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalDipinjamViewHolder {
        val binding = RowJadwalDipinjamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JadwalDipinjamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JadwalDipinjamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JadwalDipinjamViewHolder(private val binding: RowJadwalDipinjamBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(jadwalPinjam: PeminjamanFasilitas) {
            // Bind data to views in row_jadwal_pinjam.xml
            // Example:
            // binding.textViewFasilitas.text = jadwalPinjam.namaFasilitas
            // binding.textViewTanggal.text = jadwalPinjam.tanggal
            // ...
        }
    }

    class JadwalDipinjamDiffCallback : DiffUtil.ItemCallback<PeminjamanFasilitas>() {
        override fun areItemsTheSame(oldItem: PeminjamanFasilitas, newItem: PeminjamanFasilitas): Boolean {
            return oldItem.idPeminjaman == newItem.idPeminjaman
        }

        override fun areContentsTheSame(oldItem: PeminjamanFasilitas, newItem: PeminjamanFasilitas): Boolean {
            return oldItem == newItem
        }
    }
}