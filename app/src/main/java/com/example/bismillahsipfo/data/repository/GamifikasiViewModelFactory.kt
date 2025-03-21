package com.example.bismillahsipfo.data.repository

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GamifikasiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamifikasiViewModel::class.java)) {
            return GamifikasiViewModel(GamifikasiRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}