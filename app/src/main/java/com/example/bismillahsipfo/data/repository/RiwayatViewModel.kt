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
import com.example.bismillahsipfo.data.model.StatusPembayaran
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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadPendingAndFailedRiwayat() {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                val currentUserId = userRepository.getCurrentUserId()

                // Ambil pembayaran pending dan failed
                val pendingAndFailedPembayaranList = fasilitasRepository.getPendingAndFailedPembayaran(currentUserId)

                val riwayatPendingList = pendingAndFailedPembayaranList.mapNotNull { pembayaran ->
                    val peminjaman = fasilitasRepository.getPeminjamanByIdPembayaran(pembayaran.idPembayaran)
                    peminjaman?.let {
                        val fasilitas = fasilitasRepository.getFasilitasById(it.idFasilitas)
                        fasilitas?.let { f ->
                            RiwayatPending(
                                tanggalMulai = it.tanggalMulai,
                                tanggalSelesai = it.tanggalSelesai,
                                namaFasilitas = f.namaFasilitas,
                                totalBiaya = pembayaran.totalBiaya,
                                waktuKadaluwarsa = pembayaran.waktuKadaluwarsa,
                                statusPembayaran = pembayaran.statusPembayaran,
                                idPembayaran = pembayaran.idPembayaran,
                                midtransToken = pembayaran.midtransToken,
                                midtransRedirectUrl = pembayaran.midtransRedirectUrl
                            )
                        }
                    }
                }

                _pendingRiwayat.postValue(riwayatPendingList)
                _isLoading.postValue(false)

            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Error loading pending/failed riwayat: ${e.message}")
                _errorMessage.postValue("Gagal memuat data riwayat")
                _isLoading.postValue(false)
            }
        }
    }

    // Fungsi untuk regenerate token midtrans
    fun regenerateMidtransToken(paymentId: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = fasilitasRepository.regenerateMidtransToken(paymentId)
                callback(result.first, result.second)
            } catch (e: Exception) {
                Log.e("RiwayatViewModel", "Error regenerating token: ${e.message}")
                callback(false, null)
            }
        }
    }

    // Untuk backward compatibility
    fun loadPendingRiwayat() {
        loadPendingAndFailedRiwayat()
    }
}