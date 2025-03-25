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
}