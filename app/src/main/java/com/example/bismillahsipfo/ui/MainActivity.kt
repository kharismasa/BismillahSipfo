package com.example.bismillahsipfo.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.databinding.ActivityMainBinding
import com.example.bismillahsipfo.ui.fragment.login.LoginActivity
import com.example.bismillahsipfo.ui.fragment.peminjaman.PeminjamanActivity
import com.example.bismillahsipfo.utils.DebugHelper
import com.example.bismillahsipfo.utils.UserDebugHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test API configuration dan USER SESSION
        testApiAndUserConfiguration()

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

    private fun testApiAndUserConfiguration() {
        // Log API configuration
        DebugHelper.logApiInfo(this)

        // Test network connectivity
        val isConnected = DebugHelper.testNetworkConnectivity(this)

        // Test API endpoint configuration
        DebugHelper.testApiEndpoint(this)

        // DEBUG USER SESSION - INI YANG PENTING!
        UserDebugHelper.debugUserSession(this)
        UserDebugHelper.checkDatabaseTables(this)

        if (isConnected) {
            // Test API call
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val repository = FasilitasRepository()
                    val fasilitas = repository.getFasilitas()

                    Log.d("MainActivity", "✅ API Test Success: Found ${fasilitas.size} fasilitas")

                    // TAMBAHAN: Test user-specific data
                    val userRepository = UserRepository(this@MainActivity)
                    val currentUserId = userRepository.getCurrentUserId()

                    if (currentUserId != -1) {
                        Log.d("MainActivity", "🧪 Testing user-specific queries for user $currentUserId")

                        // Test the exact same queries that are failing
                        val debugResults = repository.debugUserQueries(currentUserId)
                        Log.d("MainActivity", "🧪 Debug Results:\n$debugResults")

                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this@MainActivity,
                                "API OK! Facilities: ${fasilitas.size}. Check logs for user data.",
                                Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("MainActivity", "❌ User ID is -1! Login session may be corrupted.")

                        if (BuildConfig.DEBUG) {
                            Toast.makeText(this@MainActivity,
                                "⚠️ User ID is -1! Check login.",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ API Test Failed: ${e.message}", e)

                    // Show error details for debugging
                    if (BuildConfig.DEBUG) {
                        val errorMessage = when {
                            e.message?.contains("UnknownHostException") == true -> "Cannot reach server: ${BuildConfig.BASE_URL}"
                            e.message?.contains("SSLException") == true -> "SSL/Certificate error"
                            e.message?.contains("ConnectException") == true -> "Connection refused"
                            e.message?.contains("SocketTimeoutException") == true -> "Request timeout"
                            else -> "API Failed: ${e.message}"
                        }

                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Log.e("MainActivity", "❌ No network connection available")
        }
    }

    // TAMBAHAN: Method untuk testing user session secara manual
    private fun setupDebugMenu() {
        if (BuildConfig.DEBUG) {
            // Add debug options - bisa dipanggil lewat gesture atau button
            binding.navView.setOnLongClickListener {
                showDebugOptions()
                true
            }
        }
    }

    private fun showDebugOptions() {
        val options = arrayOf(
            "Debug User Session",
            "Check Database Tables",
            "Simulate Login User 1",
            "Simulate Login User 2",
            "Clear User Data"
        )

        AlertDialog.Builder(this)
            .setTitle("Debug Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> UserDebugHelper.debugUserSession(this)
                    1 -> UserDebugHelper.checkDatabaseTables(this)
                    2 -> UserDebugHelper.simulateLogin(this, 1)
                    3 -> UserDebugHelper.simulateLogin(this, 2)
                    4 -> clearUserData()
                }
            }
            .show()
    }

    private fun clearUserData() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this, "User data cleared", Toast.LENGTH_SHORT).show()

        // Restart login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
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