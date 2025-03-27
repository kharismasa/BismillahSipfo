package com.example.bismillahsipfo.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bismillahsipfo.ui.fragment.peminjaman.FormPeminjamanFragment

import android.util.Log

class PeminjamanPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        Log.d("PeminjamanPagerAdapter", "Creating fragment for position: $position")
        return when (position) {
            0 -> FormPeminjamanFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}