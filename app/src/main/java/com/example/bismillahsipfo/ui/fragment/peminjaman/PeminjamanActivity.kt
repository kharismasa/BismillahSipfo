package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.PeminjamanPagerAdapter

class PeminjamanActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager2
    private lateinit var peminjamanPagerAdapter: PeminjamanPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peminjaman)

        viewPager = findViewById(R.id.viewPager)
        peminjamanPagerAdapter = PeminjamanPagerAdapter(this)  // Inisialisasi adapter
        viewPager.adapter = peminjamanPagerAdapter  // Set adapter ke ViewPager2
    }
}
