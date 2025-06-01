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
import androidx.fragment.app.activityViewModels
import com.example.bismillahsipfo.data.repository.PeminjamanData
import com.example.bismillahsipfo.data.repository.SharedPeminjamanViewModel

class FormTataTertibFragment : Fragment() {

    private val PDF_REQUEST_CODE = 123
    private var selectedPdfUri: Uri? = null

    // TAMBAHAN: SharedViewModel untuk data antar fragment
    private val sharedViewModel: SharedPeminjamanViewModel by activityViewModels()

    // TAMBAHAN: Variable untuk menyimpan data saat ini
    private var currentData: PeminjamanData? = null

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

        // TAMBAHAN: Setup LiveData Observer
        setupSharedViewModelObserver()

        // MODIFIKASI: Ambil data dari SharedViewModel dulu, baru dari Bundle
        retrieveDataFromSharedViewModel()

        // Fallback: Jika SharedViewModel kosong, ambil dari Bundle
        if (currentData == null) {
            retrieveDataFromBundle()
        }

        // TAMBAHAN: Log all data for debugging
        logAllData()

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

    // TAMBAHAN: Setup Observer untuk SharedViewModel
    private fun setupSharedViewModelObserver() {
        sharedViewModel.peminjamanData.observe(viewLifecycleOwner) { data ->
            Log.d("FormTataTertibFragment", "SharedViewModel data observed: $data")

            if (data != null && data != currentData) {
                Log.d("FormTataTertibFragment", "Data changed, updating UI")
                currentData = data

                // Update UI based on new data
                setupUploadSectionVisibility()
                displayData()
            }
        }
    }

    // TAMBAHAN: Method untuk log semua data
    private fun logAllData() {
        currentData?.let { data ->
            Log.d("FormTataTertibFragment", """
                === FORM TATA TERTIB DATA ===
                ID Fasilitas: ${data.idFasilitas}
                Nama Fasilitas: ${data.namaFasilitas}
                Opsi Peminjaman: ${data.opsiPeminjaman}
                Nama Acara: ${data.namaAcara}
                Nama Organisasi: ${data.namaOrganisasi}
                ID Organisasi: ${data.idOrganisasi}
                Pengguna Khusus: ${data.penggunaKhusus}
                Tanggal: ${data.tanggalMulai} - ${data.tanggalSelesai}
                Jam: ${data.jamMulai} - ${data.jamSelesai}
                Lapangan: ${data.lapanganDipinjam}
                PDF URI: ${data.pdfUri}
                ===============================
            """.trimIndent())
        } ?: Log.d("FormTataTertibFragment", "No data available to log")
    }


    // TAMBAHAN: Method untuk mengambil data dari SharedViewModel
    private fun retrieveDataFromSharedViewModel() {
        currentData = sharedViewModel.getCurrentData()
        currentData?.let { data ->
            Log.d("FormTataTertibFragment", "Data retrieved from SharedViewModel: $data")
        }
    }

    // MODIFIKASI: Method untuk mengambil data dari Bundle sebagai fallback
    private fun retrieveDataFromBundle() {
        arguments?.let { bundle ->
            val idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            val namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            val opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            val namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)

            // Create PeminjamanData from Bundle
            val bundleData = when (opsiPeminjaman) {
                "Sesuai Jadwal Rutin" -> {
                    val idOrganisasi = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_ORGANISASI, -1)
                    val namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)
                    val jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                    val listLapangan = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LIST_LAPANGAN)
                    val tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                    val tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                    val jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                    val jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                    val penggunaKhusus = if (idFasilitas == 30) {
                        bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                    } else null

