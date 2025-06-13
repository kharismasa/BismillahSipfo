package com.example.bismillahsipfo.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log

class SharedPeminjamanViewModel : ViewModel() {

    companion object {
        private const val TAG = "SharedPeminjamanViewModel"
    }

    // Data yang dibagi antar fragment
    private val _peminjamanData = MutableLiveData<PeminjamanData?>()
    val peminjamanData: LiveData<PeminjamanData?> = _peminjamanData

    fun updatePeminjamanData(data: PeminjamanData) {
        Log.d(TAG, "Updating peminjaman data: $data")
        _peminjamanData.postValue(data) // GANTI setValue dengan postValue - INI YANG PENTING!
    }

    fun getCurrentData(): PeminjamanData? {
        return _peminjamanData.value
    }

    fun clearData() {
        Log.d(TAG, "Clearing peminjaman data")
        _peminjamanData.postValue(null) // GANTI setValue dengan postValue
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}