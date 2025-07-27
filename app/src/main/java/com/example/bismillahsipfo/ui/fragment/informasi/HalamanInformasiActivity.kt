package com.example.bismillahsipfo.ui.fragment.informasi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.TabelJadwalRutinAdapter
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.JadwalDipinjamViewModel
import com.example.bismillahsipfo.data.repository.JadwalRutinViewModel
import com.example.bismillahsipfo.databinding.ActivityHalamanInformasiBinding
import com.example.bismillahsipfo.ui.adapter.TabelJadwalPeminjamanAdapter
import com.example.bismillahsipfo.ui.fragment.peminjaman.PeminjamanActivity
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

    // Variables to track expand/collapse state
    private var isJadwalPeminjamanExpanded = true // Default expanded
    private var isJadwalRutinExpanded = false
    private var isDeskripsiExpanded = false
    private var isAturanExpanded = false
    private var isKontakExpanded = false

    private var currentFasilitasId = -1

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHalamanInformasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fasilitasRepository = FasilitasRepository()

        val fasilitasId = intent.getIntExtra("FASILITAS_ID", -1)
        currentFasilitasId = fasilitasId

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
        setupExpandableCards()
        setupJadwalRutinRecyclerView()
        observeJadwalRutin()
        setupJadwalPeminjamanRecyclerView()
        observeJadwalPeminjaman()
    }

    private fun setupExpandableCards() {
        // Set initial states based on default expansion
        updateCardState(binding.contentJadwalPeminjaman, binding.iconJadwalPeminjaman, isJadwalPeminjamanExpanded)
        updateCardState(binding.contentJadwalRutin, binding.iconJadwalRutin, isJadwalRutinExpanded)
        updateCardState(binding.contentDeskripsi, binding.iconDeskripsi, isDeskripsiExpanded)
        updateCardState(binding.contentAturan, binding.iconAturan, isAturanExpanded)
        updateCardState(binding.contentKontak, binding.iconKontak, isKontakExpanded)

        // Setup click listeners for expandable headers
        binding.headerJadwalPeminjaman.setOnClickListener {
            isJadwalPeminjamanExpanded = !isJadwalPeminjamanExpanded
            toggleCard(binding.contentJadwalPeminjaman, binding.iconJadwalPeminjaman, isJadwalPeminjamanExpanded)
        }

        binding.headerJadwalRutin.setOnClickListener {
            isJadwalRutinExpanded = !isJadwalRutinExpanded
            toggleCard(binding.contentJadwalRutin, binding.iconJadwalRutin, isJadwalRutinExpanded)
        }

        binding.headerDeskripsi.setOnClickListener {
            isDeskripsiExpanded = !isDeskripsiExpanded
            toggleCard(binding.contentDeskripsi, binding.iconDeskripsi, isDeskripsiExpanded)
        }

        binding.headerAturan.setOnClickListener {
            isAturanExpanded = !isAturanExpanded
            toggleCard(binding.contentAturan, binding.iconAturan, isAturanExpanded)
        }

        binding.headerKontak.setOnClickListener {
            isKontakExpanded = !isKontakExpanded
            toggleCard(binding.contentKontak, binding.iconKontak, isKontakExpanded)
        }
    }

    private fun toggleCard(content: LinearLayout, icon: ImageView, isExpanded: Boolean) {
        if (isExpanded) {
            // Expand
            content.visibility = View.VISIBLE
            rotateIcon(icon, 0f, 180f)
        } else {
            // Collapse
            content.visibility = View.GONE
            rotateIcon(icon, 180f, 0f)
        }
    }

    private fun updateCardState(content: LinearLayout, icon: ImageView, isExpanded: Boolean) {
        if (isExpanded) {
            content.visibility = View.VISIBLE
            icon.rotation = 180f
        } else {
            content.visibility = View.GONE
            icon.rotation = 0f
        }
    }

    private fun rotateIcon(icon: ImageView, fromDegrees: Float, toDegrees: Float) {
        val rotate = RotateAnimation(
            fromDegrees, toDegrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 200
        rotate.fillAfter = true
        icon.startAnimation(rotate)
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

        // Setup booking button click listener
        binding.btnBookingSekarang.setOnClickListener {
            navigateToPeminjamanActivity()
        }
    }

    private fun navigateToPeminjamanActivity() {
        try {
            val intent = Intent(this, PeminjamanActivity::class.java)
            // Pass fasilitas ID if needed for pre-selection in booking form
            if (currentFasilitasId != -1) {
                intent.putExtra("FASILITAS_ID", currentFasilitasId)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("HalamanInformasiActivity", "Error navigating to PeminjamanActivity: ${e.message}")
            showErrorMessage("Tidak dapat membuka halaman peminjaman")
        }
    }
}