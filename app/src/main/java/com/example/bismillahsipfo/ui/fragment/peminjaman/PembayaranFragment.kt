package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.GamifikasiRepository
import com.example.bismillahsipfo.data.repository.UserRepository
import com.google.gson.Gson
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.uikit.api.model.CustomColorTheme
import com.midtrans.sdk.uikit.api.model.TransactionResult
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.internal.util.UiKitConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.jvm.java
import androidx.fragment.app.activityViewModels
import com.example.bismillahsipfo.data.repository.PeminjamanData
import com.example.bismillahsipfo.data.repository.PeminjamanRepository
import com.example.bismillahsipfo.data.repository.SharedPeminjamanViewModel

class PembayaranFragment : Fragment() {

    companion object {
        private const val TAG = "PembayaranFragment"
    }

    // SharedViewModel untuk data antar fragment
    private val sharedViewModel: SharedPeminjamanViewModel by activityViewModels()

    // Variable untuk menyimpan data saat ini
    private var currentData: PeminjamanData? = null

    // UI components
    private lateinit var tvJumlahHari: TextView
    private lateinit var tvVoucher: TextView
    private lateinit var tvPerhitungan: TextView
    private lateinit var tvTotalBayar: TextView
    private lateinit var tvOpsiPeminjaman: TextView
    private lateinit var tvNamaFasilitas: TextView
    private lateinit var tvListLapangan: TextView
    private lateinit var tvDateStartEnd: TextView
    private lateinit var tvTimeStartEnd: TextView
    private lateinit var tvOrganisasi: TextView
    private lateinit var tvEvent: TextView
    private lateinit var tvPenggunaKhusus: TextView
    private lateinit var layoutPenggunaKhusus: LinearLayout
    private lateinit var layoutJumlahHari: LinearLayout
    private lateinit var buttonBatalkan: Button
    private lateinit var buttonBayar: Button

    // Repositories
    private lateinit var userRepository: UserRepository
    private lateinit var fasilitasRepository: FasilitasRepository
    private lateinit var gamifikasiRepository: GamifikasiRepository
    private lateinit var peminjamanRepository: PeminjamanRepository

    // Payment calculation variables
    private var totalDays: Long = 1
    private var basePrice: Double = 0.0
    private var discountPercent: Double = 0.0
    private var discountAmount: Double = 0.0
    private var finalPrice: Double = 0.0

    // Formatter for currency
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    private var paymentId: String? = null
    private var peminjamanId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pembayaran, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repositories
        userRepository = UserRepository(requireContext())
        fasilitasRepository = FasilitasRepository()
        gamifikasiRepository = GamifikasiRepository(requireContext())
        peminjamanRepository = PeminjamanRepository(requireContext())

        // Inisialisasi Midtrans jika belum diinisialisasi
        initMidtrans()

        // Initialize UI components
        initializeViews(view)

        // Setup LiveData Observer
        setupSharedViewModelObserver()

        // Retrieve data dari SharedViewModel dulu, baru dari Bundle
        retrieveDataFromSharedViewModel()

        // Fallback: Jika SharedViewModel kosong, ambil dari Bundle
        if (currentData == null) {
            retrieveAllDataFromBundle()
        }

        // Log all data for debugging
        logAllData()

        // Setup UI with data
        setupUI()

        // Setup button listeners
        setupButtonListeners()

