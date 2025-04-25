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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private lateinit var allJadwalList: List<Pair<PeminjamanFasilitas, Fasilitas>>
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
            if (view == null || !isAdded) return  // Check if fragment is still attached
            Glide.with(this)
                .load(imageUrls[currentImageIndex])
                .into(binding.imageHome)
            currentImageIndex = (currentImageIndex + 1) % imageUrls.size
        }

        val imageSliderRunnable = object : Runnable {
            override fun run() {
                loadNextImage()
                handler.postDelayed(this, 3000)
            }
        }

        handler.post(imageSliderRunnable)
    }

    private fun setupFasilitasRecyclerView() {
        binding.rvFasilitas.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fasilitasList = fasilitasRepository.getFasilitas()
                fasilitasAdapter = FasilitasAdapter(fasilitasList) { fasilitas ->
                    // Ini adalah handler untuk klik item
                    openHalamanInformasi(fasilitas)
                }
                binding.rvFasilitas.adapter = fasilitasAdapter
            } catch (e: Exception) {
                // Handle any errors that occur while loading data
            }
        }
    }

    private fun openHalamanInformasi(fasilitas: Fasilitas) {
        val intent = Intent(requireContext(), HalamanInformasiActivity::class.java).apply {
            putExtra("FASILITAS_ID", fasilitas.idFasilitas)
            // Tambahkan data lain yang mungkin diperlukan oleh HalamanInformasiActivity
        }
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupJadwalDipinjamRecyclerView() {
        binding.rvJadwalFasilitas.layoutManager = LinearLayoutManager(requireContext())
        jadwalDipinjamAdapter = RowJadwalDipinjamAdapter()
        binding.rvJadwalFasilitas.adapter = jadwalDipinjamAdapter

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userId = userRepository.getCurrentUserId()
                val peminjamanList = peminjamanFasilitasRepository.getPeminjamanByUserAndDate(userId)
                allJadwalList = mutableListOf()
                val today = LocalDate.now()

                for (peminjaman in peminjamanList) {
                    val fasilitas = fasilitasRepository.getFasilitasById(peminjaman.idFasilitas)
                    if (fasilitas != null) {
                        val startDate = maxOf(peminjaman.tanggalMulai, today)
                        val endDate = peminjaman.tanggalSelesai
                        var currentDate = startDate
                        while (!currentDate.isAfter(endDate)) {
                            allJadwalList += Pair(peminjaman.copy(tanggalMulai = currentDate, tanggalSelesai = currentDate), fasilitas)
                            currentDate = currentDate.plusDays(1)
                        }
                    }
                }

                filterJadwal(FilterType.ALL)

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading jadwal: ${e.message}")
                binding.imgEmpty.visibility = View.VISIBLE
                binding.rvJadwalFasilitas.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterJadwal(filterType: FilterType) {
        val today = LocalDate.now()
        val filteredList = when (filterType) {
            FilterType.ALL -> allJadwalList
            FilterType.TODAY -> allJadwalList.filter { it.first.tanggalMulai == today }
            FilterType.TOMORROW -> allJadwalList.filter { it.first.tanggalMulai == today.plusDays(1) }
        }

        jadwalDipinjamAdapter.submitList(filteredList.sortedBy { it.first.tanggalMulai })

        if (filteredList.isEmpty()) {
            binding.imgEmpty.visibility = View.VISIBLE
            binding.rvJadwalFasilitas.visibility = View.GONE
        } else {
            binding.imgEmpty.visibility = View.GONE
            binding.rvJadwalFasilitas.visibility = View.VISIBLE
        }
    }

    enum class FilterType {
        ALL, TODAY, TOMORROW
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Remove all callbacks to prevent memory leaks
        _binding = null // Clear binding reference to avoid memory leaks
    }
}
