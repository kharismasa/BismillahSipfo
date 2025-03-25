package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.data.model.Notifikasi
import com.example.bismillahsipfo.databinding.RowNotifikasiBinding

class RowNotifikasiAdapter(private val notifikasi: List<Notifikasi>) : 
    RecyclerView.Adapter<RowNotifikasiAdapter.NotifikasiViewHolder>() {

    inner class NotifikasiViewHolder(private val binding: RowNotifikasiBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(notifikasi: Notifikasi) {
//            binding.textViewTitle.text = notifikasi.judul
//            binding.textViewDescription.text = notifikasi.deskripsi
            // Jika Anda memiliki gambar untuk notifikasi, Anda bisa mengaturnya di sini
            // binding.imageViewIcon.setImageResource(notifikasi.iconResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifikasiViewHolder {
        val binding = RowNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifikasiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifikasiViewHolder, position: Int) {
        holder.bind(notifikasi[position])
    }

    override fun getItemCount() = notifikasi.size
}