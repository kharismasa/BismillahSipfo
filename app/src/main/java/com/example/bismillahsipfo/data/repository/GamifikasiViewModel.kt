package com.example.bismillahsipfo.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.data.model.Gamifikasi
import com.example.bismillahsipfo.data.model.User
import com.example.bismillahsipfo.data.model.Voucher
import kotlinx.coroutines.launch

class GamifikasiViewModel(private val repository: GamifikasiRepository) : ViewModel() {

    private val _gamifikasiData = MutableLiveData<GamifikasiUiState>()
    val gamifikasiData: LiveData<GamifikasiUiState> = _gamifikasiData

    fun loadGamifikasiData() {
        viewModelScope.launch {
            _gamifikasiData.value = GamifikasiUiState.Loading
            try {
                val user = repository.getCurrentUser()
                if (user != null) {
                    val gamifikasi = repository.getGamifikasiForUser(user)
                    if (gamifikasi != null) {
                        val totalPembayaran = repository.getTotalPembayaranForUser(user)
                        val nextLevelGamifikasi = repository.getNextLevelGamifikasi(gamifikasi)
                        val vouchers = repository.getAllVouchers()
    
                        Log.d("GamifikasiViewModel", "User: $user")
                        Log.d("GamifikasiViewModel", "Gamifikasi: $gamifikasi")
                        Log.d("GamifikasiViewModel", "Total Pembayaran: $totalPembayaran")
                        Log.d("GamifikasiViewModel", "Next Level Gamifikasi: $nextLevelGamifikasi")
                        Log.d("GamifikasiViewModel", "Vouchers: $vouchers")
    
                        _gamifikasiData.value = GamifikasiUiState.Success(
                            user = user,
                            gamifikasi = gamifikasi,
                            totalPembayaran = totalPembayaran,
                            nextLevelGamifikasi = nextLevelGamifikasi ?: gamifikasi,
                            vouchers = vouchers
                        )
                    } else {
                        _gamifikasiData.value = GamifikasiUiState.Error("Gamifikasi data not found")
                    }
                } else {
                    _gamifikasiData.value = GamifikasiUiState.Error("User not found")
                }
            } catch (e: Exception) {
                Log.e("GamifikasiViewModel", "Error loading gamifikasi data: ${e.message}")
                _gamifikasiData.value = GamifikasiUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}

sealed class GamifikasiUiState {
    object Loading : GamifikasiUiState()
    data class Success(
        val user: User,
        val gamifikasi: Gamifikasi,
        val totalPembayaran: Double,
        val nextLevelGamifikasi: Gamifikasi,
        val vouchers: List<Voucher>
    ) : GamifikasiUiState()
    data class Error(val message: String) : GamifikasiUiState()
}