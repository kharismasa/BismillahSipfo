package com.example.bismillahsipfo.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import kotlinx.coroutines.launch

class JadwalPeminjamanViewModel : ViewModel() {

    private val repository = FasilitasRepository()
    private val _jadwalPeminjamanList = MutableLiveData<List<JadwalPeminjamanItem>>()
    val jadwalPeminjamanList: LiveData<List<JadwalPeminjamanItem>> get() = _jadwalPeminjamanList

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun loadJadwalPeminjaman(fasilitasId: Int) {
        viewModelScope.launch {
            val result = repository.getJadwalPeminjaman(fasilitasId)
            _jadwalPeminjamanList.postValue(result)
        }
    }

}