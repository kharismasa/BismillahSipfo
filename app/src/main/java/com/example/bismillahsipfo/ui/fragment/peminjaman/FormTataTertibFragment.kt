package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia

class FormTataTertibFragment : Fragment() {

    private val PDF_REQUEST_CODE = 123
    private var selectedPdfUri: Uri? = null

    private var idFasilitas: Int = -1
    private var namaFasilitas: String? = null
    private var opsiPeminjaman: String? = null
    private var namaAcara: String? = null
    private var namaOrganisasi: String? = null
    private var jadwalTersedia: JadwalTersedia? = null

    // UI components
    private lateinit var tvUploadSurat: TextView
    private lateinit var containerSurat: LinearLayout
    private lateinit var btnUpload: Button
    private lateinit var tvFileStatus: TextView
    private lateinit var checkboxTataTertib: CheckBox
    private lateinit var buttonNext: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_form_tata_tertib, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        tvUploadSurat = view.findViewById(R.id.tvUploadSurat)
        containerSurat = view.findViewById(R.id.container_surat)
        btnUpload = view.findViewById(R.id.btnUpload)
        tvFileStatus = view.findViewById(R.id.tvFileStatus)
        checkboxTataTertib = view.findViewById(R.id.checkbox_tata_tertib)
        buttonNext = view.findViewById(R.id.button_next)

        // Retrieve data from arguments
        retrieveDataFromExtras()

        // Initialize views
        setupViews(view)

        // Setup upload section visibility based on opsiPeminjaman
        setupUploadSectionVisibility()

        // Setup file upload button click listener
        setupUploadButton()

        // Setup checkbox listener to update Next button state
        setupCheckboxListener()

        // Display data
        displayData()
    }

    private fun retrieveDataFromExtras() {
        arguments?.let { bundle ->
            // Basic data
            idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)

            // Conditional data based on opsiPeminjaman
            when (opsiPeminjaman) {
                "Sesuai Jadwal Rutin" -> {
                    val idOrganisasi = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_ORGANISASI, -1)
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)
                    jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                    val listLapangan = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LIST_LAPANGAN)

                    // Log data retrieved
                    Log.d("FormTataTertibFragment", "Jadwal Tersedia: $jadwalTersedia, List Lapangan: $listLapangan")

                    // Get tanggal and jam even if jadwalTersedia is null
                    val tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                    val tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                    val jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                    val jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)

                    Log.d("FormTataTertibFragment", "Tanggal: $tanggalMulai - $tanggalSelesai, Jam: $jamMulai - $jamSelesai")

                    if (idFasilitas == 30) {
                        val penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                        Log.d("FormTataTertibFragment", "Pengguna Khusus: $penggunaKhusus")
                    }
                }
                "Diluar Jadwal Rutin" -> {
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)
                    if (idFasilitas == 30) {
                        jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                        val penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                        Log.d("FormTataTertibFragment", "Jadwal Tersedia: $jadwalTersedia, Pengguna Khusus: $penggunaKhusus")
                    } else {
                        val tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        val tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        val jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        val jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                        val lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)

                        Log.d("FormTataTertibFragment", "Tanggal: $tanggalMulai - $tanggalSelesai, Jam: $jamMulai - $jamSelesai, Lapangan: $lapanganDipinjam")
                    }
                }
            }

            Log.d("FormTataTertibFragment", "Data retrieved - Fasilitas: $namaFasilitas, Opsi: $opsiPeminjaman")
        }
    }

    private fun setupUploadSectionVisibility() {
        if (opsiPeminjaman == "Sesuai Jadwal Rutin") {
            // Hide upload section for "Sesuai Jadwal Rutin"
            tvUploadSurat.visibility = View.GONE
            containerSurat.visibility = View.GONE

            // For "Sesuai Jadwal Rutin", only need checkbox checked to enable Next button
            updateNextButtonState()
        } else {
            // Show upload section for "Diluar Jadwal Rutin"
            tvUploadSurat.visibility = View.VISIBLE
            containerSurat.visibility = View.VISIBLE

            // Default status for file
            tvFileStatus.text = "Belum ada file yang dipilih"
            tvFileStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    private fun setupUploadButton() {
        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            startActivityForResult(intent, PDF_REQUEST_CODE)
        }
    }

    private fun setupCheckboxListener() {
        checkboxTataTertib.setOnCheckedChangeListener { _, isChecked ->
            updateNextButtonState()
        }
    }

    private fun setupViews(view: View) {
        val buttonBack = view.findViewById<Button>(R.id.button_batalkan)

        // Set button disabled by default, will be enabled after validation
        buttonNext.isEnabled = false
        buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))

        buttonNext.setOnClickListener {
            // Check if we need to validate file upload for "Diluar Jadwal Rutin"
            if (opsiPeminjaman == "Diluar Jadwal Rutin" && selectedPdfUri == null) {
                Toast.makeText(requireContext(), "Mohon upload surat peminjaman terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to next page with all data
            val activity = requireActivity() as PeminjamanActivity
            val bundle = arguments ?: Bundle()

            // Add selected PDF URI to bundle if available
            selectedPdfUri?.let {
                bundle.putString("EXTRA_PDF_URI", it.toString())
            }

            activity.navigateToNextPage(bundle)
        }

        buttonBack.setOnClickListener {
            val activity = requireActivity() as PeminjamanActivity
            activity.navigateToPreviousPage()
        }
    }

    private fun updateNextButtonState() {
        val isChecked = checkboxTataTertib.isChecked
        val needsFile = opsiPeminjaman == "Diluar Jadwal Rutin"

        // Button is enabled if checkbox is checked AND
        // either no file is needed OR a file has been selected
        val isEnabled = isChecked && (!needsFile || selectedPdfUri != null)

        buttonNext.isEnabled = isEnabled

        // Set color based on enabled state
        val colorRes = if (isEnabled) R.color.dark_blue else R.color.gray
        buttonNext.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PDF_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                // Check file size (max 1MB)
                val fileSize = getFileSize(uri)
                val maxSize = 1 * 1024 * 1024 // 1MB in bytes

                if (fileSize <= maxSize) {
                    selectedPdfUri = uri

                    // Update button text to show file name
                    btnUpload.text = "Ganti File"

                    // Update status text with file info
                    val fileName = getFileName(uri)
                    val fileSizeFormatted = formatFileSize(fileSize)
                    tvFileStatus.text = "File dipilih: $fileName ($fileSizeFormatted)"
                    tvFileStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_blue))

                    // Update button state
                    updateNextButtonState()

                    Toast.makeText(requireContext(),
                        "File berhasil dipilih",
                        Toast.LENGTH_SHORT).show()
                } else {
                    // File is too large
                    Toast.makeText(requireContext(),
                        "Ukuran file melebihi batas 1MB. Mohon pilih file yang lebih kecil.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        return if (kb < 1024) {
            String.format("%.2f KB", kb)
        } else {
            String.format("%.2f MB", kb / 1024)
        }
    }

    private fun displayData() {
        // Use the retrieved data to update your UI
        // For example, display facility name, event name, etc.
        Log.d("FormTataTertibFragment", "Displaying data for: $namaFasilitas - $namaAcara")
    }
}