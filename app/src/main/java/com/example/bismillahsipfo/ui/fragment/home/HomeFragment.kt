package com.example.bismillahsipfo.ui.fragment.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.FasilitasAdapter
import com.example.bismillahsipfo.adapter.RowJadwalDipinjamAdapter
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.DipinjamFasilitasRepository
import com.example.bismillahsipfo.data.repository.UserRepository
import com.example.bismillahsipfo.databinding.FragmentHomeBinding
import com.example.bismillahsipfo.ui.fragment.informasi.HalamanInformasiActivity
import com.example.bismillahsipfo.ui.fragment.notification.NotificationActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fasilitasAdapter: FasilitasAdapter
    private lateinit var fasilitasRepository: FasilitasRepository
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var jadwalDipinjamAdapter: RowJadwalDipinjamAdapter
    private lateinit var peminjamanFasilitasRepository: DipinjamFasilitasRepository
    private lateinit var userRepository: UserRepository

    private var allJadwalList: List<Pair<PeminjamanFasilitas, Fasilitas>> = emptyList()
    private var currentFilterType: FilterType = FilterType.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fasilitasRepository = FasilitasRepository()
        peminjamanFasilitasRepository = DipinjamFasilitasRepository()
        userRepository = UserRepository(requireContext())

        setupUI()
        loadUserData()
        setupImageSlider()
        setupFasilitasRecyclerView()
        setupFilterButtons()
        setupJadwalDipinjamRecyclerView()
    }

    private fun setupUI() {
        binding.icNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFilterButtons() {
        binding.btnAll.setOnClickListener {
            filterJadwal(FilterType.ALL)
            updateButtonColors(FilterType.ALL)
        }
        binding.btnHariIni.setOnClickListener {
            filterJadwal(FilterType.TODAY)
            updateButtonColors(FilterType.TODAY)
        }
        binding.btnBesok.setOnClickListener {
            filterJadwal(FilterType.TOMORROW)
            updateButtonColors(FilterType.TOMORROW)
        }

        // Set warna default untuk tombol "All"
        updateButtonColors(FilterType.ALL)
    }

    private fun updateButtonColors(selectedFilter: FilterType) {
        // ✅ NULL CHECK - Pastikan binding masih ada
        if (_binding == null) return

        val mediumBlue = ContextCompat.getColor(requireContext(), R.color.medium_blue)
        val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        binding.btnAll.setBackgroundColor(if (selectedFilter == FilterType.ALL) mediumBlue else defaultColor)
        binding.btnHariIni.setBackgroundColor(if (selectedFilter == FilterType.TODAY) mediumBlue else defaultColor)
        binding.btnBesok.setBackgroundColor(if (selectedFilter == FilterType.TOMORROW) mediumBlue else defaultColor)
    }

    private fun loadUserData() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)
        val userName = sharedPreferences.getString("nama", "User")
        binding.tvUsername.text = userName
    }

    private fun setupImageSlider() {
        val imageUrls = listOf(
            "https://ulxdrgkjbvalhxesibpr.supabase.co/storage/v1/object/public/Fasilitas//dot1.jpg",
            "https://ulxdrgkjbvalhxesibpr.supabase.co/storage/v1/object/public/Fasilitas//dot2.jpg",
            "https://ulxdrgkjbvalhxesibpr.supabase.co/storage/v1/object/public/Fasilitas//dot3.jpg",
            "https://ulxdrgkjbvalhxesibpr.supabase.co/storage/v1/object/public/Fasilitas//dot4.jpg",
            "https://ulxdrgkjbvalhxesibpr.supabase.co/storage/v1/object/public/Fasilitas//dot5.jpg"
        )
        var currentImageIndex = 0

        fun loadNextImage() {
            // ✅ LIFECYCLE CHECK - Pastikan fragment masih aktif
            if (_binding == null || !isAdded || view == null) return

            try {
                Glide.with(this)
                    .load(imageUrls[currentImageIndex])
                    .into(binding.imageHome)
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading image: ${e.message}")
            }
        }

        val imageSliderRunnable = object : Runnable {
            override fun run() {
                loadNextImage()
                // ✅ LIFECYCLE CHECK - Pastikan fragment masih aktif sebelum schedule next
                if (_binding != null && isAdded) {
                    handler.postDelayed(this, 3000)
                }
            }
        }

        handler.post(imageSliderRunnable)
    }

    private fun setupFasilitasRecyclerView() {
        // ✅ PERBAIKAN: Setup horizontal RecyclerView dengan optimasi
        binding.rvFasilitas.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            // Nonaktifkan nested scrolling untuk performa yang lebih baik
            isNestedScrollingEnabled = false
            // Optimalkan untuk item yang memiliki ukuran tetap
            setHasFixedSize(true)
        }

        // ✅ GUNAKAN viewLifecycleOwner.lifecycleScope - Lifecycle aware
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fasilitasList = withContext(Dispatchers.IO) {
                    fasilitasRepository.getFasilitas()
                }

                // ✅ NULL CHECK - Pastikan fragment masih aktif
                if (_binding != null && isAdded) {
                    fasilitasAdapter = FasilitasAdapter(fasilitasList) { fasilitas ->
                        openHalamanInformasi(fasilitas)
                    }
                    binding.rvFasilitas.adapter = fasilitasAdapter
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading fasilitas: ${e.message}")
                // ✅ NULL CHECK untuk error handling
                if (_binding != null && isAdded) {
                    // Handle error if needed
                }
            }
        }
    }

    private fun openHalamanInformasi(fasilitas: Fasilitas) {
        val intent = Intent(requireContext(), HalamanInformasiActivity::class.java).apply {
            putExtra("FASILITAS_ID", fasilitas.idFasilitas)
        }
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupJadwalDipinjamRecyclerView() {
        // ✅ PERBAIKAN: Setup vertical RecyclerView dengan optimasi untuk NestedScrollView
        binding.rvJadwalFasilitas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // PENTING: Nonaktifkan nested scrolling karena parent sudah scrollable
            isNestedScrollingEnabled = false
            // Optimalkan untuk item yang memiliki ukuran tidak tetap
            setHasFixedSize(false)
            // Nonaktifkan overscroll untuk mencegah konflik dengan parent scroll
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        jadwalDipinjamAdapter = RowJadwalDipinjamAdapter()
        binding.rvJadwalFasilitas.adapter = jadwalDipinjamAdapter

        // ✅ GUNAKAN viewLifecycleOwner.lifecycleScope - Lifecycle aware
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    userRepository.getCurrentUserId()
                }

                val peminjamanList = withContext(Dispatchers.IO) {
                    peminjamanFasilitasRepository.getPeminjamanByUserAndDate(userId)
                }

                val tempJadwalList = mutableListOf<Pair<PeminjamanFasilitas, Fasilitas>>()
                val today = LocalDate.now()

                for (peminjaman in peminjamanList) {
                    val fasilitas = withContext(Dispatchers.IO) {
                        fasilitasRepository.getFasilitasById(peminjaman.idFasilitas)
                    }

                    if (fasilitas != null) {
                        val startDate = maxOf(peminjaman.tanggalMulai, today)
                        val endDate = peminjaman.tanggalSelesai
                        var currentDate = startDate
                        while (!currentDate.isAfter(endDate)) {
                            tempJadwalList += Pair(
                                peminjaman.copy(tanggalMulai = currentDate, tanggalSelesai = currentDate),
                                fasilitas
                            )
                            currentDate = currentDate.plusDays(1)
                        }
                    }
                }

                // ✅ NULL CHECK - Pastikan fragment masih aktif sebelum update UI
                if (_binding != null && isAdded) {
                    allJadwalList = tempJadwalList
                    filterJadwal(FilterType.ALL)
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading jadwal: ${e.message}")

                // ✅ NULL CHECK untuk error handling
                if (_binding != null && isAdded) {
                    binding.imgEmpty.visibility = View.VISIBLE
                    binding.rvJadwalFasilitas.visibility = View.GONE
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterJadwal(filterType: FilterType) {
        // ✅ NULL CHECK - Pastikan binding masih ada
        if (_binding == null) return

        val today = LocalDate.now()
        val filteredList = when (filterType) {
            FilterType.ALL -> allJadwalList
            FilterType.TODAY -> allJadwalList.filter { it.first.tanggalMulai == today }
            FilterType.TOMORROW -> allJadwalList.filter { it.first.tanggalMulai == today.plusDays(1) }
        }

        // ✅ PERBAIKAN: Update current filter type
        currentFilterType = filterType

        jadwalDipinjamAdapter.submitList(filteredList.sortedBy { it.first.tanggalMulai })

        if (filteredList.isEmpty()) {
            binding.imgEmpty.visibility = View.VISIBLE
            binding.rvJadwalFasilitas.visibility = View.GONE
        } else {
            binding.imgEmpty.visibility = View.GONE
            binding.rvJadwalFasilitas.visibility = View.VISIBLE
        }

        // ✅ OPTIONAL: Log untuk debugging
        Log.d("HomeFragment", "Filtered jadwal: ${filteredList.size} items for filter: $filterType")
    }

    enum class FilterType {
        ALL, TODAY, TOMORROW
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ CLEANUP - Remove all callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)

        // ✅ CLEANUP - Clear binding reference to avoid memory leaks
        _binding = null
    }

    // ✅ TAMBAHAN: Override onResume untuk refresh data saat kembali ke fragment
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        // Refresh jadwal data when returning to home
        if (_binding != null && ::jadwalDipinjamAdapter.isInitialized) {
            setupJadwalDipinjamRecyclerView()
        }
    }
}