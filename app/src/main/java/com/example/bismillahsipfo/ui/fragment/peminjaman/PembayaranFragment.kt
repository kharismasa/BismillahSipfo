package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.Activity
import android.content.ContentValues.TAG
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
import com.example.bismillahsipfo.data.repository.SharedPeminjamanViewModel

class PembayaranFragment : Fragment() {

    // TAMBAHAN: SharedViewModel untuk data antar fragment
    private val sharedViewModel: SharedPeminjamanViewModel by activityViewModels()

    // TAMBAHAN: Variable untuk menyimpan data saat ini
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

    // Payment calculation variables
    private var totalDays: Long = 1
    private var basePrice: Double = 0.0
    private var discountPercent: Double = 0.0
    private var discountAmount: Double = 0.0
    private var finalPrice: Double = 0.0

    // Formatter for currency
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    private var paymentId: String? = null
    private val midtransLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        buttonBayar.isEnabled = true
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val transactionResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(UiKitConstants.KEY_TRANSACTION_RESULT, TransactionResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(UiKitConstants.KEY_TRANSACTION_RESULT) as? TransactionResult
            }

            if (transactionResult != null) {
                when (transactionResult.status) {
                    // Successful status
                    UiKitConstants.STATUS_SUCCESS -> {
                        // Navigasi ke activity hasil pembayaran
                        navigateToHasilPembayaran(true, paymentId)
                    }
                    // Pending status
                    UiKitConstants.STATUS_PENDING -> {
                        Toast.makeText(requireContext(),
                            "Pembayaran masih dalam proses",
                            Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    }
                    // Other status
                    else -> {
                        Toast.makeText(requireContext(),
                            "Pembayaran: ${transactionResult.status}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(),
                    "Tidak ada hasil transaksi",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        // TAMBAHKAN: Inisialisasi Midtrans jika belum diinisialisasi
        initMidtrans()

        // Initialize UI components
        initializeViews(view)

        // TAMBAHAN: Setup LiveData Observer
        setupSharedViewModelObserver()

        // MODIFIKASI: Retrieve data dari SharedViewModel dulu, baru dari Bundle
        retrieveDataFromSharedViewModel()

        // Fallback: Jika SharedViewModel kosong, ambil dari Bundle
        if (currentData == null) {
            retrieveAllDataFromBundle()
        }

        // TAMBAHAN: Log all data for debugging
        logAllData()

        // Setup UI with data
        setupUI()

        // Setup button listeners
        setupButtonListeners()

        // Calculate payment
        calculatePayment()
    }

    // TAMBAHAN: Setup Observer untuk SharedViewModel
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

    // TAMBAHAN: Method untuk mengambil data dari SharedViewModel
    private fun retrieveDataFromSharedViewModel() {
        currentData = sharedViewModel.getCurrentData()
        currentData?.let { data ->
            Log.d("PembayaranFragment", "Data retrieved from SharedViewModel: $data")
        }
    }

    // MODIFIKASI: Method untuk mengambil data dari Bundle sebagai fallback
    private fun retrieveAllDataFromBundle() {
        arguments?.let { bundle ->
            val idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            val namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            val opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            val namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)
            val pdfUri = bundle.getString("EXTRA_PDF_URI")

            // Create PeminjamanData from Bundle similar to FormTataTertibFragment
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
                        pdfUri = pdfUri
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
                            pdfUri = pdfUri
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
                            pdfUri = pdfUri
                        )
                    }
                }

                else -> PeminjamanData(
                    idFasilitas = idFasilitas,
                    namaFasilitas = namaFasilitas,
                    opsiPeminjaman = opsiPeminjaman,
                    namaAcara = namaAcara,
                    pdfUri = pdfUri
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

    // MODIFIKASI: Update setupUI untuk menggunakan currentData
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

    // MODIFIKASI: Update setupLapanganList untuk menggunakan currentData
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processPayment() {
        currentData?.let { data ->
            lifecycleScope.launch {
                try {
                    // Disable button to prevent multiple requests
                    buttonBayar.isEnabled = false

                    // Check if this is a free booking
                    val isFreeBooking = data.opsiPeminjaman == "Sesuai Jadwal Rutin" || finalPrice <= 0

                    if (isFreeBooking) {
                        Toast.makeText(requireContext(), "Memproses peminjaman gratis...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Memproses pembayaran...", Toast.LENGTH_SHORT).show()
                    }

                    // Generate order ID
                    val orderId = "ORDER-${System.currentTimeMillis()}"

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
                        "order_id" to orderId,
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

                    // *** PERBAIKAN: Konversi format tanggal dari DD/MM/YYYY ke YYYY-MM-DD ***
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
                        // Konversi ke format yang digunakan database
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
                    peminjamanData["status_peminjaman"] = "PENDING"

                    requestMap["create_peminjaman"] = true
                    requestMap["peminjaman_data"] = peminjamanData

                    // *** TAMBAHAN: Flag untuk mengirim data saja, tanpa memproses Midtrans ***
                    if (!isFreeBooking) {
                        requestMap["save_data_only"] = true
                    }

                    // Convert to JSON string using Gson
                    val gson = Gson()
                    val jsonString = gson.toJson(requestMap)
                    Log.d("PembayaranFragment", "Request data: $jsonString")

                    // Create request body
                    val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                    // Continue with same payment processing logic as before...
                    // Sisa kode processPayment tetap sama

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing payment: ${e.message}", e)
                    showPaymentError(e.message ?: "Terjadi kesalahan")
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

    // TAMBAHAN: Override onResume untuk memperbarui data jika ada perubahan
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