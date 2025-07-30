package com.example.bismillahsipfo.ui.fragment.informasi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.example.bismillahsipfo.data.model.JadwalPeminjamanItem
import com.example.bismillahsipfo.data.model.JadwalRutinWithOrganisasi
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.JadwalDipinjamViewModel
import com.example.bismillahsipfo.data.repository.JadwalRutinViewModel
import com.example.bismillahsipfo.databinding.ActivityHalamanInformasiBinding
import com.example.bismillahsipfo.ui.adapter.TabelJadwalPeminjamanAdapter
import com.example.bismillahsipfo.ui.fragment.peminjaman.PeminjamanActivity
import com.example.bismillahsipfo.utils.SearchFilterHelper
import com.google.android.material.textfield.TextInputEditText
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

    // Search and Filter UI Components for Peminjaman
    private lateinit var searchEditTextPeminjaman: TextInputEditText
    private lateinit var dayDropdownPeminjaman: AutoCompleteTextView
    private lateinit var monthDropdownPeminjaman: AutoCompleteTextView
    private lateinit var organizationDropdownPeminjaman: AutoCompleteTextView
    private lateinit var timeSlotDropdownPeminjaman: AutoCompleteTextView

    // Search and Filter UI Components for Rutin
    private lateinit var searchEditTextRutin: TextInputEditText
    private lateinit var dayDropdownRutin: AutoCompleteTextView
    private lateinit var organizationDropdownRutin: AutoCompleteTextView
    private lateinit var timeSlotDropdownRutin: AutoCompleteTextView

    // Filter state
    private var currentSearchQueryPeminjaman = ""
    private var currentSelectedDayPeminjaman = "Semua"
    private var currentSelectedMonthPeminjaman = "Semua"
    private var currentSelectedOrganizationPeminjaman = "Semua"
    private var currentSelectedTimeSlotPeminjaman = "Semua"

    private var currentSearchQueryRutin = ""
    private var currentSelectedDayRutin = "Semua"
    private var currentSelectedOrganizationRutin = "Semua"
    private var currentSelectedTimeSlotRutin = "Semua"

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
        setupSearchFilterPeminjaman()
        setupSearchFilterRutin()
    }

    private fun setupSearchFilterPeminjaman() {
        // Initialize UI components for Peminjaman
        searchEditTextPeminjaman = binding.layoutSearchFilterPeminjaman.editTextSearch
        dayDropdownPeminjaman = binding.layoutSearchFilterPeminjaman.autoCompleteDay
        monthDropdownPeminjaman = binding.layoutSearchFilterPeminjaman.autoCompleteMonth
        organizationDropdownPeminjaman = binding.layoutSearchFilterPeminjaman.autoCompleteOrganization
        timeSlotDropdownPeminjaman = binding.layoutSearchFilterPeminjaman.autoCompleteTimeSlot

        // Show month filter for peminjaman (hide for rutin)
        binding.layoutSearchFilterPeminjaman.textInputLayoutMonth.visibility = View.VISIBLE

        // Setup dropdown adapters
        setupDropdownAdapters(
            dayDropdownPeminjaman,
            monthDropdownPeminjaman,
            organizationDropdownPeminjaman,
            timeSlotDropdownPeminjaman,
            isPeminjaman = true
        )

        // Setup search text watcher
        searchEditTextPeminjaman.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQueryPeminjaman = s.toString()
                applyFilterPeminjaman()
            }
        })

        // Setup dropdown listeners for Peminjaman
        dayDropdownPeminjaman.setOnItemClickListener { _, _, position, _ ->
            currentSelectedDayPeminjaman = SearchFilterHelper.getDayOptions()[position]
            applyFilterPeminjaman()
        }

        organizationDropdownPeminjaman.setOnItemClickListener { _, _, position, _ ->
            val adapter = organizationDropdownPeminjaman.adapter as ArrayAdapter<String>
            currentSelectedOrganizationPeminjaman = adapter.getItem(position) ?: "Semua"
            applyFilterPeminjaman()
        }

        timeSlotDropdownPeminjaman.setOnItemClickListener { _, _, position, _ ->
            currentSelectedTimeSlotPeminjaman = SearchFilterHelper.getTimeSlotOptions()[position]
            applyFilterPeminjaman()
        }

        // Setup reset button
        binding.layoutSearchFilterPeminjaman.btnResetFilter.setOnClickListener {
            resetFilterPeminjaman()
        }
    }

    private fun setupSearchFilterRutin() {
        // Initialize UI components for Rutin
        searchEditTextRutin = binding.layoutSearchFilterRutin.editTextSearch
        dayDropdownRutin = binding.layoutSearchFilterRutin.autoCompleteDay
        organizationDropdownRutin = binding.layoutSearchFilterRutin.autoCompleteOrganization
        timeSlotDropdownRutin = binding.layoutSearchFilterRutin.autoCompleteTimeSlot

        // Hide month filter for rutin (only show for peminjaman)
        binding.layoutSearchFilterRutin.textInputLayoutMonth.visibility = View.GONE

        // Setup dropdown adapters
        setupDropdownAdapters(
            dayDropdownRutin,
            null, // no month dropdown for rutin
            organizationDropdownRutin,
            timeSlotDropdownRutin,
            isPeminjaman = false
        )

        // Setup search text watcher
        searchEditTextRutin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQueryRutin = s.toString()
                applyFilterRutin()
            }
        })

        // Setup dropdown listeners for Rutin
        dayDropdownRutin.setOnItemClickListener { _, _, position, _ ->
            currentSelectedDayRutin = SearchFilterHelper.getDayOptions()[position]
            applyFilterRutin()
        }

        organizationDropdownRutin.setOnItemClickListener { _, _, position, _ ->
            val adapter = organizationDropdownRutin.adapter as ArrayAdapter<String>
            currentSelectedOrganizationRutin = adapter.getItem(position) ?: "Semua"
            applyFilterRutin()
        }

        timeSlotDropdownRutin.setOnItemClickListener { _, _, position, _ ->
            currentSelectedTimeSlotRutin = SearchFilterHelper.getTimeSlotOptions()[position]
            applyFilterRutin()
        }

        // Setup reset button
        binding.layoutSearchFilterRutin.btnResetFilter.setOnClickListener {
            resetFilterRutin()
        }
    }

    private fun setupDropdownAdapters(
        dayDropdown: AutoCompleteTextView,
        monthDropdown: AutoCompleteTextView?,
        organizationDropdown: AutoCompleteTextView,
        timeSlotDropdown: AutoCompleteTextView,
        isPeminjaman: Boolean
    ) {
        // Day adapter
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SearchFilterHelper.getDayOptions())
        dayDropdown.setAdapter(dayAdapter)
        dayDropdown.setText("Semua", false)

        // Month adapter (only for peminjaman)
        monthDropdown?.let {
            val monthOptions = listOf("Semua") // Will be updated when data loads
            val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, monthOptions)
            it.setAdapter(monthAdapter)
            it.setText("Semua", false)

            it.setOnItemClickListener { _, _, position, _ ->
                val adapter = it.adapter as ArrayAdapter<String>
                currentSelectedMonthPeminjaman = adapter.getItem(position) ?: "Semua"
                applyFilterPeminjaman()
            }
        }

        // Organization adapter
        val organizationOptions = listOf("Semua") // Will be updated when data loads
        val organizationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, organizationOptions)
        organizationDropdown.setAdapter(organizationAdapter)
        organizationDropdown.setText("Semua", false)

        // Time slot adapter
        val timeSlotAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SearchFilterHelper.getTimeSlotOptions())
        timeSlotDropdown.setAdapter(timeSlotAdapter)
        timeSlotDropdown.setText("Semua", false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDropdownOptions(
        jadwalPeminjamanList: List<JadwalPeminjamanItem>?,
        jadwalRutinList: List<JadwalRutinWithOrganisasi>?
    ) {
        // Update organization options for peminjaman
        jadwalPeminjamanList?.let { list ->
            val organizations = listOf("Semua") + SearchFilterHelper.getUniqueOrganizations(list)
            val organizationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, organizations)
            organizationDropdownPeminjaman.setAdapter(organizationAdapter)

            // Update month options for peminjaman
            val months = listOf("Semua") + SearchFilterHelper.getUniqueMonths(list)
            val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
            monthDropdownPeminjaman.setAdapter(monthAdapter)
        }

        // Update organization options for rutin
        jadwalRutinList?.let { list ->
            val organizations = listOf("Semua") + SearchFilterHelper.getUniqueOrganizationsRutin(list)
            val organizationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, organizations)
            organizationDropdownRutin.setAdapter(organizationAdapter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilterPeminjaman() {
        val originalData = peminjamanAdapter.getOriginalData()
        val filteredData = SearchFilterHelper.filterJadwalPeminjaman(
            originalData,
            currentSearchQueryPeminjaman,
            currentSelectedDayPeminjaman,
            currentSelectedMonthPeminjaman,
            currentSelectedOrganizationPeminjaman,
            currentSelectedTimeSlotPeminjaman
        )

        peminjamanAdapter.updateFilteredData(filteredData)

        // Update empty state
        if (filteredData.isEmpty()) {
            binding.tvTextKosongPeminjaman.visibility = View.VISIBLE
            binding.recyclerViewJadwalPeminjaman.visibility = View.GONE
        } else {
            binding.tvTextKosongPeminjaman.visibility = View.GONE
            binding.recyclerViewJadwalPeminjaman.visibility = View.VISIBLE
        }
    }

    private fun applyFilterRutin() {
        val originalData = jadwalRutinAdapter.getOriginalData()
        val filteredData = SearchFilterHelper.filterJadwalRutin(
            originalData,
            currentSearchQueryRutin,
            currentSelectedDayRutin,
            currentSelectedOrganizationRutin,
            currentSelectedTimeSlotRutin
        )

        jadwalRutinAdapter.updateFilteredData(filteredData)

        // Update empty state
        if (filteredData.isEmpty()) {
            binding.tvTextKosong.visibility = View.VISIBLE
            binding.recyclerViewJadwalRutin.visibility = View.GONE
        } else {
            binding.tvTextKosong.visibility = View.GONE
            binding.recyclerViewJadwalRutin.visibility = View.VISIBLE
        }
    }

    private fun resetFilterPeminjaman() {
        currentSearchQueryPeminjaman = ""
        currentSelectedDayPeminjaman = "Semua"
        currentSelectedMonthPeminjaman = "Semua"
        currentSelectedOrganizationPeminjaman = "Semua"
        currentSelectedTimeSlotPeminjaman = "Semua"

        searchEditTextPeminjaman.setText("")
        dayDropdownPeminjaman.setText("Semua", false)
        monthDropdownPeminjaman.setText("Semua", false)
        organizationDropdownPeminjaman.setText("Semua", false)
        timeSlotDropdownPeminjaman.setText("Semua", false)

        peminjamanAdapter.resetFilter()

        // Update empty state
        val originalData = peminjamanAdapter.getOriginalData()
        if (originalData.isEmpty()) {
            binding.tvTextKosongPeminjaman.visibility = View.VISIBLE
            binding.recyclerViewJadwalPeminjaman.visibility = View.GONE
        } else {
            binding.tvTextKosongPeminjaman.visibility = View.GONE
            binding.recyclerViewJadwalPeminjaman.visibility = View.VISIBLE
        }
    }

    private fun resetFilterRutin() {
        currentSearchQueryRutin = ""
        currentSelectedDayRutin = "Semua"
        currentSelectedOrganizationRutin = "Semua"
        currentSelectedTimeSlotRutin = "Semua"

        searchEditTextRutin.setText("")
        dayDropdownRutin.setText("Semua", false)
        organizationDropdownRutin.setText("Semua", false)
        timeSlotDropdownRutin.setText("Semua", false)

        jadwalRutinAdapter.resetFilter()

        // Update empty state
        val originalData = jadwalRutinAdapter.getOriginalData()
        if (originalData.isEmpty()) {
            binding.tvTextKosong.visibility = View.VISIBLE
            binding.recyclerViewJadwalRutin.visibility = View.GONE
        } else {
            binding.tvTextKosong.visibility = View.GONE
            binding.recyclerViewJadwalRutin.visibility = View.VISIBLE
        }
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

    @RequiresApi(Build.VERSION_CODES.O)
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

                // Update dropdown options
                updateDropdownOptions(null, jadwalRutinList)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeJadwalPeminjaman() {
        jadwalDipinjamViewModel.jadwalDipinjamList.observe(this) { jadwalPeminjamanList ->
            if (jadwalPeminjamanList.isEmpty()) {
                binding.tvTextKosongPeminjaman.visibility = View.VISIBLE
                binding.recyclerViewJadwalPeminjaman.visibility = View.GONE
            } else {
                binding.tvTextKosongPeminjaman.visibility = View.GONE
                binding.recyclerViewJadwalPeminjaman.visibility = View.VISIBLE
                peminjamanAdapter.updateData(jadwalPeminjamanList)

                // Update dropdown options
                updateDropdownOptions(jadwalPeminjamanList, null)
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