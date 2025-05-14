package com.example.bismillahsipfo.ui.fragment.peminjaman

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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.Gamifikasi
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import com.example.bismillahsipfo.data.model.Voucher
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.GamifikasiRepository
import com.example.bismillahsipfo.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class PembayaranFragment : Fragment() {

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
                Toast.makeText(requireContext(), "Memproses pembayaran...", Toast.LENGTH_SHORT).show()

                // Here you would integrate with your payment gateway (Midtrans)
                // For now, we'll just simulate the payment process

                // Create a PeminjamanFasilitas object
                val peminjaman = createPeminjamanObject()

                // TODO: Implement actual payment processing
                // For now, just navigate to the next page or show a success message

                Toast.makeText(requireContext(), "Pembayaran berhasil", Toast.LENGTH_SHORT).show()

                // Navigate to HasilPembayaranFragment or show success UI
                // For now, we'll finish the activity
                requireActivity().finish()
            } catch (e: Exception) {
                Log.e("PembayaranFragment", "Error processing payment: ${e.message}")
                Toast.makeText(requireContext(), "Terjadi kesalahan dalam pemrosesan pembayaran", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPeminjamanObject(): PeminjamanFasilitas? {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val startDate = LocalDate.parse(tanggalMulai, formatter)
            val endDate = LocalDate.parse(tanggalSelesai, formatter)
            val startTime = LocalTime.parse(jamMulai, timeFormatter)
            val endTime = LocalTime.parse(jamSelesai, timeFormatter)

            // Convert penggunaKhusus string to enum if not null
            val penggunaKhususEnum = if (!penggunaKhusus.isNullOrEmpty()) {
                try {
                    PenggunaKhusus.valueOf(penggunaKhusus!!)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            // Generate a unique payment ID
            val paymentId = "PAY-${System.currentTimeMillis()}"

            // Get current user ID
            val idPengguna = userRepository.getCurrentUserId()

            return PeminjamanFasilitas(
                idPeminjaman = 0, // Will be generated by the database
                idFasilitas = idFasilitas,
                tanggalMulai = startDate,
                tanggalSelesai = endDate,
                jamMulai = startTime,
                jamSelesai = endTime,
                namaOrganisasi = namaOrganisasi ?: "",
                namaAcara = namaAcara ?: "",
                idPembayaran = paymentId,
                penggunaKhusus = penggunaKhususEnum,
                idPengguna = idPengguna,
                createdAtPeminjaman = java.time.Instant.now()
            )
        } catch (e: Exception) {
            Log.e("PembayaranFragment", "Error creating peminjaman object: ${e.message}")
            return null
        }
    }
}