                    PeminjamanData(
                        idFasilitas = idFasilitas,
                        namaFasilitas = namaFasilitas,
                        opsiPeminjaman = opsiPeminjaman,
                        namaAcara = namaAcara,
                        namaOrganisasi = namaOrganisasi,
                        idOrganisasi = idOrganisasi,
                        jadwalTersedia = jadwalTersedia,
                        listLapangan = listLapangan,
                        penggunaKhusus = penggunaKhusus,
                        tanggalMulai = tanggalMulai,
                        tanggalSelesai = tanggalSelesai,
                        jamMulai = jamMulai,
                        jamSelesai = jamSelesai,
                        lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
                    )
                }

                "Diluar Jadwal Rutin" -> {
                    val namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)

                    if (idFasilitas == 30) {
                        val jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                        val penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                        val tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        val tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        val jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        val jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)

                        PeminjamanData(
                            idFasilitas = idFasilitas,
                            namaFasilitas = namaFasilitas,
                            opsiPeminjaman = opsiPeminjaman,
                            namaAcara = namaAcara,
                            namaOrganisasi = namaOrganisasi,
                            jadwalTersedia = jadwalTersedia,
                            penggunaKhusus = penggunaKhusus,
                            tanggalMulai = tanggalMulai,
                            tanggalSelesai = tanggalSelesai,
                            jamMulai = jamMulai,
                            jamSelesai = jamSelesai,
                            lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
                        )
                    } else {
                        val tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        val tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        val jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        val jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                        val lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)

                        PeminjamanData(
                            idFasilitas = idFasilitas,
                            namaFasilitas = namaFasilitas,
                            opsiPeminjaman = opsiPeminjaman,
                            namaAcara = namaAcara,
                            namaOrganisasi = namaOrganisasi,
                            tanggalMulai = tanggalMulai,
                            tanggalSelesai = tanggalSelesai,
                            jamMulai = jamMulai,
                            jamSelesai = jamSelesai,
                            lapanganDipinjam = lapanganDipinjam
                        )
                    }
                }

                else -> PeminjamanData(
                    idFasilitas = idFasilitas,
                    namaFasilitas = namaFasilitas,
                    opsiPeminjaman = opsiPeminjaman,
                    namaAcara = namaAcara
                )
            }

            currentData = bundleData

            // Update SharedViewModel dengan data dari Bundle
            sharedViewModel.updatePeminjamanData(bundleData)

            Log.d("FormTataTertibFragment", "Data retrieved from Bundle and saved to SharedViewModel: $bundleData")
        }
    }

    // MODIFIKASI: Update setupUploadSectionVisibility untuk menggunakan currentData
    private fun setupUploadSectionVisibility() {
        val opsi = currentData?.opsiPeminjaman

        if (opsi == "Sesuai Jadwal Rutin") {
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
            val opsi = currentData?.opsiPeminjaman

            // Check if we need to validate file upload for "Diluar Jadwal Rutin"
            if (opsi == "Diluar Jadwal Rutin" && selectedPdfUri == null) {
                Toast.makeText(requireContext(), "Mohon upload surat peminjaman terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // MODIFIKASI: Update data di SharedViewModel dengan PDF URI
            currentData?.let { data ->
                val updatedData = data.copy(pdfUri = selectedPdfUri?.toString())
                sharedViewModel.updatePeminjamanData(updatedData)
                currentData = updatedData
            }

            // Navigate to next page with all data
            val activity = requireActivity() as PeminjamanActivity
            val bundle = createBundleFromCurrentData()

            activity.navigateToNextPage(bundle)
        }

        buttonBack.setOnClickListener {
            val activity = requireActivity() as PeminjamanActivity
            activity.navigateToPreviousPage()
        }
    }

    // TAMBAHAN: Method untuk membuat Bundle dari currentData
    private fun createBundleFromCurrentData(): Bundle {
        val bundle = Bundle()

        currentData?.let { data ->
            bundle.putInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, data.idFasilitas)
            bundle.putString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS, data.namaFasilitas)
            bundle.putString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN, data.opsiPeminjaman)
            bundle.putString(FormPeminjamanFragment.EXTRA_NAMA_ACARA, data.namaAcara)
            bundle.putString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI, data.namaOrganisasi)

            if (data.idOrganisasi > 0) {
                bundle.putInt(FormPeminjamanFragment.EXTRA_ID_ORGANISASI, data.idOrganisasi)
            }

            data.jadwalTersedia?.let { jadwal ->
                if (jadwal is java.io.Serializable) {
                    bundle.putSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA, jadwal)
                }
            }

            data.listLapangan?.let { list ->
                bundle.putIntegerArrayList(FormPeminjamanFragment.EXTRA_LIST_LAPANGAN, ArrayList(list))
            }

            data.lapanganDipinjam?.let { list ->
                bundle.putIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM, ArrayList(list))
            }

            bundle.putString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS, data.penggunaKhusus)
            bundle.putString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI, data.tanggalMulai)
            bundle.putString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI, data.tanggalSelesai)
            bundle.putString(FormPeminjamanFragment.EXTRA_JAM_MULAI, data.jamMulai)
            bundle.putString(FormPeminjamanFragment.EXTRA_JAM_SELESAI, data.jamSelesai)

            // Add PDF URI if available
            data.pdfUri?.let { pdfUri ->
                bundle.putString("EXTRA_PDF_URI", pdfUri)
            }
        }

        return bundle
    }

    private fun updateNextButtonState() {
        val isChecked = checkboxTataTertib.isChecked
        val opsi = currentData?.opsiPeminjaman
        val needsFile = opsi == "Diluar Jadwal Rutin"

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
        currentData?.let { data ->
            Log.d("FormTataTertibFragment", "Displaying data for: ${data.namaFasilitas} - ${data.namaAcara}")
            Log.d("FormTataTertibFragment", "Opsi Peminjaman: ${data.opsiPeminjaman}")
        }
    }

    // TAMBAHAN: Override onResume untuk memperbarui data jika ada perubahan
    override fun onResume() {
        super.onResume()
        Log.d("FormTataTertibFragment", "Fragment resumed")

        val latestData = sharedViewModel.getCurrentData()
        if (latestData != null && latestData != currentData) {
            Log.d("FormTataTertibFragment", "Data updated from SharedViewModel on resume")
            currentData = latestData
            setupUploadSectionVisibility()
            displayData()
            logAllData() // Log data setelah update
        }
    }
}