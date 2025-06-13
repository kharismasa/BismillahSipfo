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
        _peminjamanData.value = data
    }

    fun getCurrentData(): PeminjamanData? {
        return _peminjamanData.value
    }

    fun clearData() {
        Log.d(TAG, "Clearing peminjaman data")
        _peminjamanData.value = null
    }

    // Method untuk update specific fields tanpa mengubah seluruh data
    fun updatePdfUri(pdfUri: String?) {
        _peminjamanData.value?.let { currentData ->
            val updatedData = currentData.copy(pdfUri = pdfUri)
            Log.d(TAG, "Updating PDF URI: $pdfUri")
            _peminjamanData.value = updatedData
        }
    }

    fun updateUploadedFileUrl(uploadedFileUrl: String?) {
        _peminjamanData.value?.let { currentData ->
            val updatedData = currentData.copy(uploadedFileUrl = uploadedFileUrl)
            Log.d(TAG, "Updating uploaded file URL: $uploadedFileUrl")
            _peminjamanData.value = updatedData
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}