        // Calculate payment
        calculatePayment()
    }

    // Setup Observer untuk SharedViewModel
    private fun setupSharedViewModelObserver() {
        sharedViewModel.peminjamanData.observe(viewLifecycleOwner) { data ->
            Log.d("PembayaranFragment", "SharedViewModel data observed: $data")

            if (data != null && data != currentData) {
                Log.d("PembayaranFragment", "Data changed, recalculating payment")
                currentData = data

                // Update UI dan recalculate payment
                setupUI()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    calculatePayment()
                }

                // Log updated data
                logAllData()
            }
        }
    }

    private fun initializeViews(view: View) {
        tvJumlahHari = view.findViewById(R.id.tvJumlahHari)
        tvVoucher = view.findViewById(R.id.tvVoucher)
        tvPerhitungan = view.findViewById(R.id.tvPerhitungan)
        tvTotalBayar = view.findViewById(R.id.tvTotalBayar)
        tvOpsiPeminjaman = view.findViewById(R.id.tvOpsiPeminjaman)
        tvNamaFasilitas = view.findViewById(R.id.tvNamaFasilitas)
        tvListLapangan = view.findViewById(R.id.tvListLapangan)
        tvDateStartEnd = view.findViewById(R.id.tvDateStartEnd)
        tvTimeStartEnd = view.findViewById(R.id.tvTimeStartEnd)
        tvOrganisasi = view.findViewById(R.id.tvOrganisasi)
        tvEvent = view.findViewById(R.id.tvEvent)
        tvPenggunaKhusus = view.findViewById(R.id.tvPenggunaKhusus)
        layoutPenggunaKhusus = view.findViewById(R.id.layoutPenggunaKhusus)
        layoutJumlahHari = view.findViewById(R.id.layoutJumlahHari)
        buttonBatalkan = view.findViewById(R.id.button_batalkan)
        buttonBayar = view.findViewById(R.id.button_bayar)
    }

    // Method untuk mengambil data dari SharedViewModel
    private fun retrieveDataFromSharedViewModel() {
        currentData = sharedViewModel.getCurrentData()
        currentData?.let { data ->
            Log.d("PembayaranFragment", "Data retrieved from SharedViewModel: $data")
        }
    }

    // Method untuk mengambil data dari Bundle sebagai fallback
    private fun retrieveAllDataFromBundle() {
        arguments?.let { bundle ->
            val idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            val namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            val opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            val namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)
            val pdfUri = bundle.getString("EXTRA_PDF_URI")
            val selectedFileName = bundle.getString("EXTRA_SELECTED_FILE_NAME")

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
                    val lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
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
                        lapanganDipinjam = lapanganDipinjam,
                        pdfUri = pdfUri,
                        selectedFileName = selectedFileName
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
                        val lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)

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
                            lapanganDipinjam = lapanganDipinjam,
                            pdfUri = pdfUri,
                            selectedFileName = selectedFileName
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
                            lapanganDipinjam = lapanganDipinjam,
                            pdfUri = pdfUri,
                            selectedFileName = selectedFileName
                        )
                    }
                }

                else -> PeminjamanData(
                    idFasilitas = idFasilitas,
                    namaFasilitas = namaFasilitas,
                    opsiPeminjaman = opsiPeminjaman,
                    namaAcara = namaAcara,
                    pdfUri = pdfUri,
                    selectedFileName = selectedFileName
                )
            }

            currentData = bundleData

            // Update SharedViewModel dengan data dari Bundle
            sharedViewModel.updatePeminjamanData(bundleData)

            Log.d("PembayaranFragment", "Data retrieved from Bundle and saved to SharedViewModel: $bundleData")
        }
    }

    private fun logAllData() {
        currentData?.let { data ->
            Log.d("PembayaranFragment", """
                === PEMBAYARAN FRAGMENT DATA ===
                ID Fasilitas: ${data.idFasilitas}
                Nama Fasilitas: ${data.namaFasilitas}
                Opsi Peminjaman: ${data.opsiPeminjaman}
                Nama Acara: ${data.namaAcara}
                ID Organisasi: ${data.idOrganisasi}
                Nama Organisasi: ${data.namaOrganisasi}
                Jadwal Tersedia: ${data.jadwalTersedia}
                List Lapangan: ${data.listLapangan}
                Pengguna Khusus: ${data.penggunaKhusus}
                Tanggal Mulai: ${data.tanggalMulai}
                Tanggal Selesai: ${data.tanggalSelesai}
                Jam Mulai: ${data.jamMulai}
                Jam Selesai: ${data.jamSelesai}
                Lapangan Dipinjam: ${data.lapanganDipinjam}
                PDF URI: ${data.pdfUri}
                Selected File Name: ${data.selectedFileName}
                Uploaded File URL: ${data.uploadedFileUrl}
                
                === PAYMENT CALCULATION ===
                Total Days: $totalDays
                Base Price: $basePrice
                Discount: $discountPercent%
                Discount Amount: $discountAmount
                Final Price: $finalPrice
                ================================
            """.trimIndent())
        } ?: Log.d("PembayaranFragment", "No data available to log")
    }

    // Update setupUI untuk menggunakan currentData
    private fun setupUI() {
        currentData?.let { data ->
            // Set facility name
            tvNamaFasilitas.text = data.namaFasilitas

            // Set event name
            tvEvent.text = data.namaAcara

            // Set organization name
            tvOrganisasi.text = data.namaOrganisasi

            // Set rental option
            tvOpsiPeminjaman.text = data.opsiPeminjaman

            // Set date range
            tvDateStartEnd.text = if (data.tanggalMulai == data.tanggalSelesai) {
                data.tanggalMulai
            } else {
                "${data.tanggalMulai} - ${data.tanggalSelesai}"
            }

            // Set time range
            tvTimeStartEnd.text = "${data.jamMulai} - ${data.jamSelesai}"

            // Handle special user type for UTG (id 30)
            if (data.idFasilitas == 30 && !data.penggunaKhusus.isNullOrEmpty()) {
                layoutPenggunaKhusus.visibility = View.VISIBLE

                // Convert enum name to display text
                val displayText = when (data.penggunaKhusus) {
                    PenggunaKhusus.INTERNAL_UII.name -> "Internal UII"
                    PenggunaKhusus.INTERNAL_VS_EKSTERNAL.name -> "Internal UII vs Team Eksternal"
                    PenggunaKhusus.EKSTERNAL_UII.name -> "Team Eksternal"
                    else -> data.penggunaKhusus
                }

                tvPenggunaKhusus.text = displayText
            } else {
                layoutPenggunaKhusus.visibility = View.GONE
            }

            // Set fields/courts
            setupLapanganList()
        }
    }

    // Update setupLapanganList untuk menggunakan currentData
    private fun setupLapanganList() {
        currentData?.let { data ->
            lifecycleScope.launch {
                try {
                    // Use the lapanganDipinjam list from the data
                    val lapanganIds = data.lapanganDipinjam ?: data.listLapangan ?: emptyList()

                    if (lapanganIds.isEmpty()) {
                        tvListLapangan.text = "Tidak ada lapangan yang dipilih"
                        return@launch
                    }

                    // Retrieve all lapangan for this fasilitas
                    val allLapangan = withContext(Dispatchers.IO) {
                        fasilitasRepository.getLapanganByFasilitasId(data.idFasilitas)
                    }

                    // Filter to get only the selected lapangan
                    val selectedLapangan = allLapangan.filter { it.idLapangan in lapanganIds }

                    // Join the lapangan names with commas
                    val lapanganText = selectedLapangan.joinToString(", ") { it.namaLapangan }

                    tvListLapangan.text = lapanganText
                } catch (e: Exception) {
                    Log.e("PembayaranFragment", "Error fetching lapangan: ${e.message}")
                    tvListLapangan.text = "Error: Tidak dapat memuat daftar lapangan"
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonListeners() {
        buttonBatalkan.setOnClickListener {
            // Navigate back to previous fragment
            val activity = requireActivity() as PeminjamanActivity
            activity.navigateToPreviousPage()
        }

        buttonBayar.setOnClickListener {
            // Process payment
            processPayment()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculatePayment() {
        lifecycleScope.launch {
            try {
                // Calculate number of days
                calculateDays()

                // Get user's voucher discount from gamifikasi
                getVoucherDiscount()

                // Calculate base price
                calculateBasePrice()

                // Calculate final price with discount
                calculateFinalPrice()

                // Update UI with payment information
                updatePaymentUI()
            } catch (e: Exception) {
                Log.e("PembayaranFragment", "Error calculating payment: ${e.message}")
                Toast.makeText(requireContext(), "Terjadi kesalahan dalam perhitungan pembayaran", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDays() {
        currentData?.let { data ->
            try {
                if (data.tanggalMulai == data.tanggalSelesai) {
                    totalDays = 1
                } else {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val startDate = LocalDate.parse(data.tanggalMulai, formatter)
                    val endDate = LocalDate.parse(data.tanggalSelesai, formatter)
                    totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
                }

                // Hide days layout for UTG with fixed per-schedule pricing
                if (data.idFasilitas == 30) {
                    layoutJumlahHari.visibility = View.GONE
                } else {
                    tvJumlahHari.text = "$totalDays hari"
                }
            } catch (e: Exception) {
                Log.e("PembayaranFragment", "Error calculating days: ${e.message}")
                totalDays = 1
            }
        }
    }

    private suspend fun getVoucherDiscount() {
        try {
            val user = withContext(Dispatchers.IO) {
                userRepository.getCurrentUser()
            }

            if (user != null) {
                val gamifikasi = withContext(Dispatchers.IO) {
                    gamifikasiRepository.getGamifikasiForUser(user)
                }

                if (gamifikasi != null && gamifikasi.idVoucher != null) {
                    val vouchers = withContext(Dispatchers.IO) {
                        gamifikasiRepository.getAllVouchers()
                    }

                    val userVoucher = vouchers.find { it.idVoucher == gamifikasi.idVoucher }

                    if (userVoucher != null) {
                        discountPercent = userVoucher.diskon
                        tvVoucher.text = "-${discountPercent.toInt()}%"
                    } else {
                        discountPercent = 0.0
                        tvVoucher.text = "0%"
                    }
                } else {
                    discountPercent = 0.0
                    tvVoucher.text = "0%"
                }
            } else {
                discountPercent = 0.0
                tvVoucher.text = "0%"
            }
        } catch (e: Exception) {
            Log.e("PembayaranFragment", "Error getting voucher discount: ${e.message}")
            discountPercent = 0.0
            tvVoucher.text = "0%"
        }
    }

    // MODIFIKASI: Update calculateBasePrice untuk menggunakan currentData
    private fun calculateBasePrice() {
        currentData?.let { data ->
            basePrice = when (data.opsiPeminjaman) {
                "Sesuai Jadwal Rutin" -> 0.0 // Free for regular schedule

                "Diluar Jadwal Rutin" -> {
                    if (data.idFasilitas == 30) {
                        // UTG has special pricing based on user type
                        when (data.penggunaKhusus) {
                            PenggunaKhusus.INTERNAL_UII.name -> 1_000_000.0
                            PenggunaKhusus.INTERNAL_VS_EKSTERNAL.name -> 1_500_000.0
                            PenggunaKhusus.EKSTERNAL_UII.name -> 2_750_000.0
                            else -> 1_000_000.0 // Default to Internal UII price
                        }
                    } else {
                        // Regular facilities: 200,000 per day
                        200_000.0 * totalDays
                    }
                }

                else -> 0.0
            }
        }
    }

    private fun calculateFinalPrice() {
        if (basePrice > 0 && discountPercent > 0) {
            discountAmount = basePrice * (discountPercent / 100.0)
            finalPrice = basePrice - discountAmount
        } else {
            discountAmount = 0.0
            finalPrice = basePrice
        }
    }

    // MODIFIKASI: Update updatePaymentUI untuk menggunakan currentData
    private fun updatePaymentUI() {
        currentData?.let { data ->
            val calculationText = StringBuilder()

            if (data.opsiPeminjaman == "Sesuai Jadwal Rutin") {
                calculationText.append("Peminjaman sesuai jadwal rutin: ${currencyFormat.format(0)}")
            } else {
                if (data.idFasilitas == 30) {
                    val userTypeText = when (data.penggunaKhusus) {
                        PenggunaKhusus.INTERNAL_UII.name -> "Internal UII"
                        PenggunaKhusus.INTERNAL_VS_EKSTERNAL.name -> "Internal UII vs Team Eksternal"
                        PenggunaKhusus.EKSTERNAL_UII.name -> "Team Eksternal"
                        else -> "Default"
                    }

                    calculationText.append("Tarif $userTypeText: ${currencyFormat.format(basePrice)}")
                } else {
                    calculationText.append("$totalDays hari × ${currencyFormat.format(200_000)} = ${currencyFormat.format(basePrice)}")
                }

                if (discountPercent > 0) {
                    calculationText.append("\nDiskon ${discountPercent.toInt()}% = ${currencyFormat.format(discountAmount)}")
                }
            }

            tvPerhitungan.text = calculationText.toString()
            tvTotalBayar.text = currencyFormat.format(finalPrice)
        }
    }

    private fun initMidtrans() {
        try {
            if (MidtransSDK.getInstance() == null) {
                // Inisialisasi Midtrans SDK
                UiKitApi.Builder()
                    .withContext(requireActivity().applicationContext)
                    .withMerchantUrl("https://ulxdrgkjbvalhxesibpr.supabase.co/functions/v1/midtrans-sipfo")
                    .withColorTheme(CustomColorTheme("#2A76C6", "#4294DA", "#DDDDDD"))
                    .enableLog(true)
                    .build()

                Log.d("PembayaranFragment", "Midtrans SDK berhasil diinisialisasi")
            }
        } catch (e: Exception) {
            Log.e("PembayaranFragment", "Error inisialisasi Midtrans: ${e.message}")
        }
    }

    // Method untuk upload file ke storage - HANYA DIPANGGIL SAAT TOMBOL BAYAR
    private suspend fun uploadFileToStorage(): String? {
        return withContext(Dispatchers.IO) {
            try {
                currentData?.pdfUri?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    val userId = userRepository.getCurrentUserId()

                    // Generate filename dari nama file yang dipilih
                    val originalFileName = currentData?.selectedFileName ?: "surat_peminjaman.pdf"
                    val extension = originalFileName.substringAfterLast('.', "pdf")
                    val timestamp = System.currentTimeMillis()
                    val fileName = "surat_user_${userId}_${timestamp}.${extension}"

                    Log.d("PembayaranFragment", "Uploading file: $fileName (Original: $originalFileName)")

                    // Upload to Supabase storage
                    val uploadedUrl = peminjamanRepository.uploadPdfToStorage(uri, fileName)

                    if (uploadedUrl != null) {
                        Log.d("PembayaranFragment", "File uploaded successfully: $uploadedUrl")

                        // Update data di SharedViewModel
                        currentData?.let { data ->
                            val updatedData = data.copy(uploadedFileUrl = uploadedUrl)
                            sharedViewModel.updatePeminjamanData(updatedData)
                            currentData = updatedData
                        }
                    }

                    uploadedUrl
                } ?: run {
                    Log.d("PembayaranFragment", "No file to upload")
                    null
                }
            } catch (e: Exception) {
                Log.e("PembayaranFragment", "Error uploading file: ${e.message}", e)
                null
            }
        }
    }

    // Method untuk update surat URL di database setelah peminjaman dibuat
    private suspend fun updateSuratUrlInDatabase(peminjamanId: String, suratUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requestMap = HashMap<String, Any>()
                requestMap["update_surat_url"] = true
                requestMap["id_peminjaman"] = peminjamanId
                requestMap["surat_url"] = suratUrl

                val gson = Gson()
                val jsonString = gson.toJson(requestMap)
                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = apiService.createTransaction(
                    url = "midtrans-sipfo",
                    authHeader = "Bearer ${BuildConfig.API_KEY}",
                    requestBody = requestBody
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val success = jsonResponse.optBoolean("success", false)

                    if (success) {
                        Log.d("PembayaranFragment", "Surat URL updated successfully in database")
                        true
                    } else {
                        Log.e("PembayaranFragment", "Failed to update surat URL: ${jsonResponse.optString("message")}")
                        false
                    }
                } else {
                    Log.e("PembayaranFragment", "HTTP error updating surat URL: ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                Log.e("PembayaranFragment", "Error updating surat URL in database: ${e.message}", e)
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processPayment() {
        currentData?.let { data ->
            lifecycleScope.launch {
                try {
                    // Disable button to prevent multiple requests
                    buttonBayar.isEnabled = false

                    // Check if this is a free booking
                    val isFreeBooking = data.opsiPeminjaman == "Sesuai Jadwal Rutin" || finalPrice <= 0
                    val needsFileUpload = data.opsiPeminjaman == "Diluar Jadwal Rutin" && !data.pdfUri.isNullOrEmpty()

                    Log.d("PembayaranFragment", "Processing payment - OpsiPeminjaman: ${data.opsiPeminjaman}, FinalPrice: $finalPrice, IsFreeBooking: $isFreeBooking, NeedsFileUpload: $needsFileUpload")

                    // LANGKAH 1: Upload file jika diperlukan
                    var uploadedFileUrl: String? = null
                    if (needsFileUpload) {
                        Toast.makeText(requireContext(), "Mengupload surat peminjaman...", Toast.LENGTH_SHORT).show()

                        uploadedFileUrl = uploadFileToStorage()

                        if (uploadedFileUrl == null) {
                            Toast.makeText(requireContext(), "Gagal mengupload surat peminjaman", Toast.LENGTH_LONG).show()
                            buttonBayar.isEnabled = true
                            return@launch
                        }

                        Toast.makeText(requireContext(), "Surat berhasil diupload!", Toast.LENGTH_SHORT).show()
                    }

                    if (isFreeBooking) {
                        Toast.makeText(requireContext(), "Memproses peminjaman gratis...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Memproses pembayaran...", Toast.LENGTH_SHORT).show()
                    }

                    // Get current user
                    val user = userRepository.getCurrentUser()
                    val userId = userRepository.getCurrentUserId()

                    // Prepare payment data
                    val itemDetails = ArrayList<Map<String, Any>>()
                    val item = HashMap<String, Any>()
                    item["id"] = "RENT-${data.idFasilitas}"
                    item["price"] = if (isFreeBooking) 0 else finalPrice.toInt()
                    item["quantity"] = 1
                    item["name"] = "Sewa ${data.namaFasilitas}"
                    itemDetails.add(item)

                    // Customer details
                    val customerName = user?.nama ?: ""
                    val nameParts = customerName.split(" ", limit = 2)
                    val firstName = nameParts.firstOrNull() ?: ""
                    val lastName = if (nameParts.size > 1) nameParts[1] else ""

                    // Create transaction data map
                    val requestMap = HashMap<String, Any>()
                    requestMap["transaction_details"] = mapOf(
                        "order_id" to "TEMP-${System.currentTimeMillis()}", // Will be replaced by server
                        "gross_amount" to if (isFreeBooking) 0 else finalPrice.toInt()
                    )
                    requestMap["item_details"] = itemDetails
                    requestMap["customer_details"] = mapOf(
                        "first_name" to firstName,
                        "last_name" to lastName,
                        "email" to (user?.email ?: ""),
                        "phone" to (user?.noTelp ?: "")
                    )

                    requestMap["id_pengguna"] = userId
                    requestMap["id_fasilitas"] = data.idFasilitas
                    requestMap["is_free_booking"] = isFreeBooking

                    // Get user's voucher if available
                    val userGamifikasi = withContext(Dispatchers.IO) {
                        user?.let { gamifikasiRepository.getGamifikasiForUser(it) }
                    }
                    if (userGamifikasi?.idVoucher != null) {
                        requestMap["id_voucher"] = userGamifikasi.idVoucher
                    }

                    // Konversi format tanggal dari DD/MM/YYYY ke YYYY-MM-DD
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                    val tanggalMulaiFormatted = try {
                        val date = LocalDate.parse(data.tanggalMulai, formatter)
                        date.format(outputFormatter)
                    } catch (e: Exception) {
                        data.tanggalMulai // Gunakan format asli jika parsing gagal
                    }

                    val tanggalSelesaiFormatted = try {
                        val date = LocalDate.parse(data.tanggalSelesai, formatter)
                        date.format(outputFormatter)
                    } catch (e: Exception) {
                        data.tanggalSelesai // Gunakan format asli jika parsing gagal
                    }

                    // Prepare peminjaman data
                    val peminjamanData = HashMap<String, Any?>()
                    peminjamanData["tanggal_mulai"] = tanggalMulaiFormatted
                    peminjamanData["tanggal_selesai"] = tanggalSelesaiFormatted
                    peminjamanData["jam_mulai"] = data.jamMulai
                    peminjamanData["jam_selesai"] = data.jamSelesai
                    peminjamanData["nama_organisasi"] = data.namaOrganisasi ?: ""
                    peminjamanData["nama_acara"] = data.namaAcara ?: ""

                    if (data.idOrganisasi > 0) {
                        peminjamanData["id_organisasi"] = data.idOrganisasi
                    }

                    // Format pengguna khusus sesuai dengan yang diharapkan database
                    if (!data.penggunaKhusus.isNullOrEmpty()) {
                        val formattedPenggunaKhusus = when (data.penggunaKhusus) {
                            PenggunaKhusus.INTERNAL_UII.name -> "Internal UII"
                            PenggunaKhusus.INTERNAL_VS_EKSTERNAL.name -> "Internal UII vs Team Eksternal"
                            PenggunaKhusus.EKSTERNAL_UII.name -> "Team Eksternal"
                            else -> null
                        }
                        peminjamanData["pengguna_khusus"] = formattedPenggunaKhusus
                    } else {
                        peminjamanData["pengguna_khusus"] = null
                    }

                    val lapanganIds = data.lapanganDipinjam ?: data.listLapangan ?: ArrayList<Int>()
                    if (lapanganIds.isNotEmpty()) {
                        peminjamanData["lapangan_ids"] = lapanganIds
                    }

                    peminjamanData["opsi_peminjaman"] = data.opsiPeminjaman

                    // PENTING: Jangan kirim surat_peminjaman_url lewat request utama
                    // Nanti akan diupdate via endpoint terpisah setelah peminjaman dibuat

                    requestMap["create_peminjaman"] = true
                    requestMap["peminjaman_data"] = peminjamanData

                    // Convert to JSON string using Gson
                    val gson = Gson()
                    val jsonString = gson.toJson(requestMap)
                    Log.d("PembayaranFragment", "Request data: $jsonString")

                    // Create request body
                    val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                    // LANGKAH 2: Send request to server untuk buat payment + peminjaman
                    val apiService = RetrofitClient.createService(ApiService::class.java)
                    val response = withContext(Dispatchers.IO) {
                        apiService.createTransaction(
                            url = "midtrans-sipfo",
                            authHeader = "Bearer ${BuildConfig.API_KEY}",
                            requestBody = requestBody
                        )
                    }

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        Log.d("PembayaranFragment", "Response: $responseBody")

                        if (responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.optBoolean("success", false)
                            val message = jsonResponse.optString("message", "")
                            val statusFromServer = jsonResponse.optString("status", "")
                            paymentId = jsonResponse.optString("payment_id", "")
                            peminjamanId = jsonResponse.optString("peminjaman_id", "")

                            Log.d("PembayaranFragment", "Success: $success, Status: $statusFromServer, PaymentId: $paymentId, PeminjamanId: $peminjamanId")

                            if (success) {
                                // LANGKAH 3: Update surat URL jika ada file yang diupload dan peminjaman berhasil dibuat
                                if (!uploadedFileUrl.isNullOrEmpty() && !peminjamanId.isNullOrEmpty()) {
                                    Log.d("PembayaranFragment", "Updating surat URL in database...")
                                    val suratUpdateSuccess = updateSuratUrlInDatabase(peminjamanId!!, uploadedFileUrl)

                                    if (suratUpdateSuccess) {
                                        Log.d("PembayaranFragment", "Surat URL updated successfully")
                                    } else {
                                        Log.w("PembayaranFragment", "Failed to update surat URL, but continuing...")
                                    }
                                }

                                if (isFreeBooking || statusFromServer == "success") {
                                    // Untuk peminjaman gratis, langsung navigasi ke hasil pembayaran
                                    Toast.makeText(requireContext(), "Peminjaman gratis berhasil!", Toast.LENGTH_SHORT).show()
                                    navigateToHasilPembayaran(true, paymentId)
                                } else {
                                    // Untuk peminjaman berbayar, cek apakah ada redirect_url
                                    val redirectUrl = jsonResponse.optString("redirect_url", "")
                                    val token = jsonResponse.optString("token", "")

                                    Log.d("PembayaranFragment", "RedirectUrl: $redirectUrl, Token: $token")

                                    if (redirectUrl.isNotEmpty() || token.isNotEmpty()) {
                                        // Buka WebView untuk Midtrans
                                        val intent = Intent(requireContext(), MidtransWebViewActivity::class.java).apply {
                                            putExtra("MIDTRANS_URL", redirectUrl)
                                            putExtra("PAYMENT_ID", paymentId)
                                            putExtra("NAMA_FASILITAS", data.namaFasilitas)
                                            putExtra("NAMA_ACARA", data.namaAcara)
                                            putExtra("TANGGAL", data.tanggalMulai)
                                        }
                                        startActivity(intent)
                                        requireActivity().finish()
                                    } else {
                                        showPaymentError("Tidak ada redirect URL dari server")
                                    }
                                }
                            } else {
                                showPaymentError("Gagal memproses pembayaran: $message")
                            }
                        } else {
                            showPaymentError("Respon server kosong")
                        }
                    } else {
                        showPaymentError("Gagal terhubung ke server: ${response.code()}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing payment: ${e.message}", e)
                    showPaymentError("Terjadi kesalahan: ${e.message}")
                } finally {
                    // Pastikan button enabled kembali jika ada error
                    buttonBayar.isEnabled = true
                }
            }
        }
    }

    // Tambahkan method ini di dalam class
    private fun navigateToHasilPembayaran(isSuccess: Boolean, paymentId: String? = null) {
        currentData?.let { data ->
            val intent = Intent(requireContext(), HasilPembayaranActivity::class.java).apply {
                putExtra("IS_SUCCESS", isSuccess)
                putExtra("NAMA_FASILITAS", data.namaFasilitas)
                putExtra("NAMA_ACARA", data.namaAcara)
                putExtra("TANGGAL", data.tanggalMulai)
                putExtra("PAYMENT_ID", paymentId)
            }

            startActivity(intent)
            requireActivity().finish() // Tutup activity PeminjamanActivity
        }
    }

    private fun showPaymentError(message: String) {
        buttonBayar.isEnabled = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Payment error: $message")
    }

    // Override onResume untuk memperbarui data jika ada perubahan
    override fun onResume() {
        super.onResume()
        Log.d("PembayaranFragment", "Fragment resumed")

        val latestData = sharedViewModel.getCurrentData()
        if (latestData != null && latestData != currentData) {
            Log.d("PembayaranFragment", "Data updated from SharedViewModel on resume")
            currentData = latestData

            setupUI()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                calculatePayment()
            }

            logAllData() // Log data setelah update
        }
    }
}