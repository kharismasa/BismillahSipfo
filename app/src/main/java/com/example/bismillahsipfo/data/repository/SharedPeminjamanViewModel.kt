// SharedPeminjamanViewModel.kt - ViewModel yang dibagi antar fragment
package com.example.bismillahsipfo.data.repository

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.PenggunaKhusus

class SharedPeminjamanViewModel : ViewModel() {

    // Data yang dibagi antar fragment
    private val _peminjamanData = MutableLiveData<PeminjamanData?>()
    val peminjamanData: LiveData<PeminjamanData> = _peminjamanData as LiveData<PeminjamanData>

    fun updatePeminjamanData(data: PeminjamanData) {
        _peminjamanData.value = data
    }

    fun getCurrentData(): PeminjamanData? {
        return _peminjamanData.value
    }

    fun clearData() {
        _peminjamanData.value = null
    }
}

// Data class untuk menyimpan semua data peminjaman
data class PeminjamanData(
    val idFasilitas: Int = -1,
    val namaFasilitas: String? = null,
    val opsiPeminjaman: String? = null,
    val namaAcara: String? = null,
    val namaOrganisasi: String? = null,
    val idOrganisasi: Int = -1,
    val jadwalTersedia: JadwalTersedia? = null,
    val listLapangan: List<Int>? = null,
    val penggunaKhusus: String? = null,
    val tanggalMulai: String? = null,
    val tanggalSelesai: String? = null,
    val jamMulai: String? = null,
    val jamSelesai: String? = null,
    val lapanganDipinjam: List<Int>? = null,
    val pdfUri: String? = null
)