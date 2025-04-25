package com.example.bismillahsipfo.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.BuildConfig
import com.example.bismillahsipfo.data.model.JadwalRutin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class JadwalRutinViewModel : ViewModel() {
    private val repository = FasilitasRepository()
    private val _jadwalRutinList = MutableLiveData<List<JadwalRutinWithOrganisasi>>()
    val jadwalRutinList: LiveData<List<JadwalRutinWithOrganisasi>> get() = _jadwalRutinList

    fun loadJadwalRutin(fasilitasId: Int) {
        viewModelScope.launch {
            val result = repository.getJadwalRutinByFasilitasId(fasilitasId)
            Log.d("JadwalRutinViewModel", "Jadwal rutin loaded: ${result.size}")
            _jadwalRutinList.postValue(result)
        }
    }
}