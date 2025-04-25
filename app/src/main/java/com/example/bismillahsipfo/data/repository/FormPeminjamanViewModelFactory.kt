package com.example.bismillahsipfo.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FormPeminjamanViewModelFactory(
    private val fasilitasRepository: FasilitasRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FormPeminjamanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FormPeminjamanViewModel(fasilitasRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}