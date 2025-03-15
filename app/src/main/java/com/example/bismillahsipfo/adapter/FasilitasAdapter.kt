package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.databinding.CardFasilitasBinding

class FasilitasAdapter(
    private val fasilitasList: List<Fasilitas>,
    private val onItemClick: (Fasilitas) -> Unit )
    : RecyclerView.Adapter<FasilitasAdapter.FasilitasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FasilitasViewHolder {
        val binding = CardFasilitasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FasilitasViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FasilitasViewHolder, position: Int) {
        val fasilitas = fasilitasList[position]
        holder.bind(fasilitas)
        holder.itemView.setOnClickListener { onItemClick(fasilitas) }
    }

    override fun getItemCount(): Int = fasilitasList.size

    class FasilitasViewHolder(private val binding: CardFasilitasBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fasilitas: Fasilitas) {
            binding.fasilitasTitle.text = fasilitas.namaFasilitas
            Glide.with(itemView.context)
                .load(fasilitas.photo)
                .into(binding.imageFasilitas)
        }
    }
}
