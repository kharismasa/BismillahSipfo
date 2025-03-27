package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bismillahsipfo.R

class PeminjamanActivity : AppCompatActivity(), FragmentNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peminjaman)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, FormPeminjamanFragment())
                .commit()
        }
    }

    override fun navigateToFragment(fragment: Fragment) {
        // Pastikan fragment lama di-remove sebelum mengganti dengan yang baru
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(currentFragment)  // Hapus fragment lama
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)  // Menambahkan ke backstack jika diperlukan
                .commit()
        }
    }
}

interface FragmentNavigator {
    fun navigateToFragment(fragment: Fragment)
}