package com.example.bismillahsipfo.data.repository

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
    private val _jadwalRutinList = MutableLiveData<List<JadwalRutin>>()
    val jadwalRutinList: LiveData<List<JadwalRutin>> get() = _jadwalRutinList

    private val supabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.BASE_URL,
        supabaseKey = BuildConfig.API_KEY
    ) {
        install(Postgrest)
    }

    fun loadJadwalRutin(fasilitasId: Int) {
        viewModelScope.launch {
            val result = repository.getJadwalRutinByFasilitasId(fasilitasId)
            _jadwalRutinList.postValue(result)
        }
    }

}