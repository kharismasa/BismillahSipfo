package com.example.bismillahsipfo.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.Organisasi
import com.example.bismillahsipfo.data.model.PeminjamanFasilitas
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class FormPeminjamanViewModel(
    private val fasilitasRepository: FasilitasRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _fasilitasList = MutableLiveData<List<Fasilitas>>()
    val fasilitasList: LiveData<List<Fasilitas>> = _fasilitasList

    private val _lapanganList = MutableLiveData<List<Lapangan>>()
    val lapanganList: LiveData<List<Lapangan>> = _lapanganList

    private val _showPenggunaKhusus = MutableLiveData<Boolean>()
    val showPenggunaKhusus: LiveData<Boolean> = _showPenggunaKhusus

    private val _jadwalTersedia = MutableLiveData<List<JadwalTersedia>>()
    val jadwalTersedia: LiveData<List<JadwalTersedia>> = _jadwalTersedia

    private val _organisasiList = MutableLiveData<List<Organisasi>>()
    val organisasiList: LiveData<List<Organisasi>> = _organisasiList

    private val _jadwalAvailability = MutableLiveData<JadwalAvailabilityStatus>()
    val jadwalAvailability: LiveData<JadwalAvailabilityStatus> = _jadwalAvailability

    private val _jadwalTersediaFasilitas30 = MutableLiveData<List<JadwalTersedia>>()
    val jadwalTersediaFasilitas30: LiveData<List<JadwalTersedia>> = _jadwalTersediaFasilitas30

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var selectedOrganisasiId: Int? = null

    private var selectedFasilitas: Fasilitas? = null
    private var tanggalMulai: LocalDate? = null
    private var tanggalSelesai: LocalDate? = null
    private var jamMulai: LocalTime? = null
    private var jamSelesai: LocalTime? = null
    private val selectedLapangan = mutableSetOf<Lapangan>()

    init {
        loadFasilitas()
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkJadwalAvailability(tanggalMulai: String, tanggalSelesai: String, jamMulai: String, jamSelesai: String) {
        viewModelScope.launch {
            val idFasilitas = selectedFasilitas?.idFasilitas ?: return@launch
            try {
                setLoading(true)
                val status = fasilitasRepository.checkJadwalAvailability(
                    idFasilitas,
                    tanggalMulai,
                    tanggalSelesai,
                    jamMulai,
                    jamSelesai
                )
                _jadwalAvailability.postValue(status)
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error checking jadwal availability: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadFasilitas() {
        viewModelScope.launch {
            try {
                setLoading(true)
                _fasilitasList.value = fasilitasRepository.getFasilitas()
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error loading fasilitas: ${e.message}")
                _fasilitasList.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }

    fun onFasilitasSelected(fasilitas: Fasilitas) {
        Log.d("FormPeminjamanViewModel", "Fasilitas selected: ${fasilitas.namaFasilitas}")

        // Reset semua data saat ganti fasilitas
        selectedOrganisasiId = null
        _jadwalTersedia.value = emptyList()
        _jadwalTersediaFasilitas30.value = emptyList()

        if (fasilitas.idFasilitas == -1) {
            // Pilihan default, kosongkan lapangan
            _lapanganList.value = emptyList()
            _showPenggunaKhusus.value = false
            selectedFasilitas = null
            _organisasiList.value = emptyList()
            setLoading(false)
        } else {
            selectedFasilitas = fasilitas
            viewModelScope.launch {
                try {
                    setLoading(true)

                    // Load lapangan data
                    val lapanganData = fasilitasRepository.getLapanganByFasilitasId(fasilitas.idFasilitas)
                    _lapanganList.postValue(lapanganData)

                    // Load organisasi data
                    val organisasiList = fasilitasRepository.getOrganisasiListByFasilitasId(fasilitas.idFasilitas)
                    Log.d("FormPeminjamanViewModel", "Organisasi list fetched: $organisasiList")
                    _organisasiList.postValue(organisasiList)

                } catch (e: Exception) {
                    Log.e("FormPeminjamanViewModel", "Error loading fasilitas data: ${e.message}")
                    _lapanganList.postValue(emptyList())
                    _organisasiList.postValue(emptyList())
                } finally {
                    setLoading(false)
                }
            }
            _showPenggunaKhusus.value = fasilitas.idFasilitas == 30 // UTG
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onOrganisasiSelected(organisasi: Organisasi) {
        Log.d("FormPeminjamanViewModel", "Organisasi selected: ${organisasi.namaOrganisasi}, ID: ${organisasi.idOrganisasi}")
        selectedOrganisasiId = organisasi.idOrganisasi
        viewModelScope.launch {
            loadJadwalTersedia()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadJadwalTersedia() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val jadwalList = selectedFasilitas?.idFasilitas?.let { idFasilitas ->
                    selectedOrganisasiId?.let { idOrganisasi ->
                        fasilitasRepository.getJadwalTersedia(idFasilitas, idOrganisasi)
                    } ?: emptyList()
                } ?: emptyList()

                _jadwalTersedia.postValue(jadwalList)
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error loading jadwal tersedia: ${e.message}")
                _jadwalTersedia.postValue(emptyList())
            } finally {
                setLoading(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadJadwalTersediaForFasilitas30() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val jadwalList = fasilitasRepository.getJadwalTersediaForFasilitas30()
                _jadwalTersediaFasilitas30.postValue(jadwalList)
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error loading jadwal tersedia for fasilitas 30: ${e.message}")
                _jadwalTersediaFasilitas30.postValue(emptyList())
            } finally {
                setLoading(false)
            }
        }
    }

    fun onJadwalTersediaSelected(jadwal: JadwalTersedia) {
        tanggalMulai = jadwal.tanggal
        tanggalSelesai = jadwal.tanggal
        jamMulai = jadwal.waktuMulai
        jamSelesai = jadwal.waktuSelesai

        // Update lapangan list dengan data dari jadwal tersedia
        viewModelScope.launch {
            try {
                setLoading(true)
                val currentLapangan = _lapanganList.value ?: emptyList()
                val selectedLapanganFromJadwal = jadwal.listLapangan.mapNotNull { id ->
                    currentLapangan.find { it.idLapangan == id }
                }
                _lapanganList.postValue(selectedLapanganFromJadwal)
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error updating lapangan from jadwal: ${e.message}")
            } finally {
                setLoading(false)
            }
        }

        // Tambahkan logging untuk tipe_jadwal dan urutan_slot
        Log.d("FormPeminjamanViewModel", "Jadwal tersedia selected: Tipe: ${jadwal.tipeJadwal}, Urutan: ${jadwal.urutanSlot}")
    }

    fun setTanggalMulai(date: LocalDate) {
        tanggalMulai = date
    }

    fun setTanggalSelesai(date: LocalDate) {
        tanggalSelesai = date
    }

    fun setJamMulai(time: LocalTime) {
        jamMulai = time
    }

    fun setJamSelesai(time: LocalTime) {
        jamSelesai = time
    }

    fun onLapanganChecked(lapangan: Lapangan, isChecked: Boolean) {
        if (isChecked) {
            selectedLapangan.add(lapangan)
        } else {
            selectedLapangan.remove(lapangan)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun submitForm(namaAcara: String, namaOrganisasi: String, penggunaKhusus: PenggunaKhusus?, suratPeminjamanUrl: String? = null) {
        viewModelScope.launch {
            try {
                setLoading(true)
                if (validateForm(namaAcara, namaOrganisasi)) {
                    val idPengguna = userRepository.getCurrentUserId()
                    val peminjamanFasilitas = PeminjamanFasilitas(
                        idPeminjaman = 0, // ID akan di-generate oleh database
                        idFasilitas = selectedFasilitas?.idFasilitas ?: 0,
                        tanggalMulai = tanggalMulai!!,
                        tanggalSelesai = tanggalSelesai!!,
                        jamMulai = jamMulai!!,
                        jamSelesai = jamSelesai!!,
                        namaOrganisasi = namaOrganisasi,
                        namaAcara = namaAcara,
                        idPembayaran = "", // ID pembayaran akan di-generate nanti
                        penggunaKhusus = penggunaKhusus,
                        idPengguna = idPengguna,
                        createdAtPeminjaman = java.time.Instant.now(),
                        // TAMBAHAN BARU: URL surat peminjaman
                        suratPeminjamanUrl = suratPeminjamanUrl
                    )

                    val idPeminjaman = fasilitasRepository.insertPeminjamanFasilitas(peminjamanFasilitas)

                    // Insert lapangan yang dipinjam
                    selectedLapangan.forEach { lapangan ->
                        fasilitasRepository.insertLapanganDipinjam(idPeminjaman, lapangan.idLapangan)
                    }

                    // Navigasi ke halaman berikutnya atau tampilkan pesan sukses
                    // Anda bisa menggunakan LiveData untuk ini
                }
            } catch (e: Exception) {
                Log.e("FormPeminjamanViewModel", "Error submitting form: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun validateForm(namaAcara: String, namaOrganisasi: String): Boolean {
        if (selectedFasilitas == null) return false
        if (tanggalMulai == null || tanggalSelesai == null) return false
        if (jamMulai == null || jamSelesai == null) return false
        if (namaAcara.length > 50) return false
        if (namaOrganisasi.length > 30) return false
        if (tanggalMulai!! > tanggalSelesai!!) return false // Tanggal mulai harus lebih kecil atau sama dengan tanggal selesai
        if (tanggalMulai == tanggalSelesai && jamMulai!! >= jamSelesai!!) return false // Jika tanggal sama, jam mulai harus lebih kecil dari jam selesai
        if (selectedLapangan.isEmpty()) return false
        return true
    }

}

sealed class JadwalAvailabilityStatus {
    object AVAILABLE : JadwalAvailabilityStatus()
    object UNAVAILABLE : JadwalAvailabilityStatus()
    data class HOLIDAY(val namaHariLibur: String, val tanggal: String) : JadwalAvailabilityStatus()
    object CONFLICT_WITH_JADWAL_RUTIN : JadwalAvailabilityStatus()
}