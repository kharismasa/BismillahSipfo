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

        val fasilitasRepository = FasilitasRepository() // Inisialisasi sesuai kebutuhan
        val userRepository = UserRepository(requireContext()) // Inisialisasi dengan context // Inisialisasi sesuai kebutuhan
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
                // Ambil activity parent
                val activity = requireActivity() as PeminjamanActivity

                // Jika berada di halaman pertama, kembalikan ke halaman sebelumnya
                if (activity.viewPager.currentItem > 0) {
                    activity.viewPager.currentItem = activity.viewPager.currentItem - 1
                } else {
                    // Jika sudah di halaman pertama, kembalikan ke activity sebelumnya
                    activity.finish()
                }
            }
        })
    }

    private fun setupFieldValidation() {
        // Setup text watcher for nama acara
        editTextNamaAcara.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()
            }
        })

        // Setup text watcher for nama organisasi
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
                    isNamaAcaraFilled && isNamaOrganisasiFilled &&
                            editTextTanggalMulai.text.toString().isNotEmpty() &&
                            editTextTanggalSelesai.text.toString().isNotEmpty() &&
                            editTextJamMulai.text.toString().isNotEmpty() &&
                            editTextJamSelesai.text.toString().isNotEmpty() &&
                            selectedLapangan.isNotEmpty()
                }
            }
            else -> false
        }

        buttonNext.isEnabled = isValid

        // Set warna button berdasarkan status enabled/disabled menggunakan ContextCompat
        val context = requireContext()
        if (isValid) {
            buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dark_blue))
        } else {
            buttonNext.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
        }
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
            // Skenario 1: Sesuai Jadwal Rutin
            selectedOption == "Sesuai Jadwal Rutin" -> {
                // Simpan organisasi dari spinner
                val selectedOrganisasi = spinnerNamaOrganisasi.selectedItem
                val organisasiIndex = spinnerNamaOrganisasi.selectedItemPosition
                val organisasi = viewModel.organisasiList.value?.getOrNull(organisasiIndex)

                organisasi?.let {
                    bundle.putInt(EXTRA_ID_ORGANISASI, it.idOrganisasi)
                    bundle.putString(EXTRA_NAMA_ORGANISASI, it.namaOrganisasi)
                }

                // Simpan jadwal tersedia
                selectedJadwalTersedia?.let { jadwal ->
                    if (jadwal is java.io.Serializable) {
                        bundle.putSerializable(EXTRA_JADWAL_TERSEDIA, jadwal as java.io.Serializable)

                        // Tambahkan informasi tanggal dan jam
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

                        // Simpan list lapangan dari jadwal rutin
                        val intArray = ArrayList<Int>(jadwal.listLapangan)
                        bundle.putIntegerArrayList(EXTRA_LIST_LAPANGAN, intArray)
                        // Juga simpan sebagai lapangan dipinjam untuk konsistensi
                        bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, intArray)
                    }
                }

                // Jika fasilitas 30, simpan pengguna khusus "Internal UII"
                if (selectedFasilitas?.idFasilitas == 30) {
                    bundle.putString(EXTRA_PENGGUNA_KHUSUS, PenggunaKhusus.INTERNAL_UII.name)
                }
            }

            // Skenario 2: Diluar Jadwal Rutin untuk fasilitas selain 30
            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas != 30 -> {
                bundle.putString(EXTRA_NAMA_ORGANISASI, editTextNamaOrganisasi.text.toString())
                bundle.putString(EXTRA_TANGGAL_MULAI, editTextTanggalMulai.text.toString())
                bundle.putString(EXTRA_TANGGAL_SELESAI, editTextTanggalSelesai.text.toString())
                bundle.putString(EXTRA_JAM_MULAI, editTextJamMulai.text.toString())
                bundle.putString(EXTRA_JAM_SELESAI, editTextJamSelesai.text.toString())

                // Simpan lapangan yang dicentang
                val selectedLapanganIds = selectedLapangan.map { it.idLapangan }
                bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, ArrayList<Int>(selectedLapanganIds))
            }

            // Skenario 3: Diluar Jadwal Rutin untuk fasilitas 30
            selectedOption == "Diluar Jadwal Rutin" && selectedFasilitas?.idFasilitas == 30 -> {
                bundle.putString(EXTRA_NAMA_ORGANISASI, editTextNamaOrganisasi.text.toString())

                // Simpan jadwal tersedia
                selectedJadwalTersedia?.let { jadwal ->
                    if (jadwal is java.io.Serializable) {
                        bundle.putSerializable(EXTRA_JADWAL_TERSEDIA, jadwal as java.io.Serializable)

                        // Tambahkan informasi tanggal dan jam
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

                        // Simpan list lapangan jika ada
                        if (jadwal.listLapangan.isNotEmpty()) {
                            val intArray = ArrayList<Int>(jadwal.listLapangan)
                            bundle.putIntegerArrayList(EXTRA_LIST_LAPANGAN, intArray)
                            bundle.putIntegerArrayList(EXTRA_LAPANGAN_DIPINJAM, intArray)
                        }
                    }
                }

                // Simpan pengguna khusus dari radio button
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

        // Debug log
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

            // Toast konfirmasi tambahan dari Fragment jika diperlukan
            val tanggal = jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val jamMulai = jadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm"))
            val jamSelesai = jadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm"))
            Toast.makeText(requireContext(),
                "Jadwal dipilih: ${jadwal.hari}, $tanggal ($jamMulai - $jamSelesai)",
                Toast.LENGTH_SHORT).show()
        }

        // Menggunakan GridLayoutManager dengan 2 kolom
        val layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        containerJadwalTersedia.layoutManager = layoutManager
        containerJadwalTersedia.adapter = jadwalTersediaAdapter

        // Menambahkan ItemDecoration untuk memberikan jarak antar item
        val itemDecoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = 8 // 8dp spacing
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
                val selectedOrganisasi = viewModel.organisasiList.value?.get(position)
                if (selectedOrganisasi != null) {
                    viewModel.onOrganisasiSelected(selectedOrganisasi)
                }
                checkFormValidity()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editTextTanggalMulai.setOnClickListener { showDatePicker(true) }
        editTextTanggalSelesai.setOnClickListener { showDatePicker(false) }
        editTextJamMulai.setOnClickListener { showTimePicker(true) }
        editTextJamSelesai.setOnClickListener { showTimePicker(false) }

        // Listener untuk tombol Next
        buttonNext.setOnClickListener {
            val bundle = Bundle()
            saveDataToExtras(bundle)

            // Debug log untuk memastikan data tersimpan
            Log.d("FormPeminjamanFragment", "Data saved to extras: $bundle")

            val activity = requireActivity() as PeminjamanActivity
            activity.navigateToNextPage(bundle)
        }

        // Listener untuk edittext yang akan menghilangkan hint saat diketik
        editTextNamaAcara.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaAcara.hint = "" // Hilangkan hint saat fokus
            }
        }

        editTextNamaOrganisasi.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaOrganisasi.hint = "" // Hilangkan hint saat fokus
            }
        }

        // Inisialisasi spinner opsi pinjam
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

                updateVisibilityBasedOnCurrentSelection()
                setVisibilityBasedOnSelection(selectedFasilitas?.idFasilitas, selectedOption)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Tambahkan listener untuk tanggal dan jam
        editTextTanggalMulai.addTextChangedListener(dateTimeChangeWatcher)
        editTextTanggalSelesai.addTextChangedListener(dateTimeChangeWatcher)
        editTextJamMulai.addTextChangedListener(dateTimeChangeWatcher)
        editTextJamSelesai.addTextChangedListener(dateTimeChangeWatcher)

