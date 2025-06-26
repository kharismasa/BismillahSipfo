package com.example.bismillahsipfo.ui.fragment.informasi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.TabelJadwalRutinAdapter
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.JadwalDipinjamViewModel
import com.example.bismillahsipfo.data.repository.JadwalRutinViewModel
import com.example.bismillahsipfo.databinding.ActivityHalamanInformasiBinding
import com.example.bismillahsipfo.ui.adapter.TabelJadwalPeminjamanAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HalamanInformasiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHalamanInformasiBinding
    private lateinit var fasilitasRepository: FasilitasRepository
    private lateinit var jadwalRutinAdapter: TabelJadwalRutinAdapter
    private val jadwalRutinViewModel: JadwalRutinViewModel by viewModels()
    private lateinit var peminjamanAdapter: TabelJadwalPeminjamanAdapter
    private val jadwalDipinjamViewModel: JadwalDipinjamViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHalamanInformasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fasilitasRepository = FasilitasRepository()

        val fasilitasId = intent.getIntExtra("FASILITAS_ID", -1)
        if (fasilitasId != -1) {
            // Load dan tampilkan informasi fasilitas berdasarkan fasilitasId
            loadFasilitasInfo(fasilitasId)
            loadJadwalRutin(fasilitasId)
            loadJadwalPeminjaman(fasilitasId)
        } else {
            // Handle error: ID fasilitas tidak ditemukan
            finish()
        }

        setupClickListeners()
        setupJadwalRutinRecyclerView()
        observeJadwalRutin()
        setupJadwalPeminjamanRecyclerView()
        observeJadwalPeminjaman()
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

    private fun displayFasilitasInfo(fasilitas: Fasilitas) {
        with(binding) {
            // Set nama fasilitas
            tvNamaFasilitas.text = fasilitas.namaFasilitas

            // Load image dengan error handling yang lebih baik
            Glide.with(this@HalamanInformasiActivity)
                .load(fasilitas.photo)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .centerCrop()
                .into(ivFasilitas)

            // Set content
            tvDeskripsi.text = fasilitas.deskripsi
            tvIsiFasilitasTambahan.text = fasilitas.fasilitasPlus
            tvIsiProsedur.text = fasilitas.prosedurPeminjaman
            tvIsiTataTertib.text = fasilitas.tatatertib
            tvIsiTarif.text = fasilitas.ketentuanTarif
            tvIsiAlamat.text = fasilitas.alamat
            tvIsiKontakInfo.text = fasilitas.kontak

            // Handle address click
            tvIsiAlamat.setOnClickListener {
                openMapsLocation(fasilitas.maps)
            }
        }
    }

    private fun openMapsLocation(mapsUrl: String) {
        try {
            // Coba buka dengan URI Google Maps
            val gmmIntentUri = Uri.parse(mapsUrl)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            // Jika Google Maps tidak tersedia, buka dengan browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
            startActivity(browserIntent)
        } catch (e: Exception) {
            // Tangani kesalahan lainnya
            showErrorMessage("Tidak dapat membuka peta: ${e.message}")
        }
    }

    private fun setupJadwalRutinRecyclerView() {
        jadwalRutinAdapter = TabelJadwalRutinAdapter(emptyList())
        binding.recyclerViewJadwalRutin.apply {
            layoutManager = LinearLayoutManager(this@HalamanInformasiActivity)
            adapter = jadwalRutinAdapter
            // Disable nested scrolling for smoother scrolling
            isNestedScrollingEnabled = false
        }
    }

    private fun observeJadwalRutin() {
        jadwalRutinViewModel.jadwalRutinList.observe(this) { jadwalRutinList ->
            Log.d("HalamanInformasiActivity", "Jadwal rutin observed: ${jadwalRutinList.size}")
            if (jadwalRutinList.isEmpty()) {
                binding.tvTextKosong.visibility = View.VISIBLE
                binding.recyclerViewJadwalRutin.visibility = View.GONE
            } else {
                binding.tvTextKosong.visibility = View.GONE
                binding.recyclerViewJadwalRutin.visibility = View.VISIBLE
                jadwalRutinAdapter.updateData(jadwalRutinList)
            }
        }
    }

    private fun loadJadwalRutin(fasilitasId: Int) {
        Log.d("HalamanInformasiActivity", "Loading jadwal rutin for fasilitasId: $fasilitasId")
        jadwalRutinViewModel.loadJadwalRutin(fasilitasId)
    }

    private fun setupJadwalPeminjamanRecyclerView() {
        peminjamanAdapter = TabelJadwalPeminjamanAdapter(emptyList())
        binding.recyclerViewJadwalPeminjaman.apply {
            layoutManager = LinearLayoutManager(this@HalamanInformasiActivity)
            adapter = peminjamanAdapter
            // Disable nested scrolling for smoother scrolling
            isNestedScrollingEnabled = false
        }
    }

    private fun observeJadwalPeminjaman() {
        jadwalDipinjamViewModel.jadwalDipinjamList.observe(this) { jadwalPeminjamanList ->
            if (jadwalPeminjamanList.isEmpty()) {
                binding.tvTextKosongPeminjaman.visibility = View.VISIBLE
                binding.recyclerViewJadwalPeminjaman.visibility = View.GONE
            } else {
                binding.tvTextKosongPeminjaman.visibility = View.GONE
                binding.recyclerViewJadwalPeminjaman.visibility = View.VISIBLE
                peminjamanAdapter.updateData(jadwalPeminjamanList)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun loadJadwalPeminjaman(fasilitasId: Int) {
        jadwalDipinjamViewModel.loadJadwalDipinjam(fasilitasId)
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        // Remove the old click listener for tvNamaFasilitas since it's now in toolbar
    }
}