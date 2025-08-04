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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia
import java.time.format.DateTimeFormatter

class JadwalTersediaAdapter(
    private var jadwalList: List<JadwalTersedia>,
    private val onItemClick: (JadwalTersedia) -> Unit
) : RecyclerView.Adapter<JadwalTersediaAdapter.ViewHolder>() {

    private lateinit var context: Context
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
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

        // Reset selection jika data berubah secara signifikan
        val shouldResetSelection = jadwalList.size != newList.size ||
                (selectedPosition != RecyclerView.NO_POSITION &&
                        selectedPosition >= newList.size)

        jadwalList = newList

        if (shouldResetSelection) {
            selectedPosition = RecyclerView.NO_POSITION
        }

        notifyDataSetChanged()
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
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

        // Set background color berdasarkan selection state
        updateCardBackground(holder, position)

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = currentPosition

            // Update background untuk item yang sebelumnya dipilih
            if (previousPosition != RecyclerView.NO_POSITION && previousPosition != currentPosition) {
                notifyItemChanged(previousPosition)
            }

            // Update background untuk item yang baru dipilih
            notifyItemChanged(currentPosition)

            // Pastikan data masih valid pada posisi saat ini
            if (currentPosition < jadwalList.size) {
                val selectedJadwal = jadwalList[currentPosition]
                val tanggal = selectedJadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val jamMulaiSelected = selectedJadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm"))
                val jamSelesaiSelected = selectedJadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm"))
                val message = "Jadwal tersedia terselect:\n${selectedJadwal.hari}, $tanggal\n$jamMulaiSelected - $jamSelesaiSelected"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                onItemClick(selectedJadwal)
            }
        }
    }

    private fun updateCardBackground(holder: ViewHolder, position: Int) {
        // Gunakan position parameter yang diterima dari onBindViewHolder untuk initial setup
        // karena ini dipanggil langsung dari onBindViewHolder dengan position yang valid
        if (position == selectedPosition) {
            // Warna untuk item yang dipilih
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.selected_card_background)
            )
            holder.cardView.elevation = 8f
            holder.cardView.cardElevation = 8f

            // Update text color untuk item yang dipilih
            holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.selected_text_color))
            holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.selected_date_color))
            holder.tvTime.setTextColor(ContextCompat.getColor(context, R.color.selected_text_color))
        } else {
            // Warna untuk item yang tidak dipilih (default)
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.cardView.elevation = 4f
            holder.cardView.cardElevation = 4f

            // Restore original text colors
            holder.tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.dark_blue))
            holder.tvTime.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
    }

    override fun getItemCount() = jadwalList.size
}