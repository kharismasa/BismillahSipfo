package com.example.bismillahsipfo.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.adapter.FasilitasAdapter
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.databinding.FragmentHomeBinding
import com.example.bismillahsipfo.ui.fragment.notification.NotificationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fasilitasAdapter: FasilitasAdapter
    private lateinit var fasilitasRepository: FasilitasRepository
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fasilitasRepository = FasilitasRepository()

        setupUI()
        loadUserData()
        setupImageSlider()
        setupFasilitasRecyclerView()
    }

    private fun setupUI() {
        binding.icNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
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
                fasilitasAdapter = FasilitasAdapter(fasilitasList)
                binding.rvFasilitas.adapter = fasilitasAdapter
            } catch (e: Exception) {
                // Handle any errors that occur while loading data
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Remove all callbacks to prevent memory leaks
        _binding = null // Clear binding reference to avoid memory leaks
    }
}
