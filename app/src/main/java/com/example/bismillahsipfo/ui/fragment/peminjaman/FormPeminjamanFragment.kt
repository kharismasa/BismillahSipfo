package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.example.bismillahsipfo.data.repository.UserRepository
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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupJadwalTersediaRecyclerView() {
        Log.d("FormPeminjamanFragment", "Setting up JadwalTersediaRecyclerView")
        jadwalTersediaAdapter = JadwalTersediaAdapter(emptyList()) { jadwal ->
            Log.d("FormPeminjamanFragment", "Jadwal selected: $jadwal")
            viewModel.onJadwalTersediaSelected(jadwal)
            updateSelectedJadwal(jadwal)
        }

        // Menggunakan GridLayoutManager dengan 1 kolom untuk tampilan vertikal penuh
        val layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
        containerJadwalTersedia.layoutManager = layoutManager
        containerJadwalTersedia.adapter = jadwalTersediaAdapter

        // Menambahkan ItemDecoration untuk memberikan jarak antar item
        val itemDecoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                outRect.bottom = resources.getDimensionPixelSize(R.dimen.item_jadwal_margin)
                if (position == 0) {
                    outRect.top = resources.getDimensionPixelSize(R.dimen.item_jadwal_margin)
                }
            }
        }
        containerJadwalTersedia.addItemDecoration(itemDecoration)

        Log.d("FormPeminjamanFragment", "JadwalTersediaRecyclerView setup complete")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSelectedJadwal(jadwal: JadwalTersedia) {
        editTextTanggalMulai.setText(jadwal.tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextTanggalSelesai.setText(jadwal.tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        editTextJamMulai.setText(jadwal.waktuMulai.format(DateTimeFormatter.ofPattern("HH:mm")))
        editTextJamSelesai.setText(jadwal.waktuSelesai.format(DateTimeFormatter.ofPattern("HH:mm")))
        updateLapanganCheckboxes(viewModel.lapanganList.value ?: emptyList())
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
    private fun setupListeners() {
        spinnerFasilitas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFasilitas = parent.getItemAtPosition(position) as Fasilitas
                Log.d("FormPeminjamanFragment", "Fasilitas selected: ${selectedFasilitas.namaFasilitas}")
                containerJenisLapangan.removeAllViews()  // Hapus tampilan lapangan lama
                containerPenggunaKhusus.visibility = View.GONE  // Sembunyikan pengguna khusus
                viewModel.onFasilitasSelected(selectedFasilitas)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerNamaOrganisasi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOrganisasi = viewModel.organisasiList.value?.get(position)
                if (selectedOrganisasi != null) {
                    viewModel.onOrganisasiSelected(selectedOrganisasi)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editTextTanggalMulai.setOnClickListener { showDatePicker(true) }
        editTextTanggalSelesai.setOnClickListener { showDatePicker(false) }
        editTextJamMulai.setOnClickListener { showTimePicker(true) }
        editTextJamSelesai.setOnClickListener { showTimePicker(false) }

        // Listener untuk tombol Next
        buttonNext.setOnClickListener {
            val activity = requireActivity() as PeminjamanActivity
            val nextItem = (activity.viewPager.currentItem + 1) % activity.viewPager.adapter?.itemCount!!
            activity.viewPager.setCurrentItem(nextItem, true)  // Navigasi ke fragment berikutnya
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
                when (selectedOption) {
                    "Sesuai Jadwal Rutin" -> {
                        setSesuaiJadwalRutinVisibility()
//                        loadOrganisasiList()
                    }
                    "Diluar Jadwal Rutin" -> {
                        setDiluarJadwalRutinVisibility()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        buttonNext.setOnClickListener { submitForm() }
    }

    private fun setSesuaiJadwalRutinVisibility() {
        editTextNamaOrganisasi.visibility = View.GONE
        spinnerNamaOrganisasi.visibility = View.VISIBLE
        tvTanggal.visibility = View.GONE
        tvJam.visibility = View.GONE
        tvLapangan.visibility = View.GONE
        containerJenisLapangan.visibility = View.GONE
        tvJadwalTersedia.visibility = View.VISIBLE
        containerJadwalTersedia.visibility = View.VISIBLE

        // Menampilkan containerPenggunaKhusus
        containerPenggunaKhusus.visibility = View.VISIBLE

        // Mengatur radio button "Internal UII" menjadi terpilih
        val radioInternalUii = containerPenggunaKhusus.findViewById<RadioButton>(R.id.radio_internal_uii)
        radioInternalUii.isChecked = true

        // Menonaktifkan interaksi dengan radio button
        for (i in 0 until containerPenggunaKhusus.childCount) {
            val child = containerPenggunaKhusus.getChildAt(i)
            if (child is RadioButton) {
                child.isEnabled = false
            }
        }
    }

    private fun setDiluarJadwalRutinVisibility() {
        editTextNamaOrganisasi.visibility = View.VISIBLE
        spinnerNamaOrganisasi.visibility = View.GONE
        tvTanggal.visibility = View.VISIBLE
        tvJam.visibility = View.VISIBLE
        tvLapangan.visibility = View.VISIBLE
        containerJenisLapangan.visibility = View.VISIBLE
        tvJadwalTersedia.visibility = View.GONE
        containerJadwalTersedia.visibility = View.GONE

        // Menampilkan containerPenggunaKhusus
        containerPenggunaKhusus.visibility = View.VISIBLE

        // Mengaktifkan kembali interaksi dengan radio button
        for (i in 0 until containerPenggunaKhusus.childCount) {
            val child = containerPenggunaKhusus.getChildAt(i)
            if (child is RadioButton) {
                child.isEnabled = true
            }
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
        if (lapanganList.isNotEmpty()) {
            lapanganList.forEach { lapangan ->
                val checkbox = CheckBox(context)
                checkbox.text = lapangan.namaLapangan
                checkbox.isChecked = true // Set default to checked
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.onLapanganChecked(lapangan, isChecked)
                }
                containerJenisLapangan.addView(checkbox)
            }
        } else {
            // Tambahkan TextView untuk menampilkan pesan ketika tidak ada lapangan
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