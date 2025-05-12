package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.JadwalTersedia

class PembayaranFragment : Fragment() {

    // All data collected from previous fragments
    private var idFasilitas: Int = -1
    private var namaFasilitas: String? = null
    private var opsiPeminjaman: String? = null
    private var namaAcara: String? = null
    private var namaOrganisasi: String? = null
    private var idOrganisasi: Int = -1
    private var jadwalTersedia: JadwalTersedia? = null
    private var listLapangan: List<Int>? = null
    private var penggunaKhusus: String? = null
    private var tanggalMulai: String? = null
    private var tanggalSelesai: String? = null
    private var jamMulai: String? = null
    private var jamSelesai: String? = null
    private var lapanganDipinjam: List<Int>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pembayaran, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve all data from previous fragments
        retrieveAllData()

        // Initialize your views
        setupViews(view)

        // Process payment or display summary
        processPayment()
    }

    private fun retrieveAllData() {
        arguments?.let { bundle ->
            // Basic data that's always present
            idFasilitas = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_FASILITAS, -1)
            namaFasilitas = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_FASILITAS)
            opsiPeminjaman = bundle.getString(FormPeminjamanFragment.EXTRA_OPSI_PEMINJAMAN)
            namaAcara = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ACARA)

            // Retrieve data based on opsiPeminjaman
            when (opsiPeminjaman) {
                "Sesuai Jadwal Rutin" -> {
                    idOrganisasi = bundle.getInt(FormPeminjamanFragment.EXTRA_ID_ORGANISASI, -1)
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)
                    jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                    listLapangan = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LIST_LAPANGAN)

                    if (idFasilitas == 30) {
                        penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                    }
                }

                "Diluar Jadwal Rutin" -> {
                    namaOrganisasi = bundle.getString(FormPeminjamanFragment.EXTRA_NAMA_ORGANISASI)

                    if (idFasilitas == 30) {
                        jadwalTersedia = bundle.getSerializable(FormPeminjamanFragment.EXTRA_JADWAL_TERSEDIA) as? JadwalTersedia
                        penggunaKhusus = bundle.getString(FormPeminjamanFragment.EXTRA_PENGGUNA_KHUSUS)
                    } else {
                        tanggalMulai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_MULAI)
                        tanggalSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_TANGGAL_SELESAI)
                        jamMulai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_MULAI)
                        jamSelesai = bundle.getString(FormPeminjamanFragment.EXTRA_JAM_SELESAI)
                        lapanganDipinjam = bundle.getIntegerArrayList(FormPeminjamanFragment.EXTRA_LAPANGAN_DIPINJAM)
                    }
                }
            }

            // Log all retrieved data for debugging
            logAllData()
        }
    }

    private fun logAllData() {
        Log.d("PembayaranFragment", """
            Retrieved Data Summary:
            - ID Fasilitas: $idFasilitas
            - Nama Fasilitas: $namaFasilitas
            - Opsi Peminjaman: $opsiPeminjaman
            - Nama Acara: $namaAcara
            - ID Organisasi: $idOrganisasi
            - Nama Organisasi: $namaOrganisasi
            - Jadwal Tersedia: $jadwalTersedia
            - List Lapangan: $listLapangan
            - Pengguna Khusus: $penggunaKhusus
            - Tanggal Mulai: $tanggalMulai
            - Tanggal Selesai: $tanggalSelesai
            - Jam Mulai: $jamMulai
            - Jam Selesai: $jamSelesai
            - Lapangan Dipinjam: $lapanganDipinjam
        """.trimIndent())
    }

    private fun setupViews(view: View) {
        // Initialize your views here
        // Setup payment UI components
    }

    private fun processPayment() {
        // Create peminjaman object and process payment
        // You can now use all the retrieved data to create the final transaction

        // Example: Create PeminjamanFasilitas object
        // val peminjaman = createPeminjamanObject()
        // viewModel.submitPeminjaman(peminjaman)
    }
}