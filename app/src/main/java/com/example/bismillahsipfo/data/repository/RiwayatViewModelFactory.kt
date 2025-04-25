package com.example.bismillahsipfo.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RiwayatViewModelFactory(
    private val fasilitasRepository: FasilitasRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RiwayatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RiwayatViewModel(fasilitasRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}