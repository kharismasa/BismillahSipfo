package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.data.model.JadwalRutin
import com.example.bismillahsipfo.databinding.TabelJadwalRutinBinding

class TabelJadwalRutinAdapter(private var jadwalRutinList: List<JadwalRutin>) :
    RecyclerView.Adapter<TabelJadwalRutinAdapter.ViewHolder>() {

    class ViewHolder(private val binding: TabelJadwalRutinBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(jadwalRutin: JadwalRutin) {
            binding.tvHari.text = jadwalRutin.hari
            binding.tvWaktu.text = "${jadwalRutin.waktuMulai} - ${jadwalRutin.waktuSelesai}"
            binding.tvOrganisasi.text = jadwalRutin.namaOrganisasi
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TabelJadwalRutinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(jadwalRutinList[position])
    }

    override fun getItemCount() = jadwalRutinList.size

    fun updateData(newList: List<JadwalRutin>) {
        jadwalRutinList = newList
        notifyDataSetChanged()
    }
}