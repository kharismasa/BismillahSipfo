package com.example.bismillahsipfo.adapter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bismillahsipfo.ui.fragment.peminjaman.FormPeminjamanFragment
import com.example.bismillahsipfo.ui.fragment.peminjaman.FormTataTertibFragment
import com.example.bismillahsipfo.ui.fragment.peminjaman.PembayaranFragment

class PeminjamanPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private var sharedData: Bundle = Bundle()

    // Method yang dibutuhkan untuk mengatasi error
    fun updateSharedData(data: Bundle) {
        sharedData.putAll(data)
        Log.d("PeminjamanPagerAdapter", "Data updated: ${sharedData.keySet()}")
    }

    override fun getItemCount(): Int {
        return 3  // Jumlah fragment yang akan ditampilkan di ViewPager
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FormPeminjamanFragment()
            1 -> {
                val fragment = FormTataTertibFragment()
                fragment.arguments = Bundle(sharedData)  // Copy of shared data
                fragment
            }
            2 -> {
                val fragment = PembayaranFragment()
                fragment.arguments = Bundle(sharedData)  // Copy of shared data
                fragment
            }
            else -> FormPeminjamanFragment()
        }
    }
}