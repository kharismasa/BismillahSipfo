package com.example.bismillahsipfo.ui.fragment.riwayat

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
        setupButtonListeners()
        observeViewModel()

        // Menampilkan riwayat pending secara default
        showPendingRiwayat()
        setButtonStyles(isPendingActive = true)
    }

    private fun setupRecyclerView() {
        binding.rvRiwayat.layoutManager = LinearLayoutManager(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonListeners() {
        binding.btnPending.setOnClickListener {
            showPendingRiwayat()
            setButtonStyles(isPendingActive = true)
        }

        binding.btnSelesai.setOnClickListener {
            showSelesaiRiwayat()
            setButtonStyles(isPendingActive = false)
        }
    }

    private fun setButtonStyles(isPendingActive: Boolean) {
        if (isPendingActive) {
            binding.btnPending.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
            binding.btnSelesai.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
            binding.btnSelesai.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            binding.btnSelesai.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnSelesai.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
            binding.btnPending.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
            binding.btnPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun showPendingRiwayat() {
        viewModel.loadPendingRiwayat()
    }

    private fun observeViewModel() {
        viewModel.pendingRiwayat.observe(viewLifecycleOwner) { pendingList ->
            pendingAdapter = RowRiwayatPendingAdapter(pendingList)
            binding.rvRiwayat.adapter = pendingAdapter
        }

        viewModel.selesaiRiwayat.observe(viewLifecycleOwner) { selesaiList ->
            selesaiAdapter = RowRiwayatSelesaiAdapter(selesaiList, emptyList()) // Perlu menyesuaikan parameter kedua
            binding.rvRiwayat.adapter = selesaiAdapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSelesaiRiwayat() {
        Log.d("RiwayatFragment", "showSelesaiRiwayat: Mengambil data selesai...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()
                // Mengambil data dari repository
                val peminjamanList = fasilitasRepository.getRiwayatPeminjamanSelesai(currentUserId)
                val pembayaranList = fasilitasRepository.getPembayaranListForPending()
                val fasilitasList = fasilitasRepository.getFasilitasListForSelesai()

                // Log jumlah data yang diambil
                Log.d("RiwayatFragment", "Data peminjaman: ${peminjamanList.size}")
                Log.d("RiwayatFragment", "Data pembayaran: ${pembayaranList.size}")
                Log.d("RiwayatFragment", "Data fasilitas: ${fasilitasList.size}")

                // Log detail setiap peminjaman
                peminjamanList.forEachIndexed { index, riwayat ->
                    Log.d("RiwayatFragment", "Peminjaman $index: " +
                            "namaFasilitas=${riwayat.namaFasilitas}, " +
                            "namaAcara=${riwayat.namaAcara}, " +
                            "tanggalMulai=${riwayat.tanggalMulai}, " +
                            "tanggalSelesai=${riwayat.tanggalSelesai}")
                }

                // Log detail setiap pembayaran
                pembayaranList.forEachIndexed { index, pembayaran ->
                    Log.d("RiwayatFragment", "Pembayaran $index: " +
                            "idPembayaran=${pembayaran.idPembayaran}, " +
                            "statusPembayaran=${pembayaran.statusPembayaran}")
                }

                val peminjamanFasilitasList = peminjamanList.mapNotNull { riwayat ->
                    val fasilitas = fasilitasList.find { it.namaFasilitas == riwayat.namaFasilitas }
                    val pembayaran = pembayaranList.find { it.idPembayaran == riwayat.namaFasilitas }

                    if (fasilitas != null) {
                        Log.d("RiwayatFragment", "Membuat PeminjamanFasilitas untuk: " +
                                "namaFasilitas=${riwayat.namaFasilitas}, " +
                                "namaAcara=${riwayat.namaAcara}, " +
                                "idPembayaran=${pembayaran?.idPembayaran}")

                        PeminjamanFasilitas(
                            idPeminjaman = 0,
                            idFasilitas = fasilitas.idFasilitas,
                            tanggalMulai = riwayat.tanggalMulai,
                            tanggalSelesai = riwayat.tanggalSelesai,
                            jamMulai = riwayat.jamMulai,
                            jamSelesai = riwayat.jamSelesai,
                            namaOrganisasi = "",
                            namaAcara = riwayat.namaAcara,
                            idPembayaran = pembayaran?.idPembayaran ?: "",
                            penggunaKhusus = null,
                            idPengguna = 0,
                            createdAtPeminjaman = Instant.now(),
                            suratPeminjamanUrl = null
                        )
                    } else {
                        Log.d("RiwayatFragment", "Fasilitas tidak ditemukan untuk: ${riwayat.namaFasilitas}")
                        null
                    }
                }

                Log.d("RiwayatFragment", "PeminjamanFasilitas yang akan ditampilkan: ${peminjamanFasilitasList.size}")

                // Menyiapkan adapter untuk data selesai
                selesaiAdapter = RowRiwayatSelesaiAdapter(peminjamanFasilitasList, fasilitasList)
                binding.rvRiwayat.adapter = selesaiAdapter

                // Notify adapter
                selesaiAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e("RiwayatFragment", "Terjadi kesalahan saat mengambil data selesai: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}