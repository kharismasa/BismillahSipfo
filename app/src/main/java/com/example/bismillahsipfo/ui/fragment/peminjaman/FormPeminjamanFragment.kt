package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.JadwalTersediaAdapter
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.JadwalTersedia
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.FormPeminjamanViewModel
import com.example.bismillahsipfo.data.repository.FormPeminjamanViewModelFactory
import com.example.bismillahsipfo.data.repository.JadwalAvailabilityStatus
import com.example.bismillahsipfo.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class FormPeminjamanFragment : Fragment() {

    private lateinit var viewModel: FormPeminjamanViewModel
    private lateinit var spinnerFasilitas: Spinner
    private lateinit var editTextTanggalMulai: EditText
    private lateinit var editTextTanggalSelesai: EditText
    private lateinit var editTextJamMulai: EditText
    private lateinit var editTextJamSelesai: EditText
    private lateinit var editTextNamaAcara: EditText
    private lateinit var editTextNamaOrganisasi: EditText
    private lateinit var containerJenisLapangan: LinearLayout
    private lateinit var containerPenggunaKhusus: RadioGroup
    private lateinit var buttonNext: Button
    private lateinit var spinnerOpsiPinjam: Spinner
    private lateinit var spinnerNamaOrganisasi: Spinner
    private lateinit var tvTanggal: LinearLayout
    private lateinit var tvJam: LinearLayout
    private lateinit var tvLapangan: TextView
    private lateinit var tvJadwalTersedia: TextView
    private lateinit var containerJadwalTersedia: RecyclerView
    private lateinit var jadwalTersediaAdapter: JadwalTersediaAdapter
    private lateinit var tvJadwalTidakTersedia: TextView
    private lateinit var tvDateWarning: LinearLayout

    // Loading components
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView

    // Variables untuk menyimpan data yang dipilih
    private var selectedJadwalTersedia: JadwalTersedia? = null
    private val selectedLapangan = mutableListOf<Lapangan>()

    companion object {
        // Define keys for Bundle/Intent extras
        const val EXTRA_ID_FASILITAS = "extra_id_fasilitas"
        const val EXTRA_NAMA_FASILITAS = "extra_nama_fasilitas"
        const val EXTRA_OPSI_PEMINJAMAN = "extra_opsi_peminjaman"
        const val EXTRA_NAMA_ACARA = "extra_nama_acara"
        const val EXTRA_ID_ORGANISASI = "extra_id_organisasi"
        const val EXTRA_NAMA_ORGANISASI = "extra_nama_organisasi"
        const val EXTRA_JADWAL_TERSEDIA = "extra_jadwal_tersedia"
        const val EXTRA_LIST_LAPANGAN = "extra_list_lapangan"
        const val EXTRA_PENGGUNA_KHUSUS = "extra_pengguna_khusus"
        const val EXTRA_TANGGAL_MULAI = "extra_tanggal_mulai"
        const val EXTRA_TANGGAL_SELESAI = "extra_tanggal_selesai"
        const val EXTRA_JAM_MULAI = "extra_jam_mulai"
        const val EXTRA_JAM_SELESAI = "extra_jam_selesai"
        const val EXTRA_LAPANGAN_DIPINJAM = "extra_lapangan_dipinjam"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_form_peminjaman, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fasilitasRepository = FasilitasRepository()
        val userRepository = UserRepository(requireContext())
        val factory = FormPeminjamanViewModelFactory(fasilitasRepository, userRepository)
        viewModel = ViewModelProvider(this, factory)[FormPeminjamanViewModel::class.java]

        initViews(view)
        setupListeners()
        setupJadwalTersediaRecyclerView()
        observeViewModel()
        setupFieldValidation()

        // Set tombol Next disabled secara default dan atur warnanya
        buttonNext.isEnabled = false
        buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))

        // Tambahkan callback untuk tombol back
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val activity = requireActivity() as PeminjamanActivity

                if (activity.viewPager.currentItem > 0) {
                    activity.viewPager.currentItem = activity.viewPager.currentItem - 1
                } else {
                    activity.finish()
                }
            }
        })
    }

    // Loading functions
    private fun showLoading(message: String = "Memuat data...") {
        loadingText.text = message
        loadingOverlay.visibility = View.VISIBLE

        // Disable form interactions
        setFormInteractionEnabled(false)
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE

        // Enable form interactions
        setFormInteractionEnabled(true)
    }

    private fun resetFormData() {
        // Reset selected jadwal
        selectedJadwalTersedia = null

        // Clear adapters
        jadwalTersediaAdapter.updateJadwalList(emptyList())

        // Hide jadwal elements
        tvJadwalTidakTersedia.visibility = View.GONE
        containerJadwalTersedia.visibility = View.GONE

        // Reset spinner organisasi to empty state
        val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Memuat organisasi..."))
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNamaOrganisasi.adapter = emptyAdapter
        spinnerNamaOrganisasi.isEnabled = false
    }

    private fun setFormInteractionEnabled(enabled: Boolean) {
        spinnerFasilitas.isEnabled = enabled
        spinnerOpsiPinjam.isEnabled = enabled
        spinnerNamaOrganisasi.isEnabled = enabled
        editTextNamaAcara.isEnabled = enabled
        editTextNamaOrganisasi.isEnabled = enabled
        editTextTanggalMulai.isEnabled = enabled
        editTextTanggalSelesai.isEnabled = enabled
        editTextJamMulai.isEnabled = enabled
        editTextJamSelesai.isEnabled = enabled

        // Disable/enable checkboxes in containerJenisLapangan
        for (i in 0 until containerJenisLapangan.childCount) {
            val child = containerJenisLapangan.getChildAt(i)
            if (child is CheckBox) {
                child.isEnabled = enabled
            }
        }

        // Disable/enable radio buttons
        containerPenggunaKhusus.children.forEach {
            (it as? RadioButton)?.isEnabled = enabled
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isDateTimeValid(): Boolean {
        val tanggalMulaiText = editTextTanggalMulai.text.toString().trim()
        val tanggalSelesaiText = editTextTanggalSelesai.text.toString().trim()
        val jamMulaiText = editTextJamMulai.text.toString().trim()
        val jamSelesaiText = editTextJamSelesai.text.toString().trim()

        if (tanggalMulaiText.isEmpty() || tanggalSelesaiText.isEmpty() ||
            jamMulaiText.isEmpty() || jamSelesaiText.isEmpty()) {
            hideDateWarning()
            return false
        }

        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val tanggalMulai = LocalDate.parse(tanggalMulaiText, formatter)
            val tanggalSelesai = LocalDate.parse(tanggalSelesaiText, formatter)
            val jamMulai = LocalTime.parse(jamMulaiText, timeFormatter)
            val jamSelesai = LocalTime.parse(jamSelesaiText, timeFormatter)

            val today = LocalDate.now()
            val minDate = today.plusDays(7)

            val minTime = LocalTime.of(6, 0)
            val maxTime = LocalTime.of(22, 0)

            if (jamMulai < minTime || jamMulai > maxTime || jamSelesai < minTime || jamSelesai > maxTime) {
                showDateWarning("Jam mulai dan jam selesai min max 06.00-22.00 WIB")
                return false
            }

            if (tanggalMulai < minDate) {
                showDateWarning("Masukkan tanggal minimal H+7 dari hari ini")
                return false
            }

            if (tanggalSelesai < tanggalMulai) {
                showDateWarning("Tanggal selesai harus lebih besar atau sama dengan tanggal mulai")
                return false
            }

            if (tanggalMulai != tanggalSelesai) {
                val daysBetween = ChronoUnit.DAYS.between(tanggalMulai, tanggalSelesai)
                if (daysBetween > 7) {
                    showDateWarning("Durasi peminjaman maksimal 7 hari")
                    return false
                }
            }

            if (tanggalMulai <= tanggalSelesai && jamSelesai <= jamMulai) {
                showDateWarning("Jam selesai harus lebih besar dari jam mulai")
                return false
            }

            if (tanggalMulai <= tanggalSelesai) {
                val durationMinutes = ChronoUnit.MINUTES.between(jamMulai, jamSelesai)
                if (durationMinutes < 30) {
                    showDateWarning("Durasi peminjaman minimal 30 menit")
                    return false
                }
            }

            val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
            val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

            if (selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas != 30) {
                val currentAvailabilityStatus = viewModel.jadwalAvailability.value
                when (currentAvailabilityStatus) {
                    is JadwalAvailabilityStatus.UNAVAILABLE -> {
                        showDateWarning("Jadwal tidak tersedia - sudah ada peminjaman lain")
                        return false
                    }
                    is JadwalAvailabilityStatus.HOLIDAY -> {
                        showDateWarning("Jadwal tidak tersedia - Hari Libur: ${currentAvailabilityStatus.namaHariLibur} (${currentAvailabilityStatus.tanggal})")
                        return false
                    }
                    is JadwalAvailabilityStatus.CONFLICT_WITH_JADWAL_RUTIN -> {
                        showDateWarning("Terdapat jadwal rutin, tolong hubungi pemilik jadwal terlebih dahulu")
                        return true
                    }
                    else -> {
                        // AVAILABLE atau null, lanjut ke validasi berikutnya
                    }
                }
            }

            hideDateWarning()
            return true

        } catch (e: Exception) {
            Log.e("FormPeminjamanFragment", "Error parsing date/time: ${e.message}")
            showDateWarning("Format tanggal atau jam tidak valid")
            return false
        }
    }

    private fun setupFieldValidation() {
        editTextNamaAcara.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()
            }
        })

        editTextNamaOrganisasi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editTextNamaOrganisasi.visibility == View.VISIBLE) {
                    checkFormValidity()
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkFormValidity() {
        val isNamaAcaraFilled = editTextNamaAcara.text.toString().trim().isNotEmpty()
        val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""

        val isValid = when {
            selectedOption == "Sesuai Jadwal Rutin" -> {
                isNamaAcaraFilled && selectedJadwalTersedia != null
            }
            selectedOption == "Diluar Jadwal Rutin" -> {
                val isNamaOrganisasiFilled = editTextNamaOrganisasi.text.toString().trim().isNotEmpty()
                val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

                if (selectedFasilitas?.idFasilitas == 30) {
                    isNamaAcaraFilled && isNamaOrganisasiFilled && selectedJadwalTersedia != null
                } else {
                    val isDateTimeValid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        isDateTimeValid()
                    } else {
                        editTextTanggalMulai.text.toString().isNotEmpty() &&
                                editTextTanggalSelesai.text.toString().isNotEmpty() &&
                                editTextJamMulai.text.toString().isNotEmpty() &&
                                editTextJamSelesai.text.toString().isNotEmpty()
                    }

                    isNamaAcaraFilled && isNamaOrganisasiFilled &&
                            isDateTimeValid && selectedLapangan.isNotEmpty()
                }
            }
            else -> false
        }

        buttonNext.isEnabled = isValid

        val context = requireContext()
        if (isValid) {
            buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dark_blue))
        } else {
            buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
        }
    }

    private fun setupDateTimeValidation() {
        val dateTimeWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            @RequiresApi(Build.VERSION_CODES.O)
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()

                val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
                val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

                if (selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas != 30) {
                    checkJadwalAvailability()
                }
            }
        }

        editTextTanggalMulai.addTextChangedListener(dateTimeWatcher)
        editTextTanggalSelesai.addTextChangedListener(dateTimeWatcher)
        editTextJamMulai.addTextChangedListener(dateTimeWatcher)
        editTextJamSelesai.addTextChangedListener(dateTimeWatcher)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveDataToExtras(bundle: Bundle) {
        val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas
        val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""

        selectedFasilitas?.let {
            bundle.putInt(EXTRA_ID_FASILITAS, it.idFasilitas)
            bundle.putString(EXTRA_NAMA_FASILITAS, it.namaFasilitas)
        }

        bundle.putString(EXTRA_OPSI_PEMINJAMAN, selectedOption)
        bundle.putString(EXTRA_NAMA_ACARA, editTextNamaAcara.text.toString())

        when {
            selectedOption == "Sesuai Jadwal Rutin" -> {
                val selectedOrganisasi = spinnerNamaOrganisasi.selectedItem
                val organisasiIndex = spinnerNamaOrganisasi.selectedItemPosition
                val organisasi = viewModel.organisasiList.value?.getOrNull(organisasiIndex)

                organisasi?.let {
                    bundle.putInt(EXTRA_ID_ORGANISASI, it.idOrganisasi)
                    bundle.putString(EXTRA_NAMA_ORGANISASI, it.namaOrganisasi)
                }

                selectedJadwalTersedia?.let { jadwal ->
                    if (jadwal is java.io.Serializable) {
                        bundle.putSerializable(EXTRA_JADWAL_TERSEDIA, jadwal as java.io.Serializable)

                        jadwal.tanggal?.let { tanggal ->
                            val formattedDate = tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            bundle.putString(EXTRA_TANGGAL_MULAI, formattedDate)
                            bundle.putString(EXTRA_TANGGAL_SELESAI, formattedDate)
                        }

                        jadwal.waktuMulai?.let { waktu ->
                            val formattedTime = waktu.format(DateTimeFormatter.ofPattern("HH:mm"))
                            bundle.putString(EXTRA_JAM_MULAI, formattedTime)
                        }

                        jadwal.waktuSelesai?.let { waktu ->
                            val formattedTime = waktu.format(DateTimeFormatter.ofPattern("HH:mm"))
                            bundle.putString(EXTRA_JAM_SELESAI, formattedTime)
                        }

                        val intArray = ArrayList<Int>(jadwal.listLapangan)
                        bundle.putIntegerArrayList(EXTRA_LIST_LAPANGAN, intArray)
                        bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, intArray)
                    }
                }

                if (selectedFasilitas?.idFasilitas == 30) {
                    bundle.putString(EXTRA_PENGGUNA_KHUSUS, PenggunaKhusus.INTERNAL_UII.name)
                }
            }

            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas != 30 -> {
                bundle.putString(EXTRA_NAMA_ORGANISASI, editTextNamaOrganisasi.text.toString())
                bundle.putString(EXTRA_TANGGAL_MULAI, editTextTanggalMulai.text.toString())
                bundle.putString(EXTRA_TANGGAL_SELESAI, editTextTanggalSelesai.text.toString())
                bundle.putString(EXTRA_JAM_MULAI, editTextJamMulai.text.toString())
                bundle.putString(EXTRA_JAM_SELESAI, editTextJamSelesai.text.toString())

                val selectedLapanganIds = selectedLapangan.map { it.idLapangan }
                bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, ArrayList<Int>(selectedLapanganIds))
            }

            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas == 30 -> {
                bundle.putString(EXTRA_NAMA_ORGANISASI, editTextNamaOrganisasi.text.toString())

                selectedJadwalTersedia?.let { jadwal ->
                    if (jadwal is java.io.Serializable) {
                        bundle.putSerializable(EXTRA_JADWAL_TERSEDIA, jadwal as java.io.Serializable)

                        jadwal.tanggal?.let { tanggal ->
                            val formattedDate = tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            bundle.putString(EXTRA_TANGGAL_MULAI, formattedDate)
                            bundle.putString(EXTRA_TANGGAL_SELESAI, formattedDate)
                        }

                        jadwal.waktuMulai?.let { waktu ->
                            val formattedTime = waktu.format(DateTimeFormatter.ofPattern("HH:mm"))
                            bundle.putString(EXTRA_JAM_MULAI, formattedTime)
                        }

                        jadwal.waktuSelesai?.let { waktu ->
                            val formattedTime = waktu.format(DateTimeFormatter.ofPattern("HH:mm"))
                            bundle.putString(EXTRA_JAM_SELESAI, formattedTime)
                        }

                        if (jadwal.listLapangan.isNotEmpty()) {
                            val intArray = ArrayList<Int>(jadwal.listLapangan)
                            bundle.putIntegerArrayList(EXTRA_LIST_LAPANGAN, intArray)
                            bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, intArray)
                        }
                    }
                }

                val penggunaKhusus = when (containerPenggunaKhusus.checkedRadioButtonId) {
                    R.id.radio_internal_uii -> PenggunaKhusus.INTERNAL_UII
                    R.id.radio_internal_vs_eksternal -> PenggunaKhusus.INTERNAL_VS_EKSTERNAL
                    R.id.radio_eksternal_uii -> PenggunaKhusus.EKSTERNAL_UII
                    else -> null
                }
                penggunaKhusus?.let {
                    bundle.putString(EXTRA_PENGGUNA_KHUSUS, it.name)
                }
            }
        }

        Log.d("FormPeminjamanFragment", "Bundle complete, jadwal tersedia: ${selectedJadwalTersedia != null}, " +
                "list lapangan: ${bundle.getIntegerArrayList(EXTRA_LIST_LAPANGAN)?.size ?: 0}, " +
                "tanggal: ${bundle.getString(EXTRA_TANGGAL_MULAI)}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupJadwalTersediaRecyclerView() {
        Log.d("FormPeminjamanFragment", "Setting up JadwalTersediaRecyclerView")
        jadwalTersediaAdapter = JadwalTersediaAdapter(emptyList()) { jadwal ->
            Log.d("FormPeminjamanFragment", "Jadwal selected: $jadwal")
            selectedJadwalTersedia = jadwal
            viewModel.onJadwalTersediaSelected(jadwal)
            updateSelectedJadwal(jadwal)
            checkFormValidity()

            val tanggal = jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val jamMulai = jadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm"))
            val jamSelesai = jadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm"))
            Toast.makeText(requireContext(),
                "Jadwal dipilih: ${jadwal.hari}, $tanggal ($jamMulai - $jamSelesai)",
                Toast.LENGTH_SHORT).show()
        }

        val layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        containerJadwalTersedia.layoutManager = layoutManager
        containerJadwalTersedia.adapter = jadwalTersediaAdapter

        val itemDecoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = 8
                outRect.set(spacing, spacing, spacing, spacing)
            }
        }
        containerJadwalTersedia.addItemDecoration(itemDecoration)

        Log.d("FormPeminjamanFragment", "JadwalTersediaRecyclerView setup complete")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        spinnerFasilitas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFasilitas = parent.getItemAtPosition(position) as Fasilitas
                Log.d("FormPeminjamanFragment", "Fasilitas selected: ${selectedFasilitas.namaFasilitas}")

                resetFormData()

                // Show loading when fasilitas is selected
                if (selectedFasilitas.idFasilitas != -1) {
                    showLoading("Memuat data fasilitas...")
                }

                containerJenisLapangan.removeAllViews()
                containerPenggunaKhusus.visibility = View.GONE
                selectedLapangan.clear()
                viewModel.onFasilitasSelected(selectedFasilitas)
                updateVisibilityBasedOnCurrentSelection()
                checkFormValidity()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerNamaOrganisasi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val organisasiList = viewModel.organisasiList.value

                // Check if organisasi list is not empty and position is valid
                if (organisasiList != null && organisasiList.isNotEmpty() && position < organisasiList.size) {
                    val selectedOrganisasi = organisasiList[position]
                    showLoading("Memuat jadwal tersedia...")
                    viewModel.onOrganisasiSelected(selectedOrganisasi)
                } else {
                    Log.d("FormPeminjamanFragment", "No valid organisasi to select at position $position")
                }
                checkFormValidity()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editTextTanggalMulai.setOnClickListener { showDatePicker(true) }
        editTextTanggalSelesai.setOnClickListener { showDatePicker(false) }
        editTextJamMulai.setOnClickListener { showTimePicker(true) }
        editTextJamSelesai.setOnClickListener { showTimePicker(false) }

        buttonNext.setOnClickListener {
            val bundle = Bundle()
            saveDataToExtras(bundle)

            Log.d("FormPeminjamanFragment", "Data saved to extras: $bundle")

            val activity = requireActivity() as PeminjamanActivity
            activity.navigateToNextPage(bundle)
        }

        editTextNamaAcara.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaAcara.hint = ""
            }
        }

        editTextNamaOrganisasi.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaOrganisasi.hint = ""
            }
        }

        val opsiPinjamAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.opsi_pinjam_array,
            android.R.layout.simple_spinner_item
        )
        opsiPinjamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOpsiPinjam.adapter = opsiPinjamAdapter

        spinnerOpsiPinjam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

                Log.d("FormPeminjamanFragment", "Opsi peminjaman dipilih: $selectedOption")
                Log.d("FormPeminjamanFragment", "Fasilitas terpilih: ${selectedFasilitas?.namaFasilitas}, ID: ${selectedFasilitas?.idFasilitas}")

                // Show loading when option changes and affects data loading
                if (selectedFasilitas != null && selectedFasilitas.idFasilitas != -1) {
                    when (selectedOption) {
                        "Sesuai Jadwal Rutin" -> {
                            showLoading("Memuat jadwal rutin...")
                        }
                        "Diluar Jadwal Rutin" -> {
                            if (selectedFasilitas.idFasilitas == 30) {
                                showLoading("Memuat jadwal tersedia...")
                            }
                        }
                    }
                }

                updateVisibilityBasedOnCurrentSelection()
                setVisibilityBasedOnSelection(selectedFasilitas?.idFasilitas, selectedOption)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        setupDateTimeValidation()
        setupSpinnerOpsiPinjam()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSelectedJadwal(jadwal: JadwalTersedia) {
        selectedJadwalTersedia = jadwal

        editTextTanggalMulai.setText(jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextTanggalSelesai.setText(jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextJamMulai.setText(jadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm")))
        editTextJamSelesai.setText(jadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm")))

        updateLapanganCheckboxes(viewModel.lapanganList.value ?: emptyList())

        Log.d("FormPeminjamanFragment", "Selected jadwal tersedia updated: " +
                "tanggal=${jadwal.tanggal}, waktuMulai=${jadwal.waktuMulai}, " +
                "waktuSelesai=${jadwal.waktuSelesai}, listLapangan=${jadwal.listLapangan}")
    }

    private fun initViews(view: View) {
        spinnerFasilitas = view.findViewById(R.id.spinner_fasilitas)
        editTextTanggalMulai = view.findViewById(R.id.edittext_tanggal_mulai)
        editTextTanggalSelesai = view.findViewById(R.id.edittext_tanggal_selesai)
        editTextJamMulai = view.findViewById(R.id.edittext_jam_mulai)
        editTextJamSelesai = view.findViewById(R.id.edittext_jam_selesai)
        editTextNamaAcara = view.findViewById(R.id.edittext_nama_acara)
        editTextNamaOrganisasi = view.findViewById(R.id.edittext_nama_organisasi)
        containerJenisLapangan = view.findViewById(R.id.container_jenis_lapangan)
        containerPenggunaKhusus = view.findViewById(R.id.container_pengguna_khusus)
        buttonNext = view.findViewById(R.id.button_next)
        spinnerOpsiPinjam = view.findViewById(R.id.spinner_opsi_pinjam)
        spinnerNamaOrganisasi = view.findViewById(R.id.spinner_nama_organisasi)
        tvTanggal = view.findViewById(R.id.tvTanggal)
        tvJam = view.findViewById(R.id.tvJam)
        tvLapangan = view.findViewById(R.id.tvLapangan)
        tvJadwalTersedia = view.findViewById(R.id.tvJadwalTersedia)
        containerJadwalTersedia = view.findViewById(R.id.container_jadwal_tersedia)
        tvDateWarning = view.findViewById(R.id.tv_date_warning)
        tvJadwalTidakTersedia = view.findViewById(R.id.tv_jadwal_tidak_tersedia)
        tvJadwalTidakTersedia.visibility = View.GONE

        // Initialize loading components
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        progressBar = view.findViewById(R.id.progress_bar)
        loadingText = view.findViewById(R.id.loading_text)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSpinnerOpsiPinjam() {
        val opsiPinjamAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.opsi_pinjam_array,
            android.R.layout.simple_spinner_item
        )
        opsiPinjamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOpsiPinjam.adapter = opsiPinjamAdapter

        spinnerOpsiPinjam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

                selectedJadwalTersedia = null

                setVisibilityBasedOnSelection(selectedFasilitas?.idFasilitas, selectedOption)

                tvJadwalTidakTersedia.visibility = View.GONE

                if (selectedOption == "Sesuai Jadwal Rutin" && selectedFasilitas != null && selectedFasilitas.idFasilitas != -1) {
                    val organisasiList = viewModel.organisasiList.value
                    if (organisasiList != null && organisasiList.isNotEmpty()) {
                        val organisasiIndex = spinnerNamaOrganisasi.selectedItemPosition
                        val organisasi = organisasiList.getOrNull(organisasiIndex)
                        if (organisasi != null) {
                            showLoading("Memuat jadwal tersedia...")
                            viewModel.onOrganisasiSelected(organisasi)
                        }
                    } else {
                        // No organisasi available, show not available message
                        tvJadwalTidakTersedia.visibility = View.VISIBLE
                        containerJadwalTersedia.visibility = View.GONE
                    }
                }

                if (selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas == 30) {
                    showLoading("Memuat jadwal tersedia...")
                    viewModel.loadJadwalTersediaForFasilitas30()
                }

                checkFormValidity()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showDateWarning(message: String) {
        val textView = tvDateWarning.findViewById<TextView>(R.id.warning_text)
        textView.text = message
        tvDateWarning.visibility = View.VISIBLE
    }

    private fun hideDateWarning() {
        tvDateWarning.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setVisibilityBasedOnSelection(idFasilitas: Int?, selectedOption: String) {
        Log.d("FormPeminjamanFragment", "setVisibilityBasedOnSelection called - Fasilitas ID: $idFasilitas, Opsi: $selectedOption")

        hideDateWarning()

        when {
            idFasilitas == 30 && selectedOption == "Sesuai Jadwal Rutin" -> {
                editTextNamaOrganisasi.visibility = View.GONE
                spinnerNamaOrganisasi.visibility = View.VISIBLE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvLapangan.visibility = View.GONE
                containerJenisLapangan.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                containerPenggunaKhusus.visibility = View.VISIBLE
                tvDateWarning.visibility = View.GONE

                val radioInternalUii = containerPenggunaKhusus.findViewById<RadioButton>(R.id.radio_internal_uii)
                radioInternalUii.isChecked = true
                containerPenggunaKhusus.children.forEach { (it as? RadioButton)?.isEnabled = false }

                showLoading("Memuat jadwal tersedia...")
                viewModel.loadJadwalTersediaForFasilitas30()
                Log.d("FormPeminjamanFragment", "Container jadwal tersedia ditampilkan untuk Fasilitas 30 - Sesuai Jadwal Rutin")
            }
            idFasilitas == 30 && selectedOption == "Diluar Jadwal Rutin" -> {
                editTextNamaOrganisasi.visibility = View.VISIBLE
                spinnerNamaOrganisasi.visibility = View.GONE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                tvDateWarning.visibility = View.GONE
                Log.d("FormPeminjamanFragment", "Container jadwal tersedia ditampilkan untuk Fasilitas 30 - Diluar Jadwal Rutin")
                containerPenggunaKhusus.visibility = View.VISIBLE

                containerPenggunaKhusus.children.forEach { (it as? RadioButton)?.isEnabled = true }

                showLoading("Memuat jadwal tersedia...")
                viewModel.loadJadwalTersediaForFasilitas30()
            }
            selectedOption == "Sesuai Jadwal Rutin" -> {
                editTextNamaOrganisasi.visibility = View.GONE
                spinnerNamaOrganisasi.visibility = View.VISIBLE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvLapangan.visibility = View.GONE
                containerJenisLapangan.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                containerPenggunaKhusus.visibility = View.GONE
                tvDateWarning.visibility = View.GONE
            }
            selectedOption == "Diluar Jadwal Rutin" -> {
                editTextNamaOrganisasi.visibility = View.VISIBLE
                spinnerNamaOrganisasi.visibility = View.GONE
                tvTanggal.visibility = View.VISIBLE
                tvJam.visibility = View.VISIBLE
                tvJadwalTersedia.visibility = View.GONE
                containerJadwalTersedia.visibility = View.GONE
                containerPenggunaKhusus.visibility = View.GONE
                tvLapangan.visibility = View.VISIBLE
                containerJenisLapangan.visibility = View.VISIBLE
                tvDateWarning.visibility = View.GONE
            }
            else -> {
                tvDateWarning.visibility = View.GONE
            }
        }

        editTextNamaAcara.visibility = View.VISIBLE

        view?.post {
            containerJadwalTersedia.requestLayout()
            containerJadwalTersedia.invalidate()
            Log.d("FormPeminjamanFragment", "Container jadwal tersedia visibility: ${containerJadwalTersedia.visibility == View.VISIBLE}")
        }

        if (idFasilitas == 30 && (selectedOption == "Sesuai Jadwal Rutin" || selectedOption == "Diluar Jadwal Rutin")) {
            showLoading("Memuat jadwal tersedia...")
            viewModel.loadJadwalTersediaForFasilitas30()
            Log.d("FormPeminjamanFragment", "Memanggil loadJadwalTersediaForFasilitas30()")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVisibilityBasedOnCurrentSelection() {
        val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
        val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

        tvTanggal.visibility = View.GONE
        tvJam.visibility = View.GONE
        tvLapangan.visibility = View.GONE
        containerJadwalTersedia.visibility = View.GONE
        tvJadwalTersedia.visibility = View.GONE
        tvJadwalTidakTersedia.visibility = View.GONE
        containerJenisLapangan.visibility = View.GONE
        containerPenggunaKhusus.visibility = View.GONE
        editTextNamaOrganisasi.visibility = View.GONE
        spinnerNamaOrganisasi.visibility = View.GONE

        when {
            selectedOption == "Sesuai Jadwal Rutin" -> {
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                spinnerNamaOrganisasi.visibility = View.VISIBLE

                val jadwalList = viewModel.jadwalTersedia.value ?: emptyList()
                if (jadwalList.isEmpty() && selectedFasilitas != null) {
                    tvJadwalTidakTersedia.visibility = View.VISIBLE
                    containerJadwalTersedia.visibility = View.GONE
                }
            }

            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas == 30 -> {
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                editTextNamaOrganisasi.visibility = View.VISIBLE
                containerPenggunaKhusus.visibility = View.VISIBLE

                val jadwalList = viewModel.jadwalTersediaFasilitas30.value ?: emptyList()
                if (jadwalList.isEmpty()) {
                    tvJadwalTidakTersedia.visibility = View.VISIBLE
                    containerJadwalTersedia.visibility = View.GONE
                }
            }

            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas != 30 -> {
                tvTanggal.visibility = View.VISIBLE
                tvJam.visibility = View.VISIBLE
                tvLapangan.visibility = View.VISIBLE
                containerJenisLapangan.visibility = View.VISIBLE
                editTextNamaOrganisasi.visibility = View.VISIBLE
            }
        }

        selectedJadwalTersedia = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkJadwalAvailability() {
        val tanggalMulai = editTextTanggalMulai.text.toString()
        val tanggalSelesai = editTextTanggalSelesai.text.toString()
        val jamMulai = editTextJamMulai.text.toString()
        val jamSelesai = editTextJamSelesai.text.toString()

        if (tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty() && jamMulai.isNotEmpty() && jamSelesai.isNotEmpty()) {
            viewModel.checkJadwalAvailability(tanggalMulai, tanggalSelesai, jamMulai, jamSelesai)
        }
    }

    private fun observeViewModel() {
        viewModel.fasilitasList.observe(viewLifecycleOwner) { fasilitas ->
            val fasilitasList = listOf(
                Fasilitas(
                    idFasilitas = -1,
                    namaFasilitas = "Pilih fasilitas disini",
                    deskripsi = "",
                    fasilitasPlus = "",
                    photo = "",
                    alamat = "",
                    prosedurPeminjaman = "",
                    tatatertib = "",
                    ketentuanTarif = "",
                    kontak = "",
                    maps = ""
                )
            ) + fasilitas
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fasilitasList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFasilitas.adapter = adapter
        }

        viewModel.lapanganList.observe(viewLifecycleOwner) { lapangan ->
            updateLapanganCheckboxes(lapangan)
            hideLoading() // Hide loading when lapangan data is loaded
        }

        viewModel.showPenggunaKhusus.observe(viewLifecycleOwner) { show ->
            containerPenggunaKhusus.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.organisasiList.observe(viewLifecycleOwner) { organisasiList ->
            Log.d("FormPeminjamanFragment", "Organisasi list updated: $organisasiList")

            if (organisasiList.isEmpty()) {
                // Handle empty organisasi list
                val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Tidak ada Organisasi"))
                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerNamaOrganisasi.adapter = emptyAdapter
                spinnerNamaOrganisasi.isEnabled = false

                // Show "Jadwal Rutin Tidak Tersedia" message
                val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
                if (selectedOption == "Sesuai Jadwal Rutin") {
                    tvJadwalTidakTersedia.visibility = View.VISIBLE
                    containerJadwalTersedia.visibility = View.GONE
                }
            } else {
                // Handle non-empty organisasi list
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, organisasiList.map { it.namaOrganisasi })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerNamaOrganisasi.adapter = adapter
                spinnerNamaOrganisasi.isEnabled = true

                // Hide "Jadwal Rutin Tidak Tersedia" message when organisasi is available
                tvJadwalTidakTersedia.visibility = View.GONE
            }

            hideLoading() // Hide loading when organisasi data is loaded
        }

        viewModel.jadwalTersedia.observe(viewLifecycleOwner) { jadwalList ->
            Log.d("FormPeminjamanFragment", "Jadwal tersedia updated. Size: ${jadwalList.size}")

            val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
            val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

            if (selectedOption == "Sesuai Jadwal Rutin" && jadwalList.isEmpty() && selectedFasilitas != null && selectedFasilitas.idFasilitas != -1) {
                tvJadwalTidakTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.GONE
            } else {
                tvJadwalTidakTersedia.visibility = View.GONE
                containerJadwalTersedia.visibility = if (jadwalList.isNotEmpty()) View.VISIBLE else View.GONE
                jadwalTersediaAdapter.updateJadwalList(jadwalList)
            }

            hideLoading() // Hide loading when jadwal tersedia data is loaded
        }

        viewModel.jadwalAvailability.observe(viewLifecycleOwner) { status ->
            val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""

            if (selectedOption == "Diluar Jadwal Rutin") {
                when (status) {
                    is JadwalAvailabilityStatus.AVAILABLE -> {
                        // Tidak ada peringatan tambahan
                    }
                    is JadwalAvailabilityStatus.UNAVAILABLE -> {
                        showDateWarning("Jadwal tidak tersedia - sudah ada peminjaman lain")
                    }
                    is JadwalAvailabilityStatus.HOLIDAY -> {
                        showDateWarning("Jadwal tidak tersedia - Hari Libur: ${status.namaHariLibur} (${status.tanggal})")
                    }
                    is JadwalAvailabilityStatus.CONFLICT_WITH_JADWAL_RUTIN -> {
                        showDateWarning("Terdapat jadwal rutin, tolong hubungi pemilik jadwal terlebih dahulu")
                    }
                }
                checkFormValidity()
            }
        }

        viewModel.jadwalTersediaFasilitas30.observe(viewLifecycleOwner) { jadwalList ->
            Log.d("FormPeminjamanFragment", "Jadwal tersedia for Fasilitas 30 updated. Size: ${jadwalList.size}")

            val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""
            val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas

            if (selectedOption == "Diluar Jadwal Rutin" && jadwalList.isEmpty() && selectedFasilitas?.idFasilitas == 30) {
                tvJadwalTidakTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.GONE
            } else {
                tvJadwalTidakTersedia.visibility = View.GONE
                containerJadwalTersedia.visibility = if (jadwalList.isNotEmpty()) View.VISIBLE else View.GONE
                jadwalTersediaAdapter.updateJadwalList(jadwalList)
            }

            hideLoading() // Hide loading when jadwal tersedia fasilitas 30 data is loaded
        }

        // Add loading state observer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val today = LocalDate.now()
        val minDate = today.plusDays(7)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            if (isStartDate) {
                editTextTanggalMulai.setText(formattedDate)
                viewModel.setTanggalMulai(selectedDate)
            } else {
                editTextTanggalSelesai.setText(formattedDate)
                viewModel.setTanggalSelesai(selectedDate)
            }

            checkFormValidity()
        }, year, month, day)

        val minCalendar = java.util.Calendar.getInstance()
        minCalendar.set(minDate.year, minDate.monthValue - 1, minDate.dayOfMonth)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            if (selectedHour < 6 || selectedHour > 22) {
                showDateWarning("Jam mulai dan jam selesai min max 06.00-22.00 WIB")
                return@TimePickerDialog
            }

            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formattedTime = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))

            if (isStartTime) {
                editTextJamMulai.setText(formattedTime)
                viewModel.setJamMulai(selectedTime)
            } else {
                editTextJamSelesai.setText(formattedTime)
                viewModel.setJamSelesai(selectedTime)
            }

            checkFormValidity()
        }, hour, minute, true)

        timePickerDialog.setTitle("Pilih Jam (06:00 - 22:00)")
        timePickerDialog.show()
    }

    private fun updateLapanganCheckboxes(lapanganList: List<Lapangan>) {
        containerJenisLapangan.removeAllViews()
        selectedLapangan.clear()

        if (lapanganList.isNotEmpty()) {
            lapanganList.forEach { lapangan ->
                val checkbox = CheckBox(context)
                checkbox.text = lapangan.namaLapangan
                checkbox.isChecked = true
                selectedLapangan.add(lapangan)

                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedLapangan.add(lapangan)
                    } else {
                        selectedLapangan.remove(lapangan)
                    }
                    viewModel.onLapanganChecked(lapangan, isChecked)
                    checkFormValidity()
                }
                containerJenisLapangan.addView(checkbox)
            }
        } else {
            val textView = TextView(context)
            textView.text = "Tidak ada lapangan tersedia"
            containerJenisLapangan.addView(textView)
        }
    }
}