//        buttonNext.setOnClickListener { submitForm() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSelectedJadwal(jadwal: JadwalTersedia) {
        // Pastikan jadwal tersedia di objek formnya
        selectedJadwalTersedia = jadwal

        // Update UI dengan informasi jadwal
        editTextTanggalMulai.setText(jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextTanggalSelesai.setText(jadwal.tanggal?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextJamMulai.setText(jadwal.waktuMulai?.format(DateTimeFormatter.ofPattern("HH:mm")))
        editTextJamSelesai.setText(jadwal.waktuSelesai?.format(DateTimeFormatter.ofPattern("HH:mm")))

        // Update lapangan
        updateLapanganCheckboxes(viewModel.lapanganList.value ?: emptyList())

        // Log untuk konfirmasi data
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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setVisibilityBasedOnSelection(idFasilitas: Int?, selectedOption: String) {
        Log.d("FormPeminjamanFragment", "setVisibilityBasedOnSelection called - Fasilitas ID: $idFasilitas, Opsi: $selectedOption")
        when {
            idFasilitas == 30 && selectedOption == "Sesuai Jadwal Rutin" -> {
                // Logika untuk Fasilitas 30 dan Sesuai Jadwal Rutin
                editTextNamaOrganisasi.visibility = View.GONE
                spinnerNamaOrganisasi.visibility = View.VISIBLE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvLapangan.visibility = View.GONE
                containerJenisLapangan.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                containerPenggunaKhusus.visibility = View.VISIBLE

                // Set dan nonaktifkan radio button Internal UII
                val radioInternalUii = containerPenggunaKhusus.findViewById<RadioButton>(R.id.radio_internal_uii)
                radioInternalUii.isChecked = true
                containerPenggunaKhusus.children.forEach { (it as? RadioButton)?.isEnabled = false }

                viewModel.loadJadwalTersediaForFasilitas30()
                Log.d("FormPeminjamanFragment", "Container jadwal tersedia ditampilkan untuk Fasilitas 30 - Sesuai Jadwal Rutin")
            }
            idFasilitas == 30 && selectedOption == "Diluar Jadwal Rutin" -> {
                // Logika untuk Fasilitas 30 dan Diluar Jadwal Rutin
                editTextNamaOrganisasi.visibility = View.VISIBLE
                spinnerNamaOrganisasi.visibility = View.GONE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                Log.d("FormPeminjamanFragment", "Container jadwal tersedia ditampilkan untuk Fasilitas 30 - Diluar Jadwal Rutin")
                containerPenggunaKhusus.visibility = View.VISIBLE

                // Aktifkan semua radio button
                containerPenggunaKhusus.children.forEach { (it as? RadioButton)?.isEnabled = true }

                viewModel.loadJadwalTersediaForFasilitas30()
            }
            selectedOption == "Sesuai Jadwal Rutin" -> {
                // Logika untuk Sesuai Jadwal Rutin (selain Fasilitas 30)
                editTextNamaOrganisasi.visibility = View.GONE
                spinnerNamaOrganisasi.visibility = View.VISIBLE
                tvTanggal.visibility = View.GONE
                tvJam.visibility = View.GONE
                tvLapangan.visibility = View.GONE
                containerJenisLapangan.visibility = View.GONE
                tvJadwalTersedia.visibility = View.VISIBLE
                containerJadwalTersedia.visibility = View.VISIBLE
                containerPenggunaKhusus.visibility = View.GONE
            }
            selectedOption == "Diluar Jadwal Rutin" -> {
                // Logika untuk Diluar Jadwal Rutin (selain Fasilitas 30)
                editTextNamaOrganisasi.visibility = View.VISIBLE
                spinnerNamaOrganisasi.visibility = View.GONE
                tvTanggal.visibility = View.VISIBLE
                tvJam.visibility = View.VISIBLE
                tvJadwalTersedia.visibility = View.GONE
                containerJadwalTersedia.visibility = View.GONE
                containerPenggunaKhusus.visibility = View.GONE
                tvLapangan.visibility = View.VISIBLE
                containerJenisLapangan.visibility = View.VISIBLE
            }
            else -> {
                // Logika default atau untuk pilihan lain
                // Misalnya, sembunyikan semua atau tampilkan pesan error
            }
        }

        // Elemen yang selalu ditampilkan
        editTextNamaAcara.visibility = View.VISIBLE

        // Memastikan perubahan tampilan segera terjadi
        view?.post {
            containerJadwalTersedia.requestLayout()
            containerJadwalTersedia.invalidate()
            Log.d("FormPeminjamanFragment", "Container jadwal tersedia visibility: ${containerJadwalTersedia.visibility == View.VISIBLE}")
        }

        if (idFasilitas == 30 && (selectedOption == "Sesuai Jadwal Rutin" || selectedOption == "Diluar Jadwal Rutin")) {
            viewModel.loadJadwalTersediaForFasilitas30()
            Log.d("FormPeminjamanFragment", "Memanggil loadJadwalTersediaForFasilitas30()")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVisibilityBasedOnCurrentSelection() {
        val selectedFasilitas = spinnerFasilitas.selectedItem as? Fasilitas
        val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""

        Log.d("FormPeminjamanFragment", "Updating visibility - Fasilitas: ${selectedFasilitas?.namaFasilitas}, ID: ${selectedFasilitas?.idFasilitas}, Opsi: $selectedOption")

        setVisibilityBasedOnSelection(selectedFasilitas?.idFasilitas, selectedOption)
    }

    private val dateTimeChangeWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        @RequiresApi(Build.VERSION_CODES.O)
        override fun afterTextChanged(s: Editable?) {
            checkJadwalAvailability()
        }
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
        }

        viewModel.showPenggunaKhusus.observe(viewLifecycleOwner) { show ->
            containerPenggunaKhusus.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.organisasiList.observe(viewLifecycleOwner) { organisasiList ->
            Log.d("FormPeminjamanFragment", "Organisasi list updated: $organisasiList")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, organisasiList.map { it.namaOrganisasi })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNamaOrganisasi.adapter = adapter
        }

        viewModel.jadwalTersedia.observe(viewLifecycleOwner) { jadwalList ->
            Log.d("FormPeminjamanFragment", "Jadwal tersedia updated. Size: ${jadwalList.size}")
            jadwalTersediaAdapter.updateJadwalList(jadwalList)
        }

        viewModel.jadwalAvailability.observe(viewLifecycleOwner) { status ->
            // Hanya berlaku jika opsi "Diluar Jadwal Rutin" dipilih
            val selectedOption = spinnerOpsiPinjam.selectedItem?.toString() ?: ""

            if (selectedOption == "Diluar Jadwal Rutin") {
                when (status) {
                    JadwalAvailabilityStatus.AVAILABLE -> {
                        // Tidak ada peringatan, button Next tetap enabled
                        buttonNext.isEnabled = true
                    }
                    JadwalAvailabilityStatus.UNAVAILABLE -> {
                        Toast.makeText(context, "Jadwal tidak tersedia", Toast.LENGTH_SHORT).show()
                        buttonNext.isEnabled = false
                    }
                    JadwalAvailabilityStatus.HOLIDAY -> {
                        Toast.makeText(context, "Jadwal tidak tersedia (Hari Libur)", Toast.LENGTH_SHORT).show()
                        buttonNext.isEnabled = false
                    }
                    JadwalAvailabilityStatus.CONFLICT_WITH_JADWAL_RUTIN -> {
                        Toast.makeText(context, "Terdapat jadwal rutin, tolong hubungi dengan pemilik jadwal terlebih dahulu", Toast.LENGTH_LONG).show()
                        buttonNext.isEnabled = true
                    }
                }
            } else {
                // Untuk opsi selain "Diluar Jadwal Rutin", button Next selalu enabled
                buttonNext.isEnabled = true
            }
        }

        viewModel.jadwalTersediaFasilitas30.observe(viewLifecycleOwner) { jadwalList ->
            Log.d("FormPeminjamanFragment", "Jadwal tersedia for Fasilitas 30 updated. Size: ${jadwalList.size}")
            jadwalTersediaAdapter.updateJadwalList(jadwalList)
            containerJadwalTersedia.visibility = View.VISIBLE
            containerJadwalTersedia.invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            if (isStartDate) {
                editTextTanggalMulai.setText(formattedDate)
                viewModel.setTanggalMulai(selectedDate)
            } else {
                editTextTanggalSelesai.setText(formattedDate)
                viewModel.setTanggalSelesai(selectedDate)
            }
        }, year, month, day).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formattedTime = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            if (isStartTime) {
                editTextJamMulai.setText(formattedTime)
                viewModel.setJamMulai(selectedTime)
            } else {
                editTextJamSelesai.setText(formattedTime)
                viewModel.setJamSelesai(selectedTime)
            }
        }, hour, minute, true).show()
    }

    private fun updateLapanganCheckboxes(lapanganList: List<Lapangan>) {
        containerJenisLapangan.removeAllViews()
        selectedLapangan.clear()

        if (lapanganList.isNotEmpty()) {
            lapanganList.forEach { lapangan ->
                val checkbox = CheckBox(context)
                checkbox.text = lapangan.namaLapangan
                checkbox.isChecked = true
                selectedLapangan.add(lapangan) // Add to selected list by default

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitForm() {
        val namaAcara = editTextNamaAcara.text.toString()
        val namaOrganisasi = editTextNamaOrganisasi.text.toString()
        val penggunaKhusus = when (containerPenggunaKhusus.checkedRadioButtonId) {
            R.id.radio_internal_uii -> PenggunaKhusus.INTERNAL_UII
            R.id.radio_internal_vs_eksternal -> PenggunaKhusus.INTERNAL_VS_EKSTERNAL
            R.id.radio_eksternal_uii -> PenggunaKhusus.EKSTERNAL_UII
            else -> null
        }

        viewModel.submitForm(namaAcara, namaOrganisasi, penggunaKhusus)
    }

}