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
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.User
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.databinding.ActivityDetailProfileBinding
import com.example.bismillahsipfo.ui.fragment.login.LoginActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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

                // Setelah update berhasil, simpan di SharedPreferences
                val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("no_telp", newNoTelp) // Update nomor telepon di SharedPreferences
                    apply()
                }

                // Setelah update berhasil, beri feedback ke pengguna
                Toast.makeText(this@DetailProfileActivity, "Nomor Telepon berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Jika nomor telepon kosong, beri peringatan ke pengguna
                Toast.makeText(this@DetailProfileActivity, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }

            // Cek jika ada perubahan gambar kartu identitas dan upload ke Supabase
            selectedImageUri?.let { uri ->
                val file = File(getRealPathFromURI(uri))

                // Mengonversi file menjadi ByteArray
                val byteArray = file.readBytes()
                val fileName = "kartu_identitas_${System.currentTimeMillis()}.jpg" // Nama file unik berdasarkan waktu

                // Upload ke bucket "Kartu Identitas"
                val bucket = userRepository.getSupabaseClient().storage.from("Kartu Identitas")

                try {
                    // Ambil URL gambar lama dari SharedPreferences
                    val oldImageUrl = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .getString("kartu_identitas", null)

                    oldImageUrl?.let {
                        val oldFileName = it.substringAfterLast("/")

                        // Menggantikan file lama dengan yang baru menggunakan update()
                        bucket.update(oldFileName, byteArray) {
                            // Gambar yang baru akan menggantikan gambar yang lama
                        }

                        Log.d("DetailProfileActivity", "Gambar lama digantikan dengan: $oldFileName")
                    }

                    // Setelah mengganti gambar, ambil URL publik file setelah di-upload
                    val publicUrl = bucket.publicUrl(fileName)

                    // Simpan URL gambar baru ke Supabase di kolom "kartu_identitas"
                    userRepository.updateKartuIdentitas(publicUrl)

                    Toast.makeText(this@DetailProfileActivity, "Kartu Identitas berhasil diupdate", Toast.LENGTH_SHORT).show()

                }
                catch (e: Exception) {
                    // Tangani error
                    Log.e("DetailProfileActivity", "Terjadi kesalahan saat mengupload: ${e.message}")
                    Toast.makeText(this@DetailProfileActivity, "Terjadi kesalahan saat mengupload", Toast.LENGTH_SHORT).show()
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