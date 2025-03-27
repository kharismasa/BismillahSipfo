package com.example.bismillahsipfo.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bismillahsipfo.ui.fragment.peminjaman.FormPeminjamanFragment

import android.util.Log
import com.example.bismillahsipfo.ui.fragment.peminjaman.FormTataTertibFragment
import com.example.bismillahsipfo.ui.fragment.peminjaman.PembayaranFragment

class PeminjamanPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3  // Jumlah fragment yang akan ditampilkan di ViewPager
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FormPeminjamanFragment()  // Ganti dengan fragment pertama
            1 -> FormTataTertibFragment()  // Ganti dengan fragment kedua
            2 -> PembayaranFragment()  // Ganti dengan fragment ketiga
            else -> FormPeminjamanFragment()
        }
    }
}