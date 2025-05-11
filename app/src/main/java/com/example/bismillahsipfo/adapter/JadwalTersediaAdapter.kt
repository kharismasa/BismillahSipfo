package com.example.bismillahsipfo.adapter

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia
import java.time.format.DateTimeFormatter

class JadwalTersediaAdapter(
    private var jadwalList: List<JadwalTersedia>,
    private val onItemClick: (JadwalTersedia) -> Unit
) : RecyclerView.Adapter<JadwalTersediaAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_jadwal_tersedia, parent, false)
        return ViewHolder(view)
    }

    fun updateJadwalList(newList: List<JadwalTersedia>) {
        Log.d("JadwalTersediaAdapter", "Updating jadwal list. New size: ${newList.size}")
        jadwalList = newList
        notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val jadwal = jadwalList[position]
        Log.d("JadwalTersediaAdapter", "Binding view holder for position $position: $jadwal")

        holder.tvDay.text = jadwal.hari
        holder.tvDate.text = jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val jamMulai = jadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm"))
        val jamSelesai = jadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm"))
        holder.tvTime.text = "$jamMulai - $jamSelesai"

        holder.itemView.setOnClickListener {
            val tanggal = jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val message = "Jadwal tersedia terselect:\n${jadwal.hari}, $tanggal\n$jamMulai - $jamSelesai"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            onItemClick(jadwal)
        }
    }

    override fun getItemCount() = jadwalList.size
}