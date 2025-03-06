package com.example.bismillahsipfo.ui.fragment.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

    private fun setupViews() {
        binding.ivArrow.setOnClickListener { finish() }
        binding.tvProfileInfo.setOnClickListener { finish() }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val user = userRepository.getUser()
            user?.let {
                binding.tvNama.text = it.nama
                binding.tvEmail.text = it.email
                binding.tvNoKartu.text = it.nomorInduk
                binding.tvStatus.text = it.status
                binding.tfNoTelp.setText(it.noTelp)

                Glide.with(this@DetailProfileActivity)
                    .load(it.fotoProfil)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivProfilePicture)

                Glide.with(this@DetailProfileActivity)
                    .load(it.kartuIdentitas)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivKartuIdentitas)
            }
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
            userRepository.updateNoTelp(newNoTelp)

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
                            userRepository.updateKartuIdentitas(publicUrl)
                        }
                    }
                }
            }

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