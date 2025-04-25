package com.example.bismillahsipfo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.Voucher
import com.example.bismillahsipfo.databinding.RowDiskonBinding

class RowDiskonAdapter : ListAdapter<Pair<Voucher, Boolean>, RowDiskonAdapter.VoucherViewHolder>(VoucherDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = RowDiskonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val (voucher, isActive) = getItem(position) // Periksa status aktif atau tidak
        holder.bind(voucher, isActive)
    }

    class VoucherViewHolder(private val binding: RowDiskonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(voucher: Voucher, isActive: Boolean) {
            // Memuat gambar voucher menggunakan Glide
            Glide.with(binding.root)
                .load(voucher.gambarVoucher)
                .placeholder(R.drawable.placeholder) // Placeholder jika gambar belum dimuat
                .error(R.drawable.empty) // Gambar fallback jika gagal
                .into(binding.imageVoucher) // Memasukkan gambar ke ImageView

            // Sesuaikan visibilitas nonActive berdasarkan status voucher
            binding.nonActive.visibility = if (isActive) View.GONE else View.VISIBLE
        }
    }

    class VoucherDiffCallback : DiffUtil.ItemCallback<Pair<Voucher, Boolean>>() {
        override fun areItemsTheSame(oldItem: Pair<Voucher, Boolean>, newItem: Pair<Voucher, Boolean>): Boolean {
            return oldItem.first.idVoucher == newItem.first.idVoucher
        }

        override fun areContentsTheSame(oldItem: Pair<Voucher, Boolean>, newItem: Pair<Voucher, Boolean>): Boolean {
            return oldItem == newItem
        }
    }
}

