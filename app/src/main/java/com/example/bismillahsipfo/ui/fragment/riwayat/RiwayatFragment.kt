package com.example.bismillahsipfo.ui.fragment.riwayat

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bismillahsipfo.adapter.RowRiwayatPendingAdapter
import com.example.bismillahsipfo.adapter.RowRiwayatSelesaiAdapter
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.model.RiwayatPending
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.databinding.FragmentRiwayatBinding
import kotlinx.coroutines.launch
import java.time.Instant

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private val fasilitasRepository = FasilitasRepository()

    private lateinit var pendingAdapter: RowRiwayatPendingAdapter
    private lateinit var selesaiAdapter: RowRiwayatSelesaiAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtonListeners()
    }

    private fun setupRecyclerView() {
        binding.rvRiwayat.layoutManager = LinearLayoutManager(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonListeners() {
        binding.btnPending.setOnClickListener {
            showPendingRiwayat()
        }

        binding.btnSelesai.setOnClickListener {
            showSelesaiRiwayat()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSelesaiRiwayat() {
        Log.d("RiwayatFragment", "showSelesaiRiwayat: Mengambil data selesai...")
    
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Mengambil data dari repository
                val peminjamanList = fasilitasRepository.getRiwayatPeminjamanSelesai()
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
                            createdAtPeminjaman = Instant.now()
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPendingRiwayat() {
        Log.d("RiwayatFragment", "showPendingRiwayat: Memulai pengambilan data pending...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pendingPembayaranList = fasilitasRepository.getPendingPembayaran()
                Log.d("RiwayatFragment", "Jumlah pembayaran pending: ${pendingPembayaranList.size}")
    
                val riwayatPendingList = mutableListOf<RiwayatPending>()
    
                for (pembayaran in pendingPembayaranList) {
                    Log.d("RiwayatFragment", "Memproses pembayaran: ${pembayaran.idPembayaran}")
                    val peminjaman = fasilitasRepository.getPeminjamanByIdPembayaran(pembayaran.idPembayaran)
                    if (peminjaman != null) {
                        Log.d("RiwayatFragment", "Peminjaman ditemukan untuk pembayaran: ${pembayaran.idPembayaran}")
                        val fasilitas = fasilitasRepository.getFasilitasById(peminjaman.idFasilitas)
                        if (fasilitas != null) {
                            Log.d("RiwayatFragment", "Fasilitas ditemukan: ${fasilitas.namaFasilitas}")
                            riwayatPendingList.add(
                                RiwayatPending(
                                    tanggalMulai = peminjaman.tanggalMulai,
                                    tanggalSelesai = peminjaman.tanggalSelesai,
                                    namaFasilitas = fasilitas.namaFasilitas,
                                    totalBiaya = pembayaran.totalBiaya,
                                    waktuKadaluwarsa = pembayaran.waktuKadaluwarsa
                                )
                            )
                            Log.d("RiwayatFragment", "RiwayatPending ditambahkan untuk: ${fasilitas.namaFasilitas}")
                        } else {
                            Log.e("RiwayatFragment", "Fasilitas tidak ditemukan untuk idFasilitas: ${peminjaman.idFasilitas}")
                        }
                    } else {
                        Log.e("RiwayatFragment", "Peminjaman tidak ditemukan untuk idPembayaran: ${pembayaran.idPembayaran}")
                    }
                }
    
                Log.d("RiwayatFragment", "Total RiwayatPending: ${riwayatPendingList.size}")
    
                pendingAdapter = RowRiwayatPendingAdapter(riwayatPendingList)
                binding.rvRiwayat.adapter = pendingAdapter
                Log.d("RiwayatFragment", "Adapter diset ke RecyclerView")
    
                // Notify adapter
                pendingAdapter.notifyDataSetChanged()
                Log.d("RiwayatFragment", "Adapter dinotifikasi untuk perubahan data")
    
            } catch (e: Exception) {
                Log.e("RiwayatFragment", "Terjadi kesalahan saat mengambil data pending: ${e.message}")
                Log.e("RiwayatFragment", "Stack trace: ${e.stackTraceToString()}")
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}