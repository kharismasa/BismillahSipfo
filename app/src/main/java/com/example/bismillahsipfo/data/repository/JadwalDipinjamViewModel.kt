package com.example.bismillahsipfo.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import kotlinx.coroutines.launch

class JadwalDipinjamViewModel : ViewModel() {

    private val repository = FasilitasRepository()
    private val _jadwalDipinjamList = MutableLiveData<List<JadwalPeminjamanItem>>()
    val jadwalDipinjamList: LiveData<List<JadwalPeminjamanItem>> get() = _jadwalDipinjamList

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun loadJadwalDipinjam (fasilitasId: Int) {
        viewModelScope.launch {
            val result = repository.getJadwalPeminjaman(fasilitasId)
            _jadwalDipinjamList.postValue(result)
        }
    }

}