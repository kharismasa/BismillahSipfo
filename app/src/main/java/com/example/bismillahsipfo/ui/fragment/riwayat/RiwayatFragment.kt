package com.example.bismillahsipfo.ui.fragment.riwayat

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.RowRiwayatPendingAdapter
import com.example.bismillahsipfo.adapter.RowRiwayatSelesaiAdapter
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.model.RiwayatPending
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.repository.RiwayatViewModel
import com.example.bismillahsipfo.data.repository.RiwayatViewModelFactory
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.databinding.FragmentRiwayatBinding
import com.example.bismillahsipfo.utils.UserDebugHelper
import com.example.bismillahsipfo.data.model.StatusPembayaran
import kotlinx.coroutines.launch
import java.time.Instant

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private val fasilitasRepository = FasilitasRepository()
    private lateinit var userRepository: UserRepository

    private lateinit var viewModel: RiwayatViewModel
    private lateinit var pendingAdapter: RowRiwayatPendingAdapter
    private lateinit var selesaiAdapter: RowRiwayatSelesaiAdapter

    // ✅ PERUBAHAN: Default ke Selesai (false = tidak pending active)
    private var isPendingActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userRepository = UserRepository(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fasilitasRepository = FasilitasRepository()
        val userRepository = UserRepository(requireContext())
        val viewModelFactory = RiwayatViewModelFactory(fasilitasRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[RiwayatViewModel::class.java]

        setupRecyclerView()
        setupModernToggleButtons()
        observeViewModel()

        // Debug user session di fragment ini
        debugRiwayatData()

        // ✅ PERUBAHAN: Default menampilkan riwayat SELESAI
        showSelesaiRiwayat()
        setModernButtonStyles(isPendingActive = false)
    }

    // Method untuk debug riwayat data
    private fun debugRiwayatData() {
        Log.d("RiwayatFragment", "🧪 Starting RiwayatFragment debug...")

        // Debug user session
        UserDebugHelper.debugUserSession(requireContext())

        // Test specific queries yang digunakan di RiwayatFragment
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userRepository = UserRepository(requireContext())
                val fasilitasRepository = FasilitasRepository()
                val currentUserId = userRepository.getCurrentUserId()

                Log.d("RiwayatFragment", "👤 Current User ID: $currentUserId")

                if (currentUserId == -1) {
                    Log.e("RiwayatFragment", "❌ User ID is -1! Cannot load riwayat data.")

                    if (BuildConfig.DEBUG) {
//                        Toast.makeText(requireContext(),
//                            "⚠️ User not logged in properly!",
//                            Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Test 1: Check all user's peminjaman
                Log.d("RiwayatFragment", "🧪 Test 1: Getting all peminjaman for user $currentUserId")
                val allUserPeminjaman = fasilitasRepository.getAllPeminjamanForUser(currentUserId)
                Log.d("RiwayatFragment", "📊 Found ${allUserPeminjaman.size} peminjaman for user")

                // Test 2: Check pending/failed pembayaran
                Log.d("RiwayatFragment", "🧪 Test 2: Getting pending/failed pembayaran")
                val pendingPembayaran = fasilitasRepository.getPendingAndFailedPembayaran(currentUserId)
                Log.d("RiwayatFragment", "📊 Found ${pendingPembayaran.size} pending/failed pembayaran")

                // Test 3: Check success riwayat
                Log.d("RiwayatFragment", "🧪 Test 3: Getting success riwayat")
                val successRiwayat = fasilitasRepository.getRiwayatPeminjamanSelesai(currentUserId)
                Log.d("RiwayatFragment", "📊 Found ${successRiwayat.size} success riwayat")

                // Test 4: Raw queries
                Log.d("RiwayatFragment", "🧪 Test 4: Running debug user queries")
                val debugResults = fasilitasRepository.debugUserQueries(currentUserId)

                // Show results in debug mode
                if (BuildConfig.DEBUG) {
                    val summary = "Peminjaman: ${allUserPeminjaman.size}, Pending: ${pendingPembayaran.size}, Success: ${successRiwayat.size}"
//                    Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show()
                }

                // Log specific data for analysis
                allUserPeminjaman.forEach { peminjaman ->
                    Log.d("RiwayatFragment", "🔍 Peminjaman: ID=${peminjaman.idPeminjaman}, PaymentID=${peminjaman.idPembayaran}, Fasilitas=${peminjaman.idFasilitas}")
                }

                pendingPembayaran.forEach { pembayaran ->
                    Log.d("RiwayatFragment", "🔍 Pending Payment: ID=${pembayaran.idPembayaran}, Status=${pembayaran.statusPembayaran}, Amount=${pembayaran.totalBiaya}")
                }

            } catch (e: Exception) {
                Log.e("RiwayatFragment", "❌ Error in riwayat debug: ${e.message}", e)

                if (BuildConfig.DEBUG) {
//                    Toast.makeText(requireContext(),
//                        "Debug Error: ${e.message}",
//                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvRiwayat.layoutManager = LinearLayoutManager(context)

        // Add item animator for smooth transitions
        binding.rvRiwayat.itemAnimator?.apply {
            addDuration = 300
            removeDuration = 300
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupModernToggleButtons() {
        binding.btnPending.setOnClickListener {
            if (!isPendingActive) {
                animateToggleButton(true)
                showPendingRiwayat()
                setModernButtonStyles(isPendingActive = true)
                isPendingActive = true
            }
        }

        binding.btnSelesai.setOnClickListener {
            if (isPendingActive) {
                animateToggleButton(false)
                showSelesaiRiwayat()
                setModernButtonStyles(isPendingActive = false)
                isPendingActive = false
            }
        }
    }

    private fun animateToggleButton(toPending: Boolean) {
        val pendingButton = binding.btnPending
        val selesaiButton = binding.btnSelesai

        // Create scale animation for active button
        val activeButton = if (toPending) pendingButton else selesaiButton

        // Scale animation for feedback
        val scaleDown = ObjectAnimator.ofFloat(activeButton, "scaleX", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(activeButton, "scaleX", 0.95f, 1f)

        scaleDown.duration = 100
        scaleUp.duration = 100

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(scaleDown, scaleUp)
        animatorSet.start()
    }

    private fun setModernButtonStyles(isPendingActive: Boolean) {
        val pendingButton = binding.btnPending
        val selesaiButton = binding.btnSelesai

        // Clear any existing background tint first
        pendingButton.backgroundTintList = null
        selesaiButton.backgroundTintList = null

        if (isPendingActive) {
            // Pending ACTIVE: Background biru, text putih
            pendingButton.setBackgroundResource(R.drawable.toggle_button_active)
            pendingButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            // Selesai INACTIVE: Background transparan, text putih
            selesaiButton.setBackgroundResource(R.drawable.toggle_button_inactive)
            selesaiButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            // Selesai ACTIVE: Background biru, text putih
            selesaiButton.setBackgroundResource(R.drawable.toggle_button_active)
            selesaiButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            // Pending INACTIVE: Background transparan, text putih
            pendingButton.setBackgroundResource(R.drawable.toggle_button_inactive)
            pendingButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun showPendingRiwayat() {
        Log.d("RiwayatFragment", "📱 showPendingRiwayat called")
        showLoadingState()

        // Debug log sebelum load
        if (BuildConfig.DEBUG) {
            Log.d("RiwayatFragment", "🔄 Loading pending riwayat via ViewModel...")
        }

        viewModel.loadPendingAndFailedRiwayat()
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("RiwayatFragment", "📱 Loading state: $isLoading")
            if (isLoading) {
                showLoadingState()
            } else {
                hideLoadingState()
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Log.d("RiwayatFragment", "📱 Error message: $errorMessage")
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                showEmptyState("Gagal memuat data")
            }
        }

        // Observe pending/failed riwayat
        viewModel.pendingRiwayat.observe(viewLifecycleOwner) { pendingList ->
            Log.d("RiwayatFragment", "📱 Pending riwayat observed: ${pendingList?.size ?: "null"} items")

            hideLoadingState()
            if (pendingList.isNullOrEmpty()) {
                Log.d("RiwayatFragment", "📱 Pending list is empty, showing empty state")
                showEmptyState("Belum ada riwayat pending/gagal")
            } else {
                Log.d("RiwayatFragment", "📱 Setting up pending adapter with ${pendingList.size} items")
                hideEmptyState()

                // Log each item for debugging
                pendingList.forEachIndexed { index, riwayat ->
                    Log.d("RiwayatFragment", "  [$index] Payment: ${riwayat.idPembayaran}, Fasilitas: ${riwayat.namaFasilitas}, Status: ${riwayat.statusPembayaran}")
                }

                // Create adapter with regenerate token callback
                pendingAdapter = RowRiwayatPendingAdapter(
                    riwayatList = pendingList,
                    onRegenerateToken = { paymentId, callback ->
                        // Use ViewModel to regenerate token
                        viewModel.regenerateMidtransToken(paymentId, callback)
                    }
                )
                binding.rvRiwayat.adapter = pendingAdapter

                if (BuildConfig.DEBUG) {
//                    Toast.makeText(requireContext(),
//                        "✅ Loaded ${pendingList.size} pending items",
//                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe selesai riwayat - TIDAK DIPERLUKAN KARENA TIDAK MENGGUNAKAN VIEWMODEL
        viewModel.selesaiRiwayat.observe(viewLifecycleOwner) { selesaiList ->
            Log.d("RiwayatFragment", "📱 Selesai riwayat observed: ${selesaiList?.size ?: "null"} items")

            hideLoadingState()
            if (selesaiList.isNullOrEmpty()) {
                showEmptyState("Belum ada riwayat selesai")
            } else {
                hideEmptyState()
                selesaiAdapter = RowRiwayatSelesaiAdapter(selesaiList, emptyList())
                binding.rvRiwayat.adapter = selesaiAdapter

                if (BuildConfig.DEBUG) {
//                    Toast.makeText(requireContext(),
//                        "✅ Loaded ${selesaiList.size} completed items",
//                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSelesaiRiwayat() {
        Log.d("RiwayatFragment", "showSelesaiRiwayat: Mengambil data selesai...")
        showLoadingState()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()

                // ✅ KEMBALI KE PENDEKATAN ASLI: Ambil semua peminjaman user
                val allPeminjamanUser = fasilitasRepository.getAllPeminjamanForUser(currentUserId)
                // ✅ PERBAIKAN: Ambil SEMUA pembayaran, bukan hanya pending
                val allPembayaran = fasilitasRepository.getAllPembayaran()
                val fasilitasList = fasilitasRepository.getFasilitasListForSelesai()

                Log.d("RiwayatFragment", "Data peminjaman: ${allPeminjamanUser.size}")
                Log.d("RiwayatFragment", "Data pembayaran: ${allPembayaran.size}")
                Log.d("RiwayatFragment", "Data fasilitas: ${fasilitasList.size}")

                // Filter yang status pembayarannya SUCCESS
                val successPeminjaman = allPeminjamanUser.filter { peminjaman ->
                    val pembayaran = allPembayaran.find { it.idPembayaran == peminjaman.idPembayaran }
                    val isSuccess = pembayaran?.statusPembayaran == StatusPembayaran.SUCCESS

                    if (isSuccess) {
                        Log.d("RiwayatFragment", "SUCCESS: ${peminjaman.idPembayaran}")
                    } else {
                        Log.d("RiwayatFragment", "NOT SUCCESS: ${peminjaman.idPembayaran}, Status: ${pembayaran?.statusPembayaran}")
                    }

                    isSuccess
                }.filter { peminjaman ->
                    // Pastikan fasilitas ada
                    fasilitasList.any { it.idFasilitas == peminjaman.idFasilitas }
                }

                Log.d("RiwayatFragment", "Filtered SUCCESS peminjaman: ${successPeminjaman.size}")

                // ✅ SORTING FIX: Gunakan idPeminjaman untuk sorting (descending = terbaru dulu)
                val sortedPeminjamanFasilitasList = successPeminjaman.sortedByDescending { peminjaman ->
                    val sortValue = peminjaman.idPeminjaman ?: 0
                    Log.d("RiwayatFragment", "Sort by ID: ${peminjaman.idPembayaran} -> $sortValue")
                    sortValue
                }

                Log.d("RiwayatFragment", "Sorted peminjaman by idPeminjaman: ${sortedPeminjamanFasilitasList.size}")

                hideLoadingState()

                if (sortedPeminjamanFasilitasList.isEmpty()) {
                    showEmptyState("Belum ada riwayat selesai")
                } else {
                    hideEmptyState()
                    selesaiAdapter = RowRiwayatSelesaiAdapter(sortedPeminjamanFasilitasList, fasilitasList)
                    binding.rvRiwayat.adapter = selesaiAdapter
                    selesaiAdapter.notifyDataSetChanged()

                    Log.d("RiwayatFragment", "✅ Successfully loaded ${sortedPeminjamanFasilitasList.size} completed items")
                }

            } catch (e: Exception) {
                Log.e("RiwayatFragment", "❌ Error loading selesai data: ${e.message}", e)
                hideLoadingState()
                showEmptyState("Gagal memuat data: ${e.message}")
            }
        }
    }

    private fun showLoadingState() {
        binding.rvRiwayat.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        // You can add a loading indicator here if needed
    }

    private fun hideLoadingState() {
        binding.rvRiwayat.visibility = View.VISIBLE
    }

    private fun showEmptyState(message: String) {
        binding.rvRiwayat.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE

        // Safely update empty message text
        try {
            val emptyLinearLayout = binding.emptyView as? LinearLayout
            val textViews = emptyLinearLayout?.let { layout ->
                (0 until layout.childCount).mapNotNull { index ->
                    layout.getChildAt(index) as? TextView
                }
            }
            textViews?.lastOrNull()?.text = message
        } catch (e: Exception) {
            Log.d("RiwayatFragment", "Could not update empty state message: ${e.message}")
        }
    }

    private fun hideEmptyState() {
        binding.emptyView.visibility = View.GONE
        binding.rvRiwayat.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // ✅ PERUBAHAN: Refresh data berdasarkan state aktif (default selesai)
        if (isPendingActive) {
            showPendingRiwayat()
        } else {
            showSelesaiRiwayat()
        }
    }
}