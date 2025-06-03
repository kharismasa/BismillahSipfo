package com.example.bismillahsipfo.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.databinding.ActivityMainBinding
import com.example.bismillahsipfo.ui.fragment.login.LoginActivity
import com.example.bismillahsipfo.ui.fragment.peminjaman.PeminjamanActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            // Di sini Anda bisa memeriksa lebih lanjut di database jika diperlukan
        } else {
            // Arahkan ke LoginActivity jika belum login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Toolbar sebagai ActionBar
        setSupportActionBar(binding.toolbar)
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_riwayat, R.id.navigation_peminjaman, R.id.navigation_gamifikasi, R.id.navigation_profil
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Setup custom navigation untuk peminjaman
        setupCustomNavigation(navView)

        // Check apakah ada intent untuk navigasi ke home
        handleNavigateToHome()
    }

    private fun setupCustomNavigation(navView: BottomNavigationView) {
        navView.setOnItemSelectedListener { menuItem ->
            val navController = findNavController(R.id.nav_host_fragment_activity_main)

            when (menuItem.itemId) {
                R.id.navigation_peminjaman -> {
                    // Navigate to PeminjamanActivity instead of using navigation
                    startActivity(Intent(this, PeminjamanActivity::class.java))
                    false // Don't change selection
                }
                else -> {
                    // Handle other navigation items normally
                    try {
                        navController.navigate(menuItem.itemId)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
    }

    private fun handleNavigateToHome() {
        val navigateToHome = intent.getBooleanExtra("NAVIGATE_TO_HOME", false)

        if (navigateToHome) {
            // Reset intent agar tidak trigger lagi
            intent.removeExtra("NAVIGATE_TO_HOME")

            // Navigate to home using NavController
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            navController.navigate(R.id.navigation_home)

            // Set bottom navigation selection
            binding.navView.selectedItemId = R.id.navigation_home
        }
    }

    override fun onResume() {
        super.onResume()

        // Check again untuk memastikan navigation yang benar saat activity resume
        handleNavigateToHome()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new intent jika activity sudah ada di background
        handleNavigateToHome()
    }
}