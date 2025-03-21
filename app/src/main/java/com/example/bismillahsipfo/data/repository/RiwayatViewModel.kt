package com.example.bismillahsipfo.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.adapter.RowRiwayatSelesaiAdapter
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.RiwayatPending
import kotlinx.coroutines.launch
import java.time.Instant

class RiwayatViewModel(
    private val fasilitasRepository: FasilitasRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _pendingRiwayat = MutableLiveData<List<RiwayatPending>>()
    val pendingRiwayat: LiveData<List<RiwayatPending>> = _pendingRiwayat

    private val _selesaiRiwayat = MutableLiveData<List<PeminjamanFasilitas>>()
    val selesaiRiwayat: LiveData<List<PeminjamanFasilitas>> = _selesaiRiwayat

    fun loadPendingRiwayat() {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()
                val pendingPembayaranList = fasilitasRepository.getPendingPembayaran(currentUserId)
                val riwayatPendingList = pendingPembayaranList.mapNotNull { pembayaran ->
                    val peminjaman = fasilitasRepository.getPeminjamanByIdPembayaran(pembayaran.idPembayaran)
                    peminjaman?.let {
                        val fasilitas = fasilitasRepository.getFasilitasById(it.idFasilitas)
                        fasilitas?.let { f ->
                            RiwayatPending(
                                tanggalMulai = it.tanggalMulai,
                                tanggalSelesai = it.tanggalSelesai,
                                namaFasilitas = f.namaFasilitas,
                                totalBiaya = pembayaran.totalBiaya,
                                waktuKadaluwarsa = pembayaran.waktuKadaluwarsa
                            )
                        }
                    }
                }
                _pendingRiwayat.postValue(riwayatPendingList)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun loadSelesaiRiwayat() {
//        Log.d("RiwayatFragment", "showSelesaiRiwayat: Mengambil data selesai...")
//
//        viewModelScope.launch {
//            try {
//                val currentUserId = userRepository.getCurrentUserId()
//                // Mengambil data dari repository
//                val peminjamanList = fasilitasRepository.getRiwayatPeminjamanSelesai(currentUserId)
//                val pembayaranList = fasilitasRepository.getPembayaranListForPending()
//                val fasilitasList = fasilitasRepository.getFasilitasListForSelesai()
//
//                // Log jumlah data yang diambil
//                Log.d("RiwayatFragment", "Data peminjaman: ${peminjamanList.size}")
//                Log.d("RiwayatFragment", "Data pembayaran: ${pembayaranList.size}")
//                Log.d("RiwayatFragment", "Data fasilitas: ${fasilitasList.size}")
//
//                // Log detail setiap peminjaman
//                peminjamanList.forEachIndexed { index, riwayat ->
//                    Log.d("RiwayatFragment", "Peminjaman $index: " +
//                            "namaFasilitas=${riwayat.namaFasilitas}, " +
//                            "namaAcara=${riwayat.namaAcara}, " +
//                            "tanggalMulai=${riwayat.tanggalMulai}, " +
//                            "tanggalSelesai=${riwayat.tanggalSelesai}")
//                }
//
//                // Log detail setiap pembayaran
//                pembayaranList.forEachIndexed { index, pembayaran ->
//                    Log.d("RiwayatFragment", "Pembayaran $index: " +
//                            "idPembayaran=${pembayaran.idPembayaran}, " +
//                            "statusPembayaran=${pembayaran.statusPembayaran}")
//                }
//
//                val peminjamanFasilitasList = peminjamanList.mapNotNull { riwayat ->
//                    val fasilitas = fasilitasList.find { it.namaFasilitas == riwayat.namaFasilitas }
//                    val pembayaran = pembayaranList.find { it.idPembayaran == riwayat.namaFasilitas }
//
//                    if (fasilitas != null) {
//                        Log.d("RiwayatFragment", "Membuat PeminjamanFasilitas untuk: " +
//                                "namaFasilitas=${riwayat.namaFasilitas}, " +
//                                "namaAcara=${riwayat.namaAcara}, " +
//                                "idPembayaran=${pembayaran?.idPembayaran}")
//
//                        PeminjamanFasilitas(
//                            idPeminjaman = 0,
//                            idFasilitas = fasilitas.idFasilitas,
//                            tanggalMulai = riwayat.tanggalMulai,
//                            tanggalSelesai = riwayat.tanggalSelesai,
//                            jamMulai = riwayat.jamMulai,
//                            jamSelesai = riwayat.jamSelesai,
//                            namaOrganisasi = "",
//                            namaAcara = riwayat.namaAcara,
//                            idPembayaran = pembayaran?.idPembayaran ?: "",
//                            penggunaKhusus = null,
//                            idPengguna = 0,
//                            createdAtPeminjaman = Instant.now()
//                        )
//                    } else {
//                        Log.d("RiwayatFragment", "Fasilitas tidak ditemukan untuk: ${riwayat.namaFasilitas}")
//                        null
//                    }
//                }
//
//                Log.d("RiwayatFragment", "PeminjamanFasilitas yang akan ditampilkan: ${peminjamanFasilitasList.size}")
//
//                // Update LiveData
//                _selesaiRiwayat.postValue(peminjamanFasilitasList)
//
//            } catch (e: Exception) {
//                Log.e("RiwayatFragment", "Terjadi kesalahan saat mengambil data selesai: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//    }
}