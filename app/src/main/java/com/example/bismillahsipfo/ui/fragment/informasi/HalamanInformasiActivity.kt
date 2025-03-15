package com.example.bismillahsipfo.ui.fragment.informasi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.databinding.ActivityHalamanInformasiBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HalamanInformasiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHalamanInformasiBinding
    private lateinit var fasilitasRepository: FasilitasRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHalamanInformasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fasilitasRepository = FasilitasRepository()

        val fasilitasId = intent.getIntExtra("FASILITAS_ID", -1)
        if (fasilitasId != -1) {
            // Load dan tampilkan informasi fasilitas berdasarkan fasilitasId
            loadFasilitasInfo(fasilitasId)
        } else {
            // Handle error: ID fasilitas tidak ditemukan
            finish()
        }

        setupClickListeners()
    }

    private fun loadFasilitasInfo(fasilitasId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fasilitas = withContext(Dispatchers.IO) {
                    fasilitasRepository.getFasilitasById(fasilitasId)
                }
                if (fasilitas != null) {
                    displayFasilitasInfo(fasilitas)
                } else {
                    // Handle case when fasilitas is not found
                    showErrorMessage("Fasilitas tidak ditemukan")
                }
            } catch (e: Exception) {
                // Handle error
                showErrorMessage("Terjadi kesalahan saat memuat data")
            }
        }
    }

    private fun showErrorMessage(message: String) {
        // Implement this method to show error message to user
        // For example, you could use a Toast or update a TextView
    }

    private fun displayFasilitasInfo(fasilitas: Fasilitas) {
        with(binding) {
            tvNamaFasilitas.text = fasilitas.namaFasilitas
            Glide.with(this@HalamanInformasiActivity)
                .load(fasilitas.photo)
                .into(ivFasilitas)
            tvDeskripsi.text = fasilitas.deskripsi
            tvIsiFasilitasTambahan.text = fasilitas.fasilitasPlus
            tvIsiProsedur.text = fasilitas.prosedurPeminjaman
            tvIsiTataTertib.text = fasilitas.tatatertib
            tvIsiTarif.text = fasilitas.ketentuanTarif
            tvIsiAlamat.text = fasilitas.alamat
            tvIsiKontakInfo.text = fasilitas.kontak

            tvIsiAlamat.setOnClickListener {
                val gmmIntentUri = Uri.parse(fasilitas.maps)
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvNamaFasilitas.setOnClickListener { finish() }
    }
}