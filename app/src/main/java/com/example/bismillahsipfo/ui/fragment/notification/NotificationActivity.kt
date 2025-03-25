package com.example.bismillahsipfo.ui.fragment.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bismillahsipfo.adapter.RowNotifikasiAdapter
import com.example.bismillahsipfo.data.model.Notifikasi
import com.example.bismillahsipfo.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var adapter: RowNotifikasiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish() // Kembali ke fragment home
        }

        binding.tvHalamanNotifikasi.setOnClickListener {
            finish() // Kembali ke fragment home
        }
    }

    private fun setupRecyclerView() {
        binding.rvNotifikasi.layoutManager = LinearLayoutManager(this)
        
        // Contoh data notifikasi (ganti dengan data sebenarnya dari repository atau sumber data lainnya)
//        val notifikasiList = listOf(
//            Notifikasi("Notifikasi 1", "Deskripsi notifikasi 1"),
//            Notifikasi("Notifikasi 2", "Deskripsi notifikasi 2"),
//            Notifikasi("Notifikasi 3", "Deskripsi notifikasi 3")
//        )

//        adapter = RowNotifikasiAdapter(notifikasiList)
        binding.rvNotifikasi.adapter = adapter
    }
}