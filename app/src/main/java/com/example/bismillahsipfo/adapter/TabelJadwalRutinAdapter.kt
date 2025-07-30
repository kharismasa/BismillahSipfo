package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.data.model.JadwalRutinWithOrganisasi
import com.example.bismillahsipfo.databinding.TabelJadwalRutinBinding

class TabelJadwalRutinAdapter(
    private var originalJadwalRutinList: List<JadwalRutinWithOrganisasi>,
    private var filteredJadwalRutinList: List<JadwalRutinWithOrganisasi> = originalJadwalRutinList
) : RecyclerView.Adapter<TabelJadwalRutinAdapter.ViewHolder>() {

    class ViewHolder(private val binding: TabelJadwalRutinBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(jadwalRutinWithOrganisasi: JadwalRutinWithOrganisasi) {
            val jadwalRutin = jadwalRutinWithOrganisasi.jadwalRutin
            binding.tvHari.text = jadwalRutin.hari
            binding.tvWaktu.text = "${jadwalRutin.waktuMulai} - ${jadwalRutin.waktuSelesai}"
            binding.tvOrganisasi.text = jadwalRutinWithOrganisasi.namaOrganisasi
            binding.tvLapangan.text = jadwalRutinWithOrganisasi.namaLapangan.joinToString(", ")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TabelJadwalRutinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredJadwalRutinList[position])
    }

    override fun getItemCount() = filteredJadwalRutinList.size

    /**
     * Update data dengan list baru
     */
    fun updateData(newList: List<JadwalRutinWithOrganisasi>) {
        originalJadwalRutinList = newList
        filteredJadwalRutinList = newList
        notifyDataSetChanged()
    }

    /**
     * Update filtered data (untuk hasil search/filter)
     */
    fun updateFilteredData(filteredList: List<JadwalRutinWithOrganisasi>) {
        filteredJadwalRutinList = filteredList
        notifyDataSetChanged()
    }

    /**
     * Get original data (untuk filter operations)
     */
    fun getOriginalData(): List<JadwalRutinWithOrganisasi> {
        return originalJadwalRutinList
    }

    /**
     * Reset filter (show all data)
     */
    fun resetFilter() {
        filteredJadwalRutinList = originalJadwalRutinList
        notifyDataSetChanged()
    }
}