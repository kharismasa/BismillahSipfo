package com.example.bismillahsipfo.ui.fragment.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.databinding.ActivityLoginBinding
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.ui.MainActivity
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.BASE_URL,
            supabaseKey = BuildConfig.API_KEY
        ) {
            install(Postgrest)
        }

        userRepository = UserRepository(this)

        setupAction()
    }

    private fun setupAction() {
        binding.loginButton.setOnClickListener {
            val email = binding.edLoginEmail.text.toString()
            val password = binding.edLoginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val user = userRepository.loginUser(email, password)
                    withContext(Dispatchers.Main) {
                        if (user != null) {
                            // Simpan informasi pengguna menggunakan UserRepository
//                            userRepository.saveUserId(user.idPengguna)
                            // Simpan informasi tambahan jika diperlukan
                            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putInt("id_pengguna", user.idPengguna)
                                putString("email", user.email)
                                putString("nama", user.nama)
                                putString("nomor_induk", user.nomorInduk)
                                putString("status", user.status)
                                putString("no_telp", user.noTelp)
                                putInt("id_gamifikasi", user.idGamifikasi)
                                putString("kartu_identitas", user.kartuIdentitas)
                                putString("foto_profil", user.fotoProfil)
                                putBoolean("is_logged_in", true) // Tambahkan ini untuk menyimpan status login
                                apply()
                            }

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                            Log.d("LoginActivity", "Saving user data: ${user.email}")
                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}