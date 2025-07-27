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
import androidx.appcompat.app.AlertDialog
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

class DetailProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailProfileBinding
    private lateinit var userRepository: UserRepository

    // Separate URIs for different image types
    private var selectedProfileImageUri: Uri? = null
    private var selectedKartuIdentitasUri: Uri? = null

    // Image selection type enum
    private enum class ImageType {
        PROFILE_PICTURE,
        KARTU_IDENTITAS
    }

    private var currentImageType = ImageType.KARTU_IDENTITAS

    // Activity result launcher for image selection
    private val getImageContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                when (currentImageType) {
                    ImageType.PROFILE_PICTURE -> {
                        selectedProfileImageUri = uri
                        Glide.with(this)
                            .load(uri)
                            .transform(CircleCrop())
                            .into(binding.ivProfilePicture)
                        Log.d("DetailProfile", "Profile picture selected: $uri")
                    }
                    ImageType.KARTU_IDENTITAS -> {
                        selectedKartuIdentitasUri = uri
                        Glide.with(this)
                            .load(uri)
                            .into(binding.ivKartuIdentitas)
                        Log.d("DetailProfile", "Kartu identitas selected: $uri")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
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
        val userCardImage = sharedPreferences.getString("kartu_identitas", null)

        binding.tvNama.text = userName
        binding.tvEmail.text = userEmail
        binding.tvNoKartu.text = sharedPreferences.getString("nomor_induk", "")
        binding.tvStatus.text = sharedPreferences.getString("status", "")
        binding.tfNoTelp.setText(userPhone)

        // Load profile picture
        if (userProfileImage != null && userProfileImage.isNotEmpty()) {
            Log.d("DetailProfile", "Loading profile image: $userProfileImage")
            Glide.with(this)
                .load(userProfileImage)
                .transform(CircleCrop())
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(binding.ivProfilePicture)
        }

        // Load kartu identitas
        if (userCardImage != null && userCardImage.isNotEmpty()) {
            Log.d("DetailProfile", "Loading kartu identitas: $userCardImage")
            Glide.with(this)
                .load(userCardImage)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(binding.ivKartuIdentitas)
        }
    }

    private fun setupListeners() {
        // Profile picture edit button
        binding.btnEditProfilePicture.setOnClickListener {
            showImageSelectionDialog(ImageType.PROFILE_PICTURE)
        }

        // Kartu identitas click
        binding.ivKartuIdentitas.setOnClickListener {
            showImageSelectionDialog(ImageType.KARTU_IDENTITAS)
        }

        // Save changes button
        binding.btnSimpanPerubahan.setOnClickListener {
            saveChanges()
        }
    }

    private fun showImageSelectionDialog(imageType: ImageType) {
        val title = when (imageType) {
            ImageType.PROFILE_PICTURE -> "Pilih Foto Profil"
            ImageType.KARTU_IDENTITAS -> "Pilih Kartu Identitas"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Pilih gambar dari galeri")
            .setPositiveButton("Galeri") { _, _ ->
                openImagePicker(imageType)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openImagePicker(imageType: ImageType) {
        currentImageType = imageType
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        getImageContent.launch(intent)
    }

    private fun saveChanges() {
        lifecycleScope.launch {
            var hasError = false
            val loadingDialog = createLoadingDialog()
            loadingDialog.show()

            try {
                // Update phone number
                val newNoTelp = binding.tfNoTelp.text.toString().trim()
                if (newNoTelp.isNotEmpty()) {
                    userRepository.updateNoTelp(newNoTelp)
                    Log.d("DetailProfile", "Phone number updated successfully")
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this@DetailProfileActivity, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ UPLOAD PROFILE PICTURE WITH DELETE OLD
                selectedProfileImageUri?.let { uri ->
                    Log.d("DetailProfile", "Uploading profile picture...")
                    val profileUploadResult = uploadImageWithDeleteOld(
                        uri = uri,
                        bucketName = "Foto Profil",
                        filePrefix = "profile_",
                        oldImageKey = "foto_profil"
                    )
                    if (profileUploadResult != null) {
                        userRepository.updateProfilePicture(profileUploadResult)
                        Log.d("DetailProfile", "Profile picture updated successfully: $profileUploadResult")
                    } else {
                        hasError = true
                        Log.e("DetailProfile", "Failed to upload profile picture")
                    }
                }

                // ✅ UPLOAD KARTU IDENTITAS WITH DELETE OLD
                selectedKartuIdentitasUri?.let { uri ->
                    Log.d("DetailProfile", "Uploading kartu identitas...")
                    val kartuUploadResult = uploadImageWithDeleteOld(
                        uri = uri,
                        bucketName = "Kartu Identitas",
                        filePrefix = "kartu_",
                        oldImageKey = "kartu_identitas"
                    )
                    if (kartuUploadResult != null) {
                        userRepository.updateKartuIdentitas(kartuUploadResult)
                        Log.d("DetailProfile", "Kartu identitas updated successfully: $kartuUploadResult")
                    } else {
                        hasError = true
                        Log.e("DetailProfile", "Failed to upload kartu identitas")
                    }
                }

                loadingDialog.dismiss()

                if (hasError) {
                    Toast.makeText(this@DetailProfileActivity, "Beberapa data gagal disimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@DetailProfileActivity, "Semua perubahan berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e("DetailProfile", "Error saving changes: ${e.message}", e)
                Toast.makeText(this@DetailProfileActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ NEW FUNCTION: Upload image and delete old one
    private suspend fun uploadImageWithDeleteOld(
        uri: Uri,
        bucketName: String,
        filePrefix: String,
        oldImageKey: String
    ): String? {
        return try {
            Log.d("DetailProfile", "Starting upload with delete old to bucket: $bucketName")

            // Get old image URL from SharedPreferences
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val oldImageUrl = sharedPreferences.getString(oldImageKey, null)

            // Read new file as bytes
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val byteArray = inputStream?.readBytes()
            inputStream?.close()

            if (byteArray == null) {
                Log.e("DetailProfile", "Failed to read file bytes")
                return null
            }

            Log.d("DetailProfile", "New file size: ${byteArray.size} bytes")

            // Generate unique filename for new image
            val newFileName = "${filePrefix}${UUID.randomUUID()}.jpg"
            Log.d("DetailProfile", "Generated new filename: $newFileName")

            // Get storage bucket
            val bucket = userRepository.getSupabaseClient().storage.from(bucketName)

            // Upload new file first
            bucket.upload(newFileName, byteArray)
            val newPublicUrl = bucket.publicUrl(newFileName)
            Log.d("DetailProfile", "New image uploaded successfully: $newPublicUrl")

            // ✅ DELETE OLD IMAGE if exists
            if (!oldImageUrl.isNullOrEmpty()) {
                try {
                    val oldFileName = extractFileNameFromUrl(oldImageUrl)
                    if (oldFileName != null) {
                        Log.d("DetailProfile", "Attempting to delete old image: $oldFileName")
                        bucket.delete(oldFileName)
                        Log.d("DetailProfile", "✅ Old image deleted successfully: $oldFileName")
                    } else {
                        Log.w("DetailProfile", "Could not extract filename from old URL: $oldImageUrl")
                    }
                } catch (deleteError: Exception) {
                    // Don't fail the entire operation if delete fails
                    Log.w("DetailProfile", "⚠️ Failed to delete old image: ${deleteError.message}")
                }
            } else {
                Log.d("DetailProfile", "No old image to delete (first upload)")
            }

            newPublicUrl

        } catch (e: Exception) {
            Log.e("DetailProfile", "Upload error for $bucketName: ${e.message}", e)
            Toast.makeText(this, "Gagal upload gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // ✅ HELPER FUNCTION: Extract filename from Supabase URL
    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            // Supabase storage URL format:
            // https://project.supabase.co/storage/v1/object/public/bucket-name/filename.ext
            val parts = url.split("/")
            if (parts.isNotEmpty()) {
                parts.last() // Get the last part which should be the filename
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DetailProfile", "Error extracting filename from URL: $url", e)
            null
        }
    }

    // ✅ ALTERNATIVE HELPER: More robust filename extraction
    private fun extractFileNameFromSupabaseUrl(url: String): String? {
        return try {
            // Handle various Supabase URL formats
            when {
                url.contains("/storage/v1/object/public/") -> {
                    // Standard Supabase public URL
                    val publicIndex = url.indexOf("/storage/v1/object/public/")
                    val afterPublic = url.substring(publicIndex + "/storage/v1/object/public/".length)
                    val bucketAndFile = afterPublic.substringAfter("/") // Remove bucket name
                    bucketAndFile
                }
                url.contains("/object/public/") -> {
                    // Alternative format
                    val publicIndex = url.indexOf("/object/public/")
                    val afterPublic = url.substring(publicIndex + "/object/public/".length)
                    val bucketAndFile = afterPublic.substringAfter("/")
                    bucketAndFile
                }
                else -> {
                    // Fallback: just get the last part of URL
                    url.substringAfterLast("/")
                }
            }
        } catch (e: Exception) {
            Log.e("DetailProfile", "Error extracting filename from Supabase URL: $url", e)
            null
        }
    }

    private fun createLoadingDialog(): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle("Menyimpan Perubahan")
            .setMessage("Sedang mengupload gambar dan menyimpan data...")
            .setCancelable(false)
            .create()
    }
}