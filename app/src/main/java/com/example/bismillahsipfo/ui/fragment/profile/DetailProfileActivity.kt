package com.example.bismillahsipfo.ui.fragment.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.databinding.ActivityDetailProfileBinding
import com.example.bismillahsipfo.ui.fragment.login.LoginActivity
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.launch
import java.io.File

class DetailProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProfileBinding
    private lateinit var userRepository: UserRepository
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.ivKartuIdentitas)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek apakah pengguna sudah login
        if (!isUserLoggedIn()) {
            // Jika pengguna belum login, arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        userRepository = UserRepository(this)

        setupViews()
        loadUserData()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun setupViews() {
        binding.ivArrow.setOnClickListener { finish() }
        binding.tvProfileInfo.setOnClickListener { finish() }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("nama", "Nama Pengguna")
        val userEmail = sharedPreferences.getString("email", "")
        val userProfileImage = sharedPreferences.getString("foto_profil", null)
        val userPhone = sharedPreferences.getString("no_telp", "")

        binding.tvNama.text = userName
        binding.tvEmail.text = userEmail
        binding.tvNoKartu.text = sharedPreferences.getString("nomor_induk", "")
        binding.tvStatus.text = sharedPreferences.getString("status", "")
        binding.tfNoTelp.setText(userPhone)

        if (userProfileImage != null) {
            Glide.with(this)
                .load(userProfileImage)
                .transform(CircleCrop())
                .placeholder(R.drawable.placeholder)
                .into(binding.ivProfilePicture)
        }

        // Jika data kartu identitas sudah ada di SharedPreferences
        val userCardImage = sharedPreferences.getString("kartu_identitas", null)
        if (userCardImage != null) {
            Glide.with(this)
                .load(userCardImage)
                .placeholder(R.drawable.placeholder)
                .into(binding.ivKartuIdentitas)
        }
    }


    private fun setupListeners() {
        binding.ivKartuIdentitas.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        binding.btnSimpanPerubahan.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        lifecycleScope.launch {
            val newNoTelp = binding.tfNoTelp.text.toString()

            // Pastikan nomor telepon yang baru diambil dari input
            if (newNoTelp.isNotEmpty()) {
                // Mengupdate nomor telepon di Supabase
                userRepository.updateNoTelp(newNoTelp)

                // Setelah update berhasil, simpan perubahan nomor telepon di SharedPreferences
                val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("no_telp", newNoTelp) // Update nomor telepon di SharedPreferences
                    apply()
                }

                // Setelah update berhasil, beri feedback ke pengguna dan tutup activity
                Toast.makeText(this@DetailProfileActivity, "Nomor Telepon berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish() // Menutup activity setelah berhasil mengupdate data
            } else {
                // Jika nomor telepon kosong, beri peringatan ke pengguna
                Toast.makeText(this@DetailProfileActivity, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }

            // Cek jika ada perubahan gambar kartu identitas dan upload ke Supabase
            selectedImageUri?.let { uri ->
                val file = File(getRealPathFromURI(uri))
                val bucket = userRepository.getSupabaseClient().storage.from("kartu_identitas")

                bucket.uploadAsFlow(file.name, file).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            val progress = status.totalBytesSend.toFloat() / status.contentLength * 100
                            // Update progress UI if needed
                        }
                        is UploadStatus.Success -> {
                            val publicUrl = bucket.publicUrl(file.name)
                            // Update URL gambar kartu identitas di Supabase
                            userRepository.updateKartuIdentitas(publicUrl)
                        }
                    }
                }
            }

            // Menutup activity setelah semua perubahan berhasil disimpan
            finish()
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        val result = cursor?.getString(idx ?: 0)
        cursor?.close()
        return result ?: ""
    }
}