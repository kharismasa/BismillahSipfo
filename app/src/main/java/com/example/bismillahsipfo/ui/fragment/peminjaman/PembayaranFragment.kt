package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import com.example.bismillahsipfo.data.network.ApiService
import com.example.bismillahsipfo.data.network.RetrofitClient
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.GamifikasiRepository
import com.example.bismillahsipfo.data.repository.UserRepository
import com.google.gson.Gson
import com.midtrans.sdk.uikit.api.model.TransactionResult
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.internal.util.UiKitConstants
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class PembayaranFragment : Fragment() {

    private val supabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
//        install(Storage)
    }

    // Midtrans launcher
    private lateinit var midtransLauncher: ActivityResultLauncher<Intent>

    // All data collected from previous fragments
    private var idFasilitas: Int = -1
    private var namaFasilitas: String? = null
    private var opsiPeminjaman: String? = null
    private var namaAcara: String? = null
    private var namaOrganisasi: String? = null
    private var idOrganisasi: Int = -1
    private var jadwalTersedia: JadwalTersedia? = null
    private var listLapangan: List<Int>? = null
    private var penggunaKhusus: String? = null
    private var tanggalMulai: String? = null
    private var tanggalSelesai: String? = null
    private var jamMulai: String? = null
    private var jamSelesai: String? = null
    private var lapanganDipinjam: List<Int>? = null
    private var pdfUri: String? = null

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

        // Initialize UI components
        initializeViews(view)

        // Retrieve all data from previous fragments
        retrieveAllData()

        // Setup UI with data
        setupUI()

        // Setup button listeners
        setupButtonListeners()

        // Calculate payment
        calculatePayment()
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

    private fun retrieveAllData() {
        arguments?.let { bundle ->
            // Basic data that's always present
            idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)

            // Retrieve data based on opsiPeminjaman
            when (opsiPeminjaman) {
                "Sesuai Jadwal Rutin" -> {
                    idOrganisasi = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_ORGANISASI, -1)
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)
                    jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                    listLapangan = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LIST_LAPANGAN)
                    tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                    tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                    jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                    jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                    lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)

                    if (idFasilitas == 30) {
                        penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                    }
                }

                "Diluar Jadwal Rutin" -> {
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)

                    if (idFasilitas == 30) {
                        jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                        penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                        tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                        lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
                    } else {
                        tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                        lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
                    }
                }
            }

            // Get PDF URI if available (from FormTataTertibFragment)
            pdfUri = bundle.getString("EXTRA_PDF_URI")

            // Log all retrieved data for debugging
            logAllData()
        }
    }

    private fun logAllData() {
        Log.d("PembayaranFragment", """
            Retrieved Data Summary:
            - ID Fasilitas: $idFasilitas
            - Nama Fasilitas: $namaFasilitas
            - Opsi Peminjaman: $opsiPeminjaman
            - Nama Acara: $namaAcara
            - ID Organisasi: $idOrganisasi
            - Nama Organisasi: $namaOrganisasi
            - Jadwal Tersedia: $jadwalTersedia
            - List Lapangan: $listLapangan
            - Pengguna Khusus: $penggunaKhusus
            - Tanggal Mulai: $tanggalMulai
            - Tanggal Selesai: $tanggalSelesai
            - Jam Mulai: $jamMulai
            - Jam Selesai: $jamSelesai
            - Lapangan Dipinjam: $lapanganDipinjam
            - PDF URI: $pdfUri
        """.trimIndent())
    }

    private fun setupUI() {
        // Set facility name
        tvNamaFasilitas.text = namaFasilitas

        // Set event name
        tvEvent.text = namaAcara

        // Set organization name
        tvOrganisasi.text = namaOrganisasi

        // Set rental option
        tvOpsiPeminjaman.text = opsiPeminjaman

        // Set date range
        tvDateStartEnd.text = if (tanggalMulai == tanggalSelesai) {
            tanggalMulai
        } else {
            "$tanggalMulai - $tanggalSelesai"
        }

        // Set time range
        tvTimeStartEnd.text = "$jamMulai - $jamSelesai"

        // Handle special user type for UTG (id 30)
        if (idFasilitas == 30 && !penggunaKhusus.isNullOrEmpty()) {
            layoutPenggunaKhusus.visibility = View.VISIBLE

            // Convert enum name to display text
            val displayText = when (penggunaKhusus) {
                PenggunaKhusus.INTERNAL_UII.name -> "Internal UII"
                PenggunaKhusus.INTERNAL_VS_EKSTERNAL.name -> "Internal UII vs Team Eksternal"
                PenggunaKhusus.EKSTERNAL_UII.name -> "Team Eksternal"
                else -> penggunaKhusus
            }

            tvPenggunaKhusus.text = displayText
        } else {
            layoutPenggunaKhusus.visibility = View.GONE
        }

        // Set fields/courts
        setupLapanganList()
    }

    private fun setupLapanganList() {
        // We need to fetch the actual Lapangan objects to display their names
        lifecycleScope.launch {
            try {
                // Use the lapanganDipinjam list from the bundle
                val lapanganIds = lapanganDipinjam ?: listLapangan ?: emptyList()

                if (lapanganIds.isEmpty()) {
                    tvListLapangan.text = "Tidak ada lapangan yang dipilih"
                    return@launch
                }

                // Retrieve all lapangan for this fasilitas
                val allLapangan = withContext(Dispatchers.IO) {
                    fasilitasRepository.getLapanganByFasilitasId(idFasilitas)
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
        try {
            if (tanggalMulai == tanggalSelesai) {
                totalDays = 1
            } else {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val startDate = LocalDate.parse(tanggalMulai, formatter)
                val endDate = LocalDate.parse(tanggalSelesai, formatter)
                totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
            }

            // Hide days layout for UTG with fixed per-schedule pricing
            if (idFasilitas == 30) {
                layoutJumlahHari.visibility = View.GONE
            } else {
                tvJumlahHari.text = "$totalDays hari"
            }
        } catch (e: Exception) {
            Log.e("PembayaranFragment", "Error calculating days: ${e.message}")
            totalDays = 1
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

    private fun calculateBasePrice() {
        basePrice = when (opsiPeminjaman) {
            "Sesuai Jadwal Rutin" -> 0.0 // Free for regular schedule

            "Diluar Jadwal Rutin" -> {
                if (idFasilitas == 30) {
                    // UTG has special pricing based on user type
                    when (penggunaKhusus) {
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

    private fun calculateFinalPrice() {
        if (basePrice > 0 && discountPercent > 0) {
            discountAmount = basePrice * (discountPercent / 100.0)
            finalPrice = basePrice - discountAmount
        } else {
            discountAmount = 0.0
            finalPrice = basePrice
        }
    }

    private fun updatePaymentUI() {
        val calculationText = StringBuilder()

        if (opsiPeminjaman == "Sesuai Jadwal Rutin") {
            calculationText.append("Peminjaman sesuai jadwal rutin: ${currencyFormat.format(0)}")
        } else {
            if (idFasilitas == 30) {
                val userTypeText = when (penggunaKhusus) {
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processPayment() {
        lifecycleScope.launch {
            try {
                // Disable button to prevent multiple requests
                buttonBayar.isEnabled = false

                // Check if this is a free booking
                val isFreeBooking = opsiPeminjaman == "Sesuai Jadwal Rutin" || finalPrice <= 0

                if (isFreeBooking) {
                    Toast.makeText(requireContext(), "Memproses peminjaman gratis...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Memproses pembayaran...", Toast.LENGTH_SHORT).show()
                }

                // Generate order ID
                val orderId = "ORDER-${System.currentTimeMillis()}"

                // Get current user
                val user = userRepository.getCurrentUser()

                // Prepare payment data
                val itemDetails = ArrayList<Map<String, Any>>()
                val item = HashMap<String, Any>()
                item["id"] = "RENT-$idFasilitas"
                item["price"] = if (isFreeBooking) 0 else finalPrice.toInt()
                item["quantity"] = 1
                item["name"] = "Sewa $namaFasilitas"
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

                // Add data for Supabase database
                requestMap["id_pengguna"] = userRepository.getCurrentUserId()
                requestMap["id_fasilitas"] = idFasilitas

                // Add flag indicating if this is a free booking
                requestMap["is_free_booking"] = isFreeBooking

                // Get user's voucher if available
                val userGamifikasi = withContext(Dispatchers.IO) {
                    user?.let { gamifikasiRepository.getGamifikasiForUser(it) }
                }
                if (userGamifikasi?.idVoucher != null) {
                    requestMap["id_voucher"] = userGamifikasi.idVoucher
                }

                // Add booking data
                val peminjamanData = HashMap<String, Any?>()
                peminjamanData["tanggal_mulai"] = tanggalMulai
                peminjamanData["tanggal_selesai"] = tanggalSelesai
                peminjamanData["jam_mulai"] = jamMulai
                peminjamanData["jam_selesai"] = jamSelesai
                peminjamanData["nama_organisasi"] = namaOrganisasi
                peminjamanData["nama_acara"] = namaAcara
                peminjamanData["pengguna_khusus"] = penggunaKhusus

                // Add the selected courts as integer list
                val lapanganIds = lapanganDipinjam ?: listLapangan ?: ArrayList<Int>()
                peminjamanData["lapangan_ids"] = lapanganIds

                requestMap["peminjaman_data"] = peminjamanData

                // Convert to JSON string using Gson
                val gson = Gson()
                val jsonString = gson.toJson(requestMap)

                // Create request body
                val requestBody = jsonString.toRequestBody("application/json".toMediaType())

                // Call Supabase Function via Retrofit
                val apiService = RetrofitClient.createService(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    apiService.createTransaction(
                        url = "midtrans-sipfo",
                        authHeader = "Bearer ${BuildConfig.API_KEY}",
                        requestBody = requestBody
                    )
                }

                // Process response
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)

                        // For free bookings, just check for success and finish
                        if (isFreeBooking) {
                            val success = jsonResponse.optBoolean("success", false)
                            if (success) {
                                Toast.makeText(requireContext(),
                                    "Peminjaman gratis berhasil dicatat",
                                    Toast.LENGTH_SHORT).show()
                                requireActivity().finish()
                            } else {
                                val errorMessages = jsonResponse.optJSONArray("error_messages")
                                val errorMsg = if (errorMessages != null && errorMessages.length() > 0)
                                    errorMessages.getString(0) else "Gagal menyimpan peminjaman"
                                showPaymentError(errorMsg)
                            }
                            return@launch
                        }

                        // For paid bookings, get the token and proceed with Midtrans
                        val token = jsonResponse.optString("token")
                        if (token.isNotEmpty()) {
                            Log.d(TAG, "Midtrans token received: $token")

                            // Register launcher for Midtrans response
                            midtransLauncher = registerForActivityResult(
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
                                                Toast.makeText(requireContext(),
                                                    "Pembayaran berhasil",
                                                    Toast.LENGTH_SHORT).show()
                                                requireActivity().finish()
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

                            // Start Midtrans payment flow
                            UiKitApi.getDefaultInstance().startPaymentUiFlow(
                                activity = requireActivity(),
                                launcher = midtransLauncher,
                                snapToken = token
                            )
                        } else {
                            showPaymentError("Token tidak ditemukan dalam respons server")
                        }
                    } else {
                        showPaymentError("Respons kosong dari server")
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Error ${response.code()}"
                    showPaymentError(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment: ${e.message}", e)
                showPaymentError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    private fun showPaymentError(message: String) {
        buttonBayar.isEnabled = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Payment error: $message")
    }

}