package com.example.bismillahsipfo.ui.fragment.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bismillahsipfo.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Implementasi logika untuk menampilkan notifikasi
        // Misalnya, mengambil data notifikasi dari repository dan menampilkannya
    }
}